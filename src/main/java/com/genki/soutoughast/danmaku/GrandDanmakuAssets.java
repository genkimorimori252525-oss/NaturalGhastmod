package com.genki.soutoughast.danmaku;

import com.genki.soutoughast.danmaku.core.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable built-in authored resources; visual sidecars never enter the Score parser. */
public final class GrandDanmakuAssets {
    public static final int MAX_SESSIONS_PER_LEVEL=2,MAX_WATCHERS=64,MAX_CANDIDATES=128,MAX_PACKET_BYTES=262144;
    public enum Material {
        WHITE("white",.65f,.65f,.7f,false),ORANGE("orange",1f,.35f,.07f,true),DARK_RED("dark_red",.5f,.035f,.085f,false),RED("red",1f,.06f,.07f,true);
        public final String texture;public final float r,g,b;public final boolean additive;
        Material(String texture,float r,float g,float b,boolean additive){this.texture=texture;this.r=r;this.g=g;this.b=b;this.additive=additive;}
        public String resource(){return "assets/soutou_ghast/textures/danmaku/"+texture+".png";}
    }
    public record Pattern(int index,String id,String hash,GrandDanmakuScore.Config score,List<Material> materials,List<Integer> trailTicks,List<Double> glowScales) {
        public Pattern {materials=List.copyOf(materials);trailTicks=List.copyOf(trailTicks);glowScales=List.copyOf(glowScales);}
        public int preludeTicks(){return score.tracks().stream().mapToInt(GrandDanmakuScore.Track::startTick).min().orElseThrow();}
    }
    private static final String[] IDS={"kouengeki","kokuseki","seisou"};
    private static final String[] FILES={"kouengeki_grand_danmaku_v1.json","kokuseki_kairo_grand_danmaku_v1.json","seisou_tenmaku_grand_danmaku_v2.json"};
    private static final String[] HASHES={"4b1d8ac5e7ab0c9934bd5b1115724914df74ffda937df25a3ef280521130cfe7","e1dfbe8d2a5535635c5312ea2138ca1b6ac83458c8ecd50f93087bb3a82996be","e3a180ff4b2073a7b4718945bab8acbf0a4fba93107d8891f5aead1e81da8847"};
    private static final int[] TOTALS={813,690,3065},PEAKS={226,193,1020};
    private static List<Pattern> loaded;private static String failure="NOT_LOADED";
    private GrandDanmakuAssets(){}
    public static synchronized boolean ready(){
        if(loaded==null)try{var list=new ArrayList<Pattern>();for(int i=0;i<3;i++)list.add(load(i));loaded=List.copyOf(list);failure="NONE";}
        catch(IOException|RuntimeException error){loaded=List.of();failure=error.getClass().getSimpleName();LogUtils.getLogger().error("Grand Danmaku resources rejected; ordinary combat remains available",error);}
        return loaded.size()==3;
    }
    public static Pattern get(int index){if(!ready()||index<0||index>=3)throw new IllegalStateException("DANMAKU_CATALOG_UNAVAILABLE:"+failure);return loaded.get(index);}
    public static String failure(){ready();return failure;}
    public static byte[] resource(String name,int limit)throws IOException{
        try(var in=GrandDanmakuAssets.class.getClassLoader().getResourceAsStream(name)){if(in==null)throw new IOException("DANMAKU_MISSING_RESOURCE:"+name);byte[] b=in.readNBytes(limit+1);if(b.length>limit)throw new IOException("DANMAKU_RESOURCE_BUDGET");return b;}
    }
    public static String hash(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static Pattern load(int index)throws IOException{
        byte[] bytes=resource("data/soutou_ghast/danmaku/"+FILES[index],262144);if(!hash(bytes).equals(HASHES[index]))throw new IOException("DANMAKU_SCORE_HASH");
        var score=GrandDanmakuScoreJson.read(new String(bytes,StandardCharsets.UTF_8));
        var visual=JsonParser.parseString(new String(resource("data/soutou_ghast/danmaku_visual/"+IDS[index]+"_visual_and_gameplay.json",32768),StandardCharsets.UTF_8)).getAsJsonObject();
        if(visual.get("visualSpecVersion").getAsInt()!=1||!visual.get("patternId").getAsString().equals(IDS[index])||!visual.get("scoreSha256").getAsString().equals(HASHES[index]))throw new IOException("DANMAKU_VISUAL_IDENTITY");
        JsonObject mapping=visual.getAsJsonObject("trackMaterials"),palette=visual.getAsJsonObject("palette"),render=visual.getAsJsonObject("renderOnly");
        var expectedNames=new HashSet<String>();for(var t:score.tracks())expectedNames.add(t.name());if(expectedNames.size()!=score.tracks().size()||!mapping.keySet().equals(expectedNames))throw new IOException("DANMAKU_MATERIAL_COVERAGE");
        if(render.get("particleTrailsHaveHitboxes").getAsBoolean()||render.get("cosmeticParticlesAreDamaging").getAsBoolean()||render.get("bloomHasHitbox").getAsBoolean())throw new IOException("DANMAKU_COSMETIC_AUTHORITY");
        var damage=visual.getAsJsonObject("damageCandidateNotBalanceApproved");
        if(damage.get("bulletDamage").getAsDouble()!=1||damage.get("contactCooldownTicksPerTarget").getAsInt()!=10||damage.get("hitRadiusMultiplierRelativeToVisual").getAsDouble()!=.48||damage.get("blockDestruction").getAsBoolean()||damage.get("terrainExplosion").getAsBoolean())throw new IOException("DANMAKU_UNREVIEWED_GAMEPLAY_METADATA");
        var materials=new ArrayList<Material>();var tails=new ArrayList<Integer>();var glows=new ArrayList<Double>();
        for(var track:score.tracks()){
            String name=mapping.get(track.name()).getAsString();Material m=switch(name){case "ember"->Material.ORANGE;case "blood"->Material.RED;case "dark"->Material.DARK_RED;case "shadow"->Material.WHITE;default->throw new IOException("DANMAKU_MATERIAL_UNKNOWN");};
            var definition=palette.getAsJsonObject(name);double seconds=Math.min(definition.get("trailSeconds").getAsDouble(),render.get("trailMaxSeconds").getAsDouble()),glow=definition.get("glowScale").getAsDouble();
            if(!Double.isFinite(seconds)||seconds<0||seconds>.7||!Double.isFinite(glow)||glow<1||glow>3)throw new IOException("DANMAKU_VISUAL_BUDGET");
            materials.add(m);tails.add((int)Math.ceil(seconds*20));glows.add(glow);
        }
        int total=0,peak=0;for(int tick=0;tick<=score.durationTicks();tick++){int births=GrandDanmakuScore.emitAt(score,tick).size();if(births>96)throw new IOException("DANMAKU_BIRTH_BUDGET");total+=births;int live=0;for(var t:score.tracks())live+=GrandDanmakuScore.liveCount(t,tick);peak=Math.max(peak,live);}
        if(total!=TOTALS[index]||peak!=PEAKS[index])throw new IOException("DANMAKU_CANONICAL_COUNTS");
        return new Pattern(index,IDS[index],HASHES[index],score,materials,tails,glows);
    }
}

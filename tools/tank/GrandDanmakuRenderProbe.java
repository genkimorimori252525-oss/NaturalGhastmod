package com.genki.soutoughast.client.danmaku;
import com.genki.soutoughast.danmaku.core.*;

import com.genki.soutoughast.danmaku.*;
import com.genki.soutoughast.entity.SoutouGhast;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Private client-only canonical replay; actual GPU, synthetic visual data, no gameplay/network authority. */
@Mod("naturalghast_render_probe")
public final class GrandDanmakuRenderProbe {
    private record Binding(Path output,UUID subject,UUID player,String nonce,int maxImages) {}
    private static volatile Binding binding;
    private static volatile boolean done;
    private static final AtomicInteger images=new AtomicInteger();
    private JsonObject request,codec;
    private long startTick,deadline;private int ticks;
    public GrandDanmakuRenderProbe(){MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||done)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        try{step(server.overworld());}catch(Exception failure){fail(failure);}
    }
    private void initialize(ServerLevel level)throws IOException{
        try(var input=getClass().getResourceAsStream("/grand-native-probe-request.json")){
            if(input==null)throw new IOException("GRAND_PROBE_REQUEST_MISSING");byte[] data=input.readNBytes(4097);if(data.length>4096)throw new IOException("GRAND_PROBE_REQUEST_SIZE");request=JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();
        }
        if(!request.keySet().equals(Set.of("schema","mode","nonce","subjectUuid","playerUuid","baselineProductSha256","maxTicks","maxWallMs","maxSamples","maxImages","imageAuthority"))
                ||!request.get("schema").getAsString().equals("naturalghast.client-render-replay/v1")||!request.get("mode").getAsString().equals("CLIENT_RENDER_RELIABILITY_NO_NETWORK")
                ||request.get("maxTicks").getAsInt()!=2000||request.get("maxWallMs").getAsInt()!=100000||request.get("maxSamples").getAsInt()!=1000||request.get("maxImages").getAsInt()!=4
                ||!request.get("imageAuthority").getAsString().equals("FINITE_PRIVATE_CONSUMER_VISUAL_SUPPLEMENT_NOT_CANONICAL_TANK_CAPTURE"))throw new IOException("GRAND_PROBE_REQUEST_SCOPE");
        var runtime=JsonParser.parseString(Files.readString(Path.of(System.getenv("KNEEKURA_TANK_PROFILE")))).getAsJsonObject();
        var manual=JsonParser.parseString(Files.readString(Path.of(System.getenv("KNEEKURA_MANUAL_REQUEST")))).getAsJsonObject();
        Path world=level.getServer().getWorldPath(LevelResource.ROOT).toRealPath(),run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();
        if(!world.equals(Path.of(runtime.get("worldDir").getAsString()).toRealPath())||!world.equals(Path.of(manual.get("worldDir").getAsString()).toRealPath())
                ||!manual.get("subjectUuid").getAsString().equals(request.get("subjectUuid").getAsString())||!manual.get("playerUuid").getAsString().equals(request.get("playerUuid").getAsString())
                ||manual.get("maxTicks").getAsInt()!=2400||manual.get("maxWallMs").getAsInt()!=120000||manual.get("captures").getAsInt()!=0
                ||Files.exists(run.resolve("control/owner-envelope.json"))||Files.exists(run.resolve("control/owner-status.json")))throw new IOException("GRAND_PROBE_WORLD_ACTORS_OR_OWNER");
        try(var input=getClass().getResourceAsStream("/grand-probe-base-product.json")){
            if(input==null)throw new IOException("GRAND_PROBE_PROVENANCE_MISSING");var base=JsonParser.parseString(new String(input.readNBytes(4096),StandardCharsets.UTF_8)).getAsJsonObject();if(!base.get("sha256").getAsString().equals(request.get("baselineProductSha256").getAsString()))throw new IOException("GRAND_PROBE_PRODUCT_BINDING");
        }
        Path output=run.resolve("evidence/derived/grand-render-replay");Files.createDirectories(output);
        binding=new Binding(output,UUID.fromString(request.get("subjectUuid").getAsString()),UUID.fromString(request.get("playerUuid").getAsString()),request.get("nonce").getAsString(),4);
        startTick=level.getGameTime();deadline=System.currentTimeMillis()+100000;
    }

    private static void fail(Exception failure){
        done=true;try{if(binding!=null){var r=new JsonObject();r.addProperty("status","FAIL");r.addProperty("reason",failure.getClass().getSimpleName()+":"+failure.getMessage());Files.writeString(binding.output().resolve("result.json"),r+"\n",StandardOpenOption.CREATE_NEW);}}catch(IOException ignored){}
    }
    private void step(ServerLevel level)throws IOException{
        if(binding==null)initialize(level);
        if(++ticks>2000||System.currentTimeMillis()>=deadline)throw new IOException("RENDER_REPLAY_FINITE_LIMIT");
        var p=level.getServer().getPlayerList().getPlayer(binding.player());
        if(p==null&&level.getGameTime()-startTick<200)return;
        if(p==null||p instanceof FakePlayer||p.connection==null||!p.connection.connection.isConnected())throw new IOException("REPLAY_GENUINE_CONNECTION_LOST");
    }
    @Mod.EventBusSubscriber(modid="naturalghast_render_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Replay {
        private static int patternIndex,sequence,renderPasses;private static long started;
        private static WorldDanmakuSession swarm;private static DanmakuPlayback playback;private static DanmakuFrame frame;
        private static final JsonArray results=new JsonArray();private static boolean captured,cleanupReady;
        private static int peakCores,peakTrails,peakGhosts;private static long maxFrameNanos;private static int drainTicks;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END||binding==null||done)return;
            var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.screen!=null||!GrandDanmakuRenderer.ready())return;
            try{
                long tick=mc.level.getGameTime();
                if(patternIndex>=3){
                    if(!cleanupReady){
                        var p=GrandDanmakuAssets.get(0);var copy=new DanmakuPlayback();var id=UUID.randomUUID();
                        var live=new WorldDanmakuSession(p.score());for(int i=0;i<=153;i++)live.step(frame,(b,a,z)->false);
                        if(live.liveCount()==0)throw new IOException("HARD_CLEAR_EMPTY_SETUP");
                        var sync=new DanmakuPlayback.Sync(id,binding.subject(),0,mc.level.dimension().location().toString(),0,p.hash(),0,153,tick,live.bullets());
                        if(!copy.sync(sync,p.score(),p.hash(),sync.dimension(),tick))throw new IOException("HARD_CLEAR_SYNC");
                        copy.finish(true,tick);if(copy.valid()||copy.liveCount()!=0||!copy.ghosts().isEmpty())throw new IOException("HARD_CLEAR_RETAINED");
                        playback=copy;cleanupReady=true;
                    }return;
                }
                var pattern=GrandDanmakuAssets.get(patternIndex);
                if(swarm==null){
                    var camera=mc.gameRenderer.getMainCamera();var pos=camera.getPosition();var forward=new org.joml.Vector3f(camera.getLookVector());
                    var eye=new DanmakuFrame.Vec(pos.x,pos.y,pos.z);var mouth=eye.add(new DanmakuFrame.Vec(forward.x,forward.y,forward.z).scale(22));frame=DanmakuFrame.toward(mouth,eye,null);
                    swarm=new WorldDanmakuSession(pattern.score());playback=new DanmakuPlayback();var sync=new DanmakuPlayback.Sync(UUID.randomUUID(),binding.subject(),0,mc.level.dimension().location().toString(),patternIndex,pattern.hash(),0,0,tick,List.of());
                    if(!playback.sync(sync,pattern.score(),pattern.hash(),sync.dimension(),tick))throw new IOException("REPLAY_SYNC");
                    started=tick;sequence=peakCores=peakTrails=peakGhosts=drainTicks=0;maxFrameNanos=0;captured=false;
                }
                if(swarm.finished()){
                    if(drainTicks==0)playback.finish(false,tick);playback.prune(playback.renderAge(tick));
                    if(++drainTicks<16)return;
                    if(!captured||!playback.ghosts().isEmpty()||peakCores==0||peakTrails==0||peakGhosts==0)throw new IOException("REPLAY_DRAW_OR_DRAIN_MISSING");
                    int[] births={813,690,3065},peaks={226,193,1020};if(swarm.totalEmitted()!=births[patternIndex]||swarm.peakLive()!=peaks[patternIndex])throw new IOException("CANONICAL_TOTALS");
                    var r=new JsonObject();r.addProperty("pattern",patternIndex);r.addProperty("scoreHash",pattern.hash());r.addProperty("births",swarm.totalEmitted());r.addProperty("peakLive",swarm.peakLive());r.addProperty("lastAge",swarm.age());r.addProperty("actualRenderPasses",renderPasses);r.addProperty("peakCores",peakCores);r.addProperty("peakTrails",peakTrails);r.addProperty("peakGhosts",peakGhosts);r.addProperty("maxFrameNanos",maxFrameNanos);r.addProperty("normalDrainEmpty",true);results.add(r);patternIndex++;swarm=null;playback=null;return;
                }
                for(int speed=0;speed<3&&!swarm.finished();speed++){
                    var before=new HashMap<Long,WorldDanmakuSession.Bullet>();for(var b:swarm.bullets())before.put(b.id(),b);
                    var batch=swarm.step(frame,(b,a,z)->false);var removed=batch.removed().stream().map(id->{var b=before.get(id);return new DanmakuPlayback.Removal(id,b.at(batch.age()-b.bornTick()));}).toList();
                    if(!playback.delta(new DanmakuPlayback.Delta(playback.identity().session(),++sequence,batch.age(),tick,frame,batch.bursts(),removed),tick))throw new IOException("REPLAY_DELTA");
                    if(playback.liveCount()!=swarm.liveCount())throw new IOException("REPLAY_CORE_COUNT");
                }
            }catch(Exception failure){fail(failure);}
        }
        @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){
            if(binding==null||done||playback==null||event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
            GrandDanmakuRenderer.renderPlaybacks(event,List.of(playback));var d=GrandDanmakuRenderer.diagnostics();renderPasses++;
            peakCores=Math.max(peakCores,d.cores());peakTrails=Math.max(peakTrails,d.trails());peakGhosts=Math.max(peakGhosts,d.ghosts());maxFrameNanos=Math.max(maxFrameNanos,d.frameNanos());
        }
        @SubscribeEvent public static void capture(net.minecraftforge.client.event.RenderGuiEvent.Post event){
            if(binding==null||done||playback==null)return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.screen!=null)return;
            boolean cleanup=patternIndex>=3&&cleanupReady;var d=GrandDanmakuRenderer.diagnostics();
            if(!cleanup&&(captured||swarm==null||swarm.age()<110||d.cores()<=0||d.trails()<=0))return;
            try{
                if(cleanup&&(d.cores()!=0||d.trails()!=0||d.ghosts()!=0))throw new IOException("REPLAY_CLEAR_DRAW_RETAINED");
                int index=images.get();if(index>=4)throw new IOException("REPLAY_IMAGE_CAP");Path file=binding.output().resolve("native-replay-"+index+".png");
                try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(file);}
                var info=new JsonObject();info.addProperty("index",index);info.addProperty("pattern",patternIndex);info.addProperty("age",swarm==null?-1:swarm.age());info.addProperty("file",file.getFileName().toString());info.addProperty("sha256",GrandDanmakuAssets.hash(Files.readAllBytes(file)));info.addProperty("cores",d.cores());info.addProperty("trails",d.trails());info.addProperty("ghosts",d.ghosts());info.addProperty("width",mc.getMainRenderTarget().width);info.addProperty("height",mc.getMainRenderTarget().height);info.addProperty("actualRenderPasses",renderPasses);info.addProperty("source","ACTUAL_MINECRAFT_FRAMEBUFFER_SYNTHETIC_CLIENT_ONLY_REPLAY");info.addProperty("cameraChanged",false);info.addProperty("networkSent",false);info.addProperty("serverDamageOrAIChanged",false);
                Files.writeString(binding.output().resolve("native-replay-"+index+".json"),info+"\n",StandardOpenOption.CREATE_NEW);images.incrementAndGet();captured=true;
                if(cleanup){var result=new JsonObject();result.addProperty("status","PASS_SYNTHETIC_PLAYBACK_ACTUAL_GPU");result.addProperty("scope","NO_NATURAL_AI_NETWORK_HIT_BALANCE_OR_HUMAN_ACCEPTANCE");result.addProperty("nonce",binding.nonce());result.addProperty("savedImages",images.get());result.addProperty("canonicalTankCaptures",0);result.addProperty("replaySpeed",3);result.addProperty("hardClearEmpty",true);result.add("patterns",results);Files.writeString(binding.output().resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);done=true;playback.clear();}
            }catch(Exception failure){fail(failure);}
        }
    }
}

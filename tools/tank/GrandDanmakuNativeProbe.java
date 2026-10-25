package com.genki.soutoughast.tank;

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

/** Private finite natural observation/codec/native-pixel supplement. No synthetic gameplay inputs or selection. */
@Mod("naturalghast_grand_probe")
public final class GrandDanmakuNativeProbe {
    private record Binding(Path output,UUID subject,UUID player,String nonce,int maxImages) {}
    private static volatile Binding binding;
    private static volatile boolean done;
    private static final AtomicInteger images=new AtomicInteger();
    private JsonObject request,codec;
    private long startTick,deadline;private int ticks,rows,firstPattern=-1;private boolean sawActive,sawFiring,sawAck;
    public GrandDanmakuNativeProbe(){MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||done)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        try{step(server.overworld());}catch(Exception failure){try{finish("FAIL",failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage()).substring(0,Math.min(100,String.valueOf(failure.getMessage()).length())));}catch(IOException ignored){done=true;}}
    }
    private void initialize(ServerLevel level)throws IOException{
        try(var input=getClass().getResourceAsStream("/grand-native-probe-request.json")){
            if(input==null)throw new IOException("GRAND_PROBE_REQUEST_MISSING");byte[] data=input.readNBytes(4097);if(data.length>4096)throw new IOException("GRAND_PROBE_REQUEST_SIZE");request=JsonParser.parseString(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();
        }
        if(!request.keySet().equals(Set.of("schema","mode","nonce","subjectUuid","playerUuid","baselineProductSha256","maxTicks","maxWallMs","maxSamples","maxImages","imageAuthority"))
                ||!request.get("schema").getAsString().equals("naturalghast.grand-native-probe/v1")||!request.get("mode").getAsString().equals("NATURAL_OBSERVE_NO_FORCE")
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
        Path output=run.resolve("evidence/derived/grand-native-observation");Files.createDirectories(output);
        binding=new Binding(output,UUID.fromString(request.get("subjectUuid").getAsString()),UUID.fromString(request.get("playerUuid").getAsString()),request.get("nonce").getAsString(),4);
        startTick=level.getGameTime();deadline=System.currentTimeMillis()+100000;
    }
    private void step(ServerLevel level)throws IOException{
        if(binding==null)initialize(level);
        if(++ticks>2000||System.currentTimeMillis()>=deadline){finish(sawFiring&&images.get()>0?"PARTIAL":"NOT_OBSERVED","FINITE_WINDOW_COMPLETE");return;}
        var p=level.getServer().getPlayerList().getPlayer(binding.player());var entity=level.getEntity(binding.subject());
        if(p==null&&level.getGameTime()-startTick<200)return;
        if(p==null||p instanceof FakePlayer||p.connection==null||!p.connection.connection.isConnected()||p.isCreative()||p.isSpectator()
                ||!(entity instanceof SoutouGhast boss)||boss.isNoAi()||p.level()!=level)throw new IOException("GRAND_PROBE_GENUINE_ACTORS");
        if(codec==null){codec=com.genki.soutoughast.danmaku.GrandDanmakuCodecProbe.run();Files.writeString(binding.output().resolve("codec.json"),codec+"\n",StandardOpenOption.CREATE_NEW);}
        var grand=boss.getGrandDanmaku();boolean active=grand.active();if(active){sawActive=true;if(firstPattern<0)firstPattern=grand.patternIndex();if(grand.liveCount()>0)sawFiring=true;if(grand.targetAcknowledged())sawAck=true;}
        if(ticks%2==0&&rows<1000){
            var row=new JsonObject();row.addProperty("sample",rows++);row.addProperty("tick",level.getGameTime());row.addProperty("active",active);row.addProperty("pattern",grand.patternIndex());row.addProperty("age",grand.age());row.addProperty("live",grand.liveCount());row.addProperty("emitted",grand.totalEmitted());row.addProperty("peak",grand.peakLive());row.addProperty("contacts",grand.contacts());row.addProperty("damageAttempts",grand.damageAttempts());row.addProperty("targetAcknowledged",grand.targetAcknowledged());row.addProperty("watchers",grand.watcherCount());row.addProperty("reason",grand.lastReason());row.addProperty("playerHealth",p.getHealth());row.addProperty("subjectUuid",boss.getUUID().toString());row.addProperty("playerUuid",p.getUUID().toString());row.addProperty("noAI",boss.isNoAi());row.addProperty("width",boss.getBbWidth());row.addProperty("height",boss.getBbHeight());
            var control=(com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl)boss.getMoveControl();
            row.addProperty("mobility",control.getMobilityContext().name());row.addProperty("majorActive",boss.getMajorDirector().active());row.addProperty("majorQuiet",boss.getMajorDirector().quietTicks());row.addProperty("majorRecent",boss.getMajorDirector().recentTicks());row.addProperty("majorDecision",boss.getMajorDirector().decision());row.addProperty("ordinaryFace",boss.getStandardAttack().engaged());row.addProperty("tacticalAction",control.getTacticalState().action().name());row.addProperty("range",boss.distanceTo(p));row.addProperty("targetMatches",boss.getTarget()==p);row.addProperty("bossX",boss.getX());row.addProperty("bossY",boss.getY());row.addProperty("bossZ",boss.getZ());row.addProperty("playerX",p.getX());row.addProperty("playerY",p.getY());row.addProperty("playerZ",p.getZ());
            Files.writeString(binding.output().resolve("rows.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }
        if(sawActive&&!active){boolean normal="NORMAL_DRAIN_COMPLETE".equals(grand.lastReason());finish(normal&&sawFiring&&sawAck&&images.get()==4?"PASS":"PARTIAL","SESSION_ENDED:"+grand.lastReason());}
        else if(!p.isAlive())finish("PARTIAL","PLAYER_DIED");
    }
    private void finish(String status,String reason)throws IOException{
        done=true;if(binding==null)return;var result=new JsonObject();result.addProperty("status",status);result.addProperty("reason",reason);result.addProperty("scope","NATURAL_SELECTION_PASSIVE_NATIVE_OBSERVATION_NOT_HUMAN_OR_BALANCE_ACCEPTANCE");result.addProperty("nonce",binding.nonce());result.addProperty("rows",rows);result.addProperty("ticks",ticks);result.addProperty("sawActive",sawActive);result.addProperty("sawFiring",sawFiring);result.addProperty("targetAcknowledged",sawAck);result.addProperty("firstPattern",firstPattern);result.addProperty("savedImages",images.get());result.addProperty("maxImages",4);result.addProperty("canonicalTankCaptures",0);result.addProperty("runtimeInputOrSelectionMutation",false);if(codec!=null)result.add("codec",codec);Files.writeString(binding.output().resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
    }
    @Mod.EventBusSubscriber(modid="naturalghast_grand_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class NativeFrames {
        private static UUID observedSession;private static int index;private static boolean readinessWritten;
        @SubscribeEvent public static void frame(net.minecraftforge.client.event.RenderGuiEvent.Post event){
            Binding current=binding;if(current==null||done||index>=current.maxImages())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.player==null||!mc.player.getUUID().equals(current.player())||mc.screen!=null)return;
            if(!readinessWritten){
                readinessWritten=true;try{var d=com.genki.soutoughast.client.danmaku.GrandDanmakuRenderer.diagnostics();var info=new JsonObject();info.addProperty("shaderAndTexturesReady",d.ready());info.addProperty("actualRendererFrames",d.frames());info.addProperty("tick",mc.level.getGameTime());info.addProperty("scope","ACTUAL_CLIENT_RESOURCE_READY_NOT_PIXEL_OR_GAMEPLAY_ACCEPTANCE");Files.writeString(current.output().resolve("client-render-readiness.json"),info+"\n",StandardOpenOption.CREATE_NEW);}catch(IOException ignored){}
            }
            for(var playback:com.genki.soutoughast.client.danmaku.GrandDanmakuClient.sessions()){
                var id=playback.identity();if(id==null||!id.owner().equals(current.subject())||!playback.valid()||playback.ended())continue;
                if(observedSession==null)observedSession=id.session();if(!observedSession.equals(id.session()))continue;
                var pattern=GrandDanmakuAssets.get(id.pattern());int[] ages={pattern.preludeTicks()+15,153,300,pattern.score().durationTicks()-8};double age=playback.renderAge(mc.level.getGameTime());
                var diagnostics=com.genki.soutoughast.client.danmaku.GrandDanmakuRenderer.diagnostics();if(age<ages[index]||!diagnostics.ready()||diagnostics.cores()<=0)continue;
                try{
                    Path file=current.output().resolve("native-grand-"+index+"-age"+(int)age+".png");if(Files.exists(file))throw new IOException("NATIVE_FRAME_ALREADY_EXISTS");
                    try(var pixels=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(file);}
                    var info=new JsonObject();info.addProperty("index",index);info.addProperty("source","ACTUAL_MINECRAFT_FRAMEBUFFER_RENDER_GUI_POST");info.addProperty("session",id.session().toString());info.addProperty("owner",id.owner().toString());info.addProperty("pattern",id.pattern());info.addProperty("age",age);info.addProperty("tick",mc.level.getGameTime());info.addProperty("file",file.getFileName().toString());info.addProperty("sha256",GrandDanmakuAssets.hash(Files.readAllBytes(file)));info.addProperty("width",mc.getMainRenderTarget().width);info.addProperty("height",mc.getMainRenderTarget().height);info.addProperty("cores",diagnostics.cores());info.addProperty("trails",diagnostics.trails());info.addProperty("ghostTrails",diagnostics.ghosts());info.addProperty("frameRenderNanos",diagnostics.frameNanos());info.addProperty("cameraX",mc.gameRenderer.getMainCamera().getPosition().x);info.addProperty("cameraY",mc.gameRenderer.getMainCamera().getPosition().y);info.addProperty("cameraZ",mc.gameRenderer.getMainCamera().getPosition().z);info.addProperty("cameraYaw",mc.gameRenderer.getMainCamera().getYRot());info.addProperty("cameraPitch",mc.gameRenderer.getMainCamera().getXRot());info.addProperty("cameraChangedByProbe",false);info.addProperty("authority","FINITE_PRIVATE_CONSUMER_VISUAL_SUPPLEMENT_NOT_CANONICAL_TANK_CAPTURE");
                    Files.writeString(current.output().resolve("native-frame-"+index+".json"),info+"\n",StandardOpenOption.CREATE_NEW);index++;images.incrementAndGet();
                }catch(Exception failure){index=current.maxImages();try{var problem=new JsonObject();problem.addProperty("status","FAIL");problem.addProperty("reason",failure.getClass().getSimpleName());Files.writeString(current.output().resolve("native-frame-failure.json"),problem+"\n",StandardOpenOption.CREATE_NEW);}catch(IOException ignored){}}
                return;
            }
        }
    }
}

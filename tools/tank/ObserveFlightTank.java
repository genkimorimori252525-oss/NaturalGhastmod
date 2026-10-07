package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
import com.genki.soutoughast.entity.projectile.StandardSoutouFireball;
import com.genki.soutoughast.entity.projectile.CommittedSoutouFireball;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.UUID;

/** Opt-in finite read-only server supplement; never included in production artifact. */
@Mod("naturalghast_tank_observer")
public final class ObserveFlightTank {
 private static int samples;
 private static final java.util.Set<UUID> serverSeen=new java.util.HashSet<>();
 private static boolean standard(){return "1".equals(System.getenv("KNEEKURA_DEBUG_NATURAL_STANDARD"));}
 private static int sampleLimit(){return standard()?180:900;}
 private static volatile boolean ready;
 private static Path output(){return Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("evidence/derived/naturalghast-flight");}
 private static boolean active(){return "1".equals(System.getenv("KNEEKURA_DEBUG_ENABLED"));}
 private static boolean owned()throws Exception{
  try{
   JsonObject status=JsonParser.parseString(Files.readString(Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("control/owner-status.json"))).getAsJsonObject();
   return "ACTIVE_SCOPED_CONTROL".equals(status.get("status").getAsString())
     && System.getenv("KNEEKURA_DEBUG_RUN_ID").equals(status.get("runId").getAsString())
     && System.getenv("KNEEKURA_DEBUG_SESSION_ID").equals(status.get("debugSessionId").getAsString());
  }catch(NoSuchFileException pending){return false;}
 }
 @Mod.EventBusSubscriber(modid="naturalghast_tank_observer")
 public static final class ServerObserver {
  @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event)throws Exception{
   if(!active()||event.phase!=TickEvent.Phase.END||samples>=sampleLimit()||!owned())return;
   if(standard()&&!Files.exists(output().resolve("request-window.json")))return;
   var entity=event.getServer().overworld().getEntity(UUID.fromString("67676767-1007-4000-8000-000000000001"));
   if(!(entity instanceof SoutouGhast ghast))return;
   var control=(SoutouGhastInertialMoveControl)ghast.getMoveControl();var target=ghast.getTarget();var velocity=ghast.getDeltaMovement();
   var players=event.getServer().getPlayerList().getPlayers();
   var player=players.size()==1?players.get(0):null;
   JsonObject row=new JsonObject();row.addProperty("tick",ghast.level().getGameTime());row.addProperty("uuid",ghast.getUUID().toString());row.addProperty("noAI",ghast.isNoAi());
   row.addProperty("x",ghast.getX());row.addProperty("y",ghast.getY());row.addProperty("z",ghast.getZ());
   row.addProperty("vx",velocity.x);row.addProperty("vy",velocity.y);row.addProperty("vz",velocity.z);row.addProperty("speed",velocity.length());
   row.addProperty("targetUuid",target==null?null:target.getUUID().toString());row.addProperty("targetType",target==null?null:BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
   row.addProperty("range",target==null?-1:ghast.distanceTo(target));row.addProperty("lineOfSight",target!=null&&ghast.getSensing().hasLineOfSight(target));
   boolean frontal=false;
   if(target!=null){
    var offset=ghast.position().subtract(target.position());var look=target.getLookAngle();
    double horizontal=Math.hypot(offset.x,offset.z),lookLength=Math.hypot(look.x,look.z);
    frontal=offset.y>=4&&offset.y<=10&&horizontal>0&&lookLength>0
      &&(offset.x*look.x+offset.z*look.z)/(horizontal*lookLength)>=Math.cos(Math.toRadians(35));
   }
   row.addProperty("inFrontalVolume",frontal);
   if(player!=null){
    row.addProperty("playerUuid",player.getUUID().toString());row.addProperty("playerX",player.getX());row.addProperty("playerY",player.getY());row.addProperty("playerZ",player.getZ());
    row.addProperty("playerYaw",player.getYRot());row.addProperty("playerPitch",player.getXRot());
    row.addProperty("playerHealth",player.getHealth());row.addProperty("playerMode",player.gameMode.getGameModeForPlayer().getName());
   }
   row.addProperty("intent",control.getIntent().mode().name());row.addProperty("primitive",control.getPrimitive().name());row.addProperty("context",control.getMobilityContext().name());
   row.addProperty("clearanceBlocked",control.isClearanceBlocked());row.addProperty("collisionFree",ghast.level().noCollision(ghast,ghast.getBoundingBox()));
   var tactical=control.getTacticalState();
   row.addProperty("tacticalAction",tactical.action().name());row.addProperty("tacticalPhase",tactical.phase().name());
   row.addProperty("tacticalTiming",tactical.timingSlot().name());row.addProperty("tacticalCommitted",tactical.committed());
   var region=control.getCombatRegion();
   if(region!=null){
    row.addProperty("regionGeneration",region.generation());row.addProperty("regionReason",region.reason().name());
    row.addProperty("regionX",region.center().x());row.addProperty("regionY",region.center().y());row.addProperty("regionZ",region.center().z());
    row.addProperty("radiusX",region.radii().x());row.addProperty("radiusY",region.radii().y());row.addProperty("radiusZ",region.radii().z());
    row.addProperty("inCombatRegion",region.contains(SoutouGhastInertialMoveControl.from(ghast.position())));
   }
   row.addProperty("width",ghast.getBbWidth());row.addProperty("height",ghast.getBbHeight());row.addProperty("health",ghast.getHealth());
   if(standard()){
    var attack=ghast.getStandardAttack();var state=attack.state();
    row.addProperty("mobGriefing",ghast.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING));
    row.addProperty("attackPhase",state.phase().name());row.addProperty("attackTicks",state.ticks());row.addProperty("attackFire",state.fire());row.addProperty("firingFace",ghast.isCharging());row.addProperty("firedCount",attack.firedCount());
    row.addProperty("aimX",state.direction().x());row.addProperty("aimY",state.direction().y());row.addProperty("aimZ",state.direction().z());
    row.addProperty("rallyFace",attack.rallyState().face());
    row.addProperty("lastFiredProfile",attack.lastFiredProfile().name());
    JsonArray projectiles=new JsonArray();var all=ghast.level().getEntitiesOfClass(StandardSoutouFireball.class,new net.minecraft.world.phys.AABB(0,224,0,52,248,52));
    row.addProperty("projectileCoverage",all.size()>16?"PARTIAL":"LOADED_ROOM_SELECTED_TYPE");
    for(var p:all.stream().limit(16).toList()){
     JsonObject shot=new JsonObject();shot.addProperty("uuid",p.getUUID().toString());shot.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(p.getType()).toString());
     shot.addProperty("x",p.getX());shot.addProperty("y",p.getY());shot.addProperty("z",p.getZ());shot.addProperty("speed",p.getDeltaMovement().length());
     shot.addProperty("width",p.getBbWidth());shot.addProperty("height",p.getBbHeight());shot.addProperty("pickRadius",p.getPickRadius());
     shot.addProperty("origin",p.originUuid()==null?null:p.originUuid().toString());shot.addProperty("owner",p.getOwner()==null?null:p.getOwner().getUUID().toString());
     shot.addProperty("playerDeflected",p.isPlayerDeflected());shot.addProperty("returns",p.bossReturns());
     shot.addProperty("savedOrigin",p.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getUUID("StandardOrigin").toString());
     if(p instanceof CommittedSoutouFireball special&&special.flight()!=null){
      var flight=special.flight();var point=flight.path().points().get(flight.index());
      shot.addProperty("kind",flight.path().kind().name());shot.addProperty("phase",special.phase());shot.addProperty("index",flight.index());shot.addProperty("normalized",flight.normalized());
      shot.addProperty("expectedX",point.x());shot.addProperty("expectedY",point.y());shot.addProperty("expectedZ",point.z());
      if(serverSeen.size()<16&&serverSeen.add(p.getUUID())){
       JsonObject snapshot=shot.deepCopy();JsonArray points=new JsonArray();for(var pos:flight.path().points()){
        JsonArray xyz=new JsonArray();xyz.add(pos.x());xyz.add(pos.y());xyz.add(pos.z());points.add(xyz);
       }
       snapshot.add("path",points);var proof=special.preflight();snapshot.addProperty("preflightScope",proof.getString("Scope"));snapshot.addProperty("preflightGameTime",proof.getLong("GameTime"));snapshot.addProperty("preflightSegments",proof.getInt("Segments"));
       JsonArray terminal=new JsonArray();for(var raw:proof.getList("Terminal",10)){
        var tag=(net.minecraft.nbt.CompoundTag)raw;JsonObject cell=new JsonObject();cell.addProperty("x",tag.getInt("X"));cell.addProperty("y",tag.getInt("Y"));cell.addProperty("z",tag.getInt("Z"));cell.addProperty("state",tag.getString("State"));terminal.add(cell);
       }
       snapshot.add("terminal",terminal);Files.createDirectories(output());Files.writeString(output().resolve("profile-paths.jsonl"),snapshot+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
      }
     }
     projectiles.add(shot);
    }
    row.add("projectiles",projectiles);
   }
   Files.createDirectories(output());Files.writeString(output().resolve("flight.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);ready=++samples>=sampleLimit();
  }
 }
 @Mod.EventBusSubscriber(modid="naturalghast_tank_observer",value=Dist.CLIENT)
 public static final class ClientObserver {
  private static boolean captured;
  private static final java.util.Set<UUID> seen=new java.util.HashSet<>();
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event)throws Exception{
   if(!active()||!standard()||ready||event.phase!=TickEvent.Phase.END||seen.size()>=16||!owned())return;
   if(!Files.exists(output().resolve("request-window.json")))return;
   var mc=Minecraft.getInstance();if(mc.level==null)return;
   for(var p:mc.level.getEntitiesOfClass(StandardSoutouFireball.class,new net.minecraft.world.phys.AABB(0,224,0,52,248,52)).stream().limit(16).toList()){
    if(!seen.add(p.getUUID()))continue;
    JsonObject row=new JsonObject();row.addProperty("tick",mc.level.getGameTime());row.addProperty("uuid",p.getUUID().toString());row.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(p.getType()).toString());
    row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(p).getClass().getName());
    row.addProperty("powerMagnitude",Math.sqrt(p.xPower*p.xPower+p.yPower*p.yPower+p.zPower*p.zPower));row.addProperty("speed",p.getDeltaMovement().length());
    if(p instanceof CommittedSoutouFireball special&&special.flight()!=null){row.addProperty("kind",special.flight().path().kind().name());row.addProperty("phase",special.phase());row.addProperty("index",special.flight().index());}
    Files.createDirectories(output());Files.writeString(output().resolve("client-projectiles.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
   }
  }
  @SubscribeEvent public static void frame(RenderLevelStageEvent event)throws Exception{
   if(!active()||!ready||captured||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||!owned())return;
   // The opt-in pilot creates a single explicit request; default observer writes no frame.
   if(!Files.exists(output().resolve("request-frame.json")))return;
   var mc=Minecraft.getInstance();if(mc.level==null)return;
   var entity=mc.level.getEntitiesOfClass(SoutouGhast.class,new net.minecraft.world.phys.AABB(0,224,0,52,248,52));if(entity.size()!=1)return;
   captured=true;
   try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output().resolve("frame.png"));}
   JsonObject frame=new JsonObject();frame.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(entity.get(0)).getClass().getName());
   frame.addProperty("capture","EXPLICIT_SUPPLEMENTARY_RAW_FRAMEBUFFER_AFTER_FLIGHT_WINDOW_NOT_CARDINAL");
   frame.addProperty("targetUuid",entity.get(0).getUUID().toString());frame.addProperty("cameraEntity",mc.getCameraEntity().getUUID().toString());
   Files.writeString(output().resolve("frame.json"),frame+"\n",StandardOpenOption.CREATE_NEW);
  }
 }
}

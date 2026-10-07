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
 private static boolean overhead(){return "1".equals(System.getenv("KNEEKURA_DEBUG_NATURAL_OVERHEAD"));}
 private static int sampleLimit(){return overhead()?900:standard()?180:900;}
 private static int deathSample=-1;
 private static int impacts;
 private static volatile long wallDeadline=-1;
 private static volatile boolean ready;
 private static final ObservationFailureBoundary boundary=new ObservationFailureBoundary();
 private static boolean failureReported;
 private static int eventCount,eventOrder;
 private static Path output(){return Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("evidence/derived/naturalghast-flight");}
 private static boolean active(){return "1".equals(System.getenv("KNEEKURA_DEBUG_ENABLED"));}
 private static boolean withinOverheadWindow()throws Exception{
  if(!overhead())return true;
  if(wallDeadline<0){
   var file=output().resolve("request-window.json");if(!Files.exists(file)||Files.size(file)>4096)return false;
   try{var request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    if(!request.has("wallDeadlineEpochMs")||!request.has("maxTicks")||!request.has("maxWallMs")||request.get("maxTicks").getAsInt()!=900||request.get("maxWallMs").getAsInt()!=50000)return false;
    wallDeadline=request.get("wallDeadlineEpochMs").getAsLong();
   }catch(JsonParseException pending){return false;}
  }
  return wallDeadline>0&&System.currentTimeMillis()<wallDeadline;
 }
 private static void endWindow(String reason,long tick)throws Exception{
  if(ready)return;JsonObject end=new JsonObject();end.addProperty("samples",samples);end.addProperty("tick",tick);end.addProperty("reason",reason);end.addProperty("deathSample",deathSample);Files.createDirectories(output());Files.writeString(output().resolve("window-end.json"),end+"\n",StandardOpenOption.CREATE_NEW);ready=true;
 }
 private static void reportFailure(long tick){
  if(failureReported)return;failureReported=true;
  try{JsonObject failure=new JsonObject();failure.addProperty("status","OBSERVER_FAILED_GAMEPLAY_CONTINUES");failure.addProperty("error",boundary.failure());failure.addProperty("tick",tick);Files.createDirectories(output());Files.writeString(output().resolve("observer-failure.json"),failure+"\n",StandardOpenOption.CREATE_NEW);endWindow("OBSERVER_FAILURE",tick);}catch(Exception unavailable){ready=true;}
 }
 private static void appendEvent(JsonObject row)throws Exception{
  if(eventCount>=8)throw new IllegalStateException("RELEASE_DEATH_EVENT_BOUND_EXCEEDED");
  eventCount++;row.addProperty("order",++eventOrder);Files.createDirectories(output());Files.writeString(output().resolve("release-death-events.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
 }
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
  @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
  public static void impact(net.minecraftforge.event.entity.ProjectileImpactEvent event){boundary.run(()->observeImpact(event));}
  private static void observeImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event)throws Exception{
   if(!active()||!overhead()||ready||impacts>=16||event.getProjectile().level().isClientSide||!owned()||!Files.exists(output().resolve("request-window.json"))||!withinOverheadWindow())return;
   if(!(event.getProjectile() instanceof CommittedSoutouFireball bomb)||bomb.flight()==null||bomb.flight().path().kind()!=com.genki.soutoughast.entity.ai.flight.CommittedTrajectory.Kind.BOMB)return;
   var hit=event.getRayTraceResult();JsonObject row=new JsonObject();row.addProperty("producer","NATIVE_PROJECTILE_IMPACT_EVENT_LOWEST_PRE_DAMAGE");row.addProperty("tick",bomb.level().getGameTime());row.addProperty("uuid",bomb.getUUID().toString());row.addProperty("index",bomb.flight().index());row.addProperty("normalized",bomb.flight().normalized());row.addProperty("hitType",hit.getType().name());row.addProperty("x",hit.getLocation().x);row.addProperty("y",hit.getLocation().y);row.addProperty("z",hit.getLocation().z);row.addProperty("canceledAtListener",event.isCanceled());row.addProperty("resultAtListener",event.getImpactResult().name());
   if(hit instanceof net.minecraft.world.phys.EntityHitResult entityHit){var victim=entityHit.getEntity();row.addProperty("victimUuid",victim.getUUID().toString());row.addProperty("victimType",BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString());if(victim instanceof net.minecraft.world.entity.LivingEntity living)row.addProperty("victimHealthAtListener",living.getHealth());}
   if(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit){var pos=blockHit.getBlockPos();JsonArray block=new JsonArray();block.add(pos.getX());block.add(pos.getY());block.add(pos.getZ());row.add("block",block);}
   impacts++;Files.createDirectories(output());Files.writeString(output().resolve("bomb-impacts.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  }
  @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
  public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){boundary.run(()->{
   if(!active()||!overhead()||ready||event.getLevel().isClientSide||!Files.exists(output().resolve("request-window.json"))||!withinOverheadWindow()||!owned())return;
   if(!(event.getEntity() instanceof CommittedSoutouFireball bomb)||bomb.flight()==null||bomb.flight().path().kind()!=com.genki.soutoughast.entity.ai.flight.CommittedTrajectory.Kind.BOMB||!(bomb.getOwner() instanceof SoutouGhast boss)||!boss.getUUID().equals(UUID.fromString("67676767-1007-4000-8000-000000000001")))return;
   var target=boss.getTarget();JsonObject row=new JsonObject();row.addProperty("event","RELEASE_JOIN_LISTENER");row.addProperty("tick",boss.level().getGameTime());row.addProperty("uuid",bomb.getUUID().toString());row.addProperty("canceledAtListener",event.isCanceled());
   row.addProperty("targetUuid",target==null?null:target.getUUID().toString());row.addProperty("targetType",target==null?null:BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());row.addProperty("targetAlive",target!=null&&target.isAlive());row.addProperty("targetHealth",target==null?0:target.getHealth());row.addProperty("lineOfSight",target!=null&&boss.getSensing().hasLineOfSight(target));
   if(target!=null){row.addProperty("targetX",target.getX());row.addProperty("targetY",target.getY());row.addProperty("targetZ",target.getZ());}row.addProperty("bossX",boss.getX());row.addProperty("bossY",boss.getY());row.addProperty("bossZ",boss.getZ());row.addProperty("majorPitch",boss.majorPitch());appendEvent(row);
  });}
  @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
  public static void died(net.minecraftforge.event.entity.living.LivingDeathEvent event){boundary.run(()->{
   if(!active()||!overhead()||ready||!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)||!Files.exists(output().resolve("request-window.json"))||!withinOverheadWindow()||!owned())return;
   if(player.level().dimension()!=net.minecraft.world.level.Level.OVERWORLD||!new net.minecraft.world.phys.AABB(0,224,0,52,248,52).contains(player.position()))return;
   JsonObject row=new JsonObject();row.addProperty("event","PLAYER_DEATH_LISTENER");row.addProperty("tick",player.level().getGameTime());row.addProperty("uuid",player.getUUID().toString());row.addProperty("canceledAtListener",event.isCanceled());appendEvent(row);
  });}
  @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event)throws Exception{
   if(overhead()){
    if(boundary.failed()){if(event.phase==TickEvent.Phase.END)reportFailure(event.getServer().overworld().getGameTime());return;}
    boundary.run(()->observeTick(event));
   }else observeTick(event);
  }
  private static void observeTick(TickEvent.ServerTickEvent event)throws Exception{
   if(!active()||ready||event.phase!=TickEvent.Phase.END||samples>=sampleLimit()||!owned())return;
   if(standard()&&!Files.exists(output().resolve("request-window.json")))return;
   if(overhead()&&!withinOverheadWindow()){
    if(wallDeadline>0)endWindow("WALL_CLOCK_DEADLINE",event.getServer().overworld().getGameTime());
    return;
   }
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
    row.addProperty("selectedProfile",attack.selectedProfile().name());row.addProperty("lastCandidate",attack.lastCandidate());row.addProperty("lastRejection",attack.lastRejection());
    row.addProperty("stationaryTicks",attack.stationaryTicks());row.addProperty("observedDisplacement",attack.observedDisplacement());
    row.addProperty("profileCueTick",attack.profileCueTick());var recipe=attack.committedProfile();if(recipe!=null){
     row.addProperty("recipe",recipe.toString());JsonArray endpoint=new JsonArray();endpoint.add(recipe.endpoint().x());endpoint.add(recipe.endpoint().y());endpoint.add(recipe.endpoint().z());row.add("recipeEndpoint",endpoint);
    }
    if(overhead()){
     var major=ghast.getOverheadAttack();var majorState=major.state();
     row.addProperty("majorPhase",majorState.phase().name());row.addProperty("majorTicks",majorState.ticks());row.addProperty("majorActive",major.active());
     row.addProperty("majorDownward",majorState.downward());row.addProperty("majorFace",majorState.face());row.addProperty("majorReleaseRequested",majorState.releaseRequested());row.addProperty("majorReason",majorState.reason());
     row.addProperty("majorSequences",major.sequenceCount());row.addProperty("bombCount",major.bombCount());row.addProperty("majorPitch",ghast.majorPitch());row.addProperty("lookPitch",ghast.getXRot());
     if(majorState.goal()!=null){JsonArray goal=new JsonArray();goal.add(majorState.goal().x());goal.add(majorState.goal().y());goal.add(majorState.goal().z());row.add("majorGoal",goal);}
     if(deathSample<0&&player!=null&&!player.isAlive())deathSample=samples;
     row.addProperty("windowStage",deathSample<0?"COMBAT":"POST_DEATH_RECOVERY");
    }
    var candidateProof=attack.lastPreflight();if(candidateProof!=null){row.addProperty("preflightClear",candidateProof.result().clear());row.addProperty("preflightSegments",candidateProof.result().segments());}
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
   Files.createDirectories(output());Files.writeString(output().resolve("flight.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);samples++;
   if(overhead()&&deathSample>=0){
    boolean recovered=!ghast.getOverheadAttack().active(),bounded=samples-deathSample>=200;
    if(recovered||bounded)endWindow(recovered?"PLAYER_DEATH_RECOVERY_COMPLETE":"PLAYER_DEATH_RECOVERY_TIMEOUT",ghast.level().getGameTime());
   }
   if(samples>=sampleLimit()){
    if(overhead())endWindow("MAX_TICKS",ghast.level().getGameTime());else ready=true;
   }
  }
 }
 @Mod.EventBusSubscriber(modid="naturalghast_tank_observer",value=Dist.CLIENT)
 public static final class ClientObserver {
  private static boolean captured;
  private static boolean downwardCaptured;
  private static final java.util.Set<UUID> seen=new java.util.HashSet<>();
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event)throws Exception{
   if(overhead())boundary.run(()->observeTick(event));else observeTick(event);
  }
  private static void observeTick(TickEvent.ClientTickEvent event)throws Exception{
   if(!active()||!standard()||ready||event.phase!=TickEvent.Phase.END||seen.size()>=16||!owned())return;
   if(!Files.exists(output().resolve("request-window.json")))return;
   if(!withinOverheadWindow())return;
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
   if(overhead())boundary.run(()->observeFrame(event));else observeFrame(event);
  }
  private static void observeFrame(RenderLevelStageEvent event)throws Exception{
   if(!active()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||!owned())return;
   if(overhead()&&!ready&&!downwardCaptured&&Files.exists(output().resolve("request-major-frames.json"))&&withinOverheadWindow()){
    var mc=Minecraft.getInstance();if(mc.level!=null){
     var subjects=mc.level.getEntitiesOfClass(SoutouGhast.class,new net.minecraft.world.phys.AABB(0,224,0,52,248,52));
     if(subjects.size()==1&&subjects.get(0).majorRenderPitch(1)>=80){
      var boss=subjects.get(0);downwardCaptured=true;
      try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(output().resolve("downward-frame.png"));}
      JsonObject pose=new JsonObject();pose.addProperty("tick",mc.level.getGameTime());pose.addProperty("subjectUuid",boss.getUUID().toString());pose.addProperty("renderPitch",boss.majorRenderPitch(1));pose.addProperty("syncedPitch",boss.majorPitch());pose.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(boss).getClass().getName());pose.addProperty("cameraUuid",mc.getCameraEntity().getUUID().toString());pose.addProperty("cameraPitch",mc.getCameraEntity().getXRot());pose.addProperty("capture","ONE_EXPLICIT_NATURALLY_OBSERVED_DOWNWARD_POSE_PLAYER_FRAME_NOT_CARDINAL");Files.writeString(output().resolve("downward-frame.json"),pose+"\n",StandardOpenOption.CREATE_NEW);
     }
    }
   }
   captureFinal("EXPLICIT_SUPPLEMENTARY_RAW_FRAMEBUFFER_AFTER_FLIGHT_WINDOW_NOT_CARDINAL");
  }
  @SubscribeEvent public static void gui(net.minecraftforge.client.event.RenderGuiEvent.Post event){
   if(overhead()&&ready)boundary.run(()->{if(active()&&owned())captureFinal("EXPLICIT_FINAL_GUI_FRAME_SAME_PLAYER_CAMERA_AFTER_WINDOW_NOT_CARDINAL");});
  }
  private static void captureFinal(String capture)throws Exception{
   if(!ready||captured)return;
   // The opt-in pilot creates a single explicit request; default observer writes no frame.
   if(!Files.exists(output().resolve("request-frame.json")))return;
   var mc=Minecraft.getInstance();if(mc.level==null)return;
   if(mc.getCameraEntity()==null)return;
   var entity=mc.level.getEntitiesOfClass(SoutouGhast.class,new net.minecraft.world.phys.AABB(0,224,0,52,248,52));if(!overhead()&&entity.size()!=1)return;
   captured=true;
   try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(output().resolve("frame.png"));}
   JsonObject frame=new JsonObject();frame.addProperty("renderer",entity.size()==1?mc.getEntityRenderDispatcher().getRenderer(entity.get(0)).getClass().getName():null);
   frame.addProperty("capture",capture);frame.addProperty("subjectPresent",entity.size()==1);
   frame.addProperty("targetUuid",entity.size()==1?entity.get(0).getUUID().toString():null);frame.addProperty("cameraEntity",mc.getCameraEntity().getUUID().toString());
   Files.writeString(output().resolve("frame.json"),frame+"\n",StandardOpenOption.CREATE_NEW);
  }
 }
}

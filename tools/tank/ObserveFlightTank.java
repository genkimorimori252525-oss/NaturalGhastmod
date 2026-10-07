package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
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
   if(!active()||event.phase!=TickEvent.Phase.END||samples>=600||!owned())return;
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
   }
   row.addProperty("intent",control.getIntent().mode().name());row.addProperty("primitive",control.getPrimitive().name());row.addProperty("context",control.getMobilityContext().name());
   row.addProperty("clearanceBlocked",control.isClearanceBlocked());row.addProperty("collisionFree",ghast.level().noCollision(ghast,ghast.getBoundingBox()));
   var region=control.getCombatRegion();
   if(region!=null){
    row.addProperty("regionGeneration",region.generation());row.addProperty("regionReason",region.reason().name());
    row.addProperty("regionX",region.center().x());row.addProperty("regionY",region.center().y());row.addProperty("regionZ",region.center().z());
    row.addProperty("radiusX",region.radii().x());row.addProperty("radiusY",region.radii().y());row.addProperty("radiusZ",region.radii().z());
    row.addProperty("inCombatRegion",region.contains(SoutouGhastInertialMoveControl.from(ghast.position())));
   }
   row.addProperty("width",ghast.getBbWidth());row.addProperty("height",ghast.getBbHeight());row.addProperty("health",ghast.getHealth());
   Files.createDirectories(output());Files.writeString(output().resolve("flight.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);ready=++samples>=600;
  }
 }
 @Mod.EventBusSubscriber(modid="naturalghast_tank_observer",value=Dist.CLIENT)
 public static final class ClientObserver {
  private static boolean captured;
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

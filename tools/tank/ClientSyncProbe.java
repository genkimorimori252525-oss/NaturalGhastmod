package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Private passive ordinary tracking/deflection observer; no packet or tracker manipulation. */
@Mod("naturalghast_client_sync_probe")
public final class ClientSyncProbe {
 private static final String SCOPE="PASSIVE_NATIVE_TRACKER_SPAWN_DEFLECTION_NOT_RETRACK_OR_LATE_JOIN";
 private static final String[] NAMES={"BURST","CURVE","LOB","BOMB","DEFLECTED_CURVE","STANDARD","GROUND"};
 private static final ConcurrentMap<UUID,String> names=new ConcurrentHashMap<>();
 private static final ConcurrentLinkedQueue<JsonObject> clientQueue=new ConcurrentLinkedQueue<>();
 private static final AtomicInteger queueSize=new AtomicInteger();private static volatile boolean active;private static volatile String clientError;
 private final Map<String,StandardSoutouFireball> balls=new LinkedHashMap<>();
 private final JsonArray spawns=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),serverRows=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request,deflection;private Path output;private long start,deadline,deflectTick;private int samples;private boolean done,canonicalPlayer;
 public ClientSyncProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_CLIENT_SYNC"));}
 @SubscribeEvent public void tracking(PlayerEvent.StartTracking event){
  if(!active||done||!(event.getTarget() instanceof StandardSoutouFireball ball)||!names.containsKey(ball.getUUID()))return;
  try{require(event.getEntity().getUUID().toString().equals(request.get("playerUuid").getAsString()),"TRACKING_PLAYER");var row=snapshot(ball);row.addProperty("tick",ball.level().getGameTime());spawns.add(row);append("spawns.jsonl",row);}catch(Exception e){errors.add("Tracking:"+e.getMessage());}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/client-sync");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_CLIENT_SYNC_NONCE"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_CLIENT_SYNC_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+20000;start=level.getGameTime();Files.createDirectories(output);active=true;
  }
  require(samples++<240&&System.currentTimeMillis()<deadline&&clientError==null&&errors.isEmpty(),"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var entity=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(entity instanceof SoutouGhast&&entity.isAlive()&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level,"GENUINE_PARTICIPANTS");
  canonicalPlayer=true;
  drain();if(balls.isEmpty()){create(level,(SoutouGhast)entity);return;}
  for(var ball:balls.values()){if(!ball.isRemoved()){var row=snapshot(ball);row.addProperty("tick",level.getGameTime());row.addProperty("alive",true);serverRows.add(row);require(serverRows.size()<=1536,"SERVER_ROW_BOUND");append("serverRows.jsonl",row);}}
  var curve=(CommittedSoutouFireball)balls.get("DEFLECTED_CURVE");
  if(deflection==null&&curve.flight().index()>=4&&clientRows.asList().stream().filter(r->r.getAsJsonObject().get("name").getAsString().equals("DEFLECTED_CURVE")).count()>=2){
   require(!curve.isRemoved()&&curve.hurt(player.damageSources().playerAttack(player),1)&&curve.flight().normalized()&&curve.getOwner()==player,"SYNTHETIC_DEFLECTION");deflection=snapshot(curve);deflection.addProperty("synthetic",true);deflectTick=level.getGameTime();
  }
  boolean complete=spawns.size()==7&&joins.size()==7&&deflection!=null&&level.getGameTime()-deflectTick>=6;
  for(String name:NAMES){long count=clientRows.asList().stream().filter(r->r.getAsJsonObject().get("name").getAsString().equals(name)).count();complete&=count>=4;}
  long normalized=clientRows.asList().stream().filter(r->{var v=r.getAsJsonObject();return v.get("name").getAsString().equals("DEFLECTED_CURVE")&&v.get("normalized").getAsBoolean();}).count();complete&=normalized>=4;
  if(complete&&level.getGameTime()-start>=18)finish("PASS");
 }
 private void create(ServerLevel level,SoutouGhast boss)throws IOException{
  for(String name:NAMES){
   StandardSoutouFireball ball;
   if(name.equals("STANDARD")||name.equals("GROUND"))ball=name.equals("STANDARD")?new StandardSoutouFireball(boss,new Vec3(3,236,20),new Vec3(1,0,0)):new GroundSoutouFireball(boss,new Vec3(3,236,27),new Vec3(1,0,0));
   else{
    var path=switch(name){
     case "BURST" -> CommittedTrajectory.burst(new FlightVector(3,236,5),new FlightVector(49,236,5));
     case "CURVE" -> CommittedTrajectory.curve(new FlightVector(3,236,12),new FlightVector(49,236,12),CommittedTrajectory.Strength.NORMAL,1);
     case "LOB" -> CommittedTrajectory.lob(new FlightVector(3.5,236,45.5),new FlightVector(49.5,223.75,45.5),6);
     case "BOMB" -> CommittedTrajectory.bomb(new FlightVector(45.5,246,35.5),new FlightVector(45.5,223.75,35.5));
     default -> CommittedTrajectory.curve(new FlightVector(3,230,5),new FlightVector(49,230,5),CommittedTrajectory.Strength.NORMAL,1);
    };
    var profile=new CommittedSoutouFireball(boss,path);var proof=CommittedPathClearance.validate(level,profile,path);require(proof.result().clear(),"LOADED_FULL_PREFLIGHT");profile.setPreflight(proof);ball=profile;
   }
   names.put(ball.getUUID(),name);balls.put(name,ball);require(level.noCollision(ball,ball.getBoundingBox())&&level.addFreshEntity(ball),"NATIVE_CREATION");
  }
 }
 private void drain()throws IOException{JsonObject row;while((row=clientQueue.poll())!=null){queueSize.decrementAndGet();String kind=row.remove("record").getAsString();if(kind.equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=2048,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;drain();done=true;var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("overrides",false);result.add("spawns",spawns);result.add("joins",joins);result.add("clientRows",clientRows);result.add("serverRows",serverRows);result.add("deflection",deflection);if(clientError!=null)errors.add(clientError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject snapshot(StandardSoutouFireball ball){
  var row=new JsonObject();row.addProperty("name",names.get(ball.getUUID()));row.addProperty("uuid",ball.getUUID().toString());row.addProperty("owner",ball.getOwner()==null?"":ball.getOwner().getUUID().toString());row.addProperty("inWater",ball.isInWater());row.addProperty("x",ball.getX());row.addProperty("y",ball.getY());row.addProperty("z",ball.getZ());row.addProperty("vx",ball.getDeltaMovement().x);row.addProperty("vy",ball.getDeltaMovement().y);row.addProperty("vz",ball.getDeltaMovement().z);row.addProperty("px",ball.xPower);row.addProperty("py",ball.yPower);row.addProperty("pz",ball.zPower);
  if(ball instanceof CommittedSoutouFireball profile&&profile.flight()!=null){var data=CommittedTrajectoryCodec.write(profile.flight());data.remove("Index");data.remove("Normalized");row.addProperty("path",data.toString());row.addProperty("index",profile.flight().index());row.addProperty("normalized",profile.flight().normalized());}else{row.addProperty("path","");row.addProperty("index",0);row.addProperty("normalized",false);}return row;
 }
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>256){queueSize.decrementAndGet();clientError="CLIENT_QUEUE_BOUND";return;}clientQueue.add(row);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("CLIENT_SYNC_"+reason);}
 @Mod.EventBusSubscriber(modid="naturalghast_client_sync_probe",value=Dist.CLIENT)
 public static final class Client {
  private static final Map<UUID,StandardSoutouFireball> pending=new LinkedHashMap<>(),observed=new LinkedHashMap<>();private static final Map<UUID,JsonObject> before=new HashMap<>();private static int frame;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){
   if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof StandardSoutouFireball ball)||!names.containsKey(ball.getUUID()))return;
   if(event.isCanceled()||pending.containsKey(ball.getUUID())||observed.containsKey(ball.getUUID())){clientError="DUPLICATE_OR_CANCELED_CLIENT_JOIN";return;}pending.put(ball.getUUID(),ball);
  }
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
   if(!active||!enabled())return;
   try{
    var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
    if(event.phase==TickEvent.Phase.START){
     frame++;for(var e:pending.entrySet()){var row=snapshot(e.getValue());row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(e.getValue()).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed.put(e.getKey(),e.getValue());}pending.clear();
     before.clear();for(var ball:observed.values())if(!ball.isRemoved()){
      var row=snapshot(ball);if(ball instanceof CommittedSoutouFireball profile&&profile.flight()!=null&&!profile.flight().normalized()&&profile.flight().index()<profile.flight().path().points().size()-1){var v=profile.flight().path().velocity(profile.flight().index());row.addProperty("dx",v.x());row.addProperty("dy",v.y());row.addProperty("dz",v.z());}before.put(ball.getUUID(),row);
     }
    }else for(var ball:observed.values()){
     if(ball.isRemoved())continue;if(mc.level.getEntity(ball.getId())!=ball){clientError="CLIENT_ENTITY_IDENTITY";return;}var row=snapshot(ball);row.add("before",before.get(ball.getUUID()));row.addProperty("frame",frame);row.addProperty("record","ROW");enqueue(row);
    }
   }catch(Exception error){clientError="Client:"+error.getClass().getSimpleName()+":"+error.getMessage();}
  }
 }
}

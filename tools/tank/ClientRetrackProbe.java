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

/** Private controlled native tracker remove/re-add; server entities continue natural ticking. */
@Mod("naturalghast_retracking_probe")
public final class ClientRetrackProbe {
 private static final String SCOPE="CONTROLLED_NATIVE_TRACKER_REMOVE_READD_NOT_ENTITY_RELOAD_OR_LATE_JOIN";
 private static final String[] NAMES={"BURST","CURVE","LOB","BOMB","DEFLECTED_CURVE","STANDARD","GROUND"};
 private static final ConcurrentMap<UUID,String> names=new ConcurrentHashMap<>();
 private static final ConcurrentLinkedQueue<JsonObject> clientQueue=new ConcurrentLinkedQueue<>();
 private static final AtomicInteger queueSize=new AtomicInteger();private static volatile boolean active;private static volatile String clientError;
 private static final Set<UUID> finished=ConcurrentHashMap.newKeySet();
 private final Map<String,StandardSoutouFireball> balls=new LinkedHashMap<>();
 private final Map<String,Control> controls=new LinkedHashMap<>();
 private final JsonArray spawns=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),serverRows=new JsonArray();private final List<String> errors=new ArrayList<>();
 private final JsonArray stops=new JsonArray(),absences=new JsonArray(),readds=new JsonArray(),cleanup=new JsonArray();
 private static final class Control {String stage="INITIAL";boolean ownedRemoval;final TrackerReaddIntent intent=new TrackerReaddIntent();long removedTick;}
 private JsonObject request,deflection;private Path output;private long start,deadline,deflectTick;private int samples;private boolean done,canonicalPlayer;
 public ClientRetrackProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_RETRACKING"));}
 @SubscribeEvent public void tracking(PlayerEvent.StartTracking event){
  if(!active||done||!(event.getTarget() instanceof StandardSoutouFireball ball)||!names.containsKey(ball.getUUID()))return;
  try{require(event.getEntity().getUUID().toString().equals(request.get("playerUuid").getAsString()),"TRACKING_PLAYER");var control=controls.get(names.get(ball.getUUID()));require(control!=null&&(control.stage.equals("INITIAL")||control.stage.equals("RETRACKED")),"UNEXPECTED_TRACKER_START");var row=snapshot(ball);row.addProperty("generation",control.stage.equals("INITIAL")?0:1);row.addProperty("tick",ball.level().getGameTime());spawns.add(row);if(control.stage.equals("RETRACKED"))control.ownedRemoval=false;append("spawns.jsonl",row);}catch(Exception e){errors.add("Tracking:"+e.getMessage());}
 }
 @SubscribeEvent public void untracking(PlayerEvent.StopTracking event){
  if(!active||done||!(event.getTarget() instanceof StandardSoutouFireball ball)||!names.containsKey(ball.getUUID()))return;
  try{var control=controls.get(names.get(ball.getUUID()));if(control.stage.equals("DONE"))return;require(control.stage.equals("GAP")&&event.getEntity().getUUID().toString().equals(request.get("playerUuid").getAsString()),"UNEXPECTED_TRACKER_STOP");control.ownedRemoval=true;var row=snapshot(ball);row.addProperty("owned",true);row.addProperty("tick",ball.level().getGameTime());int remaining=ball instanceof CommittedSoutouFireball p&&!p.flight().normalized()?p.flight().path().points().size()-1-p.flight().index():-1;row.addProperty("remaining",remaining);stops.add(row);append("stops.jsonl",row);}catch(Exception e){errors.add("StopTracking:"+e.getMessage());}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/retracking");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_RETRACKING_NONCE"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_RETRACKING_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+20000;start=level.getGameTime();Files.createDirectories(output);active=true;
  }
  require(samples++<240&&System.currentTimeMillis()<deadline&&clientError==null&&errors.isEmpty(),"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var entity=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(entity instanceof SoutouGhast&&entity.isAlive()&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level,"GENUINE_PARTICIPANTS");
  require(level.players().size()==1,"SINGLE_CANONICAL_CONNECTION");
  canonicalPlayer=true;
  drain();if(balls.isEmpty()){create(level,(SoutouGhast)entity);return;}
  var curve=(CommittedSoutouFireball)balls.get("DEFLECTED_CURVE");
  if(deflection==null&&curve.flight().index()>=4&&clientRows.asList().stream().filter(r->r.getAsJsonObject().get("name").getAsString().equals("DEFLECTED_CURVE")).count()>=2){
   require(!curve.isRemoved()&&curve.hurt(player.damageSources().playerAttack(player),1)&&curve.flight().normalized()&&curve.getOwner()==player,"SYNTHETIC_DEFLECTION");deflection=snapshot(curve);deflection.addProperty("synthetic",true);deflectTick=level.getGameTime();
  }
  for(String name:NAMES){
   var ball=balls.get(name);var c=controls.get(name);if(c.stage.equals("DONE"))continue;
   require(!ball.isRemoved()&&level.getEntity(ball.getUUID())==ball&&level.hasChunkAt(ball.blockPosition()),"SAME_LIVE_REGISTERED_LOADED_ENTITY");
   long first=count(name,0,false);boolean initialComplete=first>=4&&(!name.equals("DEFLECTED_CURVE")||count(name,0,true)>=4);
   if(c.stage.equals("INITIAL")&&initialComplete){
    if(ball instanceof CommittedSoutouFireball p&&!p.flight().normalized())require(p.flight().path().points().size()-1-p.flight().index()>=12,"REMAINING_NATIVE_PATH");
    c.stage="GAP";c.removedTick=level.getGameTime();level.getChunkSource().removeEntity(ball);require(c.ownedRemoval,"ACTUAL_TRACKER_STOP_ACK");
   }else if(c.stage.equals("GAP")){
    require(level.getGameTime()-c.removedTick<=6,"BOUNDED_GAP");
    boolean absent=absences.asList().stream().anyMatch(r->r.getAsJsonObject().get("uuid").getAsString().equals(ball.getUUID().toString()));
    if(absent&&level.getGameTime()-c.removedTick>=2){c.intent.execute(()->{c.stage="RETRACKED";var row=snapshot(ball);row.addProperty("tick",level.getGameTime());row.addProperty("registered",true);readds.add(row);append("readds.jsonl",row);},()->level.getChunkSource().addEntity(ball));require(!c.ownedRemoval,"ACTUAL_TRACKER_START_ACK");}
   }
   var row=snapshot(ball);row.addProperty("generation",c.stage.equals("RETRACKED")?1:0);row.addProperty("tick",level.getGameTime());row.addProperty("alive",!ball.isRemoved());row.addProperty("registered",level.getEntity(ball.getUUID())==ball);row.addProperty("loaded",level.hasChunkAt(ball.blockPosition()));serverRows.add(row);require(serverRows.size()<=1536,"SERVER_ROW_BOUND");append("serverRows.jsonl",row);
   if(c.stage.equals("RETRACKED")&&count(name,1,false)>=4&&serverRows.asList().stream().filter(r->{var x=r.getAsJsonObject();return x.get("name").getAsString().equals(name)&&x.get("generation").getAsInt()==1;}).count()>=5){c.stage="DONE";finished.add(ball.getUUID());}
  }
  if(controls.values().stream().allMatch(c->c.stage.equals("DONE")))finish("PASS");
 }
 private long count(String name,int generation,boolean normalized){return clientRows.asList().stream().filter(r->{var row=r.getAsJsonObject();return row.get("name").getAsString().equals(name)&&row.get("generation").getAsInt()==generation&&(!normalized||row.get("normalized").getAsBoolean());}).count();}
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
   names.put(ball.getUUID(),name);balls.put(name,ball);controls.put(name,new Control());require(level.noCollision(ball,ball.getBoundingBox())&&level.addFreshEntity(ball),"NATIVE_CREATION");
  }
 }
 private void drain()throws IOException{JsonObject row;while((row=clientQueue.poll())!=null){queueSize.decrementAndGet();String kind=row.remove("record").getAsString();if(kind.equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else if(kind.equals("ABSENCE")){absences.add(row);append("absences.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=2048,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  restoreTrackers();active=false;drain();done=true;var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("overrides",false);result.add("spawns",spawns);result.add("joins",joins);result.add("clientRows",clientRows);result.add("serverRows",serverRows);result.add("stops",stops);result.add("absences",absences);result.add("readds",readds);result.add("cleanup",cleanup);result.add("deflection",deflection);if(clientError!=null)errors.add(clientError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private void restoreTrackers(){
  for(var entry:controls.entrySet()){var c=entry.getValue();if(!c.ownedRemoval)continue;var ball=balls.get(entry.getKey());var row=new JsonObject();row.addProperty("name",entry.getKey());
   try{require(c.intent.canCleanup(),"UNCERTAIN_ADD_NOT_REPEATED");if(ball.isRemoved()){row.addProperty("status","ALREADY_REMOVED");}else{c.intent.execute(()->{require(ball.level() instanceof ServerLevel level&&level.getEntity(ball.getUUID())==ball&&level.hasChunkAt(ball.blockPosition()),"CLEANUP_ENTITY_IDENTITY");c.stage="RETRACKED";},()->((ServerLevel)ball.level()).getChunkSource().addEntity(ball));require(!c.ownedRemoval,"CLEANUP_ACTUAL_START_ACK");row.addProperty("status","RESTORED");}}catch(Exception e){row.addProperty("status","FAIL");row.addProperty("error",e.getMessage());errors.add("Cleanup:"+e.getMessage());}cleanup.add(row);
  }
 }
 private static JsonObject snapshot(StandardSoutouFireball ball){
  var row=new JsonObject();row.addProperty("name",names.get(ball.getUUID()));row.addProperty("uuid",ball.getUUID().toString());row.addProperty("owner",ball.getOwner()==null?"":ball.getOwner().getUUID().toString());row.addProperty("inWater",ball.isInWater());row.addProperty("x",ball.getX());row.addProperty("y",ball.getY());row.addProperty("z",ball.getZ());row.addProperty("vx",ball.getDeltaMovement().x);row.addProperty("vy",ball.getDeltaMovement().y);row.addProperty("vz",ball.getDeltaMovement().z);row.addProperty("px",ball.xPower);row.addProperty("py",ball.yPower);row.addProperty("pz",ball.zPower);
  row.addProperty("id",ball.getId());row.addProperty("age",ball.provenance().age());row.addProperty("epoch",System.currentTimeMillis());
  if(ball instanceof CommittedSoutouFireball profile&&profile.flight()!=null){var data=CommittedTrajectoryCodec.write(profile.flight());data.remove("Index");data.remove("Normalized");row.addProperty("path",data.toString());row.addProperty("index",profile.flight().index());row.addProperty("normalized",profile.flight().normalized());}else{row.addProperty("path","");row.addProperty("index",0);row.addProperty("normalized",false);}return row;
 }
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>256){queueSize.decrementAndGet();clientError="CLIENT_QUEUE_BOUND";return;}clientQueue.add(row);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("RETRACKING_"+reason);}
 @Mod.EventBusSubscriber(modid="naturalghast_retracking_probe",value=Dist.CLIENT)
 public static final class Client {
  private static final Map<UUID,StandardSoutouFireball> pending=new LinkedHashMap<>(),observed=new LinkedHashMap<>(),original=new LinkedHashMap<>();private static final Map<UUID,Integer> generations=new HashMap<>();private static final Set<UUID> absent=new HashSet<>();private static final Map<UUID,JsonObject> before=new HashMap<>();private static int frame;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){
   if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof StandardSoutouFireball ball)||!names.containsKey(ball.getUUID()))return;
   UUID uuid=ball.getUUID();int generation=generations.getOrDefault(uuid,-1)+1;
   if(event.isCanceled()||pending.containsKey(uuid)||generation>1||generation==1&&(!absent.contains(uuid)||original.get(uuid)==ball||!original.get(uuid).isRemoved())){clientError="DUPLICATE_OR_CANCELED_CLIENT_JOIN";return;}
   generations.put(uuid,generation);if(generation==0)original.put(uuid,ball);pending.put(uuid,ball);
  }
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
   if(!active||!enabled())return;
   try{
    var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
    if(event.phase==TickEvent.Phase.START){
     frame++;
     for(var e:original.entrySet())if(!absent.contains(e.getKey())&&e.getValue().isRemoved()&&mc.level.getEntity(e.getValue().getId())==null){var row=snapshot(e.getValue());row.addProperty("oldRemoved",true);row.addProperty("idAbsent",true);row.addProperty("record","ABSENCE");enqueue(row);absent.add(e.getKey());}
     for(var e:pending.entrySet()){var row=snapshot(e.getValue());row.addProperty("generation",generations.get(e.getKey()));row.addProperty("distinct",e.getValue()!=original.get(e.getKey()));row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(e.getValue()).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed.put(e.getKey(),e.getValue());}pending.clear();
     before.clear();for(var ball:observed.values())if(!ball.isRemoved()&&!finished.contains(ball.getUUID())){
      var row=snapshot(ball);if(ball instanceof CommittedSoutouFireball profile&&profile.flight()!=null&&!profile.flight().normalized()&&profile.flight().index()<profile.flight().path().points().size()-1){var v=profile.flight().path().velocity(profile.flight().index());row.addProperty("dx",v.x());row.addProperty("dy",v.y());row.addProperty("dz",v.z());}before.put(ball.getUUID(),row);
     }
    }else for(var ball:observed.values()){
     if(ball.isRemoved()||finished.contains(ball.getUUID()))continue;if(mc.level.getEntity(ball.getId())!=ball){clientError="CLIENT_ENTITY_IDENTITY";return;}int generation=generations.get(ball.getUUID());if(generation==1&&!original.get(ball.getUUID()).isRemoved()){clientError="OLD_CLIENT_REVIVED";return;}var row=snapshot(ball);row.addProperty("generation",generation);row.addProperty("oldRemoved",generation==1&&original.get(ball.getUUID()).isRemoved());row.add("before",before.get(ball.getUUID()));row.addProperty("frame",frame);row.addProperty("record","ROW");enqueue(row);
    }
   }catch(Exception error){clientError="Client:"+error.getClass().getSimpleName()+":"+error.getMessage();}
  }
 }
}

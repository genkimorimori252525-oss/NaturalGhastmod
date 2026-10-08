package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
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

/** Private ordinary network delivery while a new test owner's native tracker is absent. */
@Mod("naturalghast_delayed_owner_probe")
public final class DelayedOwnerProbe {
 private static final String SCOPE="CONTROLLED_OWNER_AVAILABILITY_NOT_GENUINE_LATE_JOIN";
 private static final ConcurrentMap<UUID,String> names=new ConcurrentHashMap<>();
 private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queueSize=new AtomicInteger();
 private static volatile boolean active;private static volatile String clientError,stage="INITIAL";private static volatile UUID ownerUuid;private static volatile int ownerId;private static volatile boolean returned;
 private final Map<String,StandardSoutouFireball> balls=new LinkedHashMap<>();private final TrackerReaddIntent intent=new TrackerReaddIntent();
 private final JsonArray spawns=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),serverRows=new JsonArray(),cleanup=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request,ownerInitial,ownerStop,ownerAbsence,ownerReadd,ownerJoin,deflection,bossReturn;private SoutouGhast owner;private Path output;private long deadline,removedTick;private int samples;private boolean done,ownedRemoval,canonicalPlayer,initialClientOwner;
 public DelayedOwnerProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_DELAYED_OWNER"));}
 @SubscribeEvent public void tracking(PlayerEvent.StartTracking event){
  if(!active||done)return;var entity=event.getTarget();
  try{
   if(entity==owner){require(event.getEntity().getUUID().toString().equals(request.get("playerUuid").getAsString()),"OWNER_TRACKING_PLAYER");var row=ownerSnapshot(entity);if(stage.equals("INITIAL")){require(ownerInitial==null,"DUPLICATE_INITIAL_TRACKING");ownerInitial=row;append("ownerInitial.jsonl",row);}else{require(stage.equals("READDED")&&ownedRemoval,"OWNED_READMISSION");ownedRemoval=false;append("ownerPairing.jsonl",row);}}
   else if(entity instanceof StandardSoutouFireball ball&&names.containsKey(ball.getUUID())){var row=snapshot(ball);spawns.add(row);append("spawns.jsonl",row);}
  }catch(Exception e){errors.add("Tracking:"+e.getMessage());}
 }
 @SubscribeEvent public void untracking(PlayerEvent.StopTracking event){
  if(!active||done||event.getTarget()!=owner)return;
  try{require(stage.equals("HIDING")&&event.getEntity().getUUID().toString().equals(request.get("playerUuid").getAsString()),"CONTROLLED_OWNER_STOP");ownedRemoval=true;ownerStop=ownerSnapshot(owner);ownerStop.addProperty("owned",true);append("ownerStop.jsonl",ownerStop);}catch(Exception e){errors.add("Stop:"+e.getMessage());}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/delayed-owner");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_DELAYED_OWNER_NONCE"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_DELAYED_OWNER_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+20000;Files.createDirectories(output);active=true;
  }
  require(samples++<240&&System.currentTimeMillis()<deadline&&clientError==null&&errors.isEmpty(),"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var canonical=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(canonical instanceof SoutouGhast&&canonical.isAlive()&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PARTICIPANTS");canonicalPlayer=true;
  drain();
  if(owner==null){owner=ModEntities.SOUTOU_GHAST.get().create(level);require(owner!=null,"CREATE_OWNER");owner.setPos(38,232,38);ownerUuid=owner.getUUID();ownerId=owner.getId();require(level.noCollision(owner,owner.getBoundingBox())&&level.addFreshEntity(owner),"NATIVE_NEW_OWNER");return;}
  require(!owner.isRemoved()&&level.getEntity(ownerUuid)==owner&&level.hasChunkAt(owner.blockPosition()),"SAME_ACTIVE_LOADED_OWNER");
  if(stage.equals("INITIAL")&&initialClientOwner&&balls.isEmpty()){addProfile(level,"RETURNED_CURVE",new FlightVector(15,232,5),new FlightVector(49,232,5));return;}
  var curve=(CommittedSoutouFireball)balls.get("RETURNED_CURVE");
  if(curve!=null&&deflection==null&&count("RETURNED_CURVE","INITIAL")>=2){require(curve.hurt(player.damageSources().playerAttack(player),1)&&curve.flight().normalized()&&curve.getOwner()==player,"ACTUAL_PLAYER_API_DEFLECTION");deflection=snapshot(curve);deflection.addProperty("synthetic",true);append("deflection.jsonl",deflection);}
  if(stage.equals("INITIAL")&&deflection!=null&&count("RETURNED_CURVE","PLAYER")>=4){stage="HIDING";removedTick=level.getGameTime();level.getChunkSource().removeEntity(owner);require(ownedRemoval,"ACTUAL_OWNER_STOP_ACK");}
  else if(stage.equals("HIDING")&&ownerAbsence!=null){
   stage="HIDDEN";
   add(level,"STANDARD",new StandardSoutouFireball(owner,new Vec3(3,236,20),new Vec3(1,0,0)));
   add(level,"GROUND",new GroundSoutouFireball(owner,new Vec3(3,236,27),new Vec3(1,0,0)));
   addProfile(level,"CURVE",new FlightVector(3,236,12),new FlightVector(49,236,12));
   require(curve.returnByBoss(owner,new Vec3(1,0,0))&&curve.getOwner()==owner&&curve.flight().normalized(),"ACTUAL_BOSS_RETURN");bossReturn=snapshot(curve);bossReturn.addProperty("synthetic",true);returned=true;append("bossReturn.jsonl",bossReturn);
  }else if(stage.equals("HIDDEN")&&balls.keySet().stream().allMatch(n->count(n,"HIDDEN")>=4)){
   intent.execute(()->{stage="READDED";ownerReadd=ownerSnapshot(owner);ownerReadd.addProperty("registered",true);append("ownerReadd.jsonl",ownerReadd);},()->level.getChunkSource().addEntity(owner));require(!ownedRemoval,"ACTUAL_OWNER_READMISSION_ACK");
  }
  if(stage.equals("HIDING")||stage.equals("HIDDEN"))require(level.getGameTime()-removedTick<=40,"BOUNDED_OWNER_GAP");
  for(var ball:balls.values()){require(!ball.isRemoved()&&level.getEntity(ball.getUUID())==ball&&level.hasChunkAt(ball.blockPosition()),"SAME_LIVE_LOADED_PROJECTILE");var row=snapshot(ball);row.addProperty("registered",true);row.addProperty("loaded",true);row.addProperty("alive",true);serverRows.add(row);require(serverRows.size()<=1536,"SERVER_ROW_BOUND");append("serverRows.jsonl",row);}
  if(stage.equals("READDED")&&ownerJoin!=null&&balls.keySet().stream().allMatch(n->count(n,"RESOLVED")>=4))finish("PASS");
 }
 private long count(String name,String phase){return clientRows.asList().stream().filter(r->{var x=r.getAsJsonObject();return x.get("name").getAsString().equals(name)&&x.get("phase").getAsString().equals(phase);}).count();}
 private void addProfile(ServerLevel level,String name,FlightVector from,FlightVector to)throws IOException{var path=CommittedTrajectory.curve(from,to,CommittedTrajectory.Strength.NORMAL,1);var ball=new CommittedSoutouFireball(owner,path);var proof=CommittedPathClearance.validate(level,ball,path);require(proof.result().clear(),"LOADED_FULL_PREFLIGHT");ball.setPreflight(proof);add(level,name,ball);}
 private void add(ServerLevel level,String name,StandardSoutouFireball ball)throws IOException{names.put(ball.getUUID(),name);balls.put(name,ball);require(level.noCollision(ball,ball.getBoundingBox())&&level.addFreshEntity(ball),"NATIVE_PROJECTILE_CREATION");}
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queueSize.decrementAndGet();String kind=row.remove("record").getAsString();switch(kind){case "OWNER_INITIAL"->initialClientOwner=true;case "OWNER_ABSENCE"->{require(ownerAbsence==null,"DUPLICATE_ABSENCE");ownerAbsence=row;append("ownerAbsence.jsonl",row);}case "OWNER_JOIN"->{require(ownerJoin==null,"DUPLICATE_READMISSION");ownerJoin=row;append("ownerJoin.jsonl",row);}case "JOIN"->{joins.add(row);append("joins.jsonl",row);}default->{clientRows.add(row);require(clientRows.size()<=2048,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}}
 private void finish(String status)throws IOException{
  restoreTracker();active=false;drain();done=true;var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("overrides",false);
  result.add("ownerInitial",ownerInitial);result.add("ownerStop",ownerStop);result.add("ownerAbsence",ownerAbsence);result.add("ownerReadd",ownerReadd);result.add("ownerJoin",ownerJoin);result.add("deflection",deflection);result.add("bossReturn",bossReturn);result.add("spawns",spawns);result.add("joins",joins);result.add("clientRows",clientRows);result.add("serverRows",serverRows);result.add("cleanup",cleanup);if(clientError!=null)errors.add(clientError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private void restoreTracker(){
  if(!ownedRemoval)return;var row=new JsonObject();
  try{require(intent.canCleanup(),"UNCERTAIN_ADD_NOT_REPEATED");if(owner.isRemoved())row.addProperty("status","ALREADY_REMOVED");else{intent.execute(()->{require(owner.level() instanceof ServerLevel level&&level.getEntity(ownerUuid)==owner&&level.hasChunkAt(owner.blockPosition()),"CLEANUP_OWNER_IDENTITY");stage="READDED";},()->((ServerLevel)owner.level()).getChunkSource().addEntity(owner));require(!ownedRemoval,"CLEANUP_START_ACK");row.addProperty("status","RESTORED");}}catch(Exception e){row.addProperty("status","FAIL");row.addProperty("error",e.getMessage());errors.add("Cleanup:"+e.getMessage());}cleanup.add(row);
 }
 private static JsonObject ownerSnapshot(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("epoch",System.currentTimeMillis());row.addProperty("tick",e.level().getGameTime());return row;}
 private static JsonObject snapshot(StandardSoutouFireball ball){
  var row=ownerSnapshot(ball);row.addProperty("name",names.get(ball.getUUID()));row.addProperty("owner",ball.getOwner()==null?"":ball.getOwner().getUUID().toString());row.addProperty("age",ball.provenance().age());row.addProperty("inWater",ball.isInWater());row.addProperty("x",ball.getX());row.addProperty("y",ball.getY());row.addProperty("z",ball.getZ());row.addProperty("vx",ball.getDeltaMovement().x);row.addProperty("vy",ball.getDeltaMovement().y);row.addProperty("vz",ball.getDeltaMovement().z);row.addProperty("px",ball.xPower);row.addProperty("py",ball.yPower);row.addProperty("pz",ball.zPower);
  if(ball instanceof CommittedSoutouFireball p&&p.flight()!=null){var data=CommittedTrajectoryCodec.write(p.flight());data.remove("Index");data.remove("Normalized");row.addProperty("path",data.toString());row.addProperty("index",p.flight().index());row.addProperty("normalized",p.flight().normalized());}else{row.addProperty("path","");row.addProperty("index",0);row.addProperty("normalized",false);}return row;
 }
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>256){queueSize.decrementAndGet();clientError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("DELAYED_OWNER_"+reason);}
 @Mod.EventBusSubscriber(modid="naturalghast_delayed_owner_probe",value=Dist.CLIENT)
 public static final class Client {
  private static final Map<UUID,StandardSoutouFireball> pending=new LinkedHashMap<>(),observed=new LinkedHashMap<>();private static final Map<UUID,JsonObject> before=new HashMap<>();private static Entity originalOwner,pendingOwner;private static boolean absenceSent;private static int frame;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){
   if(!active||!event.getLevel().isClientSide())return;var e=event.getEntity();
   if(e.getUUID().equals(ownerUuid)){if(event.isCanceled()||pendingOwner!=null||originalOwner!=null&&(!absenceSent||originalOwner==e||!originalOwner.isRemoved())){clientError="OWNER_JOIN_IDENTITY";return;}pendingOwner=e;}
   else if(e instanceof StandardSoutouFireball ball&&names.containsKey(ball.getUUID())){if(event.isCanceled()||observed.containsKey(ball.getUUID())||pending.putIfAbsent(ball.getUUID(),ball)!=null)clientError="DUPLICATE_PROJECTILE_JOIN";}
  }
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
   if(!active||!enabled())return;
   try{
    var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
    if(event.phase==TickEvent.Phase.START){
     frame++;
     if(originalOwner!=null&&!absenceSent&&originalOwner.isRemoved()&&mc.level.getEntity(ownerId)==null){var row=ownerSnapshot(originalOwner);row.addProperty("oldRemoved",true);row.addProperty("idAbsent",true);row.addProperty("record","OWNER_ABSENCE");enqueue(row);absenceSent=true;}
     if(pendingOwner!=null){var row=ownerSnapshot(pendingOwner);row.addProperty("distinct",originalOwner!=null&&pendingOwner!=originalOwner);row.addProperty("record",originalOwner==null?"OWNER_INITIAL":"OWNER_JOIN");enqueue(row);if(originalOwner==null)originalOwner=pendingOwner;pendingOwner=null;}
     for(var ball:pending.values()){var row=snapshot(ball);row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(ball).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed.put(ball.getUUID(),ball);}pending.clear();before.clear();
     for(var ball:observed.values())if(!ball.isRemoved()){var row=snapshot(ball);if(ball instanceof CommittedSoutouFireball p&&p.flight()!=null&&!p.flight().normalized()&&p.flight().index()<p.flight().path().points().size()-1){var v=p.flight().path().velocity(p.flight().index());row.addProperty("dx",v.x());row.addProperty("dy",v.y());row.addProperty("dz",v.z());}before.put(ball.getUUID(),row);}
    }else for(var ball:observed.values()){
     if(ball.isRemoved())continue;require(mc.level.getEntity(ball.getId())==ball,"CLIENT_ENTITY_IDENTITY");var row=snapshot(ball);String name=names.get(ball.getUUID());boolean update=name.equals("RETURNED_CURVE")?returned&&Math.abs(ball.xPower-.1)<1e-9:!stage.equals("INITIAL");boolean absent=mc.level.getEntity(ownerId)==null;
     String phase=update?(absent?"HIDDEN":"RESOLVED"):ball instanceof CommittedSoutouFireball p&&p.flight().normalized()?"PLAYER":"INITIAL";
     row.addProperty("phase",phase);row.addProperty("ownerAbsent",absent);row.add("before",before.get(ball.getUUID()));row.addProperty("frame",frame);row.addProperty("record","ROW");enqueue(row);
    }
   }catch(Exception e){clientError="Client:"+e.getClass().getSimpleName()+":"+e.getMessage();}
  }
 }
}

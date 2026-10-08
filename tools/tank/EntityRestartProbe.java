package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Private clean-stop persistence pair; phase B only observes genuine native disk loads. */
@Mod("naturalghast_entity_restart_probe")
public final class EntityRestartProbe {
 private static final String SCOPE="CLEAN_STOP_NEW_PROCESS_NATIVE_ENTITY_PERSISTENCE_NOT_POWER_LOSS_OR_INPUT";
 private static final String[] NAMES={"BOSS","BURST","CURVE","LOB","BOMB","NORMALIZED_CURVE","STANDARD","GROUND"};
 private final String phase=System.getenv("KNEEKURA_DEBUG_ENTITY_RESTART_PHASE");
 private final Map<String,Entity> entities=new LinkedHashMap<>();private final Map<String,Long> joinTicks=new HashMap<>();private final Set<String> removed=new HashSet<>();
 private final JsonArray loaded=new JsonArray(),saved=new JsonArray(),rows=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request;private Path output,world;private long start,deadline;private int samples;private boolean haltRequested,stopping,done,canonicalPlayer;
 public EntityRestartProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private boolean enabled(){return "A".equals(phase)||"B".equals(phase);}
 private boolean initialize(ServerLevel level)throws IOException{
  if(request!=null)return true;
  Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/entity-restart");
  Path file="A".equals(phase)?output.resolve("request.json"):Path.of(System.getenv("KNEEKURA_DEBUG_ENTITY_RESTART_EXPECTED"));if(!Files.exists(file))return false;
  require(!Files.isSymbolicLink(file)&&Files.size(file)<=131072,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
  var keys=new HashSet<>(Set.of("phase","nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs"));keys.add("A".equals(phase)?"dispatchEpochMs":"prior");
  require(request.keySet().equals(keys)&&request.get("phase").getAsString().equals(phase)&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_ENTITY_RESTART_NONCE"))&&request.get("maxWallMs").getAsInt()==15000&&request.get("maxTicks").getAsInt()==("A".equals(phase)?120:180),"REQUEST_IDENTITY");
  world=Path.of(System.getenv("KNEEKURA_DEBUG_ENTITY_RESTART_WORLD")).toRealPath();require(world.equals(Path.of(request.get("world").getAsString()).toRealPath())&&world.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
  require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");
  start=level.getGameTime();long now=System.currentTimeMillis();
  if("A".equals(phase)){long dispatch=request.get("dispatchEpochMs").getAsLong();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_EXPIRED");deadline=Math.addExact(dispatch,15000);}
  else{var prior=request.getAsJsonObject("prior");require(prior.get("phase").getAsString().equals("A")&&prior.get("scope").getAsString().equals(SCOPE)&&prior.get("nonce").getAsString().equals(request.get("nonce").getAsString())&&prior.get("processStart").getAsLong()<ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli()&&prior.get("serverStopped").getAsBoolean()&&prior.get("worldClosed").getAsBoolean()&&prior.get("status").getAsString().equals("STOPPED"),"PRIOR_NATIVE_STOP");require(prior.getAsJsonArray("saved").size()==8,"EXPECTED_ENTITY_SET");deadline=Math.addExact(now,15000);}
  Files.createDirectories(output);return true;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void join(EntityJoinLevelEvent event){
  if(!enabled()||!"B".equals(phase)||done||!(event.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread())return;
  try{
   if(!initialize(level))return;var expected=expected(event.getEntity().getUUID());if(expected==null)return;
   require(!event.isCanceled()&&entities.size()<8,"NATIVE_JOIN_ADMISSION");String name=expected.get("name").getAsString();require(!entities.containsKey(name),"DUPLICATE_NATIVE_JOIN");
   var actual=snapshot(name,event.getEntity());actual.addProperty("joinTick",level.getGameTime());
   require(TagParser.parseTag(expected.get("state").getAsString()).equals(TagParser.parseTag(actual.get("state").getAsString())),"NATIVE_STOP_LOAD_STATE_"+name);
   if(name.equals("BOSS")){
    var boss=(SoutouGhast)event.getEntity();boolean safe=boss.getTarget()==null&&!boss.isCharging()&&boss.getGroundCombat().state().phase()==GroundCombat.Phase.AIR&&boss.getStandardAttack().state().phase()==StandardAttack.Phase.IDLE&&!boss.getOverheadAttack().active()&&!boss.getDomainAttack().active();
    require(safe,"SAFE_TRANSIENT_BOSS_RESET");actual.addProperty("bossSafeReset",safe);
   }
   entities.put(name,event.getEntity());joinTicks.put(name,level.getGameTime());loaded.add(actual);append("loaded.jsonl",actual);
  }catch(Exception error){errors.add("Join:"+error.getClass().getSimpleName()+":"+error.getMessage());}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||!enabled()||done||haltRequested)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception error){errors.add(error.getClass().getSimpleName()+":"+error.getMessage());haltRequested=true;server.halt(false);}
 }
 private void step(ServerLevel level)throws IOException{
  if(!initialize(level))return;require(samples++<request.get("maxTicks").getAsInt()&&System.currentTimeMillis()<deadline,"FINITE_WINDOW");
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  if(player!=null){require(!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.level()==level&&!player.isCreative()&&!player.isSpectator()&&player.isAlive(),"GENUINE_PLAYER");canonicalPlayer=true;}
  if("A".equals(phase)&&entities.isEmpty()){
   require(canonicalPlayer,"LIVE_CREATION_PLAYER");var entity=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));require(entity instanceof SoutouGhast&&entity.isAlive(),"LIVE_NATIVE_BOSS");create(level,(SoutouGhast)entity,player);return;
  }
  for(var entry:entities.entrySet()){
   var entity=entry.getValue();if(removed.contains(entry.getKey()))continue;
   require(entity.isRemoved()||level.getEntity(entity.getUUID())==entity,"NATIVE_REGISTERED_ENTITY");var row=row(level,entry.getKey(),entity);rows.add(row);require(rows.size()<=2048,"ROW_BOUND");append("rows.jsonl",row);if(entity.isRemoved())removed.add(entry.getKey());
  }
  if("A".equals(phase)){
   require(entities.size()==8&&entities.values().stream().noneMatch(Entity::isRemoved),"PRE_STOP_PREMATURE_REMOVAL");
   var burst=(CommittedSoutouFireball)entities.get("BURST");if(burst.flight().index()==9){for(String name:new String[]{"BURST","CURVE","LOB","BOMB"}){var ball=(CommittedSoutouFireball)entities.get(name);require(ball.flight().index()==9&&ball.flight().path().points().size()-1-ball.flight().index()>=4,"REMAINING_FINITE_PATH");}haltRequested=true;level.getServer().halt(false);}
  }else{
   boolean complete=entities.size()==8&&canonicalPlayer&&Set.of("BURST","CURVE","LOB","BOMB").stream().allMatch(removed::contains);
   for(String name:new String[]{"NORMALIZED_CURVE","STANDARD","GROUND"}){
    var expected=expectedName(name);var entity=entities.get(name);complete&=entity instanceof StandardSoutouFireball ball&&ball.provenance().age()>=expected.get("age").getAsInt()+4;
   }
   // The real client must publish READY before halt, so the ordinary supervisor can close evidence.
   if(complete&&Files.isRegularFile(Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("ready.json"),LinkOption.NOFOLLOW_LINKS)){haltRequested=true;level.getServer().halt(false);}
  }
 }
 private void create(ServerLevel level,SoutouGhast boss,ServerPlayer player)throws IOException{
  entities.put("BOSS",boss);
  for(String name:Arrays.copyOfRange(NAMES,1,NAMES.length)){
   StandardSoutouFireball ball;
   if(name.equals("STANDARD")||name.equals("GROUND"))ball=name.equals("STANDARD")?new StandardSoutouFireball(boss,new Vec3(3,236,20),new Vec3(1,0,0)):new GroundSoutouFireball(boss,new Vec3(3,236,27),new Vec3(1,0,0));
   else{
    CommittedTrajectory path=switch(name){
     case "BURST" -> CommittedTrajectory.burst(new FlightVector(3,236,5),new FlightVector(49,236,5));
     case "CURVE" -> CommittedTrajectory.curve(new FlightVector(3,236,12),new FlightVector(49,236,12),CommittedTrajectory.Strength.NORMAL,1);
     case "LOB" -> CommittedTrajectory.lob(new FlightVector(3.5,236,45.5),new FlightVector(49.5,223.75,45.5),6);
     case "BOMB" -> CommittedTrajectory.bomb(new FlightVector(45.5,246,35.5),new FlightVector(45.5,223.75,35.5));
     default -> CommittedTrajectory.curve(new FlightVector(3,230,5),new FlightVector(49,230,5),CommittedTrajectory.Strength.NORMAL,1);
    };
    var profile=new CommittedSoutouFireball(boss,path);var proof=CommittedPathClearance.validate(level,profile,path);require(proof.result().clear()&&path.points().size()-1>=13,"LOADED_FULL_PREFLIGHT_"+name);profile.setPreflight(proof);ball=profile;
    if(name.equals("NORMALIZED_CURVE"))require(ball.hurt(player.damageSources().playerAttack(player),1)&&profile.flight().normalized()&&ball.isPlayerDeflected(),"SYNTHETIC_REAL_PLAYER_NORMALIZATION");
   }
   require(level.noCollision(ball,ball.getBoundingBox())&&level.addFreshEntity(ball),"NATIVE_REGISTER_"+name);entities.put(name,ball);
  }
 }
 private JsonObject snapshot(String name,Entity entity)throws IOException{
  var tag=new CompoundTag();require(entity.save(tag),"ACTUAL_ENTITY_SAVE");var selected=new CompoundTag();
  String[] fields=entity instanceof SoutouGhast?new String[]{"UUID","Pos","Motion","Health","Temperament","OrbitDirection","OrbitAngleOffset","PreferredAltitudeOffset","LastSeenX","LastSeenY","LastSeenZ","LastSeenTicks"}:new String[]{"UUID","Pos","Motion","power","Owner","StandardOrigin","StandardDeflector","PlayerDeflected","BossAttempted","BossReturns","StandardAge","CommittedProfile","ProfilePreflight"};
  for(String key:fields)if(tag.contains(key))selected.put(key,tag.get(key).copy());
  var value=new JsonObject();value.addProperty("name",name);value.addProperty("uuid",entity.getUUID().toString());value.addProperty("type",EntityType.getKey(entity.getType()).toString());value.addProperty("state",selected.toString());value.addProperty("ownerUuid",tag.hasUUID("Owner")?tag.getUUID("Owner").toString():"");value.addProperty("x",entity.getX());value.addProperty("y",entity.getY());value.addProperty("z",entity.getZ());
  value.addProperty("age",entity instanceof StandardSoutouFireball ball?ball.provenance().age():0);value.addProperty("index",entity instanceof CommittedSoutouFireball profile?profile.flight().index():0);value.addProperty("normalized",entity instanceof CommittedSoutouFireball profile&&profile.flight().normalized());value.addProperty("kind",entity instanceof CommittedSoutouFireball profile?profile.flight().path().kind().name():"NONE");
  if(entity instanceof CommittedSoutouFireball profile){var points=new JsonArray();for(var p:profile.flight().path().points()){var point=new JsonArray();point.add(p.x());point.add(p.y());point.add(p.z());points.add(point);}value.add("points",points);}return value;
 }
 private JsonObject row(ServerLevel level,String name,Entity entity)throws IOException{
  var value=new JsonObject();value.addProperty("name",name);value.addProperty("uuid",entity.getUUID().toString());value.addProperty("tick",level.getGameTime());value.addProperty("x",entity.getX());value.addProperty("y",entity.getY());value.addProperty("z",entity.getZ());value.addProperty("alive",!entity.isRemoved());value.addProperty("age",entity instanceof StandardSoutouFireball ball?ball.provenance().age():0);value.addProperty("index",entity instanceof CommittedSoutouFireball profile?profile.flight().index():0);value.addProperty("normalized",entity instanceof CommittedSoutouFireball profile&&profile.flight().normalized());
  Entity owner=entity instanceof StandardSoutouFireball ball?ball.getOwner():null;value.addProperty("ownerUuid",owner==null?"":owner.getUUID().toString());value.addProperty("ownerResolved",owner!=null||name.equals("BOSS"));
  var expected=expectedName(name);value.addProperty("ownerAvailable",expected==null||expected.get("ownerUuid").getAsString().isEmpty()||level.getEntity(UUID.fromString(expected.get("ownerUuid").getAsString()))!=null);return value;
 }
 private JsonObject expected(UUID uuid){if(request==null||!"B".equals(phase))return null;for(var row:request.getAsJsonObject("prior").getAsJsonArray("saved")){var value=row.getAsJsonObject();if(value.get("uuid").getAsString().equals(uuid.toString()))return value;}return null;}
 private JsonObject expectedName(String name){if(request==null||!"B".equals(phase))return null;for(var row:request.getAsJsonObject("prior").getAsJsonArray("saved")){var value=row.getAsJsonObject();if(value.get("name").getAsString().equals(name))return value;}return null;}
 @SubscribeEvent public void stopping(ServerStoppingEvent event){
  if(!enabled()||done)return;
  try{
   require(request!=null&&haltRequested,"REQUESTED_NATIVE_STOP");stopping=true;
   if("A".equals(phase)){require(entities.size()==8,"STOP_ENTITY_SET");for(var entry:entities.entrySet()){require(!entry.getValue().isRemoved(),"STOP_PREMATURE_REMOVAL");var value=snapshot(entry.getKey(),entry.getValue());saved.add(value);append("saved.jsonl",value);}}
  }catch(Exception error){errors.add("Stopping:"+error.getClass().getSimpleName()+":"+error.getMessage());}
 }
 @SubscribeEvent public void stopped(ServerStoppedEvent event){
  if(!enabled()||done||request==null)return;
  try{
   boolean closed=false;try(var channel=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.READ);var lock=channel.tryLock(0,Long.MAX_VALUE,true)){closed=lock!=null;}
   require(stopping&&closed,"NATIVE_WORLD_CLOSED");var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("phase",phase);result.addProperty("status",errors.isEmpty()?"STOPPED":"FAIL");result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("pid",ProcessHandle.current().pid());result.addProperty("processStart",ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli());result.addProperty("serverStopped",true);result.addProperty("worldClosed",closed);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("overrides",false);result.addProperty("samples",samples);result.add("saved",saved);result.add("loaded",loaded);result.add("rows",rows);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);done=true;
  }catch(Exception error){try{Files.writeString(output.resolve("stop-failure.json"),new Gson().toJson(Map.of("phase",phase,"error",error.getClass().getSimpleName()+":"+error.getMessage()))+"\n",StandardOpenOption.CREATE_NEW);}catch(IOException ignored){}done=true;}
 }
 private void append(String file,JsonObject value)throws IOException{Files.writeString(output.resolve(file),value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("ENTITY_RESTART_"+reason);}
}

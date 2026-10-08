package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Private explicitly requested save/reload reliability; never manipulates client input. */
@Mod("naturalghast_profile_reload_probe")
public final class ProfileReloadProbe {
 private static final String SCOPE="EXPLICIT_INSTANTIATED_PROFILE_RELOAD_NOT_DURABLE_RESTART_OR_MELEE";
 private static final String[] NAMES={"DEFLECTED_CURVE","BURST","CURVE","LOB","MISSING_PROFILE","INVALID_INDEX","SHIFTED_POSITION","INCONSISTENT_DEFLECTION"};
 private JsonObject request;private Path output;private long start,deadline;private int samples,rows,caseIndex,nativeTicks;private boolean done,waiting,loaded;
 private CommittedSoutouFireball ball;private CompoundTag saved;private JsonObject descriptor;private Vec3 savedPosition,savedVelocity;private double[] savedPower;private long discardedTick;
 private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
 public ProfileReloadProbe(){MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!"1".equals(System.getenv("KNEEKURA_DEBUG_PROFILE_RELOAD")))return;
  var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception error){failures.add(error.getClass().getSimpleName()+":"+error.getMessage());if(ball!=null&&!ball.isRemoved())ball.discard();try{finish(server.overworld(),"FAIL");}catch(IOException writeFailure){done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/profile-reload");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"REQUEST_PATH_SIZE");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("maxTicks").getAsInt()==180&&request.get("maxWallMs").getAsInt()==15000&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_PROFILE_RELOAD_NONCE")),"REQUEST_SCOPE");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_PROFILE_RELOAD_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");
   long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_EXPIRED");deadline=Math.addExact(dispatch,15000);start=level.getGameTime();Files.createDirectories(output);
  }
  require(samples++<180&&System.currentTimeMillis()<deadline,"BOUNDED_WINDOW");
  var subject=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(subject instanceof SoutouGhast&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.level()==level&&!player.isCreative()&&!player.isSpectator(),"GENUINE_PARTICIPANTS");
  var boss=(SoutouGhast)subject;
  if(ball==null){create(level,boss,player);return;}
  if(waiting){
   require(level.getGameTime()==discardedTick+1&&level.getEntity(ball.getUUID())==null,"UNREGISTER_BEFORE_RELOAD");
   Files.writeString(output.resolve(NAMES[caseIndex]+"-load-input.snbt"),saved.toString()+"\n",StandardOpenOption.CREATE_NEW);
   var restored=EntityType.loadEntityRecursive(saved.copy(),level,e->e);require(restored instanceof CommittedSoutouFireball,"REGISTERED_ENTITY_RELOAD");ball=(CommittedSoutouFireball)restored;
   var checks=new JsonObject();boolean negative=caseIndex>=4;
   if(!negative){
    checks.addProperty("stateEqual",ball.position().distanceToSqr(savedPosition)<1e-12&&ball.getDeltaMovement().distanceToSqr(savedVelocity)<1e-12&&ball.xPower==savedPower[0]&&ball.yPower==savedPower[1]&&ball.zPower==savedPower[2]&&CommittedTrajectoryCodec.write(ball.flight()).equals(saved.getCompound("CommittedProfile")));
    checks.addProperty("ownerResolved",ball.getOwner()==(caseIndex==0?player:boss));checks.addProperty("preflightEqual",ball.preflight().equals(saved.getCompound("ProfilePreflight")));
    var again=new CompoundTag();ball.provenance().write(again);var original=new CompoundTag();StandardProvenance.read(saved).write(original);checks.addProperty("provenanceEqual",again.equals(original));checks.addProperty("unregisteredBeforeLoad",true);
    for(String key:checks.keySet())require(checks.get(key).getAsBoolean(),"RELOAD_"+key);
   }
   descriptor.add("checks",checks);require(level.addFreshEntity(ball),"RELOAD_ADD");waiting=false;loaded=true;nativeTicks=0;row(level,"LOAD");return;
  }
  if(caseIndex>=4){require(ball.isRemoved(),"INVALID_NOT_DISCARDED_NEXT_TICK");row(level,"REMOVED");next();return;}
  if(ball.isRemoved()){
   require(loaded&&nativeTicks>0&&ball.flight().index()==ball.flight().path().points().size()-1,"PREMATURE_PROFILE_REMOVAL");row(level,"REMOVED");next();return;
  }
  if(loaded){
   nativeTicks++;row(level,"NATIVE");
   if(caseIndex==0&&nativeTicks==4){require(ball.flight().normalized()&&ball.flight().index()==0&&ball.position().distanceToSqr(savedPosition)>.1,"NORMALIZED_RELOAD_CONTINUATION");ball.discard();row(level,"REMOVED");next();}
  }else if(ball.flight().index()==9){save(level);row(level,"SAVE");ball.discard();discardedTick=level.getGameTime();waiting=true;}
  else row(level,"PRE_SAVE");
 }
 private void create(ServerLevel level,SoutouGhast boss,ServerPlayer player)throws IOException{
  if(caseIndex==NAMES.length){finish(level,"PASS");return;}
  int lane=caseIndex==1?5:caseIndex==3?45:12;var from=new FlightVector(caseIndex==0?26:3,caseIndex==0?238:236,caseIndex==0?42:lane);
  var to=new FlightVector(caseIndex==0?46:49,caseIndex==3?223.75:from.y(),from.z());
  var path=caseIndex==1?CommittedTrajectory.burst(from,to):caseIndex==3?CommittedTrajectory.lob(from,to,6):CommittedTrajectory.curve(from,to,CommittedTrajectory.Strength.NORMAL,1);
  ball=new CommittedSoutouFireball(boss,path);var proof=CommittedPathClearance.validate(level,ball,path);require(proof.result().clear(),"LOADED_PREFLIGHT_"+proof.result().reason());ball.setPreflight(proof);
  descriptor=new JsonObject();descriptor.addProperty("name",NAMES[caseIndex]);descriptor.addProperty("uuid",ball.getUUID().toString());descriptor.addProperty("kind",path.kind().name());descriptor.addProperty("negative",caseIndex>=4);descriptor.addProperty("normalized",caseIndex==0);
  var points=new JsonArray();for(var p:path.points()){var coordinates=new JsonArray();coordinates.add(p.x());coordinates.add(p.y());coordinates.add(p.z());points.add(coordinates);}descriptor.add("points",points);cases.add(descriptor);
  if(caseIndex==0){require(player.isAlive()&&ball.hurt(player.damageSources().playerAttack(player),1)&&ball.isPlayerDeflected()&&ball.flight().normalized(),"EXPLICIT_PLAYER_ATTACK_NORMALIZATION");}
  if(caseIndex==0||caseIndex>=4){
   save(level);if(caseIndex==0)row(level,"SAVE");
   if(caseIndex==4)saved.remove("CommittedProfile");if(caseIndex==5)saved.getCompound("CommittedProfile").putInt("Index",999);
   if(caseIndex==6){var pos=saved.getList("Pos",Tag.TAG_DOUBLE);pos.set(0,DoubleTag.valueOf(pos.getDouble(0)+1));}
   if(caseIndex==7){var p=ball.provenance();new StandardProvenance(p.origin(),player.getUUID(),true,p.bossAttempted(),p.returns(),p.age()).write(saved);}
   ball.discard();discardedTick=level.getGameTime();waiting=true;
  }else{require(level.addFreshEntity(ball),"ORIGINAL_ADD");row(level,"PRE_SAVE");}
 }
 private void save(ServerLevel level)throws IOException{
  saved=new CompoundTag();require(ball.save(saved),"ENTITY_SAVE");savedPosition=ball.position();savedVelocity=ball.getDeltaMovement();savedPower=new double[]{ball.xPower,ball.yPower,ball.zPower};descriptor.addProperty("savedIndex",ball.flight().index());
  Files.writeString(output.resolve(NAMES[caseIndex]+"-saved.snbt"),saved.toString()+"\n",StandardOpenOption.CREATE_NEW);
 }
 private void row(ServerLevel level,String event)throws IOException{
  require(rows++<1024,"ROW_BOUND");var r=new JsonObject();r.addProperty("case",NAMES[caseIndex]);r.addProperty("uuid",ball.getUUID().toString());r.addProperty("event",event);r.addProperty("tick",level.getGameTime());r.addProperty("index",ball.flight()==null?-1:ball.flight().index());r.addProperty("x",ball.getX());r.addProperty("y",ball.getY());r.addProperty("z",ball.getZ());r.addProperty("alive",!ball.isRemoved());r.addProperty("normalized",ball.flight()!=null&&ball.flight().normalized());r.addProperty("ownerUuid",ball.getOwner()==null?"":ball.getOwner().getUUID().toString());
  Files.writeString(output.resolve("rows.jsonl"),r+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
 }
 private void next(){ball=null;saved=null;waiting=false;loaded=false;nativeTicks=0;caseIndex++;}
 private void finish(ServerLevel level,String status)throws IOException{done=true;var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("status",status);result.addProperty("nonce",System.getenv("KNEEKURA_DEBUG_PROFILE_RELOAD_NONCE"));result.addProperty("startGameTime",start);result.addProperty("endGameTime",level.getGameTime());result.addProperty("rows",rows);result.addProperty("connectedPlayer",request!=null);result.add("cases",cases);result.add("failures",new Gson().toJsonTree(failures));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("PROFILE_RELOAD_"+reason);}
}

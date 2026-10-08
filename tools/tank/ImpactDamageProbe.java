package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Finite native impact/damage reliability, with explicitly injected production API sources. */
@Mod("naturalghast_impact_damage_probe")
public final class ImpactDamageProbe {
 private static final String SCOPE="EXPLICIT_NATIVE_IMPACT_DAMAGE_NOT_HUMAN_MELEE_OR_BALANCE";
 private static final String[] NAMES={"LOB_BLOCK","BURST_MISS","STANDARD_COW","GROUND_COW","OWN_RETURN","DEFLECTION_RULES","ATTRIBUTION_REJECTION"};
 private static final int[] ORDER={4,5,6,0,1,2,3};
 private JsonObject request,current;private Path output;private long start,deadline,caseStart,removedTick=-1;private int caseIndex=4,ordinal,samples,rows;private boolean done,eventError;
 private StandardSoutouFireball ball;private LivingEntity victim;private CommittedPathClearance.Proof preflight;private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
 private final List<Runnable> pendingEvents=new ArrayList<>();
 public ImpactDamageProbe(){MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!"1".equals(System.getenv("KNEEKURA_DEBUG_IMPACT_DAMAGE")))return;
  var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{flushEvents();step(server.overworld());}catch(Exception error){failures.add(error.getClass().getSimpleName()+":"+error.getMessage());if(ball!=null&&!ball.isRemoved())ball.discard();try{flushEvents();finish(server.overworld(),"FAIL");}catch(IOException writeFailure){done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/impact-damage");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"REQUEST_PATH_SIZE");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_IMPACT_DAMAGE_NONCE")),"REQUEST_SCOPE");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_IMPACT_DAMAGE_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_EXPIRED");deadline=Math.addExact(dispatch,20000);start=level.getGameTime();Files.createDirectories(output);
  }
  require(samples++<240&&System.currentTimeMillis()<deadline&&!eventError,"BOUND_OR_EVENT_WRITE");
  var subject=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(subject instanceof SoutouGhast&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.level()==level&&!player.isCreative()&&!player.isSpectator(),"GENUINE_PARTICIPANTS");var boss=(SoutouGhast)subject;
  if(current==null){if(caseIndex==NAMES.length){finish(level,"PASS");return;}begin(level,boss,player);return;}
  var row=new JsonObject();row.addProperty("case",NAMES[caseIndex]);row.addProperty("tick",level.getGameTime());row.addProperty("uuid",ball.getUUID().toString());row.addProperty("x",ball.getX());row.addProperty("y",ball.getY());row.addProperty("z",ball.getZ());row.addProperty("removed",ball.isRemoved());row.addProperty("victimHealth",victim==null?0:victim.getHealth());row.addProperty("playerHealth",player.getHealth());if(ball instanceof CommittedSoutouFireball profile)row.addProperty("index",profile.flight().index());append("rows.jsonl",row);
  require(level.getGameTime()-caseStart<90,"CASE_TIMEOUT");
  if(ball.isRemoved()){
   if(removedTick<0)removedTick=level.getGameTime();
   if(level.getGameTime()-removedTick>=(caseIndex==4?21:3)){end(level);}
  }
 }
 private void begin(ServerLevel level,SoutouGhast boss,ServerPlayer player)throws IOException{
  caseStart=level.getGameTime();removedTick=-1;preflight=null;victim=null;current=new JsonObject();current.addProperty("name",NAMES[caseIndex]);current.add("impacts",new JsonArray());current.add("explosions",new JsonArray());current.add("damage",new JsonArray());var checks=new JsonObject();current.add("checks",checks);cases.add(current);
  if(caseIndex<=1){var path=caseIndex==0?CommittedTrajectory.lob(new FlightVector(3.5,236,45.5),new FlightVector(49.5,223.75,45.5),6):CommittedTrajectory.burst(new FlightVector(3,236,5),new FlightVector(49,236,5));var profile=new CommittedSoutouFireball(boss,path);ball=profile;preflight=CommittedPathClearance.validate(level,ball,path);require(preflight.result().clear(),"LOADED_PREFLIGHT");profile.setPreflight(preflight);}
  else if(caseIndex==2||caseIndex==3){
   var cow=EntityType.COW.create(level);require(cow!=null,"COW_CREATE");cow.setPos(44,224,caseIndex==2?5:12);cow.setNoAi(true);require(!cow.isNoGravity()&&cow.getHealth()==10&&cow.getArmorValue()==0&&level.addFreshEntity(cow),"COW_NATIVE_SETUP");victim=cow;current.addProperty("controlledCowNoAI",true);current.addProperty("nativeGravity",true);
   Vec3 pos=new Vec3(38,225,caseIndex==2?5:12),dir=new Vec3(1,0,0);ball=caseIndex==2?new StandardSoutouFireball(boss,pos,dir):new GroundSoutouFireball(boss,pos,dir);
  }else{
   require(player.isAlive()&&boss.isAlive(),"LIVE_API_ACTORS");victim=boss;Vec3 direction=player.getLookAngle().normalize();require(direction.lengthSqr()>.9,"PLAYER_DIRECTION");
   Vec3 position=boss.getBoundingBox().getCenter().subtract(direction.scale(4.25));ball=new StandardSoutouFireball(boss,position,direction);
   if(caseIndex==4){require(ball.hurt(player.damageSources().playerAttack(player),1)&&ball.canBossReact(boss)&&ball.returnByBoss(boss,direction)&&ball.hurt(player.damageSources().playerAttack(player),1)&&ball.isOwnReturn(boss)&&ball.bossReturns()==1&&!ball.canBossReact(boss)&&!ball.returnByBoss(boss,direction),"RETURN_ROUNDTRIP_ONCE");require(!boss.isInvulnerableTo(level.damageSources().fireball(ball,player)),"ATTRIBUTED_FIRE_ADMISSION");}
   if(caseIndex==5){
    var before=ball.provenance();Vec3 velocity=ball.getDeltaMovement();var attack=player.damageSources().playerAttack(player);
    require(!ball.hurt(attack,0)&&!ball.hurt(attack,-1)&&!ball.hurt(level.damageSources().mobAttack(boss),1)&&!ball.hurt(level.damageSources().fireball(ball,player),1)&&ball.provenance().equals(before)&&ball.getDeltaMovement().equals(velocity)&&ball.getOwner()==boss,"INVALID_DEFLECTION_SOURCES");
    require(ball.hurt(attack,1)&&ball.returnByBoss(boss,direction)&&!ball.returnByBoss(boss,direction)&&ball.hurt(attack,1)&&ball.bossReturns()==1&&ball.isOwnReturn(boss)&&!ball.canBossReact(boss)&&!ball.returnByBoss(boss,direction),"BOUNDED_RALLY_RULES");
   }
   if(caseIndex==6){
    ball.setOwner(player);var source=level.damageSources().fireball(ball,player);float health=boss.getHealth();require(boss.isInvulnerableTo(source)&&!boss.hurt(source,20)&&boss.getHealth()==health,"UNATTRIBUTED_REJECT");
    var saved=new CompoundTag();ball.addAdditionalSaveData(saved);new StandardProvenance(UUID.randomUUID(),player.getUUID(),true,false,0,0).write(saved);ball.readAdditionalSaveData(saved);ball.setOwner(player);source=level.damageSources().fireball(ball,player);
    require(!ball.isOwnReturn(boss)&&boss.isInvulnerableTo(source)&&!boss.hurt(source,20)&&boss.getHealth()==health,"WRONG_ORIGIN_REJECT");
    checks.addProperty("sourceImmunity",true);
   }
  }
  current.addProperty("uuid",ball.getUUID().toString());current.addProperty("initialHealth",victim==null?0:victim.getHealth());current.addProperty("victimUuid",victim==null?"":victim.getUUID().toString());checks.addProperty("productionRules",true);
  if(caseIndex>=5){current.addProperty("finalHealth",victim.getHealth());current.addProperty("elapsedTicks",0);current.addProperty("ended",true);ball.discard();next();return;}
  require(level.addFreshEntity(ball),"PROJECTILE_REGISTER");
 }
 private void end(ServerLevel level)throws IOException{
  if(caseIndex==4){
   var source=level.damageSources().explosion(ball,ball.getOwner());float health=victim.getHealth();
   require(victim.invulnerableTime==0&&ball.wasDirectVictim(victim)&&ball.isAttributedOwnReturn(source,victim)&&!victim.isInvulnerableTo(source),"DELAYED_DUPLICATE_SOURCE_NOT_COOLDOWN_OR_IMMUNITY");
   require(!victim.hurt(source,20)&&victim.getHealth()==health,"DEDICATED_DELAYED_DUPLICATE_REJECTION");
   var checks=current.getAsJsonObject("checks");checks.addProperty("delayedDuplicateRejected",true);checks.addProperty("cooldownAtDuplicate",victim.invulnerableTime);checks.addProperty("ownDirectVictim",true);checks.addProperty("nonImmuneDuplicate",true);
  }
  current.addProperty("finalHealth",victim==null?0:victim.getHealth());current.addProperty("elapsedTicks",level.getGameTime()-caseStart);current.addProperty("ended",true);
  if(caseIndex==1)current.addProperty("finiteEnd",ball instanceof CommittedSoutouFireball p&&p.flight().index()==p.flight().path().points().size()-1&&current.getAsJsonArray("impacts").isEmpty());
  if(victim instanceof Cow cow)require(Math.abs(cow.getY()-224)<.2,"COW_FLOOR");next();
 }
 private void next(){ordinal++;caseIndex=ordinal<ORDER.length?ORDER[ordinal]:NAMES.length;current=null;ball=null;victim=null;preflight=null;}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void impact(ProjectileImpactEvent event){
  if(done||current==null||event.getProjectile()!=ball||ball.level().isClientSide)return;
  var hit=event.getRayTraceResult();var row=new JsonObject();row.addProperty("type",hit.getType().name());row.addProperty("canceled",event.isCanceled());row.addProperty("impactResult",event.getImpactResult().name());row.addProperty("tick",ball.level().getGameTime());
  if(hit instanceof EntityHitResult entity)row.addProperty("victimUuid",entity.getEntity().getUUID().toString());
  if(hit instanceof BlockHitResult block){boolean declared=preflight!=null&&ball.level().hasChunkAt(block.getBlockPos())&&preflight.terminal().stream().anyMatch(t->t.position().equals(block.getBlockPos())&&t.state()==ball.level().getBlockState(block.getBlockPos()));row.addProperty("declared",declared);row.addProperty("block",block.getBlockPos().toShortString());}
  current.getAsJsonArray("impacts").add(row);queueEvent("IMPACT",row,()->{row.addProperty("canceled",event.isCanceled());row.addProperty("impactResult",event.getImpactResult().name());});
 }
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void explosionStart(ExplosionEvent.Start event){explosion("START",event,event::isCanceled);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public void explosionDetonate(ExplosionEvent.Detonate event){explosion("DETONATE",event,()->false);}
 private void explosion(String stage,ExplosionEvent event,java.util.function.BooleanSupplier canceled){
  if(done||current==null||event.getExplosion().getDirectSourceEntity()!=ball||ball.level().isClientSide)return;
  var row=new JsonObject();row.addProperty("stage",stage);row.addProperty("tick",ball.level().getGameTime());row.addProperty("directUuid",ball.getUUID().toString());row.addProperty("ownerUuid",event.getExplosion().getIndirectSourceEntity()==null?"":event.getExplosion().getIndirectSourceEntity().getUUID().toString());current.getAsJsonArray("explosions").add(row);queueEvent("EXPLOSION_"+stage,row,()->row.addProperty("canceled",canceled.getAsBoolean()));
 }
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void attack(LivingAttackEvent event){damage("ATTACK",event.getEntity(),event.getSource(),event::getAmount);}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void hurt(LivingHurtEvent event){damage("HURT",event.getEntity(),event.getSource(),event::getAmount);}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void applied(LivingDamageEvent event){damage("DAMAGE",event.getEntity(),event.getSource(),event::getAmount);}
 private void damage(String stage,LivingEntity entity,DamageSource source,java.util.function.DoubleSupplier amount){
  if(done||current==null||source.getDirectEntity()!=ball||entity!=victim||entity.level().isClientSide)return;
  var row=new JsonObject();row.addProperty("stage",stage);row.addProperty("kind",source.is(DamageTypeTags.IS_EXPLOSION)?"EXPLOSION":source.is(DamageTypes.FIREBALL)?"FIREBALL":"OTHER");row.addProperty("victimUuid",entity.getUUID().toString());row.addProperty("healthBefore",entity.getHealth());row.addProperty("directUuid",ball.getUUID().toString());row.addProperty("ownerUuid",source.getEntity()==null?"":source.getEntity().getUUID().toString());current.getAsJsonArray("damage").add(row);queueEvent(stage,row,()->row.addProperty("amount",amount.getAsDouble()));
 }
 private void queueEvent(String stage,JsonObject value,Runnable finalizedFields){
  if(pendingEvents.size()>=32){eventError=true;return;}
  String name=NAMES[caseIndex];long tick=ball.level().getGameTime();
  pendingEvents.add(()->{finalizedFields.run();try{var row=value.deepCopy();row.addProperty("case",name);row.addProperty("tick",tick);row.addProperty("event",stage);append("events.jsonl",row);}catch(IOException error){eventError=true;}});
 }
 private void flushEvents(){var ready=List.copyOf(pendingEvents);pendingEvents.clear();ready.forEach(Runnable::run);}
 private void append(String name,JsonObject value)throws IOException{require(rows++<1024,"ROW_BOUND");Files.writeString(output.resolve(name),value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private void finish(ServerLevel level,String status)throws IOException{done=true;var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("status",status);result.addProperty("nonce",System.getenv("KNEEKURA_DEBUG_IMPACT_DAMAGE_NONCE"));result.addProperty("startGameTime",start);result.addProperty("endGameTime",level.getGameTime());result.addProperty("canonicalPlayer",request!=null);result.add("cases",cases);result.add("failures",new Gson().toJsonTree(failures));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("IMPACT_DAMAGE_"+reason);}
}

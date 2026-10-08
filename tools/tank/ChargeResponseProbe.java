package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.StandardSoutouFireball;
import com.google.gson.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** New private actors only: actual Cow navigation + production attack adapter; not Player input. */
@Mod("naturalghast_charge_response_probe")
public final class ChargeResponseProbe {
 private static final String SCOPE="CONTROLLED_NEW_TARGET_CHARGE_RESPONSE_NOT_PLAYER_INPUT_OR_NATURAL_SELECTION";
 private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queueSize=new AtomicInteger();
 private static volatile boolean active;private static volatile UUID bossUuid;private static volatile String observerError;
 private final JsonArray rows=new JsonArray(),shots=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),cleanup=new JsonArray();private final List<String> errors=new ArrayList<>();private final Map<UUID,Entity> ownedShots=new LinkedHashMap<>();
 private JsonObject request,actors;private Path output;private long deadline;private int samples;private boolean done,canonicalPlayer,fixtureSubjectAbsent;private Scenario scenario;
 private static final class Scenario {TestGhast boss;TestCow target;CombatAnchor.Region region;long tick=-1,recorded=-1;JsonObject goalRow;int goalTicks,navigationCalls,chargeSerial;boolean wasCharging;double startZ;}
 public ChargeResponseProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_CHARGE_RESPONSE"));}
 private static Object inspect(Object object,String name){try{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_READ_"+name,e);}}
 private static final class TestGhast extends SoutouGhast {
  Scenario s;TestGhast(Level level){super(ModEntities.SOUTOU_GHAST.get(),level);}
  @Override protected void registerGoals(){goalSelector.addGoal(4,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
   @Override public boolean canUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||s==null)return;try{execute(s);}catch(Exception e){observerError="ATTACK_GOAL:"+e.getMessage();}}
  });}
 }
 private static final class TestCow extends Cow {
  Scenario s;TestCow(Level level){super(EntityType.COW,level);}
  @Override protected void registerGoals(){goalSelector.addGoal(1,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE));}
   @Override public boolean canUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||s==null||s.boss==null)return;var state=s.boss.getStandardAttack().state();boolean charging=state.phase()==StandardAttack.Phase.CHARGE;
    if(charging&&!s.wasCharging){s.chargeSerial++;s.startZ=getZ();navigate(s.startZ+(s.chargeSerial%2==1?-2.5:2.5));}
    if(charging&&state.ticks()==20)navigate(s.startZ+(s.chargeSerial%2==1?-8:8));s.wasCharging=charging;
   }
   private void navigate(double z){if(z<9||z>43||++s.navigationCalls>6){observerError="NAVIGATION_BOUND";return;}if(!getNavigation().moveTo(50,224,z,1.4))observerError="NATIVE_NAVIGATION_REJECTED";}
  });}
 }
 private static void execute(Scenario s){
  var boss=s.boss;var attack=boss.getStandardAttack();var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();
  control.sampleMobility();control.setCombatRegion(s.region);control.setIntent(FlightController.Intent.hold());
  boolean visible=s.target.isAlive()&&boss.getSensing().hasLineOfSight(s.target);int before=attack.firedCount();Vec3 look=attack.tick(s.target,visible,false);
  if(look!=null)((SoutouGhastFlightLookControl)boss.getLookControl()).setIntent(look);
  var memory=(ObservedChargeResponse)inspect(attack,"chargeResponse");var state=attack.state();var row=new JsonObject();
  row.addProperty("goalTick",++s.goalTicks);row.addProperty("phase",state.phase().name());row.addProperty("chargeTick",state.ticks());row.addProperty("fire",state.fire());row.addProperty("emitted",attack.firedCount()>before);row.addProperty("fired",attack.firedCount());row.addProperty("visible",visible);
  row.addProperty("history",memory.count());row.addProperty("repeated",memory.repeatedTiming());row.addProperty("stationary",memory.stationaryIntervals());row.addProperty("onset",memory.pendingOnset());row.addProperty("qualified",memory.qualified());
  row.addProperty("choice",attack.selectedProfile().name());row.addProperty("lastFired",attack.lastFiredProfile().name());row.addProperty("rejection",attack.lastRejection());row.addProperty("navigationCalls",s.navigationCalls);
  row.add("observedTarget",vector(s.target.position()));row.add("observedVelocity",vector(s.target.getDeltaMovement()));row.add("region",region(s.region));
  if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==19){
   var selector=(ProjectileSelector)inspect(attack,"selector");double variation=(double)inspect(attack,"selectionVariation");var eye=(Vec3)inspect(attack,"lockedEye");double range=boss.getEyePosition().distanceTo(eye);var context=control.getMobilityContext();
   row.addProperty("variation",variation);row.addProperty("range",range);row.addProperty("context",context.name());row.addProperty("stationaryTicks",attack.stationaryTicks());
   row.addProperty("withoutHistory",selector.choose(context,range,from(s.target.getDeltaMovement()),attack.stationaryTicks(),variation,false).name());row.addProperty("withHistory",selector.choose(context,range,from(s.target.getDeltaMovement()),attack.stationaryTicks(),variation,memory.repeatedTiming()).name());
  }
  s.goalRow=row;s.tick=boss.level().getGameTime();
 }
 @SubscribeEvent public void join(EntityJoinLevelEvent event){
  if(!active||event.getLevel().isClientSide()||!(event.getEntity() instanceof StandardSoutouFireball shot)||!Objects.equals(shot.originUuid(),bossUuid))return;
  ownedShots.put(shot.getUUID(),shot);var row=entity(shot);row.addProperty("origin",shot.originUuid().toString());row.addProperty("owner",shot.getOwner()==null?"":shot.getOwner().getUUID().toString());row.addProperty("type",shot.getType().toString());shots.add(row);try{append("shots.jsonl",row);}catch(IOException e){observerError="SHOT_PUBLICATION:"+e.getMessage();}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/charge-response");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_CHARGE_RESPONSE_NONCE"))&&request.get("maxTicks").getAsInt()==430&&request.get("maxWallMs").getAsInt()==40000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_CHARGE_RESPONSE_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+40000;Files.createDirectories(output);active=true;
  }
  require(samples++<430&&System.currentTimeMillis()<deadline&&observerError==null,"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PLAYER");canonicalPlayer=true;drain();
  if(scenario==null){
   require(level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()))==null&&level.getEntitiesOfClass(SoutouGhast.class,new AABB(0,224,0,52,248,52)).isEmpty(),"NO_CANONICAL_COMBAT_SUBJECT");fixtureSubjectAbsent=true;
   scenario=new Scenario();var s=scenario;s.target=new TestCow(level);s.target.s=s;s.target.setPos(50,224,26);require(level.noCollision(s.target,s.target.getBoundingBox())&&level.addFreshEntity(s.target),"NEW_NATIVE_TARGET");
   s.boss=new TestGhast(level);s.boss.s=s;s.boss.setPos(3,230,26);s.region=new CombatAnchor.Region(from(s.boss.position()),CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);bossUuid=s.boss.getUUID();require(level.noCollision(s.boss,s.boss.getBoundingBox())&&level.addFreshEntity(s.boss),"NEW_NATIVE_BOSS");
   actors=entity(s.boss);actors.addProperty("targetUuid",s.target.getUUID().toString());actors.addProperty("privateControlledGoals",true);actors.addProperty("targetMovement","NEW_COW_NATIVE_NAVIGATION_1.4_TWO_WAYPOINTS_PER_CHARGE");actors.addProperty("width",s.boss.getBbWidth());actors.addProperty("height",s.boss.getBbHeight());actors.add("region",region(s.region));Files.writeString(output.resolve("actors.json"),actors+"\n",StandardOpenOption.CREATE_NEW);return;
  }
  var s=scenario;require(s.boss.isAlive()&&s.target.isAlive()&&!s.boss.isNoAi()&&!s.target.isNoAi()&&level.getEntity(s.boss.getUUID())==s.boss&&level.getEntity(s.target.getUUID())==s.target&&level.hasChunkAt(s.boss.blockPosition())&&level.hasChunkAt(s.target.blockPosition()),"LIVE_LOADED_OWNED_ACTORS");
  if(s.tick==level.getGameTime()&&s.recorded!=s.tick){s.recorded=s.tick;var row=entity(s.boss);for(var e:s.goalRow.entrySet())row.add(e.getKey(),e.getValue());row.add("targetNow",vector(s.target.position()));row.addProperty("targetHealth",s.target.getHealth());row.addProperty("registered",true);row.addProperty("loaded",true);rows.add(row);require(rows.size()<=430,"ROW_BOUND");append("rows.jsonl",row);}
  var memory=(ObservedChargeResponse)inspect(s.boss.getStandardAttack(),"chargeResponse");if(s.boss.getStandardAttack().firedCount()>=3&&memory.count()>=2&&memory.repeatedTiming()&&clientRows.size()>=4)finish("PASS");
 }
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queueSize.decrementAndGet();if(row.remove("record").getAsString().equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=4,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;done=true;try{drain();}catch(IOException e){errors.add("Drain:"+e.getMessage());status="FAIL";}
  var owned=new ArrayList<Entity>(ownedShots.values());if(scenario!=null){if(scenario.boss!=null)owned.add(scenario.boss);if(scenario.target!=null)owned.add(scenario.target);}
  for(Entity e:owned){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());if(e.level() instanceof ServerLevel level&&level.getEntity(e.getUUID())==e){e.discard();row.addProperty("status","DISCARDED_PRIVATE_ACTOR");}else row.addProperty("status","ALREADY_REMOVED");cleanup.add(row);}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("fixtureSubjectAbsent",fixtureSubjectAbsent);result.addProperty("canonicalOverrides",false);result.add("actors",actors);result.add("rows",rows);result.add("shots",shots);result.add("joins",joins);result.add("clientRows",clientRows);result.add("cleanup",cleanup);if(observerError!=null)errors.add(observerError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject entity(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("tick",e.level().getGameTime());row.addProperty("epoch",System.currentTimeMillis());row.add("position",vector(e.position()));return row;}
 private static JsonObject vector(Vec3 v){var row=new JsonObject();row.addProperty("x",v.x);row.addProperty("y",v.y);row.addProperty("z",v.z);return row;}
 private static JsonObject region(CombatAnchor.Region r){var row=new JsonObject();row.add("center",vector(SoutouGhastInertialMoveControl.to(r.center())));row.add("radii",vector(SoutouGhastInertialMoveControl.to(r.radii())));row.addProperty("generation",r.generation());return row;}
 private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("CHARGE_RESPONSE_"+reason);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>128){queueSize.decrementAndGet();observerError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 @Mod.EventBusSubscriber(modid="naturalghast_charge_response_probe",value=Dist.CLIENT)
 public static final class Client {
  private static SoutouGhast pending,observed;private static int count;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof SoutouGhast boss)||!boss.getUUID().equals(bossUuid))return;if(event.isCanceled()||pending!=null||observed!=null){observerError="DUPLICATE_CLIENT_BOSS";return;}pending=boss;}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!active||!enabled()||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
   if(pending!=null){var row=entity(pending);row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(pending).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed=pending;pending=null;}
   if(observed!=null&&count<4){if(observed.isRemoved()||mc.level.getEntity(observed.getId())!=observed){observerError="CLIENT_BOSS_IDENTITY";return;}var row=entity(observed);row.addProperty("record","ROW");enqueue(row);count++;}
  }
 }
}

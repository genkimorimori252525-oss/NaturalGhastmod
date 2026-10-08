package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
import com.genki.soutoughast.entity.ai.flight.*;
import com.google.gson.*;
import java.io.IOException;
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

/** Explicit new private controlled-Goal actors; production composer/controller/native travel, not natural selection. */
@Mod("naturalghast_complete_feints_probe")
public final class CompleteFeintsProbe {
 private static final String SCOPE="EXPLICIT_NEW_BOSS_COMPOSER_NATIVE_NOT_NATURAL_SELECTION_OR_HUMAN_READABILITY";
 private static final ConcurrentMap<UUID,String> names=new ConcurrentHashMap<>();private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queueSize=new AtomicInteger();
 private static volatile boolean active;private static volatile String clientError;
 private static final java.lang.reflect.Method bodyRoute=bodyRouteMethod();
 // Read-only access from an isolated private mod package; keep the product API unchanged.
 private static java.lang.reflect.Method bodyRouteMethod(){try{var method=SoutouGhastInertialMoveControl.class.getDeclaredMethod("hasManeuverClearance",FlightVector.class,FlightVector.class);method.setAccessible(true);return method;}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_BODY_ROUTE_ACCESS",e);}}
 private static boolean route(SoutouGhastInertialMoveControl control,FlightVector from,FlightVector to){try{return (boolean)bodyRoute.invoke(control,from,to);}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_BODY_ROUTE_QUERY",e);}}
 private final List<Scenario> scenarios=new ArrayList<>();private final JsonArray cases=new JsonArray(),rows=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),cleanup=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request;private Path output;private long deadline;private int samples;private boolean done,canonicalPlayer,fixtureSubjectAbsent;
 private static final class Scenario {
  String name;TestGhast boss;Cow target;FlightVector observed;CombatAnchor.Region region;ManeuverComposer composer=new ManeuverComposer();
  MovementPlanner.Plan plan;FlightVector before,velocityBefore;long tick=-1,recorded=-1;boolean started,complete;int goalTicks;
 }
 public CompleteFeintsProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_COMPLETE_FEINTS"));}
 /** Probe-only subclass replaces its own Goal, never changes any production/canonical actor. */
 private static final class TestGhast extends SoutouGhast {
  Scenario scenario;
  TestGhast(Level level){super(ModEntities.SOUTOU_GHAST.get(),level);}
  @Override protected void registerGoals(){goalSelector.addGoal(4,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
   @Override public boolean canUse(){return true;}
   @Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||scenario==null||scenario.complete)return;try{execute(scenario);}catch(Exception e){clientError="Goal:"+e.getMessage();}}
  });}
 }
 private static void execute(Scenario s)throws IOException{
  var boss=s.boss;var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();var position=from(boss.position());
  if(!s.started){
   require(s.target.isAlive()&&boss.getSensing().hasLineOfSight(s.target),"OBSERVED_LIVING_TARGET");s.observed=from(s.target.position());
   require(s.composer.start(TacticalEvaluator.Action.valueOf(s.name),s.observed,position,s.region,control.sampleMobility(),.99,(from,to)->route(control,from,to)),"FULL_BODY_RECIPE_ADMISSION");s.started=true;
  }
  s.before=position;s.velocityBefore=from(boss.getDeltaMovement());
  s.plan=s.composer.step(position,CombatAnchor.Range.COMFORTABLE,d->control.hasDirectionalClearance(d),
    ()->new MovementPlanner.Plan(MovementPrimitive.HOLD,FlightController.Intent.hold(),position,CombatAnchor.Range.COMFORTABLE,s.region.contains(position)));
  control.setCombatRegion(s.region);control.setTacticalState(s.composer.state());control.setMovementPlan(s.plan);
  s.tick=boss.level().getGameTime();s.goalTicks++;s.complete=!s.composer.active();
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/complete-feints");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_COMPLETE_FEINTS_NONCE"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_COMPLETE_FEINTS_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+20000;Files.createDirectories(output);active=true;
  }
  require(samples++<240&&System.currentTimeMillis()<deadline&&clientError==null,"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PLAYER");canonicalPlayer=true;
  drain();
  if(scenarios.isEmpty()){
   require(level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()))==null&&level.getEntitiesOfClass(SoutouGhast.class,new AABB(0,224,0,52,248,52)).isEmpty(),"FRESH_COMPOSER_FIXTURE_NO_COMBAT_SUBJECT");fixtureSubjectAbsent=true;
   create(level,"PASS_BY_FAKE",26,230,38);create(level,"DOUBLE_FAKE",12,240,38);create(level,"ABORT_FAKE",42,240,38);return;
  }
  for(var s:scenarios){
   var boss=s.boss;require(!boss.isRemoved()&&!boss.isNoAi()&&boss.isAlive()&&level.getEntity(boss.getUUID())==boss&&level.hasChunkAt(boss.blockPosition())&&boss.getBbWidth()==4&&boss.getBbHeight()==4,"LIVE_NATIVE_TEST_BODY");
   if(s.tick!=level.getGameTime()||s.recorded==s.tick)continue;s.recorded=s.tick;
   var row=entity(boss);row.addProperty("name",s.name);row.addProperty("goalTick",s.goalTicks);row.addProperty("phase",s.composer.state().phase().name());row.addProperty("committed",s.composer.state().committed());row.addProperty("completed",s.complete);
   row.add("observedTarget",vector(s.observed));row.add("targetNow",vector(from(s.target.position())));row.add("before",vector(s.before));row.add("velocityBefore",vector(s.velocityBefore));row.add("velocity",vector(from(boss.getDeltaMovement())));row.add("waypoint",vector(s.plan.waypoint()));
   row.addProperty("mode",s.plan.intent().mode().name());row.addProperty("primitive",s.plan.primitive().name());row.addProperty("speed",boss.getDeltaMovement().length());row.addProperty("blocked",((SoutouGhastInertialMoveControl)boss.getMoveControl()).isClearanceBlocked());row.addProperty("registered",true);row.addProperty("loaded",true);row.add("region",region(s.region));rows.add(row);require(rows.size()<=640,"ROW_BOUND");append("rows.jsonl",row);
  }
  if(scenarios.stream().allMatch(s->s.complete)&&scenarios.stream().allMatch(s->clientRows.asList().stream().filter(x->x.getAsJsonObject().get("name").getAsString().equals(s.name)).count()>=4))finish("PASS");
 }
 private void create(ServerLevel level,String name,double x,double y,double z)throws IOException{
  var s=new Scenario();s.name=name;scenarios.add(s);s.target=EntityType.COW.create(level);require(s.target!=null,"CREATE_TEST_TARGET");s.target.setPos(x,224,z-10);require(level.noCollision(s.target,s.target.getBoundingBox())&&level.addFreshEntity(s.target),"NEW_NATIVE_TARGET");
  s.boss=new TestGhast(level);s.boss.setPos(x,y,z);s.region=new CombatAnchor.Region(from(s.boss.position()),CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);s.boss.scenario=s;names.put(s.boss.getUUID(),name);
  require(level.noCollision(s.boss,s.boss.getBoundingBox())&&level.addFreshEntity(s.boss),"NEW_NATIVE_TEST_BOSS");
  var c=entity(s.boss);c.addProperty("name",name);c.addProperty("privateControlledGoal",true);c.addProperty("width",s.boss.getBbWidth());c.addProperty("height",s.boss.getBbHeight());c.addProperty("targetUuid",s.target.getUUID().toString());c.add("region",region(s.region));
  var control=(SoutouGhastInertialMoveControl)s.boss.getMoveControl();c.addProperty("clearBody",route(control,from(s.boss.position()),from(s.boss.position()).add(new FlightVector(0,0,-4))));
  c.addProperty("wallRejected",!route(control,new FlightVector(48,y,z),new FlightVector(52,y,z)));
  c.addProperty("unloadedRejected",!route(control,new FlightVector(10000,y,10000),new FlightVector(10008,y,10000)));cases.add(c);append("cases.jsonl",c);
 }
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queueSize.decrementAndGet();if(row.remove("record").getAsString().equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=640,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;done=true;try{drain();}catch(IOException e){errors.add("Drain:"+e.getMessage());status="FAIL";}
  for(var s:scenarios)for(Entity e:new Entity[]{s.boss,s.target}){if(e==null)continue;var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());if(e.level() instanceof ServerLevel level&&level.getEntity(e.getUUID())==e){e.discard();row.addProperty("status","DISCARDED_PRIVATE_ACTOR");}else row.addProperty("status","ALREADY_REMOVED");cleanup.add(row);}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("fixtureSubjectAbsent",fixtureSubjectAbsent);result.addProperty("canonicalOverrides",false);result.add("cases",cases);result.add("rows",rows);result.add("joins",joins);result.add("clientRows",clientRows);result.add("cleanup",cleanup);if(clientError!=null)errors.add(clientError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject entity(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("tick",e.level().getGameTime());row.addProperty("epoch",System.currentTimeMillis());row.add("position",vector(from(e.position())));return row;}
 private static JsonObject vector(FlightVector v){var row=new JsonObject();row.addProperty("x",v.x());row.addProperty("y",v.y());row.addProperty("z",v.z());return row;}
 private static JsonObject region(CombatAnchor.Region r){var row=new JsonObject();row.add("center",vector(r.center()));row.add("radii",vector(r.radii()));row.addProperty("generation",r.generation());return row;}
 private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("COMPLETE_FEINTS_"+reason);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>128){queueSize.decrementAndGet();clientError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 @Mod.EventBusSubscriber(modid="naturalghast_complete_feints_probe",value=Dist.CLIENT)
 public static final class Client {
  private static final Map<UUID,SoutouGhast> pending=new LinkedHashMap<>(),observed=new LinkedHashMap<>();private static final Map<UUID,Integer> counts=new HashMap<>();
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof SoutouGhast boss)||!names.containsKey(boss.getUUID()))return;if(event.isCanceled()||observed.containsKey(boss.getUUID())||pending.putIfAbsent(boss.getUUID(),boss)!=null)clientError="DUPLICATE_CLIENT_ADMISSION";}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
   if(!active||!enabled()||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
   for(var boss:pending.values()){var row=entity(boss);row.addProperty("name",names.get(boss.getUUID()));row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(boss).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed.put(boss.getUUID(),boss);}pending.clear();
   for(var boss:observed.values()){int count=counts.getOrDefault(boss.getUUID(),0);if(count>=4)continue;if(boss.isRemoved()||mc.level.getEntity(boss.getId())!=boss){clientError="CLIENT_ACTOR_IDENTITY";return;}var row=entity(boss);row.addProperty("name",names.get(boss.getUUID()));row.addProperty("record","ROW");enqueue(row);counts.put(boss.getUUID(),count+1);}
  }
 }
}

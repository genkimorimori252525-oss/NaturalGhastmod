package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
import com.genki.soutoughast.entity.ai.flight.*;
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
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** New native arrows/actors and actual perception adapter; explicit fixture, unforced fallible decisions. */
@Mod("naturalghast_projectile_dodge_probe")
public final class ProjectileDodgeProbe {
 private static final String SCOPE="NEW_NATIVE_ARROW_DODGE_ADAPTER_NOT_PRODUCTION_GOAL_OR_PLAYER_INPUT";
 private static final Map<UUID,Integer> names=new ConcurrentHashMap<>();private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queued=new AtomicInteger();
 private static volatile boolean active;private static volatile String observerError;
 private final List<Scenario> scenarios=new ArrayList<>();private final List<Entity> owned=new ArrayList<>();private final JsonArray actors=new JsonArray(),births=new JsonArray(),rows=new JsonArray(),impacts=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),cleanup=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request;private Path output;private long deadline;private int samples;private boolean done,canonicalPlayer,fixtureSubjectAbsent;
 private static final class Scenario {int index,goalTicks,normalTicks;TestGhast boss;Cow target;Arrow arrow;CombatAnchor anchor=new CombatAnchor();CombatAnchor.Region region;SoutouGhastProjectileDodge dodge;MovementPlanner planner=new MovementPlanner();long tick=-1,recorded=-1,travelTick=-1;Vec3 travelPosition,travelVelocity;float travelHealth;JsonObject row;JsonArray hits=new JsonArray();}
 public ProjectileDodgeProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_PROJECTILE_DODGE"));}
 private static Object inspect(Object object,String name){try{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_READ_"+name,e);}}
 private static final class TestGhast extends SoutouGhast {
  Scenario s;TestGhast(Level level){super(ModEntities.SOUTOU_GHAST.get(),level);}
  @Override protected void registerGoals(){goalSelector.addGoal(4,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));} @Override public boolean canUse(){return true;} @Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||s==null)return;try{execute(s);}catch(Exception e){observerError="DODGE_GOAL:"+e.getMessage();}}
  });}
  @Override public void travel(Vec3 input){super.travel(input);if(active&&s!=null&&s.tick==level().getGameTime()){s.travelTick=level().getGameTime();s.travelPosition=position();s.travelVelocity=getDeltaMovement();s.travelHealth=getHealth();}}
  @Override public boolean hurt(DamageSource source,float amount){
   if(!active||s==null||!(source.getDirectEntity() instanceof Arrow arrow))return super.hurt(source,amount);
   var hit=new JsonObject();hit.addProperty("case",s.index);hit.addProperty("tick",level().getGameTime());hit.addProperty("afterTravelTick",s.travelTick);hit.addProperty("bossUuid",getUUID().toString());hit.addProperty("directUuid",arrow.getUUID().toString());hit.addProperty("ownerUuid",arrow.getOwner()==null?"NONE":arrow.getOwner().getUUID().toString());hit.addProperty("amount",amount);hit.add("beforePosition",vector(position()));hit.add("beforeVelocity",vector(getDeltaMovement()));hit.addProperty("beforeHealth",getHealth());
   boolean accepted=super.hurt(source,amount);hit.addProperty("accepted",accepted);hit.add("afterPosition",vector(position()));hit.add("afterVelocity",vector(getDeltaMovement()));hit.addProperty("afterHealth",getHealth());s.hits.add(hit);return accepted;
  }
 }
 private static void execute(Scenario s){
  var boss=s.boss;var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();var sample=control.sampleMobility();var before=boss.position();var velocity=boss.getDeltaMovement();boolean visible=s.target.isAlive()&&boss.getSensing().hasLineOfSight(s.target);
  var row=new JsonObject();row.addProperty("case",s.index);row.addProperty("goalTick",++s.goalTicks);row.add("before",vector(before));row.add("velocityBefore",vector(velocity));row.addProperty("visible",visible);row.addProperty("context",control.getMobilityContext().name());row.add("region",region(s.anchor.region()));row.add("targetBefore",vector(s.target.position()));
  row.add("arrowBefore",s.arrow==null?JsonNull.INSTANCE:arrow(s.arrow));
  if(s.arrow!=null){row.addProperty("arrowVisible",boss.level().hasChunkAt(s.arrow.blockPosition())&&boss.level().clip(new ClipContext(boss.getEyePosition(),s.arrow.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getType()==HitResult.Type.MISS);}
  var plan=s.dodge.tick(s.target,visible,s.anchor.region(),false);var state=s.dodge.state();
  var core=(ObservedProjectileDodge)inspect(s.dodge,"dodge");var attempts=(Map<?,?>)inspect(core,"attempted");boolean attempted=s.arrow!=null&&attempts.containsKey(s.arrow.getUUID());
  row.addProperty("attempted",attempted);row.addProperty("phase",state.phase().name());row.addProperty("startTick",state.startTick());row.addProperty("impactTicks",Double.isFinite(state.impactTicks())?state.impactTicks():-1);row.add("waypoint",state.waypoint()==null?JsonNull.INSTANCE:vector(SoutouGhastInertialMoveControl.to(state.waypoint())));
  if(plan==null){
   if(s.arrow==null||!attempted)plan=new MovementPlanner.Plan(MovementPrimitive.HOLD,FlightController.Intent.hold(),from(before),CombatAnchor.Range.COMFORTABLE,true);
   else {plan=s.planner.step(s.anchor,from(s.target.position()),from(before),FlightVector.ZERO,control.getMobilityContext(),sample,.3,control::hasDirectionalClearance);s.normalTicks++;}
  }
  control.setCombatRegion(s.region);control.setMovementPlan(plan);if(visible)((SoutouGhastFlightLookControl)boss.getLookControl()).setIntent(s.target.getEyePosition().subtract(boss.getEyePosition()));else ((SoutouGhastFlightLookControl)boss.getLookControl()).clearIntent();
  row.addProperty("primitive",plan.primitive().name());row.addProperty("mode",plan.intent().mode().name());row.add("direction",vector(SoutouGhastInertialMoveControl.to(plan.intent().direction())));row.addProperty("speed",plan.intent().speed());row.addProperty("normalTicks",s.normalTicks);s.row=row;s.tick=boss.level().getGameTime();
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}}
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/projectile-dodge");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_PROJECTILE_DODGE_NONCE"))&&request.get("maxTicks").getAsInt()==160&&request.get("maxWallMs").getAsInt()==16000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_PROJECTILE_DODGE_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+16000;Files.createDirectories(output);Files.writeString(output.resolve("impacts.jsonl"),"",StandardOpenOption.CREATE_NEW);active=true;
  }
  require(samples++<160&&System.currentTimeMillis()<deadline&&observerError==null,"FINITE_WINDOW_OR_OBSERVER_ERROR");var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));require(player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PLAYER");canonicalPlayer=true;drain();
  if(scenarios.isEmpty()){
   require(level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()))==null&&level.getEntitiesOfClass(SoutouGhast.class,new AABB(0,224,0,52,248,52)).isEmpty(),"NO_CANONICAL_COMBAT_SUBJECT");fixtureSubjectAbsent=true;
   for(int i=0;i<3;i++){var s=new Scenario();s.index=i;scenarios.add(s);s.target=new Cow(EntityType.COW,level);owned.add(s.target);s.target.setPos(12+i*14,224,22);require(level.noCollision(s.target,s.target.getBoundingBox())&&level.addFreshEntity(s.target),"NEW_NATIVE_TARGET");
    s.boss=new TestGhast(level);owned.add(s.boss);s.boss.s=s;s.boss.setPos(12+i*14,230,34);s.anchor.observe(s.target.getUUID(),from(s.target.position()),from(s.boss.position()),false);s.region=s.anchor.region();s.dodge=new SoutouGhastProjectileDodge(s.boss);names.put(s.boss.getUUID(),i);require(level.noCollision(s.boss,s.boss.getBoundingBox())&&level.addFreshEntity(s.boss),"NEW_NATIVE_BOSS");var actor=entity(s.boss);actor.addProperty("case",i);actor.addProperty("targetUuid",s.target.getUUID().toString());actor.addProperty("width",s.boss.getBbWidth());actor.addProperty("height",s.boss.getBbHeight());actor.addProperty("initialHealth",s.boss.getHealth());actor.addProperty("privateControlledGoal",true);actor.addProperty("decisionRngOverridden",false);actor.add("region",region(s.region));actors.add(actor);
   }Files.writeString(output.resolve("actors.json"),actors+"\n",StandardOpenOption.CREATE_NEW);return;
  }
  for(var s:scenarios){
   require(s.boss.isAlive()&&s.target.isAlive()&&!s.boss.isNoAi()&&!s.target.isNoAi()&&level.getEntity(s.boss.getUUID())==s.boss&&level.getEntity(s.target.getUUID())==s.target&&level.hasChunkAt(s.boss.blockPosition())&&level.hasChunkAt(s.target.blockPosition()),"LIVE_LOADED_OWNED_ACTORS");
   if(s.tick==level.getGameTime()&&s.recorded!=s.tick){s.recorded=s.tick;require(s.travelTick==s.tick&&s.travelPosition!=null&&s.travelVelocity!=null,"POST_TRAVEL_BOUNDARY");var row=entity(s.boss);for(var e:s.row.entrySet())row.add(e.getKey(),e.getValue());row.add("velocity",vector(s.boss.getDeltaMovement()));row.add("travelPosition",vector(s.travelPosition));row.add("travelVelocity",vector(s.travelVelocity));row.addProperty("travelHealth",s.travelHealth);row.addProperty("travelTick",s.travelTick);row.add("impacts",s.hits.deepCopy());for(var hit:s.hits){impacts.add(hit);append("impacts.jsonl",hit.getAsJsonObject());}s.hits=new JsonArray();row.addProperty("blocked",((SoutouGhastInertialMoveControl)s.boss.getMoveControl()).isClearanceBlocked());row.addProperty("health",s.boss.getHealth());row.addProperty("loaded",true);row.addProperty("registered",true);row.add("arrowAfter",s.arrow==null?JsonNull.INSTANCE:arrow(s.arrow));rows.add(row);require(rows.size()<=480,"ROW_BOUND");append("rows.jsonl",row);}
   if(s.goalTicks==24&&s.arrow==null){s.arrow=new Arrow(level,s.target);owned.add(s.arrow);s.arrow.setPos(s.boss.getX(),s.boss.getY()+2,s.boss.getZ()+16);s.arrow.setNoGravity(true);s.arrow.shoot(0,0,-1,1,0);require(level.noCollision(s.arrow,s.arrow.getBoundingBox())&&level.hasChunkAt(s.arrow.blockPosition())&&level.addFreshEntity(s.arrow),"NEW_DECLARED_STRAIGHT_ARROW");var birth=arrow(s.arrow);birth.addProperty("case",s.index);birth.addProperty("ownerUuid",s.target.getUUID().toString());birth.addProperty("declaredFixtureNoGravity",true);births.add(birth);append("births.jsonl",birth);}
  }
  if(scenarios.stream().allMatch(s->s.goalTicks>=90)){require(joins.size()==3&&clientRows.size()>=12,"CLIENT_ADMISSION");boolean admitted=scenarios.stream().anyMatch(s->s.dodge.state().startTick()>=0);finish(admitted?"PASS":"INCONCLUSIVE_ALL_DECLINED");}
 }
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queued.decrementAndGet();if(row.remove("record").getAsString().equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=72,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;done=true;try{drain();}catch(IOException e){errors.add("Drain:"+e.getMessage());status="FAIL";}for(var e:owned){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());if(e.level() instanceof ServerLevel level&&level.getEntity(e.getUUID())==e){e.discard();row.addProperty("status","DISCARDED_PRIVATE_ACTOR");}else row.addProperty("status","ALREADY_REMOVED");cleanup.add(row);}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request==null?System.getenv("KNEEKURA_DEBUG_PROJECTILE_DODGE_NONCE"):request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("fixtureSubjectAbsent",fixtureSubjectAbsent);result.addProperty("canonicalOverrides",false);result.add("actors",actors);result.add("births",births);result.add("rows",rows);result.add("impacts",impacts);result.add("joins",joins);result.add("clientRows",clientRows);result.add("cleanup",cleanup);if(observerError!=null)errors.add(observerError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject entity(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("tick",e.level().getGameTime());row.addProperty("epoch",System.currentTimeMillis());row.add("position",vector(e.position()));return row;}
 private static JsonObject arrow(Arrow a){var row=entity(a);var box=a.getBoundingBox();row.add("center",vector(box.getCenter()));row.add("halfSize",vector(new Vec3(box.getXsize()/2,box.getYsize()/2,box.getZsize()/2)));row.add("velocity",vector(a.getDeltaMovement()));row.addProperty("alive",a.isAlive());row.addProperty("loaded",a.level().hasChunkAt(a.blockPosition()));row.addProperty("noGravity",a.isNoGravity());row.addProperty("ownerUuid",a.getOwner()==null?"NONE":a.getOwner().getUUID().toString());return row;}
 private static JsonObject vector(Vec3 v){var row=new JsonObject();row.addProperty("x",v.x);row.addProperty("y",v.y);row.addProperty("z",v.z);return row;}
 private static JsonObject region(CombatAnchor.Region r){var row=new JsonObject();row.add("center",vector(SoutouGhastInertialMoveControl.to(r.center())));row.add("radii",vector(SoutouGhastInertialMoveControl.to(r.radii())));row.addProperty("generation",r.generation());row.addProperty("reason",r.reason().name());return row;}
 private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("PROJECTILE_DODGE_"+reason);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void enqueue(JsonObject row){if(queued.incrementAndGet()>128){queued.decrementAndGet();observerError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 @Mod.EventBusSubscriber(modid="naturalghast_projectile_dodge_probe",value=Dist.CLIENT)
 public static final class Client {
  private static final Map<Integer,SoutouGhast> pending=new HashMap<>(),observed=new HashMap<>();private static final Map<Integer,Integer> counts=new HashMap<>();
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof SoutouGhast boss))return;var index=names.get(boss.getUUID());if(index==null)return;if(event.isCanceled()||pending.containsKey(index)||observed.containsKey(index)){observerError="DUPLICATE_CLIENT_BOSS";return;}pending.put(index,boss);}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!active||!enabled()||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;for(var entry:pending.entrySet()){var row=entity(entry.getValue());row.addProperty("case",entry.getKey());row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(entry.getValue()).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed.put(entry.getKey(),entry.getValue());}pending.clear();for(var entry:observed.entrySet()){int index=entry.getKey(),count=counts.getOrDefault(index,0);if(count>=24)continue;var boss=entry.getValue();if(boss.isRemoved()||mc.level.getEntity(boss.getId())!=boss){observerError="CLIENT_BOSS_IDENTITY";return;}var row=entity(boss);row.addProperty("case",index);row.addProperty("record","ROW");enqueue(row);counts.put(index,count+1);}}
 }
}

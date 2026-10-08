package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
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

/** New private actors only: frozen observed geometry + production transit/controller; not natural selection. */
@Mod("naturalghast_reanchor_probe")
public final class ReanchorProbe {
 private static final String SCOPE="EXPLICIT_NEW_BOSS_TRANSIT_NOT_NATURAL_SELECTION_OR_HUMAN_READABILITY";
 private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queueSize=new AtomicInteger();
 private static volatile boolean active;private static volatile UUID bossUuid;private static volatile String observerError;
 private final JsonArray rows=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),cleanup=new JsonArray();private final List<String> errors=new ArrayList<>();
 private JsonObject request,actors;private Path output;private long deadline;private int samples;private boolean done,canonicalPlayer,fixtureSubjectAbsent;private Scenario scenario;
 private static final class Scenario {TestGhast boss;Cow target;CombatAnchor anchor=new CombatAnchor();OverheadReanchor transit=new OverheadReanchor();CombatAnchor.Region region;long tick=-1,recorded=-1;JsonObject goalRow;int goalTicks;boolean committed;}
 public ReanchorProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_REANCHOR"));}
 private static final class TestGhast extends SoutouGhast {
  Scenario s;TestGhast(Level level){super(ModEntities.SOUTOU_GHAST.get(),level);}
  @Override protected void registerGoals(){goalSelector.addGoal(4,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
   @Override public boolean canUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||s==null)return;try{execute(s);}catch(Exception e){observerError="TRANSIT_GOAL:"+e.getMessage();}}
  });}
 }
 private static boolean route(Scenario s,FlightVector a,FlightVector b){
  try{var method=SoutouGhastInertialMoveControl.class.getDeclaredMethod("hasManeuverClearance",FlightVector.class,FlightVector.class);method.setAccessible(true);return (boolean)method.invoke(s.boss.getMoveControl(),a,b);}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_BODY_ROUTE",e);}
 }
 private static void execute(Scenario s){
  if(!s.transit.active())return;
  var boss=s.boss;var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();control.sampleMobility();
  boolean visible=s.target.isAlive()&&boss.getSensing().hasLineOfSight(s.target);
  var before=boss.position();var velocity=boss.getDeltaMovement();var old=s.anchor.region();
  var state=s.transit.step(s.target.getUUID(),visible,old,from(before),from(velocity),(a,b)->route(s,a,b));
  if(state.commit()){if(s.committed||!s.anchor.commitRelocation(s.target.getUUID(),s.region.generation(),from(before)))throw new IllegalStateException("GUARDED_COMMIT");s.committed=true;}
  control.setCombatRegion(s.anchor.region());control.setMajorIntent(state.intent(),state.phase()==OverheadReanchor.Phase.CROSS?MovementPrimitive.OVERSHOOT:state.phase()==OverheadReanchor.Phase.TELL||state.phase()==OverheadReanchor.Phase.CLIMB?MovementPrimitive.RISE:MovementPrimitive.BRAKE);
  var row=new JsonObject();row.addProperty("goalTick",++s.goalTicks);row.addProperty("phase",state.phase().name());row.addProperty("commit",state.commit());row.addProperty("visible",visible);row.add("before",vector(before));row.add("velocityBefore",vector(velocity));row.add("regionBefore",region(old));row.add("region",region(s.anchor.region()));
  row.add("overhead",vector(SoutouGhastInertialMoveControl.to(s.transit.overhead())));row.add("destination",vector(SoutouGhastInertialMoveControl.to(s.transit.destination())));s.goalRow=row;s.tick=boss.level().getGameTime();
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/reanchor");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_REANCHOR_NONCE"))&&request.get("maxTicks").getAsInt()==200&&request.get("maxWallMs").getAsInt()==20000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_REANCHOR_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+20000;Files.createDirectories(output);active=true;
  }
  require(samples++<200&&System.currentTimeMillis()<deadline&&observerError==null,"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PLAYER");canonicalPlayer=true;drain();
  if(scenario==null){
   require(level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()))==null&&level.getEntitiesOfClass(SoutouGhast.class,new AABB(0,224,0,52,248,52)).isEmpty(),"NO_CANONICAL_COMBAT_SUBJECT");fixtureSubjectAbsent=true;
   scenario=new Scenario();var s=scenario;s.target=new Cow(EntityType.COW,level);s.target.setPos(26,224,26);require(level.noCollision(s.target,s.target.getBoundingBox())&&level.addFreshEntity(s.target),"NEW_NATIVE_TARGET");
   s.boss=new TestGhast(level);s.boss.s=s;s.boss.setPos(26,230,38);s.anchor.observe(s.target.getUUID(),from(s.target.position()),from(s.boss.position()),false);s.region=s.anchor.region();bossUuid=s.boss.getUUID();require(level.noCollision(s.boss,s.boss.getBoundingBox())&&level.addFreshEntity(s.boss),"NEW_NATIVE_BOSS");
   require(s.transit.begin(s.target.getUUID(),from(s.target.position()),s.target.getBoundingBox().maxY,from(s.boss.position()),s.region,(a,b)->route(s,a,b)),"EXPLICIT_TRANSIT_ADMISSION");
   actors=entity(s.boss);actors.addProperty("targetUuid",s.target.getUUID().toString());actors.addProperty("privateControlledGoal",true);actors.addProperty("width",s.boss.getBbWidth());actors.addProperty("height",s.boss.getBbHeight());actors.add("observedTarget",vector(s.target.position()));actors.addProperty("targetTop",s.target.getBoundingBox().maxY);actors.addProperty("routeLength",s.transit.routeLength());actors.add("region",region(s.region));Files.writeString(output.resolve("actors.json"),actors+"\n",StandardOpenOption.CREATE_NEW);return;
  }
  var s=scenario;require(s.boss.isAlive()&&s.target.isAlive()&&!s.boss.isNoAi()&&!s.target.isNoAi()&&level.getEntity(s.boss.getUUID())==s.boss&&level.getEntity(s.target.getUUID())==s.target&&level.hasChunkAt(s.boss.blockPosition())&&level.hasChunkAt(s.target.blockPosition()),"LIVE_LOADED_OWNED_ACTORS");
  if(s.tick==level.getGameTime()&&s.recorded!=s.tick){s.recorded=s.tick;var row=entity(s.boss);for(var e:s.goalRow.entrySet())row.add(e.getKey(),e.getValue());row.add("velocity",vector(s.boss.getDeltaMovement()));row.addProperty("blocked",((SoutouGhastInertialMoveControl)s.boss.getMoveControl()).isClearanceBlocked());row.add("targetNow",vector(s.target.position()));row.addProperty("targetHealth",s.target.getHealth());row.addProperty("registered",true);row.addProperty("loaded",true);rows.add(row);require(rows.size()<=200,"ROW_BOUND");append("rows.jsonl",row);}
  if(!s.transit.active()){require(s.committed,"TRANSIT_ABORT_OR_TIMEOUT");if(clientRows.size()>=4)finish("PASS");}

 }
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queueSize.decrementAndGet();if(row.remove("record").getAsString().equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=4,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;done=true;try{drain();}catch(IOException e){errors.add("Drain:"+e.getMessage());status="FAIL";}
  var owned=new ArrayList<Entity>();if(scenario!=null){if(scenario.boss!=null)owned.add(scenario.boss);if(scenario.target!=null)owned.add(scenario.target);}
  for(Entity e:owned){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());if(e.level() instanceof ServerLevel level&&level.getEntity(e.getUUID())==e){e.discard();row.addProperty("status","DISCARDED_PRIVATE_ACTOR");}else row.addProperty("status","ALREADY_REMOVED");cleanup.add(row);}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("fixtureSubjectAbsent",fixtureSubjectAbsent);result.addProperty("canonicalOverrides",false);result.add("actors",actors);result.add("rows",rows);result.add("joins",joins);result.add("clientRows",clientRows);result.add("cleanup",cleanup);if(observerError!=null)errors.add(observerError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject entity(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("tick",e.level().getGameTime());row.addProperty("epoch",System.currentTimeMillis());row.add("position",vector(e.position()));return row;}
 private static JsonObject vector(Vec3 v){var row=new JsonObject();row.addProperty("x",v.x);row.addProperty("y",v.y);row.addProperty("z",v.z);return row;}
 private static JsonObject region(CombatAnchor.Region r){var row=new JsonObject();row.add("center",vector(SoutouGhastInertialMoveControl.to(r.center())));row.add("radii",vector(SoutouGhastInertialMoveControl.to(r.radii())));row.addProperty("generation",r.generation());row.addProperty("reason",r.reason().name());return row;}
 private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("REANCHOR_"+reason);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>128){queueSize.decrementAndGet();observerError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 @Mod.EventBusSubscriber(modid="naturalghast_reanchor_probe",value=Dist.CLIENT)
 public static final class Client {
  private static SoutouGhast pending,observed;private static int count;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof SoutouGhast boss)||!boss.getUUID().equals(bossUuid))return;if(event.isCanceled()||pending!=null||observed!=null){observerError="DUPLICATE_CLIENT_BOSS";return;}pending=boss;}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!active||!enabled()||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
   if(pending!=null){var row=entity(pending);row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(pending).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed=pending;pending=null;}
   if(observed!=null&&count<4){if(observed.isRemoved()||mc.level.getEntity(observed.getId())!=observed){observerError="CLIENT_BOSS_IDENTITY";return;}var row=entity(observed);row.addProperty("record","ROW");enqueue(row);count++;}
  }
 }
}

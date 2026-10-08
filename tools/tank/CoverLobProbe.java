package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.*;
import com.google.gson.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
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

/** Fresh private static wall/new actors. Native perception and RNG; not natural Player/Goal selection. */
@Mod("naturalghast_cover_lob_probe")
public final class CoverLobProbe {
 private static final String SCOPE="NEW_PRIVATE_STATIC_COVER_NATIVE_PERCEPTION_NOT_PLAYER_INPUT_OR_PRODUCTION_GOAL";
 static final Vec3 TARGET_START=new Vec3(42.5,224,26.5);
 private static final ConcurrentLinkedQueue<JsonObject> queue=new ConcurrentLinkedQueue<>();private static final AtomicInteger queueSize=new AtomicInteger();
 private static volatile boolean active;private static volatile UUID bossUuid;private static volatile String observerError;
 private final JsonArray rows=new JsonArray(),shots=new JsonArray(),joins=new JsonArray(),clientRows=new JsonArray(),cleanup=new JsonArray(),wallCleanup=new JsonArray();
 private final List<String> errors=new ArrayList<>();private final Map<UUID,Entity> ownedShots=new LinkedHashMap<>();private final List<BlockPos> wall=new ArrayList<>();
 private JsonObject request,actors;private Path output;private long deadline;private int samples;private boolean done,canonicalPlayer,fixtureSubjectAbsent;private Scenario scenario;private ServerLevel level;
 private static final class Scenario {TestGhast boss;TestCow target;CombatAnchor.Region region;long tick=-1,recorded=-1;JsonObject goalRow;int goalTicks,postFire,navigationCalls;boolean hidden;}
 public CoverLobProbe(){MinecraftForge.EVENT_BUS.register(this);}
 private static boolean enabled(){return "1".equals(System.getenv("KNEEKURA_DEBUG_COVER_LOB"));}
 private static Object inspect(Object object,String name){try{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException("PRIVATE_READ_"+name,e);}}
 private static final class TestGhast extends SoutouGhast {
  Scenario s;TestGhast(Level level){super(ModEntities.SOUTOU_GHAST.get(),level);}
  @Override protected void registerGoals(){goalSelector.addGoal(4,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
   @Override public boolean canUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(!active||s==null)return;try{execute(s);}catch(Exception e){observerError="COVER_GOAL:"+e.getMessage();}}
  });}
 }
 private static final class TestCow extends Cow {
  Scenario s;TestCow(Level level){super(EntityType.COW,level);}
  @Override protected void registerGoals(){goalSelector.addGoal(1,new Goal(){
   {setFlags(EnumSet.of(Flag.MOVE));}
   @Override public boolean canUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
   @Override public void tick(){if(active&&s!=null&&s.hidden&&s.navigationCalls==0){s.navigationCalls++;if(!getNavigation().moveTo(TARGET_START.x,TARGET_START.y,TARGET_START.z+12,1))observerError="NATIVE_NAVIGATION_REJECTED";}}
  });}
 }
 private static void execute(Scenario s){
  var boss=s.boss;var attack=boss.getStandardAttack();var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();
  control.sampleMobility();control.setCombatRegion(s.region);
  boolean visible=s.target.isAlive()&&boss.getSensing().hasLineOfSight(s.target);if(!visible)s.hidden=true;
  control.setIntent(s.goalTicks>=62&&!s.hidden?FlightController.Intent.move(new FlightVector(0,0,1),.14):FlightController.Intent.hold());
  int before=attack.firedCount();long started=System.nanoTime();Vec3 look=attack.tick(s.target,visible,false);long elapsed=System.nanoTime()-started;
  if(look!=null)((SoutouGhastFlightLookControl)boss.getLookControl()).setIntent(look);
  var memory=(RememberedCoverLob)inspect(attack,"cover");var state=attack.state();var row=new JsonObject();
  row.addProperty("goalTick",++s.goalTicks);row.addProperty("phase",state.phase().name());row.addProperty("chargeTick",state.ticks());row.addProperty("fire",state.fire());row.addProperty("emitted",attack.firedCount()>before);row.addProperty("fired",attack.firedCount());row.addProperty("visible",visible);row.addProperty("coverActive",memory.active());
  row.addProperty("age",memory.age());row.addProperty("choice",attack.selectedProfile().name());row.addProperty("lastFired",attack.lastFiredProfile().name());row.addProperty("rejection",attack.lastRejection());row.addProperty("context",control.getMobilityContext().name());row.addProperty("navigationCalls",s.navigationCalls);row.addProperty("attackNanos",elapsed);
  row.addProperty("lastCandidate",attack.lastCandidate());
  row.add("beforePosition",vector(boss.position()));row.add("region",region(s.region));row.addProperty("profileCueTick",attack.profileCueTick());
  if(memory.snapshot()!=null)row.add("snapshot",new Gson().toJsonTree(memory.snapshot()));
  if(attack.committedProfile()!=null)row.add("recipe",new Gson().toJsonTree(attack.committedProfile()));
  if(attack.lastPreflight()!=null){var proof=attack.lastPreflight();row.addProperty("clear",proof.result().clear());row.addProperty("segments",proof.result().segments());row.add("terminal",new Gson().toJsonTree(proof.terminal().stream().map(t->new int[]{t.position().getX(),t.position().getY(),t.position().getZ()}).toList()));}
  if(memory.snapshot()!=null){var point=SoutouGhastInertialMoveControl.to(memory.snapshot().eye());boolean loaded=CommittedPathClearance.loadedRay(boss.level(),boss.getEyePosition(),point);row.addProperty("storedRayLoaded",loaded);if(loaded){var hit=boss.level().clip(new ClipContext(boss.getEyePosition(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss));row.addProperty("storedRay",hit.getType().name());if(hit.getType()==HitResult.Type.BLOCK)row.add("storedRayBlock",new Gson().toJsonTree(new int[]{hit.getBlockPos().getX(),hit.getBlockPos().getY(),hit.getBlockPos().getZ()}));}}
  s.goalRow=row;s.tick=boss.level().getGameTime();if(attack.firedCount()>0)s.postFire++;
 }
 @SubscribeEvent public void join(EntityJoinLevelEvent event){
  if(!active||event.getLevel().isClientSide()||!(event.getEntity() instanceof StandardSoutouFireball shot)||!Objects.equals(shot.originUuid(),bossUuid))return;
  ownedShots.put(shot.getUUID(),shot);var row=entity(shot);row.addProperty("origin",shot.originUuid().toString());row.addProperty("owner",shot.getOwner()==null?"":shot.getOwner().getUUID().toString());row.addProperty("type",shot.getType().toString());if(shot instanceof CommittedSoutouFireball committed)row.add("path",new Gson().toJsonTree(committed.flight().path()));shots.add(row);try{append("shots.jsonl",row);}catch(IOException e){observerError="SHOT_PUBLICATION:"+e.getMessage();}
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!enabled())return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception e){errors.add(e.getClass().getSimpleName()+":"+e.getMessage());try{finish("FAIL");}catch(IOException ignored){active=false;done=true;}}
 }
 private void step(ServerLevel current)throws IOException{
  level=current;
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/cover-lob");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"BOUNDED_REQUEST");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_COVER_LOB_NONCE"))&&request.get("maxTicks").getAsInt()==180&&request.get("maxWallMs").getAsInt()==18000,"REQUEST_IDENTITY");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_COVER_LOB_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_AGE");deadline=dispatch+18000;Files.createDirectories(output);active=true;
  }
  require(samples++<180&&System.currentTimeMillis()<deadline&&observerError==null,"FINITE_WINDOW_OR_OBSERVER_ERROR");
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&player.level()==level&&level.players().size()==1,"GENUINE_PLAYER");canonicalPlayer=true;drain();
  if(scenario==null){
   require(level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()))==null&&level.getEntitiesOfClass(SoutouGhast.class,new AABB(0,224,0,52,248,52)).isEmpty(),"NO_CANONICAL_COMBAT_SUBJECT");fixtureSubjectAbsent=true;
   for(int y=224;y<=231;y++)for(int z=26;z<=42;z++){var pos=new BlockPos(27,y,z);require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir()&&level.getEntities(null,new AABB(pos)).isEmpty(),"NEW_STATIC_WALL_AIR");}
   for(int y=224;y<=231;y++)for(int z=26;z<=42;z++){var pos=new BlockPos(27,y,z);require(level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState()),"NEW_STATIC_WALL_WRITE");wall.add(pos);}
   scenario=new Scenario();var s=scenario;s.target=new TestCow(level);s.target.s=s;s.target.setPos(TARGET_START);require(level.noCollision(s.target,s.target.getBoundingBox())&&level.addFreshEntity(s.target),"NEW_NATIVE_TARGET");
   s.boss=new TestGhast(level);s.boss.s=s;s.boss.setPos(12,230,23);s.region=new CombatAnchor.Region(from(s.boss.position()),CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);bossUuid=s.boss.getUUID();require(level.noCollision(s.boss,s.boss.getBoundingBox())&&level.addFreshEntity(s.boss),"NEW_NATIVE_BOSS");
   actors=entity(s.boss);actors.addProperty("targetUuid",s.target.getUUID().toString());actors.addProperty("privateControlledGoals",true);actors.addProperty("targetMovement","NEW_COW_NATIVE_NAVIGATION_AFTER_ACTUAL_LOS_LOSS");actors.addProperty("bossMovement","HOLD62_THEN_CONTROLLER_DRIFT_Z_0.14_UNTIL_LOS_LOSS");actors.addProperty("width",s.boss.getBbWidth());actors.addProperty("height",s.boss.getBbHeight());actors.add("region",region(s.region));actors.add("wall",new Gson().toJsonTree(wall.stream().map(p->new int[]{p.getX(),p.getY(),p.getZ()}).toList()));Files.writeString(output.resolve("actors.json"),actors+"\n",StandardOpenOption.CREATE_NEW);return;
  }
  var s=scenario;require(s.boss.isAlive()&&s.target.isAlive()&&!s.boss.isNoAi()&&!s.target.isNoAi()&&level.getEntity(s.boss.getUUID())==s.boss&&level.getEntity(s.target.getUUID())==s.target&&level.hasChunkAt(s.boss.blockPosition())&&level.hasChunkAt(s.target.blockPosition()),"LIVE_LOADED_OWNED_ACTORS");
  if(s.tick==level.getGameTime()&&s.recorded!=s.tick){s.recorded=s.tick;var row=entity(s.boss);for(var e:s.goalRow.entrySet())row.add(e.getKey(),e.getValue());row.add("targetNow",vector(s.target.position()));row.addProperty("targetHealth",s.target.getHealth());row.addProperty("registered",true);row.addProperty("loaded",true);
   var projectiles=new JsonArray();for(var owned:ownedShots.values())if(owned instanceof CommittedSoutouFireball shot&&shot.isAlive()&&level.getEntity(shot.getUUID())==shot){var measured=entity(shot);measured.addProperty("index",shot.flight().index());measured.addProperty("kind",shot.flight().path().kind().name());measured.addProperty("phase",shot.phase());projectiles.add(measured);}row.add("projectiles",projectiles);
   rows.add(row);require(rows.size()<=180,"ROW_BOUND");append("rows.jsonl",row);}
  if(s.postFire>=36&&clientRows.size()>=4)finish("PASS");
 }
 private void drain()throws IOException{JsonObject row;while((row=queue.poll())!=null){queueSize.decrementAndGet();if(row.remove("record").getAsString().equals("JOIN")){joins.add(row);append("joins.jsonl",row);}else{clientRows.add(row);require(clientRows.size()<=4,"CLIENT_ROW_BOUND");append("clientRows.jsonl",row);}}}
 private void finish(String status)throws IOException{
  active=false;done=true;try{drain();}catch(IOException e){errors.add("Drain:"+e.getMessage());status="FAIL";}
  var owned=new ArrayList<Entity>(ownedShots.values());if(scenario!=null){if(scenario.boss!=null)owned.add(scenario.boss);if(scenario.target!=null)owned.add(scenario.target);}
  for(Entity e:owned){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());if(e.level() instanceof ServerLevel world&&world.getEntity(e.getUUID())==e){e.discard();row.addProperty("status","DISCARDED_PRIVATE_ACTOR");}else row.addProperty("status","ALREADY_REMOVED");cleanup.add(row);}
  for(var pos:wall){var row=new JsonObject();row.add("position",new Gson().toJsonTree(new int[]{pos.getX(),pos.getY(),pos.getZ()}));if(level.getBlockState(pos).is(Blocks.STONE)&&level.getEntities(null,new AABB(pos)).isEmpty()&&level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()))row.addProperty("status","RESTORED_NEW_PRIVATE_AIR");else{row.addProperty("status","CONFLICT_RETAINED");errors.add("PRIVATE_WALL_RESTORE_CONFLICT");status="FAIL";}wallCleanup.add(row);}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("status",status);result.addProperty("samples",samples);result.addProperty("canonicalPlayer",canonicalPlayer);result.addProperty("fixtureSubjectAbsent",fixtureSubjectAbsent);result.addProperty("canonicalOverrides",false);result.add("actors",actors);result.add("rows",rows);result.add("shots",shots);result.add("joins",joins);result.add("clientRows",clientRows);result.add("cleanup",cleanup);result.add("wallCleanup",wallCleanup);if(observerError!=null)errors.add(observerError);result.add("errors",new Gson().toJsonTree(errors));Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
 private static JsonObject entity(Entity e){var row=new JsonObject();row.addProperty("uuid",e.getUUID().toString());row.addProperty("id",e.getId());row.addProperty("tick",e.level().getGameTime());row.addProperty("epoch",System.currentTimeMillis());row.add("position",vector(e.position()));return row;}
 private static JsonObject vector(Vec3 v){var row=new JsonObject();row.addProperty("x",v.x);row.addProperty("y",v.y);row.addProperty("z",v.z);return row;}
 private static JsonObject region(CombatAnchor.Region r){var row=new JsonObject();row.add("center",vector(SoutouGhastInertialMoveControl.to(r.center())));row.add("radii",vector(SoutouGhastInertialMoveControl.to(r.radii())));row.addProperty("generation",r.generation());return row;}
 private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
 private static void require(boolean condition,String reason)throws IOException{if(!condition)throw new IOException("COVER_LOB_"+reason);}
 private void append(String name,JsonObject row)throws IOException{Files.writeString(output.resolve(name),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void enqueue(JsonObject row){if(queueSize.incrementAndGet()>128){queueSize.decrementAndGet();observerError="CLIENT_QUEUE_BOUND";return;}queue.add(row);}
 @Mod.EventBusSubscriber(modid="naturalghast_cover_lob_probe",value=Dist.CLIENT)
 public static final class Client {
  private static SoutouGhast pending,observed;private static int count;
  @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!active||!event.getLevel().isClientSide()||!(event.getEntity() instanceof SoutouGhast boss)||!boss.getUUID().equals(bossUuid))return;if(event.isCanceled()||pending!=null||observed!=null){observerError="DUPLICATE_CLIENT_BOSS";return;}pending=boss;}
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!active||!enabled()||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.isSameThread())return;
   if(pending!=null){var row=entity(pending);row.addProperty("renderer",mc.getEntityRenderDispatcher().getRenderer(pending).getClass().getName());row.addProperty("record","JOIN");enqueue(row);observed=pending;pending=null;}
   if(observed!=null&&count<4){if(observed.isRemoved()||mc.level.getEntity(observed.getId())!=observed){observerError="CLIENT_BOSS_IDENTITY";return;}var row=entity(observed);row.addProperty("record","ROW");enqueue(row);count++;}
  }
 }
}

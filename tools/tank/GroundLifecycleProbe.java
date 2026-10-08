package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
import com.genki.soutoughast.entity.ai.flight.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Controlled environment changes only; actual Ground AI/controller run normally. */
@Mod("naturalghast_ground_lifecycle_probe")
public final class GroundLifecycleProbe {
 private static final String SCOPE="CONTROLLED_NATIVE_GROUND_LIFECYCLE_NOT_INPUT_OR_FULL_RELEASE";
 private JsonObject request;private Path output;private long start,stageStart,deadline;private int samples;
 private String stage="WAIT_GROUND";private boolean done;private BlockPos hole;
 private final GroundCeilingLedger ceiling=new GroundCeilingLedger();
 private final JsonArray rows=new JsonArray(),changes=new JsonArray();private final List<String> errors=new ArrayList<>();
 public GroundLifecycleProbe(){MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!"1".equals(System.getenv("KNEEKURA_DEBUG_GROUND_LIFECYCLE")))return;
  var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception error){errors.add(error.getClass().getSimpleName()+":"+error.getMessage());try{finish(server.overworld(),"FAIL");}catch(Exception writeFailure){done=true;}}
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/ground-lifecycle");Path file=output.resolve("request.json");if(!Files.exists(file))return;
   require(!Files.isSymbolicLink(file)&&Files.size(file)<=4096,"REQUEST_PATH_SIZE");request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   require(request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","dispatchEpochMs"))&&request.get("maxTicks").getAsInt()==240&&request.get("maxWallMs").getAsInt()==20000&&request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_GROUND_LIFECYCLE_NONCE")),"REQUEST_SCOPE");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_GROUND_LIFECYCLE_WORLD")).toRealPath();require(expected.equals(Path.of(request.get("world").getAsString()).toRealPath())&&expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()),"WORLD_IDENTITY");
   require(!Files.exists(run.resolve("control/owner-envelope.json"))&&!Files.exists(run.resolve("control/owner-status.json")),"SCOPED_OWNER_PRESENT");long now=System.currentTimeMillis(),dispatch=request.get("dispatchEpochMs").getAsLong();require(dispatch<=now&&now-dispatch<=5000,"DISPATCH_EXPIRED");deadline=Math.addExact(dispatch,20000);start=level.getGameTime();stageStart=start-1;
  }
  require(samples++<240&&System.currentTimeMillis()<deadline,"BOUNDS");
  var entity=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  require(entity instanceof SoutouGhast&&player!=null&&!(player instanceof FakePlayer)&&player.connection!=null&&player.connection.connection.isConnected()&&player.level()==level&&!player.isCreative()&&!player.isSpectator()&&player.isAlive(),"GENUINE_LIVE_PLAYER");
  var boss=(SoutouGhast)entity;require(boss.isAlive()&&!boss.isNoAi()&&!boss.getDomainAttack().active()&&!boss.getOverheadAttack().active(),"NATURAL_GROUND_ONLY");
  var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();var state=boss.getGroundCombat().state();var position=SoutouGhastInertialMoveControl.from(boss.position());var velocity=boss.getDeltaMovement();var clearance=new GroundClearance(boss);
  JsonObject row=new JsonObject();long tick=level.getGameTime(),elapsed=tick-stageStart;row.addProperty("tick",tick);row.addProperty("stage",stage);row.addProperty("elapsed",elapsed);row.addProperty("phase",state.phase().name());row.addProperty("reason",state.reason());
  row.addProperty("x",boss.getX());row.addProperty("y",boss.getY());row.addProperty("z",boss.getZ());row.addProperty("vx",velocity.x);row.addProperty("vy",velocity.y);row.addProperty("vz",velocity.z);row.addProperty("intent",control.getIntent().mode().name());row.addProperty("support",clearance.support(position,position));
  int mask=0;for(int i:new int[]{0,2,4,6,8,9})if(clearance.body(position,position.add(MobilityContext.direction(i).scale(i<8?8:4))))mask|=1<<i;
  for(int i=1;i<8;i+=2)if((mask&(1<<(i-1)))!=0&&(mask&(1<<((i+1)%8)))!=0)mask|=1<<i;
  row.addProperty("mask",mask);row.addProperty("fired",boss.getGroundCombat().firedCount());row.addProperty("playerHealth",player.getHealth());row.addProperty("bossHealth",boss.getHealth());
  var region=control.getCombatRegion();if(region!=null){JsonObject anchor=new JsonObject();anchor.addProperty("generation",region.generation());anchor.addProperty("x",region.center().x());anchor.addProperty("y",region.center().y());anchor.addProperty("z",region.center().z());anchor.addProperty("rx",region.radii().x());anchor.addProperty("ry",region.radii().y());anchor.addProperty("rz",region.radii().z());row.add("region",anchor);}else row.add("region",JsonNull.INSTANCE);
  rows.add(row);append("rows.jsonl",row);require(rows.size()<240,"ROW_BOUND");
  switch(stage){
   case "WAIT_GROUND" -> {
    require(elapsed<=80,"NATURAL_LANDING_TIMEOUT");
    if(state.phase()==GroundCombat.Phase.GROUNDED&&state.reason().equals("READABLE_PAUSE")&&velocity.length()<.03&&region!=null){
     hole=new BlockPos((int)Math.floor(boss.getX()-1.5),223,(int)Math.floor(boss.getZ()-1.5));require(player.getBoundingBox().inflate(1).intersects(new AABB(hole))==false&&clearance.support(position,position),"CONTROLLED_SUPPORT_TILE");changeFloor(level,false,"FLOOR_OPEN");next(level,"FLOOR_LOST");
    }
   }
   case "FLOOR_LOST" -> {require(state.phase()==GroundCombat.Phase.SAFE_HOLD,"SUPPORT_LOSS_NOT_HELD");if(elapsed==8){changeFloor(level,true,"FLOOR_RESTORE");next(level,"RESTORED");}}
   case "RESTORED" -> {require(elapsed<=45,"NATURAL_REGROUND_TIMEOUT");if(state.phase()==GroundCombat.Phase.GROUNDED&&state.reason().equals("READABLE_PAUSE")&&velocity.length()<.03){changeCeiling(level,false,"CEILING_OPEN_TRANSIENT");next(level,"TRANSIENT");}}
   case "TRANSIENT" -> {require(state.phase()==GroundCombat.Phase.GROUNDED,"TRANSIENT_TAKEOFF");if(elapsed==10){changeCeiling(level,true,"CEILING_CLOSE");next(level,"RECLOSED");}}
   case "RECLOSED" -> {require(state.phase()==GroundCombat.Phase.GROUNDED,"RECLOSED_TAKEOFF");if(elapsed==5){changeCeiling(level,false,"CEILING_OPEN_SUSTAINED");next(level,"SUSTAINED");}}
   case "SUSTAINED" -> {require(state.phase()==(elapsed==20?GroundCombat.Phase.TAKEOFF:GroundCombat.Phase.GROUNDED),"CLEAR_DWELL");if(elapsed==20)next(level,"ASCENT");}
   case "ASCENT" -> {require(elapsed<=120,"TAKEOFF_TIMEOUT");if(state.phase()==GroundCombat.Phase.AIR)finish(level,"PASS");else require(state.phase()==GroundCombat.Phase.TAKEOFF,"TAKEOFF_ABORT");}
   default -> throw new IllegalStateException("UNKNOWN_STAGE");
  }
 }
 private void next(ServerLevel level,String value){stage=value;stageStart=level.getGameTime();}
 private void changeFloor(ServerLevel level,boolean restore,String kind)throws IOException{
  require(hole!=null&&hole.getX()>=0&&hole.getX()<52&&hole.getZ()>=0&&hole.getZ()<52&&level.hasChunkAt(hole)&&level.getBlockEntity(hole)==null,"FLOOR_BOUND");
  var before=restore?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState();var after=restore?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();require(level.getBlockState(hole).equals(before),"FLOOR_OWNER_GUARD");
  if(restore)require(level.getEntities(null,new AABB(hole)).isEmpty(),"FLOOR_RESTORE_ACTOR");require(level.setBlock(hole,after,3)&&level.getBlockState(hole).equals(after),"FLOOR_WRITE");mutation(level,kind,1);if(restore)hole=null;
 }
 private GroundCeilingLedger.Cells ceilingCells(ServerLevel level){return new GroundCeilingLedger.Cells(){
  private BlockPos pos(int index){return new BlockPos(index/52,230,index%52);}
  public GroundCeilingLedger.Cell read(int index){var cell=pos(index);if(!level.hasChunkAt(cell))return GroundCeilingLedger.Cell.UNLOADED;if(level.getBlockEntity(cell)!=null)return GroundCeilingLedger.Cell.OTHER;var value=level.getBlockState(cell);return value.equals(Blocks.STONE.defaultBlockState())?GroundCeilingLedger.Cell.STONE:value.equals(Blocks.AIR.defaultBlockState())?GroundCeilingLedger.Cell.AIR:GroundCeilingLedger.Cell.OTHER;}
  public boolean actorFree(int index){return level.getEntities(null,new AABB(pos(index))).isEmpty();}
  public void write(int index,boolean stone){require(level.setBlock(pos(index),stone?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3),"CEILING_NATIVE_WRITE");}
 };}
 private void changeCeiling(ServerLevel level,boolean restore,String kind)throws IOException{ceiling.transition(ceilingCells(level),restore);mutation(level,kind,2704);}
 private void mutation(ServerLevel level,String kind,int count)throws IOException{var value=new JsonObject();value.addProperty("tick",level.getGameTime());value.addProperty("kind",kind);value.addProperty("count",count);value.addProperty("prevalidated",true);value.addProperty("readback",true);changes.add(value);append("changes.jsonl",value);}
 private void finish(ServerLevel level,String status)throws IOException{
  if(done)return;var restoration=new JsonObject();
  try{if(hole!=null)changeFloor(level,true,"FAILURE_FLOOR_RESTORE");restoration.addProperty("floor","RESTORED");}catch(Exception error){restoration.addProperty("floor","RETAINED_CONFLICT_OR_ACTOR");errors.add("FloorClosure:"+error.getMessage());status="FAIL";}
  var cleanup=ceiling.cleanup(ceilingCells(level));restoration.addProperty("ceilingRestoredCells",cleanup.restored());restoration.addProperty("ceilingActorBlockedCells",cleanup.actorBlocked());restoration.addProperty("ceilingConflictCells",cleanup.conflicts());
  restoration.addProperty("ceiling",cleanup.conflicts()>0?"RETAINED_CONFLICT":cleanup.actorBlocked()>0?"RETAINED_ACTOR_BLOCKED":"RESTORED");
  if(cleanup.conflicts()>0){errors.add("CEILING_CLOSURE_CONFLICT");status="FAIL";}
  var result=new JsonObject();result.addProperty("scope",SCOPE);result.addProperty("status",status);if(request!=null)result.addProperty("nonce",request.get("nonce").getAsString());result.addProperty("canonicalPlayer",true);result.addProperty("overrides",false);result.addProperty("startTick",start);result.addProperty("endTick",level.getGameTime());result.add("rows",rows);result.add("changes",changes);result.add("restoration",restoration);var failures=new JsonArray();for(var error:errors)failures.add(error);result.add("errors",failures);
  Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);done=true;
 }
 private void append(String file,JsonObject value)throws IOException{Files.writeString(output.resolve(file),value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
}

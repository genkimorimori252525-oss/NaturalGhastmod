package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Loaded-only native boundary. Use only through the durable overlay engine; no event registration. */
public final class DomainWorldAdapter implements DomainOverlay.World {
 public static final int MAX_PREFLIGHT_BATCH=128;
 private static final Set<Block> PALETTE=Set.of(Blocks.AIR,Blocks.CAVE_AIR,Blocks.VOID_AIR,Blocks.STONE,Blocks.COBBLESTONE,Blocks.DIRT,Blocks.GRASS_BLOCK,Blocks.DEEPSLATE,Blocks.NETHERRACK,Blocks.BLACKSTONE,Blocks.POLISHED_BLACKSTONE,Blocks.RED_NETHER_BRICKS);
 private final ServerLevel level;private final Map<DomainGeometry.Cell,DomainOverlay.Entry> entries;
 private final Map<String,BlockState> states;
 private DomainWorldAdapter(ServerLevel level,DomainOverlay.Journal journal)throws IOException{
  this.level=Objects.requireNonNull(level);thread(level);
  if(!level.dimension().location().toString().equals(journal.identity().dimension()))throw new IOException("DOMAIN_NATIVE_DIMENSION");
  Map<DomainGeometry.Cell,DomainOverlay.Entry> cells=new HashMap<>();Map<String,BlockState> decoded=new HashMap<>();
  for(var entry:journal.entries()){
   bounds(level,entry.cell());cells.put(entry.cell(),entry);
   for(String text:List.of(entry.original(),entry.overlay()))if(!decoded.containsKey(text)){BlockState state=DomainStateCodec.decode(text);safeState(state);decoded.put(text,state);}
  }
  entries=Map.copyOf(cells);states=Map.copyOf(decoded);
 }
 /** Decode the entire ledger before exposing any native mutation operation. No chunk is loaded. */
 public static DomainWorldAdapter forJournal(ServerLevel level,DomainOverlay.Journal journal)throws IOException{return new DomainWorldAdapter(level,journal);}
 @Override public boolean loaded(DomainGeometry.Cell cell)throws IOException{owned(cell);return loaded(level,cell);}
 @Override public String state(DomainGeometry.Cell cell)throws IOException{requireLoaded(cell);return DomainStateCodec.encode(level.getBlockState(pos(cell)));}
 @Override public boolean occupied(DomainGeometry.Cell cell,String desired)throws IOException{
  requireLoaded(cell);BlockState decoded=desired(cell,desired);return occupied(level,cell,decoded);
 }
 @Override public void set(DomainGeometry.Cell cell,String desired)throws IOException{
  requireLoaded(cell);BlockState decoded=desired(cell,desired);var entry=entries.get(cell);
  String predecessor=desired.equals(entry.original())?entry.overlay():entry.original();
  if(!state(cell).equals(predecessor)||occupied(level,cell,decoded)||level.getBlockState(pos(cell)).hasBlockEntity())throw new IOException("DOMAIN_NATIVE_CONTEXT_CHANGED");
  // Simple full-block/air palette needs no neighbor-dependent shape updates; clients receive changes.
  if(!level.setBlock(pos(cell),decoded,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)||!loaded(level,cell)||!level.getBlockState(pos(cell)).equals(decoded))throw new IOException("DOMAIN_NATIVE_MUTATION_UNVERIFIED");
 }
 private BlockState desired(DomainGeometry.Cell cell,String desired)throws IOException{
  var entry=entries.get(cell);if(!desired.equals(entry.original())&&!desired.equals(entry.overlay()))throw new IOException("DOMAIN_NATIVE_UNJOURNALED_STATE");return states.get(desired);
 }
 private void owned(DomainGeometry.Cell cell)throws IOException{thread(level);if(!entries.containsKey(cell))throw new IOException("DOMAIN_NATIVE_UNJOURNALED_CELL");bounds(level,cell);}
 private void requireLoaded(DomainGeometry.Cell cell)throws IOException{if(!loaded(cell))throw new IOException("DOMAIN_NATIVE_UNLOADED");}
 private static void thread(ServerLevel level)throws IOException{if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level)throw new IOException("DOMAIN_NATIVE_SERVER_THREAD_OR_LEVEL");}
 private static BlockPos pos(DomainGeometry.Cell cell){return new BlockPos(cell.x(),cell.y(),cell.z());}
 private static void bounds(ServerLevel level,DomainGeometry.Cell cell)throws IOException{
  if(Math.abs((long)cell.x())>=30_000_000||Math.abs((long)cell.z())>=30_000_000||cell.y()<level.getMinBuildHeight()||cell.y()>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(pos(cell)))throw new IOException("DOMAIN_NATIVE_BOUNDS");
 }
 private static boolean loaded(ServerLevel level,DomainGeometry.Cell cell)throws IOException{thread(level);bounds(level,cell);return level.hasChunkAt(pos(cell));}
 private static void safeState(BlockState state)throws IOException{if(!PALETTE.contains(state.getBlock())||state.hasBlockEntity()||!state.getFluidState().isEmpty())throw new IOException("DOMAIN_NATIVE_UNSAFE_STATE");}
 private static boolean occupied(ServerLevel level,DomainGeometry.Cell cell,BlockState desired)throws IOException{
  if(!loaded(level,cell))throw new IOException("DOMAIN_NATIVE_UNLOADED");
  if(desired.isAir()&&level.getBlockState(pos(cell)).isAir())return false;
  AABB box=new AABB(pos(cell));
  // Removing a block must also protect actors standing on it, including during restoration.
  if(desired.isAir())box=box.expandTowards(0,1,0);
  List<Entity> actors=level.getEntities((Entity)null,box);if(actors.size()>64)throw new IOException("DOMAIN_NATIVE_ACTOR_LIMIT");
  final AABB protectedBox=box;
  return actors.stream().anyMatch(e->!e.isRemoved()&&e.getBoundingBox().intersects(protectedBox));
 }
 /** Read-only incremental preflight. Rejection never mutates the world or reduces the planned size. */
 public static final class Preflight {
  private final ServerLevel level;private final DomainGeometry.Plan plan;private final List<DomainOverlay.Change> changes=new ArrayList<>();
  private int cursor;private boolean failed;
  public Preflight(ServerLevel level,DomainGeometry.Plan plan)throws IOException{
   this.level=Objects.requireNonNull(level);this.plan=Objects.requireNonNull(plan);thread(level);
   if(!DomainGeometry.plan(plan.centerX(),plan.floorY(),plan.centerZ(),level.getMinBuildHeight(),level.getMaxBuildHeight()).equals(plan))throw new IOException("DOMAIN_NATIVE_NONCANONICAL_PLAN");
  }
  public boolean complete(){return !failed&&cursor==plan.cells().size();}
  public int checkedCells(){return cursor;}
  public List<DomainOverlay.Change> changes()throws IOException{thread(level);if(!complete())throw new IOException("DOMAIN_NATIVE_PREFLIGHT_INCOMPLETE");return List.copyOf(changes);}
  public void step(int budget)throws IOException{
   thread(level);if(failed)throw new IOException("DOMAIN_NATIVE_PREFLIGHT_HALTED");if(budget<1||budget>MAX_PREFLIGHT_BATCH)throw new IllegalArgumentException("DOMAIN_PREFLIGHT_BATCH");
   try{for(int end=Math.min(plan.cells().size(),cursor+budget);cursor<end;cursor++){
    var tile=plan.cells().get(cursor);var cell=tile.cell();if(!loaded(level,cell))throw new IOException("DOMAIN_NATIVE_PREFLIGHT_UNLOADED");
    BlockState original=level.getBlockState(pos(cell));safeState(original);
    if(tile.role()==DomainGeometry.Role.FLOOR&&!supportsFloor(level,cell,original))throw new IOException("DOMAIN_NATIVE_EXISTING_FLOOR_REQUIRED");
    BlockState overlay=switch(tile.role()){
     case FLOOR->Blocks.STONE.defaultBlockState();case INTERIOR->Blocks.AIR.defaultBlockState();
     case SHELL->(Math.floorMod(cell.x()+cell.z(),8)==0?Blocks.RED_NETHER_BRICKS:Blocks.BLACKSTONE).defaultBlockState();
    };
    if(original.equals(overlay))continue;if(occupied(level,cell,overlay))throw new IOException("DOMAIN_NATIVE_PREFLIGHT_OCCUPIED");
    if(changes.size()>=DomainOverlay.MAX_CHANGED)throw new IOException("DOMAIN_NATIVE_CHANGE_BUDGET");changes.add(new DomainOverlay.Change(cell,DomainStateCodec.encode(original),DomainStateCodec.encode(overlay)));
   }}catch(IOException|RuntimeException error){failed=true;throw error;}
  }
 }
 /** One bounded1257-cell commitment sweep. Reuse neither stale floor proof nor forced chunks. */
 public static void verifyExistingFloor(ServerLevel level,DomainGeometry.Plan plan)throws IOException{
  thread(level);if(!DomainGeometry.plan(plan.centerX(),plan.floorY(),plan.centerZ(),level.getMinBuildHeight(),level.getMaxBuildHeight()).equals(plan))throw new IOException("DOMAIN_NATIVE_NONCANONICAL_PLAN");
  int floorCells=0;
  for(var tile:plan.cells())if(tile.role()==DomainGeometry.Role.FLOOR){
   if(++floorCells>1257)throw new IOException("DOMAIN_NATIVE_FLOOR_BUDGET");var cell=tile.cell();
   if(!loaded(level,cell))throw new IOException("DOMAIN_NATIVE_FLOOR_UNLOADED");BlockState state=level.getBlockState(pos(cell));safeState(state);
   if(!supportsFloor(level,cell,state))throw new IOException("DOMAIN_NATIVE_EXISTING_FLOOR_REQUIRED");
  }
  if(floorCells!=1257)throw new IOException("DOMAIN_NATIVE_NONCANONICAL_FLOOR");
 }
 private static boolean supportsFloor(ServerLevel level,DomainGeometry.Cell cell,BlockState state){return !state.isAir()&&Block.isFaceFull(state.getCollisionShape(level,pos(cell)),Direction.UP);}
}

package com.genki.soutoughast.entity.ai.domain;

import java.io.IOException;
import java.util.*;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.GroundClearance;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
import com.genki.soutoughast.entity.ai.flight.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/** One read-only, finite candidate. Never freezes ordinary movement or grants mutation authority. */
public final class DomainPreparation {
 private final SoutouGhast boss;private final ServerPlayer player;private final ServerLevel level;
 private final long regionGeneration;private final int createdTick;private final DomainGeometry.Plan plan;
 private final DomainWorldAdapter.Preflight preflight;private boolean discarded;
 public DomainPreparation(SoutouGhast boss,ServerPlayer player,CombatAnchor.Region region)throws IOException{
  this.boss=Objects.requireNonNull(boss);this.player=Objects.requireNonNull(player);Objects.requireNonNull(region);
  if(!(boss.level() instanceof ServerLevel nativeLevel))throw new IOException("DOMAIN_PREPARE_SERVER_LEVEL");level=nativeLevel;
  regionGeneration=region.generation();createdTick=level.getServer().getTickCount();
  var floor=new GroundClearance(boss).floor(SoutouGhastInertialMoveControl.from(boss.position()));
  if(floor==null)throw new IOException("DOMAIN_PREPARE_NO_COMMON_FLOOR");
  int x=(int)Math.floor((boss.getX()+player.getX())*.5),z=(int)Math.floor((boss.getZ()+player.getZ())*.5);
  try{plan=DomainGeometry.plan(x,(int)Math.rint(floor.y()),z,level.getMinBuildHeight(),level.getMaxBuildHeight());}
  catch(IllegalArgumentException error){throw new IOException("DOMAIN_PREPARE_BOUNDS",error);}
  preflight=new DomainWorldAdapter.Preflight(level,plan);validate(region,true);
 }
 public DomainGeometry.Plan plan(){return plan;}
 public UUID participant(){return player.getUUID();}
 public FlightVector landing(){return new FlightVector(boss.getX(),plan.floorY(),boss.getZ());}
 public boolean complete(){return !discarded&&preflight.complete();}
 public void step(CombatAnchor.Region region,boolean visible)throws IOException{
  try{validate(region,visible);preflight.step(128);}catch(IOException|RuntimeException error){discarded=true;throw error;}
 }
 public List<DomainOverlay.Change> commit(CombatAnchor.Region region,boolean visible)throws IOException{
  try{
   validate(region,visible);if(!complete())throw new IOException("DOMAIN_PREPARE_INCOMPLETE");
   DomainWorldAdapter.verifyExistingFloor(level,plan);validate(region,visible);return preflight.changes();
  }catch(IOException|RuntimeException error){discarded=true;throw error;}
 }
 /** Candidate-only freshness; committed encounters use participantsFit without the preparation age. */
 public void validate(CombatAnchor.Region region,boolean visible)throws IOException{
  int age=level.getServer().getTickCount()-createdTick;
  if(discarded||age<0||age>200||!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level
    ||region==null||region.generation()!=regionGeneration||boss.level()!=level||player.level()!=level
    ||!boss.isAlive()||boss.isRemoved()||!player.isAlive()||player.isRemoved()||player.isSpectator()||player.isCreative()
    ||level.getServer().getPlayerList().getPlayer(player.getUUID())!=player||boss.getTarget()!=player||!visible||!boss.getSensing().hasLineOfSight(player))throw new IOException("DOMAIN_PREPARE_INVALIDATED");
  if(!participantsFit(boss,player,level,plan))throw new IOException("DOMAIN_PREPARE_PARTICIPANT_FIT_OR_SUPPORT");
 }
 public static boolean participantsFit(SoutouGhast boss,ServerPlayer player,ServerLevel level,DomainGeometry.Plan plan){
  if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level||boss.level()!=level||player.level()!=level)return false;
  AABB body=boss.getBoundingBox(),participant=player.getBoundingBox();
  if(!inside(plan,body)||!inside(plan,participant)||body.minY-plan.floorY()>8||participant.minY-plan.floorY()>8)return false;
  AABB descent=body.expandTowards(0,plan.floorY()-body.minY,0);
  return loaded(level,descent)&&loaded(level,participant)&&level.noCollision(boss,descent)&&level.noCollision(player,participant)
    &&support(level,body,plan.floorY())&&support(level,participant,plan.floorY());
 }
 public static boolean inside(DomainGeometry.Plan plan,AABB box){return DomainInterior.contains(plan,box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ);}
 private static boolean support(ServerLevel level,AABB box,int floor){
  for(int x=(int)Math.floor(box.minX+1e-7);x<Math.ceil(box.maxX-1e-7);x++)for(int z=(int)Math.floor(box.minZ+1e-7);z<Math.ceil(box.maxZ-1e-7);z++){
   BlockPos pos=new BlockPos(x,floor-1,z);if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);
   if(state.hasBlockEntity()||!state.getFluidState().isEmpty()||!Block.isFaceFull(state.getCollisionShape(level,pos),Direction.UP))return false;
  }return true;
 }
 private static boolean loaded(ServerLevel level,AABB box){
  if(box.minY<level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box))return false;
  for(int x=Math.floorDiv((int)Math.floor(box.minX)-1,16);x<=Math.floorDiv((int)Math.floor(box.maxX)+1,16);x++)
   for(int z=Math.floorDiv((int)Math.floor(box.minZ)-1,16);z<=Math.floorDiv((int)Math.floor(box.maxZ)+1,16);z++)if(!level.hasChunk(x,z))return false;
  return true;
 }
}

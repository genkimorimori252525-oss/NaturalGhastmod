package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;

/** Three real changed cells, full reservation footprint, two real actor UUIDs; not boss combat. */
final class DomainCoordinatorProbe implements AutoCloseable {
 private final DomainNativeProbe.Trial t;private final DomainNativeCoordinator nativeCoordinator;
 private final DomainCoordinator coordinator;private final ArmorStand owner,incomingOwner;
 private final DomainCoordinator.Handle handle;private boolean clashed,finished;
 private final List<DomainGeometry.Cell> cells=List.of(new DomainGeometry.Cell(70,16,64),new DomainGeometry.Cell(71,16,64),new DomainGeometry.Cell(72,16,64));
 DomainCoordinatorProbe(DomainNativeProbe.Trial trial)throws Exception{
  t=trial;nativeCoordinator=DomainNativeCoordinator.open(t.level.getServer());coordinator=nativeCoordinator.coordinator();
  owner=new ArmorStand(t.level,114.5,16,64.5);incomingOwner=new ArmorStand(t.level,116.5,17,64.5);owner.setNoGravity(true);incomingOwner.setNoGravity(true);
  try{
   t.check(t.level.addFreshEntity(owner)&&t.level.addFreshEntity(incomingOwner),"NATIVE_COORDINATOR_REAL_OWNER_ACTORS");
   List<DomainOverlay.Change> changes=new ArrayList<>();for(var cell:cells)changes.add(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState()));
   var admission=coordinator.begin(UUID.randomUUID(),owner.getUUID(),t.dimension,t.plan,changes);
   t.check(admission.outcome()==DomainCoordinator.Outcome.STARTED,"NATIVE_COORDINATOR_START");handle=admission.handle();
  }catch(Exception|AssertionError error){owner.discard();incomingOwner.discard();nativeCoordinator.close();throw error;}
 }
 boolean tick()throws Exception{
  if(finished)return true;coordinator.tick();
  if(!clashed&&handle.phase()==DomainOverlay.Phase.ACTIVE){
   t.check(coordinator.offenseAllowed(handle),"NATIVE_COORDINATOR_OWNER_PRESENT_ACTIVE");
   var incoming=coordinator.begin(UUID.randomUUID(),incomingOwner.getUUID(),t.dimension,t.plan,List.of());
   t.check(incoming.outcome()==DomainCoordinator.Outcome.CLASH&&incoming.handle()==null&&incoming.clashes().equals(List.of(handle.identity().domain())),"NATIVE_INCOMING_CLASH_CANCELLED_BEFORE_PUBLICATION");
   t.check(handle.phase()==DomainOverlay.Phase.RESTORING&&!coordinator.offenseAllowed(handle)&&!coordinator.ready(),"NATIVE_CLASH_STOPS_AFFECTED_OFFENSE_AND_ADMISSION");clashed=true;
  }
  if(clashed&&handle.terminal()){
   for(var cell:cells)t.check(t.level.hasChunkAt(new net.minecraft.core.BlockPos(cell.x(),cell.y(),cell.z()))&&t.level.getBlockState(new net.minecraft.core.BlockPos(cell.x(),cell.y(),cell.z())).isAir(),"NATIVE_COORDINATOR_EXACT_RESTORE");
   t.check(coordinator.ready(),"NATIVE_COORDINATOR_DURABLE_RELEASE");finished=true;
   t.claims.add("NATIVE_THREE_CELL_FULL_FOOTPRINT_CLASH_CANCEL_RESTORE_AND_DURABLE_RELEASE");return true;
  }return false;
 }
 @Override public void close()throws IOException{try{nativeCoordinator.close();}finally{owner.discard();incomingOwner.discard();}}
}

package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;

/** Three real changed cells, full reservation footprint, two real actor UUIDs; not boss combat. */
final class DomainCoordinatorProbe implements AutoCloseable {
 private final DomainNativeProbe.Trial t;private final DomainNativeCoordinator nativeCoordinator;
 private final DomainCoordinator coordinator;private final ArmorStand owner,incomingOwner;
 private final DomainCoordinator.Handle handle;private boolean clashed,finished;private int tellTicks;
 private DomainCoordinator.Handle failureHandle;private DomainLifecycle failureLife;private boolean failureRequested;
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
  if(tellTicks<60){
   t.check(handle.phase()==DomainOverlay.Phase.PLACING&&handle.journal().entries().stream().allMatch(e->e.status()==DomainOverlay.Status.RESERVED&&!e.mutationIntent()),"NATIVE_UNARMED_TELL_NO_MUTATION_AUTHORITY");
   for(var cell:cells)t.check(t.level.getBlockState(new net.minecraft.core.BlockPos(cell.x(),cell.y(),cell.z())).isAir(),"NATIVE_UNARMED_TELL_BLOCKS_UNCHANGED");
   if(++tellTicks==60)t.check(coordinator.armPlacement(handle),"DIRECT_FIXTURE_60_NATIVE_TICKS_EXPLICIT_ARM_NOT_BOSS_SELECTION");
   return false;
  }
  if(!clashed&&handle.phase()==DomainOverlay.Phase.ACTIVE){
   t.check(coordinator.offenseAllowed(handle),"NATIVE_COORDINATOR_OWNER_PRESENT_ACTIVE");
   var incoming=coordinator.begin(UUID.randomUUID(),incomingOwner.getUUID(),t.dimension,t.plan,List.of());
   t.check(incoming.outcome()==DomainCoordinator.Outcome.CLASH&&incoming.handle()==null&&incoming.clashes().equals(List.of(handle.identity().domain())),"NATIVE_INCOMING_CLASH_CANCELLED_BEFORE_PUBLICATION");
   t.check(handle.phase()==DomainOverlay.Phase.RESTORING&&!coordinator.offenseAllowed(handle)&&!coordinator.ready(),"NATIVE_CLASH_STOPS_AFFECTED_OFFENSE_AND_ADMISSION");clashed=true;
  }
  if(clashed&&handle.terminal()){
   if(failureHandle==null){
    List<DomainOverlay.Change> changes=new ArrayList<>();for(var cell:cells)changes.add(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState()));
    var admitted=coordinator.begin(UUID.randomUUID(),owner.getUUID(),t.dimension,t.plan,changes);
    t.check(admitted.outcome()==DomainCoordinator.Outcome.STARTED&&coordinator.armPlacement(admitted.handle()),"NATIVE_FAILURE_FIXTURE_START");failureHandle=admitted.handle();
    failureLife=new DomainLifecycle();failureLife.begin();for(int i=0;i<60;i++)failureLife.step(new DomainLifecycle.Input(true,true,false,false,false,false,false));
    return false;
   }
   if(!failureRequested&&failureHandle.phase()==DomainOverlay.Phase.ACTIVE){
    failureLife.step(new DomainLifecycle.Input(true,true,true,false,false,false,false));failureLife.step(new DomainLifecycle.Input(true,true,true,true,false,false,false));
    t.check(owner.isAlive()&&coordinator.offenseAllowed(failureHandle)&&failureLife.state().offenseAllowed(),"NATIVE_HEALTHY_ACTIVE_BEFORE_INJECTED_CALLBACK_EXCEPTION");
    try{throw new IOException("INJECTED_GROUND_CALLBACK_EXCEPTION");}catch(IOException injected){t.check(DomainEncounterFailure.requestRestore(failureLife,coordinator,failureHandle),"NATIVE_CALLBACK_FAILURE_RESTORATION_ADMITTED");}
    t.check(failureHandle.phase()==DomainOverlay.Phase.RESTORING&&!coordinator.offenseAllowed(failureHandle)&&!failureLife.state().offenseAllowed(),"NATIVE_CALLBACK_FAILURE_STOPS_OFFENSE");failureRequested=true;
   }
   if(!failureRequested||!failureHandle.terminal())return false;
   for(var cell:cells)t.check(t.level.hasChunkAt(new net.minecraft.core.BlockPos(cell.x(),cell.y(),cell.z()))&&t.level.getBlockState(new net.minecraft.core.BlockPos(cell.x(),cell.y(),cell.z())).isAir(),"NATIVE_COORDINATOR_EXACT_RESTORE");
   t.check(owner.isAlive()&&coordinator.ready(),"NATIVE_COORDINATOR_DURABLE_RELEASE_WITH_LIVING_OWNER");finished=true;
   t.claims.add("NATIVE_INJECTED_CALLBACK_EXCEPTION_HEALTHY_WRITER_LIVING_OWNER_DURABLE_RESTORE");
   t.claims.add("NATIVE_THREE_CELL_FULL_FOOTPRINT_CLASH_CANCEL_RESTORE_AND_DURABLE_RELEASE");return true;
  }return false;
 }
 @Override public void close()throws IOException{try{nativeCoordinator.close();}finally{owner.discard();incomingOwner.discard();}}
}

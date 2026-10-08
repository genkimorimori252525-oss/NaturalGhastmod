package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Real unload; separately, real storage with explicitly injected acknowledgment delivery delay. */
final class DomainNativeBoundaryProbe {
 private static final TicketType<ChunkPos> TICKET=TicketType.create("natural_domain_private_probe",Comparator.comparingLong(ChunkPos::toLong));
 private final DomainNativeProbe.Trial t;private final String fault,scope;private final boolean unload;
 private final DomainGeometry.Cell cell;private final ChunkPos chunk;
 private DomainOverlay engine;private DomainPersistenceBarrier barrier;private CompletableFuture<Void> nativeWrite,delivered;
 private int stage;private long started;private boolean ticket,finished,unloaded,deadline,lateRejected;
 private DomainNativeBoundaryProbe(DomainNativeProbe.Trial t)throws IOException{
  this.t=t;fault=System.getProperty("naturalghast.domainProbe.faultCase","");unload=fault.equals("NATIVE_UNLOAD");
  if(!unload&&!fault.equals("DELAYED_NATIVE_ACK"))throw new IOException("DOMAIN_BOUNDARY_SCOPE");
  scope=unload?"DIRECT_NATIVE_DOMAIN_UNLOAD":"NATIVE_DRIVER_INJECTED_ACK_DELAY";
  cell=new DomainGeometry.Cell(unload?8192:112,16,unload?8192:64);chunk=new ChunkPos(Math.floorDiv(cell.x(),16),Math.floorDiv(cell.z(),16));
 }
 static void run(DomainNativeProbe.Trial t)throws IOException{var probe=new DomainNativeBoundaryProbe(t);t.helper.onEachTick(probe::tick);}
 private void tick(){
  if(finished)return;
  try{
   if(stage==0){
    if(unload){t.check(!t.level.getForcedChunks().contains(chunk.toLong()),"NOT_FORCED_FIXTURE_CHUNK");t.level.getChunkSource().addRegionTicket(TICKET,chunk,2,chunk);ticket=true;t.level.getChunk(chunk.x,chunk.z);}
    t.check(t.level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null&&t.level.getBlockState(pos()).isAir(),"FRESH_LOADED_NATIVE_BOUNDARY_CELL");
    t.repository=DomainJournalRepository.open(t.world);engine=t.start(0,List.of(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState())));
    var world=DomainWorldAdapter.forJournal(t.level,engine.journal());engine.place(world,1);t.check(engine.journal().phase()==DomainOverlay.Phase.ACTIVE,"NATIVE_BOUNDARY_APPLIED");engine.requestRestore("PRIVATE_"+fault);engine.restore(world,1);
    t.check(engine.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,"NATIVE_BOUNDARY_PENDING_DURABILITY");
    var driver=new DomainNativePersistence(t.level,engine.journal());
    DomainPersistenceBarrier.Driver observed=new DomainPersistenceBarrier.Driver(){
     public List<DomainPersistenceBarrier.Chunk> chunks(DomainOverlay.Journal j)throws IOException{return driver.chunks(j);}
     public DomainPersistenceBarrier.Snapshot capture(DomainPersistenceBarrier.Chunk c,DomainOverlay.Journal j)throws IOException{return driver.capture(c,j);}
     public CompletableFuture<Void> store(DomainPersistenceBarrier.Chunk c,DomainPersistenceBarrier.Snapshot s)throws IOException{nativeWrite=driver.store(c,s);if(unload)return nativeWrite;delivered=new CompletableFuture<>();return delivered;}
     public CompletableFuture<Void> flush()throws IOException{return driver.flush();}
     public CompletableFuture<Boolean> read(DomainPersistenceBarrier.Chunk c,DomainOverlay.Journal j)throws IOException{return driver.read(c,j);}
     public boolean originalsCurrent(DomainOverlay.Journal j)throws IOException{return driver.originalsCurrent(j);}
    };
    barrier=new DomainPersistenceBarrier(engine.journal(),observed,t.level::getGameTime,System::nanoTime);started=t.level.getGameTime();engine.verifyDurability(barrier);
    t.check(nativeWrite!=null,"REAL_NATIVE_STORE_ENQUEUED");
    if(unload){t.level.getChunkSource().removeRegionTicket(TICKET,chunk,2,chunk);ticket=false;}stage=1;return;
   }
   if(stage==1&&unload){
    // Deliberately pause sidecar stepping until actual normal unload, within the real deadline.
    // Product adapter never removes tickets, forces unloading or reloads this chunk.
    if(t.level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null){if(t.level.getGameTime()-started>190)throw new IOException("NATIVE_UNLOAD_UNEXERCISED_BEFORE_DEADLINE");return;}
    t.check(t.level.getGameTime()-started<=200,"ACTUAL_UNLOAD_BEFORE_TICK_DEADLINE");unloaded=true;
    t.check(nativeWrite.isDone()&&!nativeWrite.isCancelled(),"UNLOAD_RETAINS_NATIVE_FUTURE");nativeWrite.join();
    engine.verifyDurability(barrier);t.check(!engine.terminal()&&engine.journal().phase()==DomainOverlay.Phase.RESTORING,"UNLOAD_CONFLICT_RETAINS_JOURNAL");
    t.check(t.level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null,"ADAPTER_DID_NOT_RELOAD_UNLOADED_CHUNK");stage=2;return;
   }
   if(stage==1){
    t.check(!nativeWrite.isCancelled()&&!delivered.isDone(),"NATIVE_WRITE_NOT_CANCELLED_ACK_WITHHELD");
    try{engine.verifyDurability(barrier);if(engine.terminal())throw new IOException("ACK_DELAY_UNEXERCISED");}
    catch(IOException expected){if(!causedBy(expected,"DOMAIN_DURABILITY_DEADLINE"))throw expected;deadline=true;t.check(engine.halted()&&!engine.terminal(),"DEADLINE_HALTS_WITHOUT_TERMINAL");stage=2;}
    return;
   }
   if(stage==2){
    if(!nativeWrite.isDone())return;nativeWrite.join();t.check(!nativeWrite.isCancelled(),"ACTUAL_NATIVE_STORE_COMPLETED_UNCANCELLED");
    if(!unload){
     String before=journalHash();delivered.complete(null);t.check(delivered.isDone(),"DELAYED_ACK_RELEASED_AFTER_DEADLINE");
     try{engine.verifyDurability(barrier);throw new AssertionError("LATE_ACK_ACCEPTED");}catch(IOException expected){lateRejected=true;}
     t.check(!engine.terminal()&&journalHash().equals(before),"LATE_ACK_CANNOT_PUBLISH_TERMINAL");
    }
    t.repository.close();t.repository=null;
    try(var reopened=DomainNativeCoordinator.open(t.level.getServer())){var coordinator=reopened.coordinator();t.check(!coordinator.ready(),"BOUNDARY_BLOCKS_RESTART_ADMISSION");t.check(coordinator.begin(UUID.randomUUID(),UUID.randomUUID(),t.dimension,t.plan,List.of()).outcome()==DomainCoordinator.Outcome.RECONCILIATION_PENDING,"BOUNDARY_NO_SLOT_REUSE");}
    if(unload)t.check(t.level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null,"REOPEN_DID_NOT_RELOAD_FIXTURE_CHUNK");
    receipt("PASS",null);finished=true;t.helper.succeed();
   }
  }catch(Exception|AssertionError error){finished=true;removeTicket();try{if(t.repository!=null)t.repository.close();}catch(IOException ignored){}t.repository=null;try{receipt("FAIL",error);}catch(IOException ignored){}t.helper.fail("DOMAIN_PRIVATE_BOUNDARY_FAILED: "+error.getClass().getSimpleName()+":"+error.getMessage());}
 }
 private void removeTicket(){if(ticket){t.level.getChunkSource().removeRegionTicket(TICKET,chunk,2,chunk);ticket=false;}}
 private static boolean causedBy(Throwable error,String message){Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());for(int i=0;error!=null&&i<8&&seen.add(error);i++,error=error.getCause())if(message.equals(error.getMessage()))return true;return false;}
 private String journalHash()throws IOException{return DomainJournalCodec.sha(Files.readAllBytes(t.repository.slot(t.dimension,0).path()));}
 private BlockPos pos(){return new BlockPos(cell.x(),cell.y(),cell.z());}
 private void receipt(String verdict,Throwable error)throws IOException{
  var data=new JsonObject();data.addProperty("scope",scope);data.addProperty("verdict",verdict);data.addProperty("scenario",t.scenario);data.addProperty("faultCase",fault);data.addProperty("nonce",t.nonce);data.addProperty("sourceRevision",t.source);data.addProperty("pid",ProcessHandle.current().pid());data.addProperty("checks",t.checks);data.addProperty("gameTicks",t.level.getGameTime()-t.started);data.addProperty("actualUnloadedBeforeDeadline",unloaded);data.addProperty("adapterReloadedChunk",unload&&unloaded&&t.level.getChunkSource().getChunkNow(chunk.x,chunk.z)!=null);data.addProperty("actualNativeStoreCompleted",nativeWrite!=null&&nativeWrite.isDone()&&!nativeWrite.isCompletedExceptionally());data.addProperty("injectedDeadlineObserved",deadline);data.addProperty("lateAcknowledgmentRejected",lateRejected);data.addProperty("nativeWriteCancelled",nativeWrite!=null&&nativeWrite.isCancelled());data.addProperty("nonterminalJournalRetained",engine!=null&&!engine.terminal());data.addProperty("failureClass",error==null?"":error.getClass().getSimpleName());data.addProperty("limitations",unload?"ACTUAL_UNLOAD_WITH_SIDECAR_STEPPING_PAUSED; no natural combat/disk-stall/power-loss proof":"ACTUAL_NATIVE_DRIVER_WITH_INJECTED_ACK_DELIVERY_DELAY; not an observed disk stall; no natural combat/power-loss proof");
  byte[] bytes=(DomainJournalCodec.canonical(data)+"\n").getBytes(StandardCharsets.UTF_8);try(var out=FileChannel.open(t.root.resolve("native-fault-native.json"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);out.force(true);}
 }
}

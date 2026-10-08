package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Bounded asynchronous barrier contracts, explicitly not native chunk durability proof. */
public final class DomainPersistenceTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 record Snapshot(int bytes) implements DomainPersistenceBarrier.Snapshot {}
 static final class Driver implements DomainPersistenceBarrier.Driver {
  long ticks,nanos;int captures,stores,flushes,reads,bytes=128,count=2;boolean current=true,readResult=true;
  List<CompletableFuture<Void>> writes=new ArrayList<>(),forces=new ArrayList<>();List<CompletableFuture<Boolean>> readbacks=new ArrayList<>();
  public List<DomainPersistenceBarrier.Chunk> chunks(DomainOverlay.Journal j){var result=new ArrayList<DomainPersistenceBarrier.Chunk>();for(int x=0;x<count;x++)result.add(new DomainPersistenceBarrier.Chunk(x,0));return result;}
  public DomainPersistenceBarrier.Snapshot capture(DomainPersistenceBarrier.Chunk c,DomainOverlay.Journal j){captures++;return new Snapshot(bytes);}
  public CompletableFuture<Void> store(DomainPersistenceBarrier.Chunk c,DomainPersistenceBarrier.Snapshot snapshot){stores++;var future=new CompletableFuture<Void>();writes.add(future);return future;}
  public CompletableFuture<Void> flush(){flushes++;var future=new CompletableFuture<Void>();forces.add(future);return future;}
  public CompletableFuture<Boolean> read(DomainPersistenceBarrier.Chunk c,DomainOverlay.Journal j){reads++;var future=new CompletableFuture<Boolean>();readbacks.add(future);return future;}
  public boolean originalsCurrent(DomainOverlay.Journal j){return current;}
  void complete(){writes.forEach(f->f.complete(null));forces.forEach(f->f.complete(null));readbacks.forEach(f->f.complete(readResult));}
 }
 static DomainOverlay.Journal journal(){return new DomainOverlay.Journal(new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",0,1),DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,List.of(),0,"AWAIT");}
 static DomainPersistenceBarrier barrier(Driver d,DomainOverlay.Journal j)throws Exception{return new DomainPersistenceBarrier(j,d,()->d.ticks,()->d.nanos);}
 static void failed(DomainPersistenceBarrier b,DomainOverlay.Journal j,String why)throws Exception{try{b.step(j);throw new AssertionError(why);}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  var j=journal();var d=new Driver();var b=barrier(d,j);check(b.step(j)==DomainOverlay.DurabilityResult.PENDING&&d.captures==1&&d.stores==1,"one chunk snapshot per step");
  b.step(j);for(int i=0;i<3;i++)b.step(j);check(d.flushes==0&&d.captures==2,"all store acknowledgments precede flush");
  d.writes.get(0).complete(null);b.step(j);check(d.flushes==0,"one completed store is insufficient");d.writes.get(1).complete(null);
  for(int i=0;i<20;i++){var result=b.step(j);if(d.flushes==2&&d.forces.get(1).isDone()){check(result==DomainOverlay.DurabilityResult.VERIFIED,"write flush readback final flush positive contract");break;}d.complete();}
  check(d.reads==2&&d.flushes==2,"readback cache is followed by another positive flush");
  d=new Driver();b=barrier(d,j);b.step(j);d.writes.get(0).completeExceptionally(new IOException("WRITE_FAIL"));failed(b,j,"store failure must not be lost after leaving worker pending cache");
  d=new Driver();d.count=0;b=barrier(d,j);for(int i=0;i<8&&d.flushes==0;i++)b.step(j);d.forces.get(0).completeExceptionally(new IOException("FLUSH_FAIL"));failed(b,j,"flush failure");
  d=new Driver();b=barrier(d,j);b.step(j);d.ticks=201;failed(b,j,"tick deadline");check(!d.writes.get(0).isCancelled(),"timeout never cancels native writes");d.writes.get(0).complete(null);failed(b,j,"late completion cannot reopen failed barrier");
  d=new Driver();b=barrier(d,j);d.nanos=10_000_000_001L;failed(b,j,"wall deadline even without server ticks");
  d=new Driver();d.current=false;b=barrier(d,j);check(b.step(j)==DomainOverlay.DurabilityResult.CONFLICT&&d.stores==0,"unloaded/third-party current mismatch before any snapshot");
  d=new Driver();d.readResult=false;b=barrier(d,j);DomainOverlay.DurabilityResult result=null;for(int i=0;i<20;i++){result=b.step(j);d.complete();if(result!=DomainOverlay.DurabilityResult.PENDING)break;}check(result==DomainOverlay.DurabilityResult.CONFLICT,"persisted mismatch never verifies");
  d=new Driver();d.bytes=DomainPersistenceBarrier.MAX_CHUNK_BYTES+1;b=barrier(d,j);failed(b,j,"serialized chunk budget");check(d.stores==0,"oversized snapshot rejected before store");
  d=new Driver();d.count=17;try{barrier(d,j);throw new AssertionError("chunk cap");}catch(IOException expected){checks++;}
  d=new Driver();b=barrier(d,j);failed(b,journal(),"different identity cannot reuse barrier");
  d=new Driver();b=barrier(d,j);for(int i=0;i<20&&d.flushes<2;i++){b.step(j);d.complete();}d.current=false;check(b.step(j)==DomainOverlay.DurabilityResult.CONFLICT,"final native original revalidation");
  System.out.println("PASS: "+checks+" async persistence contracts; native autosave/I/O/unload gates NOT_RUN");
 }
}

package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

/** Nonblocking, finite native-store acknowledgment/flush/readback protocol. */
public final class DomainPersistenceBarrier implements DomainOverlay.Durability {
 public static final int MAX_CHUNKS=16,MAX_CHUNK_BYTES=4*1024*1024,MAX_TOTAL_BYTES=16*1024*1024;
 public record Chunk(int x,int z) {}
 public interface Snapshot {int bytes();}
 public interface Driver {
  List<Chunk> chunks(DomainOverlay.Journal journal)throws IOException;
  Snapshot capture(Chunk chunk,DomainOverlay.Journal journal)throws IOException;
  CompletableFuture<Void> store(Chunk chunk,Snapshot snapshot)throws IOException;
  CompletableFuture<Void> flush()throws IOException;
  CompletableFuture<Boolean> read(Chunk chunk,DomainOverlay.Journal journal)throws IOException;
  boolean originalsCurrent(DomainOverlay.Journal journal)throws IOException;
 }
 private enum Stage {SNAPSHOTS,WRITES,FIRST_FLUSH,READBACK,FINAL_FLUSH,DONE,CONFLICT}
 private final DomainOverlay.Journal journal;private final Driver driver;private final List<Chunk> chunks;
 private final LongSupplier ticks,nanos;private final long initialTick,initialNanos;
 private final List<CompletableFuture<Void>> writes=new ArrayList<>();
 private CompletableFuture<Void> flush;private CompletableFuture<Boolean> read;
 private int cursor,totalBytes;private Stage stage=Stage.SNAPSHOTS;private boolean failed;
 public DomainPersistenceBarrier(DomainOverlay.Journal journal,Driver driver,LongSupplier ticks,LongSupplier nanos)throws IOException{
  this.journal=Objects.requireNonNull(journal);this.driver=Objects.requireNonNull(driver);this.ticks=Objects.requireNonNull(ticks);this.nanos=Objects.requireNonNull(nanos);
  if(journal.phase()!=DomainOverlay.Phase.RESTORED_PENDING_DURABILITY)throw new IOException("DOMAIN_DURABILITY_PHASE");
  chunks=List.copyOf(driver.chunks(journal));if(chunks.size()>MAX_CHUNKS||new HashSet<>(chunks).size()!=chunks.size())throw new IOException("DOMAIN_DURABILITY_CHUNK_LIMIT");
  initialTick=ticks.getAsLong();initialNanos=nanos.getAsLong();
 }
 @Override public DomainOverlay.DurabilityResult step(DomainOverlay.Journal current)throws IOException{
  if(failed)throw new IOException("DOMAIN_DURABILITY_UNAVAILABLE");
  try{
   if(!journal.equals(current))throw new IOException("DOMAIN_DURABILITY_IDENTITY_CHANGED");
   if(ticks.getAsLong()-initialTick>200||nanos.getAsLong()-initialNanos>10_000_000_000L)throw new IOException("DOMAIN_DURABILITY_DEADLINE");
   // Retain every own store future: a completed failure may already have left native pendingWrites.
   for(var future:writes)if(future.isDone())future.join();
   switch(stage){
    case SNAPSHOTS->{
     if(!driver.originalsCurrent(journal))return conflict();
     if(cursor<chunks.size()){
      Chunk chunk=chunks.get(cursor++);Snapshot snapshot=Objects.requireNonNull(driver.capture(chunk,journal));
      if(snapshot.bytes()<0||snapshot.bytes()>MAX_CHUNK_BYTES||totalBytes>MAX_TOTAL_BYTES-snapshot.bytes())throw new IOException("DOMAIN_DURABILITY_BYTE_LIMIT");
      totalBytes+=snapshot.bytes();writes.add(Objects.requireNonNull(driver.store(chunk,snapshot)));return DomainOverlay.DurabilityResult.PENDING;
     }stage=Stage.WRITES;
    }
    case WRITES->{if(writes.stream().anyMatch(f->!f.isDone()))return DomainOverlay.DurabilityResult.PENDING;flush=Objects.requireNonNull(driver.flush());stage=Stage.FIRST_FLUSH;}
    case FIRST_FLUSH->{if(!flush.isDone())return DomainOverlay.DurabilityResult.PENDING;flush.join();cursor=0;stage=Stage.READBACK;}
    case READBACK->{
     if(read!=null){if(!read.isDone())return DomainOverlay.DurabilityResult.PENDING;if(!Boolean.TRUE.equals(read.join()))return conflict();read=null;cursor++;}
     if(cursor<chunks.size())read=Objects.requireNonNull(driver.read(chunks.get(cursor),journal));
     else {flush=Objects.requireNonNull(driver.flush());stage=Stage.FINAL_FLUSH;}
    }
    case FINAL_FLUSH->{if(!flush.isDone())return DomainOverlay.DurabilityResult.PENDING;flush.join();if(!driver.originalsCurrent(journal))return conflict();stage=Stage.DONE;return DomainOverlay.DurabilityResult.VERIFIED;}
    case DONE->{return driver.originalsCurrent(journal)?DomainOverlay.DurabilityResult.VERIFIED:conflict();}
    case CONFLICT->{return DomainOverlay.DurabilityResult.CONFLICT;}
   }
   return DomainOverlay.DurabilityResult.PENDING;
  }catch(IOException|RuntimeException error){failed=true;throw new IOException("DOMAIN_DURABILITY_UNRESOLVED",error);}
 }
 private DomainOverlay.DurabilityResult conflict(){stage=Stage.CONFLICT;return DomainOverlay.DurabilityResult.CONFLICT;}
}

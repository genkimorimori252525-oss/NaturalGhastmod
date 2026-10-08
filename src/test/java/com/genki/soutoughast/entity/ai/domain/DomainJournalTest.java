package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import com.sun.nio.file.ExtendedOpenOption;

/** Actual local files/locks and strict codec; no native block mutation or restart acceptance. */
public final class DomainJournalTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static DomainOverlay.Identity id(int slot,long generation){return new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",slot,generation);}
 static DomainOverlay.Journal journal(int slot,long generation){return new DomainOverlay.Journal(id(slot,generation),DomainOverlay.Phase.PLACING,List.of(new DomainOverlay.Entry(new DomainGeometry.Cell(0,64,0),"{\"Name\":\"minecraft:stone\"}","{\"Name\":\"minecraft:blackstone\"}",DomainOverlay.Status.RESERVED)),0,"START");}
 static void rejected(byte[] bytes,String reason)throws Exception{try{DomainJournalCodec.decode(bytes);throw new AssertionError(reason);}catch(IOException expected){checks++;}}
 static DomainOverlay.Journal restoring(DomainOverlay.Journal j){return new DomainOverlay.Journal(j.identity(),DomainOverlay.Phase.RESTORING,j.entries(),0,"ABORT");}
 static Path staging(Path target){return target.resolveSibling("slot-0.prepared.json");}
 static Path diagnostic(Path target){return target.resolveSibling("slot-0.diagnostic.json");}
 public static void main(String[] args)throws Exception{
  if(args.length==2&&args[0].equals("--lock-probe")){try(var repo=DomainJournalRepository.open(Path.of(args[1]))){System.exit(2);}catch(IOException expected){System.out.println("LOCK_REJECTED");return;}}
  var original=journal(0,1);byte[] encoded=DomainJournalCodec.encode(original);check(DomainJournalCodec.decode(encoded).equals(original),"strict journal roundtrip");
  try{DomainJournalCodec.decode(new String(encoded,java.nio.charset.StandardCharsets.UTF_8).replace("minecraft:blackstone","minecraft:obsidian").getBytes(java.nio.charset.StandardCharsets.UTF_8));throw new AssertionError("checksum");}catch(IOException expected){checks++;}
  try{DomainJournalCodec.decode("{\"schemaVersion\":1,\"schemaVersion\":1}".getBytes());throw new AssertionError("duplicate key");}catch(IOException expected){checks++;}
  rejected(new byte[]{(byte)0xc3,(byte)0x28},"malformed UTF8");
  rejected((" "+new String(encoded,java.nio.charset.StandardCharsets.UTF_8)).getBytes(java.nio.charset.StandardCharsets.UTF_8),"noncanonical record");
  rejected(Arrays.copyOf(encoded,DomainJournalCodec.MAX_BYTES+1),"record limit");
  rejected(new String(encoded,java.nio.charset.StandardCharsets.UTF_8).replace("\"schemaVersion\":2","\"schemaVersion\":2.0").getBytes(java.nio.charset.StandardCharsets.UTF_8),"ambiguous integral numeric form");
  rejected(new String(encoded,java.nio.charset.StandardCharsets.UTF_8).replace("\"schemaVersion\":2","\"schemaVersion\":1").getBytes(java.nio.charset.StandardCharsets.UTF_8),"old ownership-free schema retained/rejected, never silently migrated");
  var unknown=DomainJournalCodec.unwrap(encoded).payload().deepCopy();unknown.addProperty("unknown",true);rejected(DomainJournalCodec.wrap(unknown,DomainJournalCodec.ABSENT),"valid checksum cannot admit unknown fields");
  var fractional=DomainJournalCodec.unwrap(encoded).payload().deepCopy();fractional.getAsJsonObject("journal").getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("cell").set(0,new com.google.gson.JsonPrimitive(0.5));rejected(DomainJournalCodec.wrap(fractional,DomainJournalCodec.ABSENT),"fractional coordinate");
  Path world=Files.createTempDirectory("domain-owned-world-").toRealPath();
  try(var repo=DomainJournalRepository.open(world)){
   try(var other=DomainJournalRepository.open(world)){throw new AssertionError("second writer");}catch(IOException expected){checks++;}
   var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString(),"-cp",System.getProperty("java.class.path"),DomainJournalTest.class.getName(),"--lock-probe",world.toString()).redirectErrorStream(true).start();
   boolean exited=process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS);if(!exited)process.destroyForcibly();check(exited,"bounded independent JVM lock probe");check(process.exitValue()==0&&new String(process.getInputStream().readAllBytes()).contains("LOCK_REJECTED"),"exclusive lock rejects independent process");
   var slot=repo.slot("minecraft:overworld",0);check(slot.read()==null,"new slot empty");slot.persist(null,original);check(slot.read().equals(original),"forced atomic publication readback");
   try{slot.persist(null,original);throw new AssertionError("stale predecessor");}catch(IOException expected){checks++;}
   try{repo.slot("minecraft:overworld",4);throw new AssertionError("slot cap");}catch(IllegalArgumentException expected){checks++;}
   for(int i=1;i<4;i++){var s=repo.slot("minecraft:overworld",i);s.persist(null,journal(i,1));}
   check(repo.loadAll().size()==4,"four fixed loaded slots");
   if(System.getProperty("os.name").startsWith("Windows")){
    Path target=slot.path();byte[] before=Files.readAllBytes(target);
    try(FileChannel lock=FileChannel.open(target,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
     var restoring=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.RESTORING,original.entries(),0,"ABORT");
     try{slot.persist(original,restoring);throw new AssertionError("denied rename");}catch(IOException expected){check(Arrays.equals(Files.readAllBytes(target),before),"denied atomic publication keeps predecessor");check(Files.exists(target.resolveSibling("slot-0.prepared.json")),"actual forced preparation survived denied rename");try{slot.read();throw new AssertionError("resume failed writer");}catch(IOException halted){checks++;}}
    }
   }
  }
  try(var repo=DomainJournalRepository.open(world)){var slot=repo.slot("minecraft:overworld",0);check(repo.loadAll().size()==4&&slot.read().equals(original),"repository reopen preserves exact journals");if(System.getProperty("os.name").startsWith("Windows")){check(!Files.exists(slot.path().resolveSibling("slot-0.prepared.json")),"unpublished preparation reclaimed after classification");check(Files.readString(slot.path().resolveSibling("slot-0.diagnostic.json")).contains("ABANDONED_UNPUBLISHED_PREPARATION"),"unpublished candidate never expands restoration authority");}}
  Path corrupt=Files.createTempDirectory("domain-corrupt-world-").toRealPath();
  try(var repo=DomainJournalRepository.open(corrupt)){
   var slot=repo.slot("minecraft:overworld",0);slot.persist(null,original);Files.writeString(slot.path(),"corrupt");
   try{repo.loadAll();throw new AssertionError("corrupt journal");}catch(IOException expected){check(Files.readString(slot.path()).equals("corrupt"),"corrupt journal retained, not replaced");}
  }
  Path empty=Files.createTempDirectory("domain-unpublished-world-").toRealPath(),emptyTarget;
  try(var repo=DomainJournalRepository.open(empty)){emptyTarget=repo.slot("minecraft:overworld",0).path();Files.write(staging(emptyTarget),encoded,StandardOpenOption.CREATE_NEW);}
  try(var repo=DomainJournalRepository.open(empty)){check(repo.slot("minecraft:overworld",0).read()==null&&repo.loadAll().isEmpty(),"never-published initial journal grants no restoration ownership");check(Files.exists(diagnostic(emptyTarget))&&!Files.exists(staging(emptyTarget)),"absent predecessor abandonment classified then reclaimed");}
  try(var repo=DomainJournalRepository.open(empty)){check(repo.loadAll().isEmpty(),"diagnostic-only slot survives second reopen");}
  Path obsolete=Files.createTempDirectory("domain-obsolete-world-").toRealPath(),obsoleteTarget;
  try(var repo=DomainJournalRepository.open(obsolete)){var slot=repo.slot("minecraft:overworld",0);slot.persist(null,original);obsoleteTarget=slot.path();Files.write(staging(obsoleteTarget),Files.readAllBytes(obsoleteTarget),StandardOpenOption.CREATE_NEW);}
  try(var repo=DomainJournalRepository.open(obsolete)){check(repo.slot("minecraft:overworld",0).read().equals(original),"published duplicate remains canonical authority");check(Files.readString(diagnostic(obsoleteTarget)).contains("OBSOLETE_PUBLISHED_PREPARATION"),"obsolete preparation classified");}
  // Crash after diagnostic force, before its rename: only metadata publication may resume.
  Path diagnosticPending=diagnostic(obsoleteTarget).resolveSibling("slot-0.diagnostic.prepared.json");
  Files.move(diagnostic(obsoleteTarget),diagnosticPending,StandardCopyOption.ATOMIC_MOVE);
  try(var repo=DomainJournalRepository.open(obsolete)){check(Files.exists(diagnostic(obsoleteTarget))&&!Files.exists(diagnosticPending),"interrupted diagnostic publication reconciles exact predecessor");}
  Path ambiguous=Files.createTempDirectory("domain-ambiguous-world-").toRealPath(),ambiguousTarget;
  try(var repo=DomainJournalRepository.open(ambiguous)){var slot=repo.slot("minecraft:overworld",0);slot.persist(null,original);ambiguousTarget=slot.path();Files.write(staging(ambiguousTarget),DomainJournalCodec.encode(restoring(original),"0".repeat(64)),StandardOpenOption.CREATE_NEW);}
  byte[] ambiguousBefore=Files.readAllBytes(staging(ambiguousTarget));
  try(var repo=DomainJournalRepository.open(ambiguous)){throw new AssertionError("ambiguous predecessor");}catch(IOException expected){check(Arrays.equals(Files.readAllBytes(staging(ambiguousTarget)),ambiguousBefore)&&DomainJournalCodec.decode(Files.readAllBytes(ambiguousTarget)).equals(original),"ambiguous preparation retained without any canonical replacement");}
  Path badPreparation=Files.createTempDirectory("domain-bad-preparation-").toRealPath(),badTarget;
  try(var repo=DomainJournalRepository.open(badPreparation)){badTarget=repo.slot("minecraft:overworld",0).path();Files.writeString(staging(badTarget),"corrupt",StandardOpenOption.CREATE_NEW);}
  try(var repo=DomainJournalRepository.open(badPreparation)){throw new AssertionError("corrupt preparation");}catch(IOException expected){check(Files.readString(staging(badTarget)).equals("corrupt"),"corrupt staging retained");}
  Path reuse=Files.createTempDirectory("domain-terminal-reuse-").toRealPath();
  try(var repo=DomainJournalRepository.open(reuse)){
   var slot=repo.slot("minecraft:overworld",0);slot.persist(null,original);
   var terminal=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.VERIFIED_TERMINAL,original.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.original(),e.overlay(),DomainOverlay.Status.RESTORED,false)).toList(),0,"RESTORATION_DURABILITY_VERIFIED");
   try{slot.persist(original,terminal);throw new AssertionError("skip restoration phase");}catch(IOException expected){check(!Files.exists(staging(slot.path())),"invalid transition cannot reach publication");}
   var changed=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.PLACING,List.of(new DomainOverlay.Entry(original.entries().get(0).cell(),"different","overlay",DomainOverlay.Status.RESERVED)),0,"START");
   try{slot.persist(original,changed);throw new AssertionError("alter original ledger");}catch(IOException expected){checks++;}
   try{slot.persist(original,journal(0,2));throw new AssertionError("reuse unresolved slot");}catch(IOException expected){checks++;}
   var restoring=restoring(original);slot.persist(original,restoring);
   var enlargedOwnership=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.RESTORING,original.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.original(),e.overlay(),DomainOverlay.Status.RESTORED,true)).toList(),0,"INVALID_OWNERSHIP");
   try{slot.persist(restoring,enlargedOwnership);throw new AssertionError("expand ownership during cancellation");}catch(IOException expected){check(slot.read().equals(restoring),"unowned originals remain unowned under guarded publication");}
   try{slot.persist(restoring,terminal);throw new AssertionError("skip durability-pending phase");}catch(IOException expected){checks++;}
   var awaiting=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,terminal.entries(),0,"AWAIT_DURABILITY");slot.persist(restoring,awaiting);slot.persist(awaiting,terminal);
   try{slot.persist(terminal,journal(0,1));throw new AssertionError("stale generation");}catch(IOException expected){checks++;}
   var next=journal(0,2);slot.persist(terminal,next);check(slot.read().equals(next),"verified terminal slot reused at increased generation");
   var threadError=new java.util.concurrent.atomic.AtomicReference<Throwable>();Thread worker=new Thread(()->{try{slot.read();}catch(Throwable error){threadError.set(error);}});worker.start();worker.join(2000);check(!worker.isAlive()&&threadError.get() instanceof IOException,"single writer thread enforced");
  }
  for(var boundary:DomainJournalRepository.PublicationBoundary.values()){
   Path interrupted=Files.createTempDirectory("domain-boundary-"+boundary.name()+"-").toRealPath();
   try(var repo=DomainJournalRepository.open(interrupted)){repo.slot("minecraft:overworld",0).persist(null,original);}
   var pending=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.PLACING,original.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.original(),e.overlay(),DomainOverlay.Status.PENDING)).toList(),0,"PROBE_PENDING");
   class Abrupt extends Error{}
   try(var repo=DomainJournalRepository.open(interrupted,(observed,next)->{if(observed==boundary&&next.reason().equals("PROBE_PENDING"))throw new Abrupt();})){
    try{repo.slot("minecraft:overworld",0).persist(original,pending);throw new AssertionError("boundary did not interrupt");}catch(Abrupt expected){checks++;}
   }
   boolean published=Set.of(DomainJournalRepository.PublicationBoundary.AFTER_RENAME,DomainJournalRepository.PublicationBoundary.AFTER_READBACK,DomainJournalRepository.PublicationBoundary.PUBLISHED_BEFORE_RETURN).contains(boundary);
   try(var repo=DomainJournalRepository.open(interrupted)){var slot=repo.slot("minecraft:overworld",0);check(slot.read().equals(published?pending:original),"boundary canonical authority: "+boundary);check(!Files.exists(staging(slot.path())),"boundary classified preparation before reclamation: "+boundary);}
  }
  System.out.println("PASS: "+checks+" actual Domain journal/atomic lock/codec checks; native restart gates NOT_RUN");
 }
}

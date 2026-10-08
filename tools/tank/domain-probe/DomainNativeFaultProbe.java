package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Finite actual filesystem failure in a fresh owned fixture; never an original RegionFile opener. */
final class DomainNativeFaultProbe {
 private final DomainNativeProbe.Trial t;private final List<DomainGeometry.Cell> cells=List.of(new DomainGeometry.Cell(112,16,64),new DomainGeometry.Cell(113,16,64),new DomainGeometry.Cell(114,16,64));
 private DomainOverlay engine;private DomainWorldAdapter world;private DomainPersistenceBarrier overlaySeed,barrier;
 private DomainOverlay.Journal descriptor;private Process helper;private long helperPid;private int stage;private boolean finished,storeFailed,releaseAttested;private String failureClass="";
 private Throwable failure;private int cleanupTicks;
 private DomainNativeFaultProbe(DomainNativeProbe.Trial trial)throws IOException{
  t=trial;if(!t.dimension.equals("minecraft:overworld")||!System.getProperty("naturalghast.domainProbe.faultCase","").equals("NATIVE_WRITE_LOCK"))throw new IOException("DOMAIN_FAULT_SCOPE");
 }
 static void run(DomainNativeProbe.Trial t)throws IOException{var fault=new DomainNativeFaultProbe(t);t.helper.onEachTick(fault::tick);}
 private void tick(){
  if(finished)return;
  if(failure!=null){
   if(helper!=null&&helper.isAlive()){if(++cleanupTicks==80)helper.destroy();if(cleanupTicks==100)helper.destroyForcibly();return;}
   finished=true;try{receipt("FAIL",failure);}catch(IOException ignored){}t.helper.fail("DOMAIN_PRIVATE_WRITE_FAULT_FAILED: "+failure.getClass().getSimpleName()+":"+failure.getMessage());return;
  }
  try{
   if(stage==0){
    if(engine==null){
     t.repository=DomainJournalRepository.open(t.world);List<DomainOverlay.Change> changes=new ArrayList<>();for(var cell:cells){t.check(t.level.getBlockState(pos(cell)).isAir(),"FRESH_WRITE_FAULT_CELL");changes.add(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState()));}
     engine=t.start(0,changes);world=DomainWorldAdapter.forJournal(t.level,engine.journal());engine.place(world,3);t.check(engine.journal().phase()==DomainOverlay.Phase.ACTIVE,"FAULT_OVERLAY_APPLIED");
     var j=engine.journal();descriptor=new DomainOverlay.Journal(j.identity(),DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,j.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.overlay(),e.original(),DomainOverlay.Status.RESTORED,true)).toList(),0,"OWNED_FIXTURE_OVERLAY_BASELINE");overlaySeed=DomainNativePersistence.barrier(t.level,descriptor);
    }
    var result=overlaySeed.step(descriptor);if(result!=DomainOverlay.DurabilityResult.VERIFIED){if(result==DomainOverlay.DurabilityResult.CONFLICT)throw new IOException("FAULT_BASELINE_CONFLICT");return;}
    engine.requestRestore("NATIVE_WRITE_FAULT");engine.restore(world,3);t.check(engine.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,"FAULT_RESTORED_PENDING");
    Path region=t.world.resolve("region/r.0.0.mca");safePath(region);t.check(Files.size(region)>=8192,"EXISTING_NATIVE_REGION_HEADER");
    String script=System.getProperty("naturalghast.domainProbe.lockScript","");if(script.isEmpty())throw new IOException("FAULT_LOCK_SCRIPT_REQUIRED");
    helper=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-File",script,"-ProbeRoot",t.root.toString(),"-Nonce",t.nonce).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.to(t.root.resolve("region-lock-helper.log").toFile())).start();helperPid=helper.pid();stage=1;return;
   }
   if(stage==1){
    Path ready=t.root.resolve("region-lock-ready.json");if(!Files.exists(ready)){if(!helper.isAlive())throw new IOException("FAULT_LOCK_NOT_ACQUIRED");return;}
    var acquired=read(ready);owned(acquired);t.check(acquired.get("acquired").getAsBoolean()&&acquired.get("offset").getAsInt()==0&&acquired.get("length").getAsInt()==8192,"ACTUAL_HEADER_LOCK_ACQUIRED");
    barrier=DomainNativePersistence.barrier(t.level,engine.journal());stage=2;return;
   }
   if(stage==2){
    try{engine.verifyDurability(barrier);if(engine.terminal())throw new IOException("NATIVE_WRITE_FAULT_UNEXERCISED");}
    catch(IOException expected){
     if(!storeFailure(expected))throw expected;storeFailed=true;failureClass=expected.getClass().getSimpleName();
     t.check(engine.halted()&&!engine.terminal()&&engine.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,"EXCEPTIONAL_NATIVE_STORE_RETAINS_NONTERMINAL");
     release();stage=3;
    }return;
   }
   if(stage==3){
    if(helper.isAlive())return;var ended=read(t.root.resolve("region-lock-end.json"));owned(ended);
    t.check(helper.exitValue()==0&&ended.get("acquired").getAsBoolean()&&ended.get("released").getAsBoolean()&&!ended.get("timedOut").getAsBoolean()&&ended.get("errorClass").getAsString().isEmpty(),"LOCK_RELEASE_AND_HELPER_EXIT_ATTESTED");releaseAttested=true;
    t.repository.close();t.repository=null;
    try(var reopened=DomainNativeCoordinator.open(t.level.getServer())){
     var coordinator=reopened.coordinator();t.check(!coordinator.ready(),"FAILED_NATIVE_STORE_BLOCKS_RESTART_ADMISSION");
     t.check(coordinator.begin(UUID.randomUUID(),UUID.randomUUID(),t.dimension,t.plan,List.of()).outcome()==DomainCoordinator.Outcome.RECONCILIATION_PENDING,"FAILED_NATIVE_STORE_NO_NEW_ACTIVATION");
    }
    t.repository=DomainJournalRepository.open(t.world);t.check(t.repository.slot(t.dimension,0).read().phase()!=DomainOverlay.Phase.VERIFIED_TERMINAL,"FAILED_NATIVE_STORE_NO_TERMINAL_REUSE");t.repository.close();t.repository=null;
    receipt("PASS",null);finished=true;t.helper.succeed();
   }
  }catch(Exception|AssertionError error){
   failure=error;try{release();}catch(IOException ignored){}try{if(t.repository!=null)t.repository.close();}catch(IOException ignored){}t.repository=null;
  }
 }
 private boolean storeFailure(Throwable error){
  boolean completion=false,nativeWrite=false,io=false;Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  for(int depth=0;error!=null&&depth<8&&seen.add(error);depth++,error=error.getCause()){
   completion|=error instanceof java.util.concurrent.CompletionException;io|=error instanceof IOException;
   for(var frame:error.getStackTrace())if(frame.getClassName().equals("net.minecraft.world.level.chunk.storage.RegionFile")&&(frame.getMethodName().equals("write")||frame.getMethodName().equals("writeHeader")))nativeWrite=true;
  }return completion&&nativeWrite&&io;
 }
 private void release()throws IOException{
  if(helper==null||Files.exists(t.root.resolve("region-lock-release.txt")))return;
  Path prepared=t.root.resolve("region-lock-release.prepared");byte[] bytes=(t.nonce+"\n").getBytes(StandardCharsets.UTF_8);
  try(var out=FileChannel.open(prepared,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);out.force(true);}
  Files.move(prepared,t.root.resolve("region-lock-release.txt"),StandardCopyOption.ATOMIC_MOVE);
 }
 private void owned(JsonObject data)throws IOException{if(!data.get("nonce").getAsString().equals(t.nonce)||data.get("pid").getAsLong()!=helperPid)throw new IOException("FAULT_HELPER_OWNER");}
 private static JsonObject read(Path path)throws IOException{byte[] bytes=Files.readAllBytes(path);if(bytes.length>4096)throw new IOException("FAULT_HELPER_RECEIPT_LIMIT");return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();}
 private void safePath(Path path)throws IOException{
  if(!path.toRealPath().startsWith(t.world)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("FAULT_REGION_PATH");
  for(Path current=path;current!=null;current=current.getParent())if(Files.isSymbolicLink(current))throw new IOException("FAULT_REGION_SYMLINK");
 }
 private void receipt(String verdict,Throwable error)throws IOException{
  var data=new JsonObject();data.addProperty("scope","DIRECT_NATIVE_DOMAIN_WRITE_ERROR");data.addProperty("verdict",verdict);data.addProperty("scenario",t.scenario);data.addProperty("faultCase","NATIVE_WRITE_LOCK");data.addProperty("nonce",t.nonce);data.addProperty("sourceRevision",t.source);data.addProperty("pid",ProcessHandle.current().pid());data.addProperty("checks",t.checks);data.addProperty("actualExceptionalStoreFuture",storeFailed);data.addProperty("helperPid",helperPid);data.addProperty("helperReleaseAndExitAttested",releaseAttested);data.addProperty("remainingOwnedHelperProcesses",helper!=null&&helper.isAlive()?1:0);data.addProperty("nonterminalJournalRetained",engine!=null&&!engine.terminal());data.addProperty("failureClass",error==null?failureClass:error.getClass().getSimpleName());data.addProperty("world",t.world.toString());data.addProperty("limitations","WRITE_ERROR_ONLY; disposable region may be damaged; no crash/recovery/timeout/late-completion/unload/natural combat claim");
  byte[] bytes=(DomainJournalCodec.canonical(data)+"\n").getBytes(StandardCharsets.UTF_8);try(var output=FileChannel.open(t.root.resolve("native-fault-native.json"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())output.write(buffer);output.force(true);}
 }
 private static BlockPos pos(DomainGeometry.Cell cell){return new BlockPos(cell.x(),cell.y(),cell.z());}
}

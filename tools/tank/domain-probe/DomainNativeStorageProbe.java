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

/** Finite corruption/predecessor restart in its own freshly created disposable world only. */
final class DomainNativeStorageProbe {
 private final DomainNativeProbe.Trial t;private final String fault;private final Path canonical,prepared;
 private final List<DomainGeometry.Cell> cells=List.of(new DomainGeometry.Cell(112,16,64),new DomainGeometry.Cell(113,16,64),new DomainGeometry.Cell(114,16,64));
 private boolean finished,retained,blocked,unchanged;
 private DomainNativeStorageProbe(DomainNativeProbe.Trial t)throws IOException{
  this.t=t;fault=System.getProperty("naturalghast.domainProbe.faultCase","");if(!Set.of("STORAGE_CORRUPT_CANONICAL","STORAGE_PREDECESSOR_MISMATCH").contains(fault))throw new IOException("DOMAIN_STORAGE_PROBE_CASE");
  canonical=t.world.resolve("data/naturalghast-domain-v1").resolve(DomainJournalCodec.sha(t.dimension.getBytes(StandardCharsets.UTF_8))).resolve("slot-0.json");prepared=canonical.resolveSibling("slot-0.prepared.json");
 }
 static void run(DomainNativeProbe.Trial t)throws IOException{var probe=new DomainNativeStorageProbe(t);t.helper.onEachTick(probe::tick);}
 private void tick(){
  if(finished)return;
  try{
   if(t.scenario.equals("crash")){
    t.repository=DomainJournalRepository.open(t.world);List<DomainOverlay.Change> changes=new ArrayList<>();for(var cell:cells){t.check(t.level.getBlockState(pos(cell)).isAir(),"FRESH_STORAGE_PROBE_CELL");changes.add(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState()));}
    var engine=t.start(0,changes);engine.place(DomainWorldAdapter.forJournal(t.level,engine.journal()),3);t.check(engine.journal().phase()==DomainOverlay.Phase.ACTIVE,"STORAGE_PROBE_NATIVE_OVERLAY_APPLIED");
    var original=engine.journal();byte[] bytes=Files.readAllBytes(canonical);t.repository.close();t.repository=null;
    // Exact private owned path and predecessor; no original/finalized file is writable here.
    t.check(canonical.toRealPath().startsWith(t.world)&&!Files.isSymbolicLink(canonical)&&DomainJournalCodec.decode(bytes).equals(original),"OWNED_PRIVATE_CANONICAL_PREDECESSOR");
    if(fault.equals("STORAGE_CORRUPT_CANONICAL")){
     var root=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();String hash=root.get("bodySha256").getAsString();root.addProperty("bodySha256",(hash.charAt(0)=='0'?"1":"0")+hash.substring(1));
     if(!Arrays.equals(Files.readAllBytes(canonical),bytes))throw new IOException("PRIVATE_STORAGE_PREDECESSOR_CHANGED");force(canonical,(DomainJournalCodec.canonical(root)+"\n").getBytes(StandardCharsets.UTF_8),false);
    }else{
     String wrong="a".repeat(64);if(wrong.equals(hash(canonical)))wrong="b".repeat(64);
     var candidate=new DomainOverlay.Journal(original.identity(),DomainOverlay.Phase.RESTORING,original.entries(),0,"PRIVATE_AMBIGUOUS_PREDECESSOR");force(prepared,DomainJournalCodec.encode(candidate,wrong),true);
    }
    t.level.getServer().saveEverything(true,true,true);receipt("EXPECTED_ABRUPT_HALT",null);Runtime.getRuntime().halt(73);throw new AssertionError("halt returned");
   }
   byte[] priorBytes=Files.readAllBytes(t.root.resolve("crash-native.json"));if(priorBytes.length>8192)throw new IOException("STORAGE_PRIOR_SIZE");var prior=JsonParser.parseString(new String(priorBytes,StandardCharsets.UTF_8)).getAsJsonObject();
   t.check(prior.get("verdict").getAsString().equals("EXPECTED_ABRUPT_HALT")&&prior.get("faultCase").getAsString().equals(fault)&&prior.get("nonce").getAsString().equals(t.nonce)&&prior.get("sourceRevision").getAsString().equals(t.source),"STORAGE_OWNED_PRIOR");
   t.check(!prior.get("processStartedAt").getAsString().equals(startedAt()),"STORAGE_GENUINELY_NEW_NATIVE_PROCESS");
   String canonicalBefore=hash(canonical),preparedBefore=hash(prepared);t.check(canonicalBefore.equals(prior.get("canonicalSha256").getAsString())&&preparedBefore.equals(prior.get("preparedSha256").getAsString()),"STORAGE_EXACT_POST_RESTART_FILES");
   JsonArray expected=prior.getAsJsonArray("blockStates");for(int i=0;i<cells.size();i++)t.check(state(cells.get(i)).equals(expected.get(i).getAsString()),"STORAGE_PERSISTED_NATIVE_OVERLAY");
   String rejection=fault.equals("STORAGE_CORRUPT_CANONICAL")?"DOMAIN_RECORD_CHECKSUM":"DOMAIN_AMBIGUOUS_PREPARATION";
   try(var repository=DomainJournalRepository.open(t.world)){throw new AssertionError("INVALID_STORAGE_ADMITTED");}catch(IOException error){t.check(rejection.equals(error.getMessage()),"STORAGE_EXACT_REPOSITORY_REJECTION");}
   try(var coordinator=DomainNativeCoordinator.open(t.level.getServer())){throw new AssertionError("INVALID_STORAGE_COORDINATOR_ADMITTED");}catch(IOException error){t.check(rejection.equals(error.getMessage()),"STORAGE_ACTIVATION_BLOCKED");blocked=true;}
   t.check(hash(canonical).equals(canonicalBefore)&&hash(prepared).equals(preparedBefore),"STORAGE_UNKNOWN_FILES_RETAINED_EXACTLY");retained=true;
   for(int i=0;i<cells.size();i++)t.check(state(cells.get(i)).equals(expected.get(i).getAsString()),"STORAGE_NO_UNAUTHORIZED_NATIVE_RESTORE");unchanged=true;
   receipt("PASS",null);finished=true;t.helper.succeed();
  }catch(Exception|AssertionError error){finished=true;try{if(t.repository!=null)t.repository.close();}catch(IOException ignored){}t.repository=null;try{receipt("FAIL",error);}catch(IOException ignored){}t.helper.fail("DOMAIN_PRIVATE_STORAGE_FAILED: "+error.getClass().getSimpleName()+":"+error.getMessage());}
 }
 private static void force(Path path,byte[] bytes,boolean create)throws IOException{
  try(var out=FileChannel.open(path,create?new StandardOpenOption[]{StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE}:new StandardOpenOption[]{StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING})){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);out.force(true);}
 }
 private void receipt(String verdict,Throwable error)throws IOException{
  var result=new JsonObject();result.addProperty("scope","DIRECT_NATIVE_DOMAIN_STORAGE_RETENTION_RESTART");result.addProperty("verdict",verdict);result.addProperty("scenario",t.scenario);result.addProperty("faultCase",fault);result.addProperty("nonce",t.nonce);result.addProperty("sourceRevision",t.source);result.addProperty("pid",ProcessHandle.current().pid());result.addProperty("processStartedAt",startedAt());result.addProperty("worldFlushBeforeHalt",verdict.equals("EXPECTED_ABRUPT_HALT"));if(t.scenario.equals("crash"))result.addProperty("cleanShutdown",false);result.addProperty("checks",t.checks);result.addProperty("canonicalSha256",hash(canonical));result.addProperty("preparedSha256",hash(prepared));result.addProperty("corruptOrAmbiguousRetained",retained);result.addProperty("activationBlocked",blocked);result.addProperty("nativeBlocksUnchanged",unchanged);result.addProperty("failureClass",error==null?"":error.getClass().getSimpleName());result.addProperty("limitations","EXPECTED_REJECTION_AND_SAFE_RETENTION; no repaired world, natural boss, disk/power-loss claim");var states=new JsonArray();for(var cell:cells)states.add(state(cell));result.add("blockStates",states);
  force(t.root.resolve(t.scenario+"-native.json"),(DomainJournalCodec.canonical(result)+"\n").getBytes(StandardCharsets.UTF_8),true);
 }
 private String state(DomainGeometry.Cell cell)throws IOException{if(!t.level.hasChunkAt(pos(cell)))throw new IOException("STORAGE_SNAPSHOT_UNLOADED");return DomainStateCodec.encode(t.level.getBlockState(pos(cell)));}
 private static String hash(Path path)throws IOException{return Files.exists(path)?DomainJournalCodec.sha(Files.readAllBytes(path)):DomainJournalCodec.ABSENT;}
 private static String startedAt(){return ProcessHandle.current().info().startInstant().orElseThrow().toString();}
 private static BlockPos pos(DomainGeometry.Cell cell){return new BlockPos(cell.x(),cell.y(),cell.z());}
}

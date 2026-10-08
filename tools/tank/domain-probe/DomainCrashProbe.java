package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Abrupt process termination exists only in this explicitly owned, isolated sidecar. */
final class DomainCrashProbe {
 private final DomainNativeProbe.Trial t;private final String fault;
 private final List<DomainGeometry.Cell> cells=List.of(new DomainGeometry.Cell(112,16,64),new DomainGeometry.Cell(113,16,64),new DomainGeometry.Cell(114,16,64));
 private DomainOverlay engine;private DomainOverlay.World world;private DomainOverlay.Journal saved;
 private DomainPersistenceBarrier durability;
 private DomainPersistenceBarrier overlaySeed;private boolean overlaySaved;
 private final boolean durabilityCase;private boolean retainedConflict;private final Set<DomainGeometry.Cell> expectedAmbiguous=new HashSet<>();
 private boolean initialized,finished;private int placements,restorations;private List<String> expectedFinal;
 private DomainCrashProbe(DomainNativeProbe.Trial trial)throws IOException{t=trial;fault=System.getProperty("naturalghast.domainProbe.faultCase","");if(!valid(fault))throw new IOException("DOMAIN_PROBE_FAULT_CASE");durabilityCase=fault.startsWith("DURABILITY_");}
 static void run(DomainNativeProbe.Trial trial)throws IOException{var probe=new DomainCrashProbe(trial);trial.helper.onEachTick(probe::tick);}
 static boolean valid(String fault){
  if(Set.of("BEFORE_FIRST_MUTATION","AFTER_FIRST_MUTATION","PARTIAL_PLACEMENT","BEFORE_FIRST_RESTORE","AFTER_FIRST_RESTORE","PARTIAL_RESTORATION","UNPUBLISHED_INITIAL_THIRD_PARTY","UNPUBLISHED_PENDING_THIRD_PARTY","DURABILITY_BEFORE_BARRIER","DURABILITY_TERMINAL_PREPARE_BEGIN","PREPLACEMENT_THIRD_PARTY","REJECTED_BEFORE_CONFLICT_PUBLICATION").contains(fault))return true;
  for(String prefix:List.of("INIT_","PLACE_","RESTORE_","TERMINAL_"))if(fault.startsWith(prefix))try{DomainJournalRepository.PublicationBoundary.valueOf(fault.substring(prefix.length()));return true;}catch(IllegalArgumentException ignored){}
  return false;
 }
 private void tick(){
  if(finished)return;
  try{
   if(!initialized){if(t.scenario.equals("recover")){initializeRecovery();initialized=true;}else{initializeCrash();initialized=true;}}
   if(t.scenario.equals("recover")){
    if(engine!=null&&engine.journal().phase()==DomainOverlay.Phase.RESTORING)engine.restore(world,1);
    verifyDurability();
    if(!expectedAmbiguous.isEmpty()){
     boolean settled=engine.journal().entries().stream().allMatch(e->expectedAmbiguous.contains(e.cell())?e.status()==DomainOverlay.Status.DURABILITY_CONFLICT:e.status()==DomainOverlay.Status.RESTORED);
     if(!settled)return;
     t.check(engine.journal().phase()==DomainOverlay.Phase.RESTORING&&!engine.terminal(),"NATIVE_RESTART_UNVERIFIED_OWNERSHIP_RETAINED");
     for(int i=0;i<cells.size();i++)t.check(nativeState(cells.get(i)).equals(expectedFinal.get(i)),"AMBIGUOUS_PERSISTED_OVERLAY_NEVER_OVERWRITTEN");
     t.repository.close();t.repository=null;
     try(var nativeCoordinator=DomainNativeCoordinator.open(t.level.getServer())){
      var coordinator=nativeCoordinator.coordinator();t.check(!coordinator.ready(),"NATIVE_DURABILITY_CONFLICT_BLOCKS_ADMISSION");
      var incoming=coordinator.begin(UUID.randomUUID(),UUID.randomUUID(),t.dimension,t.plan,List.of());t.check(incoming.outcome()==DomainCoordinator.Outcome.RECONCILIATION_PENDING,"NATIVE_NO_START_WHILE_DURABILITY_UNCERTAIN");
      coordinator.tick(); // A restarted original-state cell may require one meaningful transition.
      Path canonical=canonical();String before=rawHash(canonical);coordinator.tick();t.check(rawHash(canonical).equals(before),"UNCHANGED_NATIVE_DURABILITY_CONFLICT_NO_REWRITE");
     }
     retainedConflict=true;receipt("PASS","RECOVERY_DURABILITY_CONFLICT_RETAINED",null);finished=true;t.helper.succeed();return;
    }
    if(engine==null||engine.terminal()){
     for(int i=0;i<cells.size();i++)t.check(nativeState(cells.get(i)).equals(expectedFinal.get(i)),"GENUINE_RESTART_FINAL_BLOCK_STATE");
     if(engine!=null)t.check(t.repository.slot(t.dimension,0).read().phase()==DomainOverlay.Phase.VERIFIED_TERMINAL,"GENUINE_RESTART_TERMINAL_JOURNAL");
     t.level.getServer().saveEverything(true,true,true);t.repository.close();t.repository=null;receipt("PASS","RECOVERY_VERIFIED",null);finished=true;t.helper.succeed();
    }return;
   }
   boolean restoring=durabilityCase||fault.startsWith("RESTORE_")||fault.startsWith("TERMINAL_")||Set.of("BEFORE_FIRST_RESTORE","AFTER_FIRST_RESTORE","PARTIAL_RESTORATION").contains(fault);
   if(engine.journal().phase()==DomainOverlay.Phase.PLACING)engine.place(world,fault.equals("PARTIAL_PLACEMENT")?1:3);
   if(durabilityCase&&engine.journal().phase()==DomainOverlay.Phase.ACTIVE&&!overlaySaved){
    if(overlaySeed==null){
     // Private fixture persistence descriptor only: no canonical ledger replacement or block set.
     var current=engine.journal();var entries=current.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.overlay(),e.original(),DomainOverlay.Status.RESTORED,true)).toList();
     overlaySeed=DomainNativePersistence.barrier(t.level,new DomainOverlay.Journal(current.identity(),DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,entries,0,"OWNED_FIXTURE_OVERLAY_BASELINE"));
    }
    var current=engine.journal();var descriptor=new DomainOverlay.Journal(current.identity(),DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,current.entries().stream().map(e->new DomainOverlay.Entry(e.cell(),e.overlay(),e.original(),DomainOverlay.Status.RESTORED,true)).toList(),0,"OWNED_FIXTURE_OVERLAY_BASELINE");
    var result=overlaySeed.step(descriptor);if(result==DomainOverlay.DurabilityResult.CONFLICT)throw new IOException("DOMAIN_PROBE_OVERLAY_BASELINE_CHANGED");if(result!=DomainOverlay.DurabilityResult.VERIFIED)return;overlaySaved=true;
   }
   if(restoring&&engine.journal().phase()==DomainOverlay.Phase.ACTIVE)engine.requestRestore("PRIVATE_CRASH_RESTORE");
   if(restoring&&engine.journal().phase()==DomainOverlay.Phase.RESTORING)engine.restore(world,fault.equals("PARTIAL_RESTORATION")?1:3);
   if(restoring)verifyDurability();
   if(engine.terminal()||!restoring&&engine.journal().phase()==DomainOverlay.Phase.ACTIVE)throw new IOException("DOMAIN_PROBE_SELECTED_BOUNDARY_NOT_REACHED");
  }catch(Exception|AssertionError error){finished=true;try{if(t.repository!=null)t.repository.close();}catch(IOException ignored){}t.repository=null;try{receipt("FAIL",error.getClass().getSimpleName(),null);}catch(IOException ignored){}t.helper.fail("DOMAIN_PRIVATE_CRASH_PROBE_FAILED: "+error.getClass().getSimpleName()+":"+error.getMessage());}
 }
 private void verifyDurability()throws IOException{
  if(engine!=null&&engine.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY){if(durability==null)durability=DomainNativePersistence.barrier(t.level,engine.journal());engine.verifyDurability(durability);}
 }
 private void initializeCrash()throws Exception{
  for(var cell:cells)t.check(t.level.getBlockState(pos(cell)).isAir(),"FRESH_CRASH_CELL_AIR");
  t.repository=DomainJournalRepository.open(t.world,this::boundary);
  List<DomainOverlay.Change> changes=new ArrayList<>();for(var cell:cells)changes.add(t.change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState()));
  engine=t.start(0,changes);var adapter=DomainWorldAdapter.forJournal(t.level,engine.journal());
  if(fault.equals("PREPLACEMENT_THIRD_PARTY"))t.fixture(cells.get(1),Blocks.BLACKSTONE.defaultBlockState());
  world=new DomainOverlay.World(){
   public boolean loaded(DomainGeometry.Cell c)throws IOException{return adapter.loaded(c);}
   public String state(DomainGeometry.Cell c)throws IOException{return adapter.state(c);}
   public boolean occupied(DomainGeometry.Cell c,String desired)throws IOException{return adapter.occupied(c,desired);}
   public void set(DomainGeometry.Cell c,String desired)throws IOException{
    boolean restore=desired.equals(DomainStateCodec.encode(Blocks.AIR.defaultBlockState()));
    if(!restore&&placements==0&&fault.equals("BEFORE_FIRST_MUTATION"))halt("BEFORE_FIRST_MUTATION",null);
    if(restore&&restorations==0&&fault.equals("BEFORE_FIRST_RESTORE"))halt("BEFORE_FIRST_RESTORE",null);
    adapter.set(c,desired);if(restore)restorations++;else placements++;
    if(!restore&&placements==1&&fault.equals("REJECTED_BEFORE_CONFLICT_PUBLICATION"))t.fixture(cells.get(1),Blocks.BLACKSTONE.defaultBlockState());
    if(!restore&&placements==1&&fault.equals("AFTER_FIRST_MUTATION"))halt("AFTER_FIRST_MUTATION",null);
    if(restore&&restorations==1&&fault.equals("AFTER_FIRST_RESTORE"))halt("AFTER_FIRST_RESTORE",null);
   }
  };
 }
 private void boundary(DomainJournalRepository.PublicationBoundary observed,DomainOverlay.Journal next)throws IOException{
  boolean selected=false;
  String prefix=next.reason().equals("START")?"INIT_":next.reason().equals("PLACEMENT_PENDING")?"PLACE_":next.reason().equals("RESTORATION_PENDING")?"RESTORE_":next.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL?"TERMINAL_":"";
  if(!prefix.isEmpty())selected=fault.equals(prefix+observed.name());
  if(fault.equals("PARTIAL_PLACEMENT"))selected=observed==DomainJournalRepository.PublicationBoundary.PUBLISHED_BEFORE_RETURN&&next.phase()==DomainOverlay.Phase.PLACING&&next.cursor()==1&&next.entries().get(0).status()==DomainOverlay.Status.APPLIED;
  if(fault.equals("PARTIAL_RESTORATION"))selected=observed==DomainJournalRepository.PublicationBoundary.PUBLISHED_BEFORE_RETURN&&next.reason().equals("RESTORATION_UNRESOLVED")&&next.entries().get(0).status()==DomainOverlay.Status.RESTORED;
  if(Set.of("UNPUBLISHED_INITIAL_THIRD_PARTY","UNPUBLISHED_PENDING_THIRD_PARTY").contains(fault)){
   selected=observed==DomainJournalRepository.PublicationBoundary.PREPARE_FORCED&&prefix.equals(fault.equals("UNPUBLISHED_INITIAL_THIRD_PARTY")?"INIT_":"PLACE_");
   if(selected)t.fixture(cells.get(0),Blocks.BLACKSTONE.defaultBlockState());
  }
  if(fault.equals("DURABILITY_BEFORE_BARRIER"))selected=next.phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY&&observed==DomainJournalRepository.PublicationBoundary.PUBLISHED_BEFORE_RETURN;
  if(fault.equals("DURABILITY_TERMINAL_PREPARE_BEGIN"))selected=next.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL&&observed==DomainJournalRepository.PublicationBoundary.PREPARE_BEGIN;
  if(fault.equals("PREPLACEMENT_THIRD_PARTY"))selected=next.reason().equals("PLACEMENT_CONTEXT_CHANGED")&&observed==DomainJournalRepository.PublicationBoundary.PUBLISHED_BEFORE_RETURN;
  if(fault.equals("REJECTED_BEFORE_CONFLICT_PUBLICATION"))selected=next.reason().equals("PLACEMENT_CONTEXT_CHANGED")&&observed==DomainJournalRepository.PublicationBoundary.PREPARE_BEGIN;
  if(selected)halt(prefix+observed.name(),next);
 }
 private void initializeRecovery()throws Exception{
  byte[] bytes=Files.readAllBytes(t.root.resolve("crash-native.json"));if(bytes.length>8192)throw new IOException("DOMAIN_PROBE_CRASH_RECEIPT_SIZE");JsonObject prior=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
  t.check(prior.get("verdict").getAsString().equals("EXPECTED_ABRUPT_HALT")&&prior.get("faultCase").getAsString().equals(fault)&&prior.get("nonce").getAsString().equals(t.nonce)&&prior.get("sourceRevision").getAsString().equals(t.source),"OWNED_CRASH_RECEIPT");
  t.check(!prior.get("processStartedAt").getAsString().equals(startedAt()),"GENUINELY_NEW_PROCESS_START");
  JsonArray states=prior.getAsJsonArray(durabilityCase?"expectedPersistedBlockStates":"blockStates");t.check(states.size()==cells.size(),"CRASH_BLOCK_SNAPSHOT_BOUND");for(int i=0;i<cells.size();i++)t.check(nativeState(cells.get(i)).equals(states.get(i).getAsString()),"ACTUAL_POST_RESTART_PERSISTED_BLOCK");
  Path canonical=t.world.resolve("data/naturalghast-domain-v1").resolve(DomainJournalCodec.sha(t.dimension.getBytes(StandardCharsets.UTF_8))).resolve("slot-0.json");
  t.check(rawHash(canonical).equals(prior.get("canonicalSha256").getAsString()),"ACTUAL_POST_RESTART_PUBLISHED_JOURNAL");
  t.repository=DomainJournalRepository.open(t.world);var slot=t.repository.slot(t.dimension,0);saved=slot.read();
  expectedFinal=new ArrayList<>();
  for(int i=0;i<cells.size();i++){
   String persisted=states.get(i).getAsString(),original=DomainStateCodec.encode(Blocks.AIR.defaultBlockState());
   var entry=saved==null?null:saved.entries().get(i);
   boolean ambiguous=entry!=null&&entry.mutationIntent()&&(entry.status()==DomainOverlay.Status.PENDING||entry.status()==DomainOverlay.Status.RESTORED)&&!persisted.equals(entry.original());
   if(ambiguous)expectedAmbiguous.add(cells.get(i));
   expectedFinal.add(entry==null||!entry.mutationIntent()||ambiguous?persisted:original);
  }
  if(durabilityCase)overlaySaved=prior.get("overlayBaselineDurabilityVerified").getAsBoolean();
  if(saved==null){t.check(t.repository.loadAll().isEmpty(),"UNPUBLISHED_JOURNAL_GRANTS_NO_RESTORATION_AUTHORITY");}
  else{engine=DomainOverlay.recover(saved,slot);world=DomainWorldAdapter.forJournal(t.level,engine.journal());t.check(engine.terminal()||engine.journal().phase()==DomainOverlay.Phase.RESTORING,"RESTART_NEVER_RESUMES_ACTIVATION");}
  t.check(!Files.exists(canonical.resolveSibling("slot-0.prepared.json")),"PREPARATION_CLASSIFIED_AND_RECLAIMED");
 }
 private void halt(String point,DomainOverlay.Journal candidate)throws IOException{
  // Deliberate synchronous world flush defines this process-crash durability condition, not power loss.
  if(!durabilityCase)t.level.getServer().saveEverything(true,true,true);
  receipt("EXPECTED_ABRUPT_HALT",point,candidate);
  Runtime.getRuntime().halt(73);throw new AssertionError("halt returned");
 }
 private void receipt(String verdict,String point,DomainOverlay.Journal candidate)throws IOException{
  Path canonical=t.repository==null?canonical():t.repository.slot(t.dimension,0).path();
  JsonObject result=new JsonObject();result.addProperty("scope",durabilityCase?"DIRECT_NATIVE_DOMAIN_DURABILITY_RESTART":"DIRECT_NATIVE_DOMAIN_INTERRUPTION_RESTART");result.addProperty("verdict",verdict);result.addProperty("scenario",t.scenario);result.addProperty("faultCase",fault);result.addProperty("boundary",point);result.addProperty("nonce",t.nonce);result.addProperty("sourceRevision",t.source);result.addProperty("pid",ProcessHandle.current().pid());result.addProperty("processStartedAt",startedAt());result.addProperty("world",t.world.toString());result.addProperty("worldFlushBeforeHalt",verdict.equals("EXPECTED_ABRUPT_HALT")&&!durabilityCase);if(t.scenario.equals("crash"))result.addProperty("cleanShutdown",false);result.addProperty("closureAuthority","CONTROLLER_ONLY");result.addProperty("checks",t.checks);result.addProperty("placements",placements);result.addProperty("restorations",restorations);result.addProperty("canonicalSha256",rawHash(canonical));result.addProperty("preparedSha256",rawHash(canonical.resolveSibling("slot-0.prepared.json")));result.addProperty("candidatePhase",candidate==null?"":candidate.phase().name());
  result.addProperty("unresolvedDurabilityRetained",retainedConflict);
  if(durabilityCase){
   result.addProperty("overlayBaselineDurabilityVerified",overlaySaved);result.addProperty("nativeBarrierVerifiedBeforeHalt",fault.equals("DURABILITY_TERMINAL_PREPARE_BEGIN"));
   var persisted=new JsonArray();for(var cell:cells)persisted.add(DomainStateCodec.encode((fault.equals("DURABILITY_BEFORE_BARRIER")?Blocks.BLACKSTONE:Blocks.AIR).defaultBlockState()));result.add("expectedPersistedBlockStates",persisted);
  }
  result.addProperty("publishedPhase",Files.exists(canonical)?DomainJournalCodec.decode(Files.readAllBytes(canonical)).phase().name():"ABSENT");var states=new JsonArray();for(var cell:cells)states.add(nativeState(cell));result.add("blockStates",states);
  byte[] bytes=(DomainJournalCodec.canonical(result)+"\n").getBytes(StandardCharsets.UTF_8);try(var channel=FileChannel.open(t.root.resolve(t.scenario+"-native.json"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
 }
 private Path canonical(){return t.world.resolve("data/naturalghast-domain-v1").resolve(DomainJournalCodec.sha(t.dimension.getBytes(StandardCharsets.UTF_8))).resolve("slot-0.json");}
 private String nativeState(DomainGeometry.Cell cell)throws IOException{if(!t.level.hasChunkAt(pos(cell)))throw new IOException("DOMAIN_PROBE_SNAPSHOT_UNLOADED");return DomainStateCodec.encode(t.level.getBlockState(pos(cell)));}
 private static String rawHash(Path path)throws IOException{return Files.exists(path)?DomainJournalCodec.sha(Files.readAllBytes(path)):DomainJournalCodec.ABSENT;}
 private static String startedAt(){return ProcessHandle.current().info().startInstant().orElseThrow().toString();}
 private static BlockPos pos(DomainGeometry.Cell cell){return new BlockPos(cell.x(),cell.y(),cell.z());}
}

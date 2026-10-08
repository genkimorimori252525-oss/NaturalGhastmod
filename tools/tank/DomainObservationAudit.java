package com.genki.soutoughast.tank;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
import com.genki.soutoughast.entity.ai.domain.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit finite read-only candidate and journal supplement; no writer/selector/player changes. */
public final class DomainObservationAudit {
 private static DomainPreparation candidate;private static int attempts,retry;private static boolean readinessWritten;
 private static String lastPhase="";private static int transitions;private static JsonObject journals;
 private DomainObservationAudit(){}
 public static void observe(SoutouGhast boss,ServerPlayer player,Path output,JsonObject row)throws IOException{
  var state=boss.getDomainAttack().state();String phase=state.phase().name();
  if(!readinessWritten&&phase.equals("AIR")&&player!=null&&player.isAlive()&&boss.getTarget()==player){
   var region=((SoutouGhastInertialMoveControl)boss.getMoveControl()).getCombatRegion();
   boolean visible=boss.getSensing().hasLineOfSight(player);
   try{
    if(candidate==null&&retry--<=0&&attempts<3){attempts++;candidate=new DomainPreparation(boss,player,region);}
    if(candidate!=null){candidate.step(region,visible);if(candidate.complete()){
     var changes=candidate.commit(region,visible);var proof=new JsonObject();proof.addProperty("verdict","PASS");proof.addProperty("canonicalPlayer",boss.level().getServer().getPlayerList().getPlayer(player.getUUID())==player);proof.addProperty("subjectUuid",boss.getUUID().toString());proof.addProperty("playerUuid",player.getUUID().toString());proof.addProperty("plannedCells",candidate.plan().cells().size());proof.addProperty("changedCells",changes.size());proof.addProperty("fullParticipantFit",DomainPreparation.participantsFit(boss,player,(ServerLevel)boss.level(),candidate.plan()));proof.addProperty("freshFloorCommitted",true);proof.addProperty("admissionNotYetStarted",!boss.getDomainAttack().active());proof.addProperty("tick",boss.level().getGameTime());proof.addProperty("attempts",attempts);
     Files.writeString(output.resolve("domain-readiness.json"),proof+"\n",StandardOpenOption.CREATE_NEW);readinessWritten=true;candidate=null;
    }}
   }catch(IOException|RuntimeException rejected){candidate=null;retry=20;String message=rejected.getMessage();row.addProperty("domainReadinessRejection",message!=null&&message.startsWith("DOMAIN_")?message:rejected.getClass().getSimpleName());}
  }
  if(!phase.equals(lastPhase)){
   if(++transitions>24)throw new IOException("DOMAIN_OBSERVATION_TRANSITION_LIMIT");lastPhase=phase;journals=snapshot((ServerLevel)boss.level());
   var transition=new JsonObject();transition.addProperty("tick",boss.level().getGameTime());transition.addProperty("phase",phase);transition.add("journals",journals);Files.writeString(output.resolve("domain-transitions.jsonl"),transition+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  }
  row.addProperty("domainPhase",phase);row.addProperty("domainTicks",state.ticks());row.addProperty("domainReason",state.reason());row.addProperty("domainDecision",boss.getDomainAttack().decision());row.addProperty("domainOffenseAllowed",state.offenseAllowed());row.addProperty("domainArmPlacement",state.armPlacement());row.addProperty("domainTakeoffRequested",state.requestTakeoff());row.addProperty("domainGrounded",boss.getGroundCombat().grounded());row.addProperty("domainAutonomousPrototype",DomainRuntime.released());row.add("domainJournals",journals);
  var diagnostic=boss.getDomainAttack().preparationDiagnostics();var preparation=new JsonObject();
  preparation.addProperty("startServerTick",diagnostic.startTick());preparation.addProperty("completeServerTick",diagnostic.completeTick());preparation.addProperty("checkedCells",diagnostic.checkedCells());preparation.addProperty("quietTicks",diagnostic.quietTicks());preparation.addProperty("recentTicks",diagnostic.recentTicks());preparation.addProperty("candidates",diagnostic.candidates());preparation.addProperty("selectionOpportunities",diagnostic.selectionOpportunities());row.add("domainPreparation",preparation);
 }
 public static JsonObject snapshot(ServerLevel level)throws IOException{
  if(!level.getServer().isSameThread())throw new IOException("DOMAIN_OBSERVATION_THREAD");Path world=level.getServer().getWorldPath(LevelResource.ROOT).toRealPath();
  String dim=level.dimension().location().toString();Path folder=world.resolve("data/naturalghast-domain-v1").resolve(sha(dim.getBytes(StandardCharsets.UTF_8)));
  var result=new JsonObject();var slots=new JsonArray();boolean allTerminal=true,current=true;int count=0;
  for(int i=0;i<4;i++){
   Path file=folder.resolve("slot-"+i+".json");if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))continue;
   if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||!file.toRealPath().startsWith(world)||Files.size(file)>DomainJournalCodec.MAX_BYTES)throw new IOException("DOMAIN_OBSERVATION_RECORD_PATH_OR_LIMIT");
   byte[] bytes=Files.readAllBytes(file);var journal=DomainJournalCodec.decode(bytes);if(journal.identity().slot()!=i||!journal.identity().dimension().equals(dim))throw new IOException("DOMAIN_OBSERVATION_RECORD_IDENTITY");count++;
   boolean terminal=journal.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL;allTerminal&=terminal;
   var item=new JsonObject();item.addProperty("slot",i);item.addProperty("phase",journal.phase().name());item.addProperty("domainUuid",journal.identity().domain().toString());item.addProperty("ownerUuid",journal.identity().owner().toString());item.addProperty("generation",journal.identity().generation());item.addProperty("sha256",sha(bytes));item.addProperty("changedCells",journal.entries().size());
   boolean originals=terminal;Map<String,BlockState> decoded=new HashMap<>();
   if(terminal)for(var entry:journal.entries())if(entry.mutationIntent()){
    BlockPos pos=new BlockPos(entry.cell().x(),entry.cell().y(),entry.cell().z());if(!level.hasChunkAt(pos)){originals=false;continue;}
    BlockState expected=decoded.get(entry.original());if(expected==null){expected=DomainStateCodec.decode(entry.original());decoded.put(entry.original(),expected);}originals&=level.getBlockState(pos).equals(expected);
   }
   current&=originals;item.addProperty("originalsLoadedCurrent",originals);slots.add(item);
  }
  result.addProperty("allVerifiedTerminal",count>0&&allTerminal);result.addProperty("originalsLoadedCurrent",count>0&&current);result.addProperty("tick",level.getGameTime());result.add("slots",slots);return result;
 }
 private static String sha(byte[] bytes)throws IOException{try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException error){throw new IOException("DOMAIN_OBSERVATION_HASH",error);}}
}

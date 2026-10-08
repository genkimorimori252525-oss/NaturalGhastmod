package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.*;
import com.genki.soutoughast.entity.ai.domain.*;
import com.genki.soutoughast.entity.ai.flight.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Explicit private reliability activation, outside ScopedOwner; not natural admission. */
@Mod("naturalghast_domain_boss_probe")
public final class DomainBossIntegrationProbe {
 private JsonObject request;private Path output;private long startGameTime,deadline;private int samples,attempts,combatTicks,transitions;
 private boolean done,entered,abortRequested,duplicateRejected;private String lastPhase="";private DomainPreparation candidate;
 private JsonObject journals;private int previousGroundShots;private boolean sawGround,sawTakeoff;private double lowestY=Double.POSITIVE_INFINITY;
 private final List<String> failures=new ArrayList<>();
 public DomainBossIntegrationProbe(){MinecraftForge.EVENT_BUS.register(this);}
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
  if(event.phase!=TickEvent.Phase.END||done||!"1".equals(System.getenv("KNEEKURA_DEBUG_DOMAIN_BOSS_RELIABILITY")))return;
  var server=ServerLifecycleHooks.getCurrentServer();if(server==null||!server.isSameThread())return;
  try{step(server.overworld());}catch(Exception error){
   failures.add(error.getClass().getSimpleName()+":"+(error.getMessage()!=null&&error.getMessage().startsWith("DOMAIN_")?error.getMessage():"PROBE_FAILED"));
   if(entered&&request!=null&&!abortRequested){var subject=server.overworld().getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));if(subject instanceof SoutouGhast boss){boss.getDomainAttack().abortForStop();abortRequested=true;}}
   try{finish(server.overworld(),"FAIL","PROBE_EXCEPTION");}catch(IOException unresolved){done=true;}
  }
 }
 private void step(ServerLevel level)throws IOException{
  if(request==null){
   Path run=Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).toRealPath();output=run.resolve("evidence/derived/domain-boss-integration");
   Path file=output.resolve("request.json");if(!Files.exists(file))return;
   if(Files.size(file)>4096||Files.isSymbolicLink(file))throw new IOException("DOMAIN_PROBE_REQUEST_PATH_OR_SIZE");
   request=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   if(!request.keySet().equals(Set.of("nonce","world","subjectUuid","playerUuid","maxTicks","maxWallMs","abortCombatTicks","dispatchEpochMs"))
     ||!request.get("nonce").getAsString().equals(System.getenv("KNEEKURA_DEBUG_DOMAIN_BOSS_NONCE"))
     ||request.get("maxTicks").getAsInt()!=1200||request.get("maxWallMs").getAsInt()!=60000||request.get("abortCombatTicks").getAsInt()!=40)throw new IOException("DOMAIN_PROBE_REQUEST_SCOPE");
   Path expected=Path.of(System.getenv("KNEEKURA_DEBUG_DOMAIN_BOSS_WORLD")).toRealPath();
   if(!expected.equals(Path.of(request.get("world").getAsString()).toRealPath())||!expected.equals(level.getServer().getWorldPath(LevelResource.ROOT).toRealPath()))throw new IOException("DOMAIN_PROBE_WORLD_IDENTITY");
   if(Files.exists(run.resolve("control/owner-envelope.json"))||Files.exists(run.resolve("control/owner-status.json")))throw new IOException("DOMAIN_PROBE_SCOPED_OWNER_PRESENT");
   long dispatch=request.get("dispatchEpochMs").getAsLong(),now=System.currentTimeMillis();deadline=Math.addExact(dispatch,60000);
   if(dispatch>now||now-dispatch>5000)throw new IOException("DOMAIN_PROBE_DISPATCH_EXPIRED");
   startGameTime=level.getGameTime();Files.createDirectories(output);
  }
  if(samples>=1200||System.currentTimeMillis()>=deadline){finish(level,"FAIL","BOUNDED_WINDOW_EXPIRED");return;}
  var entity=level.getEntity(UUID.fromString(request.get("subjectUuid").getAsString()));
  var player=level.getServer().getPlayerList().getPlayer(UUID.fromString(request.get("playerUuid").getAsString()));
  if(!(entity instanceof SoutouGhast boss)||player==null||player instanceof FakePlayer||player.connection==null||!player.connection.connection.isConnected()
    ||player.isCreative()||player.isSpectator()||player.level()!=level||boss.isNoAi())throw new IOException("DOMAIN_PROBE_GENUINE_PARTICIPANTS");
  var control=(SoutouGhastInertialMoveControl)boss.getMoveControl();var region=control.getCombatRegion();
  if(!entered&&player.isAlive()&&region!=null&&boss.getTarget()==player){
   boolean visible=boss.getSensing().hasLineOfSight(player);
   try{
    if(candidate==null){if(++attempts>3)throw new IOException("DOMAIN_PROBE_CANDIDATE_LIMIT");candidate=new DomainPreparation(boss,player,region);}
    candidate.step(region,visible);
    if(candidate.complete()&&!boss.getStandardAttack().engaged()&&control.getTacticalState().action()==TacticalEvaluator.Action.DRIFT){
     var handle=DomainBossReliabilityActivation.activate(boss,player,region,candidate);entered=true;
     duplicateRejected=DomainBossReliabilityActivation.duplicateRejected(boss,player,region,candidate,handle);
     if(!duplicateRejected)throw new IOException("DOMAIN_PROBE_DUPLICATE_NOT_REJECTED");
    }
   }catch(IOException rejected){
    if(entered||rejected.getMessage()!=null&&rejected.getMessage().startsWith("DOMAIN_PROBE_"))throw rejected;
    candidate=null;var rejection=new JsonObject();rejection.addProperty("tick",level.getGameTime());rejection.addProperty("reason",rejected.getMessage());append("rejections.jsonl",rejection);
   }
  }
  var state=boss.getDomainAttack().state();String phase=state.phase().name();
  if(!phase.equals(lastPhase)){if(++transitions>24)throw new IOException("DOMAIN_PROBE_PHASE_LIMIT");lastPhase=phase;journals=DomainObservationAudit.snapshot(level);}
  var ground=boss.getGroundCombat();int shots=ground.firedCount();
  boolean ending=Set.of("ENDING","EXIT_HOLD","TAKEOFF").contains(phase);
  if(ending&&(state.offenseAllowed()||shots!=previousGroundShots))throw new IOException("DOMAIN_PROBE_CLEANUP_OFFENSE");
  previousGroundShots=shots;
  if(entered&&boss.getOverheadAttack().active())throw new IOException("DOMAIN_PROBE_OVERLAPPING_MAJOR");
  if(entered&&!phase.equals("AIR")&&!boss.getMajorDirector().active())throw new IOException("DOMAIN_PROBE_SHARED_MAJOR_LOST");
  var floor=new GroundClearance(boss).floor(SoutouGhastInertialMoveControl.from(boss.position()));
  boolean supported=ground.grounded()&&floor!=null&&Math.abs(boss.getY()-floor.y())<.12;
  if(phase.equals("COMBAT")){if(!supported||!player.isAlive())throw new IOException("DOMAIN_PROBE_COMBAT_NOT_SUPPORTED_LIVE");sawGround=true;combatTicks++;lowestY=Math.min(lowestY,boss.getY());}
  if(phase.equals("TAKEOFF")){sawTakeoff=true;if(journals==null||!journals.get("allVerifiedTerminal").getAsBoolean()||!journals.get("originalsLoadedCurrent").getAsBoolean())throw new IOException("DOMAIN_PROBE_TAKEOFF_BEFORE_DURABILITY");}
  var row=new JsonObject();row.addProperty("tick",level.getGameTime());row.addProperty("sample",samples++);row.addProperty("phase",phase);row.addProperty("phaseTicks",state.ticks());row.addProperty("reason",state.reason());
  row.addProperty("subjectUuid",boss.getUUID().toString());row.addProperty("width",boss.getBbWidth());row.addProperty("height",boss.getBbHeight());row.addProperty("noAI",boss.isNoAi());
  row.addProperty("x",boss.getX());row.addProperty("y",boss.getY());row.addProperty("z",boss.getZ());row.addProperty("speed",boss.getDeltaMovement().length());row.addProperty("supported",supported);row.addProperty("groundPhase",ground.state().phase().name());row.addProperty("groundShots",shots);row.addProperty("offenseAllowed",state.offenseAllowed());row.addProperty("playerHealth",player.getHealth());row.addProperty("playerUuid",player.getUUID().toString());row.addProperty("canonicalConnectedPlayer",true);row.addProperty("majorActive",boss.getMajorDirector().active());row.add("journals",journals);append("rows.jsonl",row);
  if(entered&&combatTicks==40&&!abortRequested){boss.getDomainAttack().abortForStop();abortRequested=true;}
  if(entered&&phase.equals("AIR")){
   var finalJournals=DomainObservationAudit.snapshot(level);boolean complete=abortRequested&&combatTicks==40&&sawGround&&sawTakeoff&&ground.state().phase()==GroundCombat.Phase.AIR
    &&!boss.getMajorDirector().active()&&boss.getY()>lowestY+2&&finalJournals.get("allVerifiedTerminal").getAsBoolean()&&finalJournals.get("originalsLoadedCurrent").getAsBoolean();
   if(!complete)failures.add("DOMAIN_PROBE_INCOMPLETE_ABORT_CYCLE");finish(level,complete?"PASS":"FAIL","EXIT_OBSERVED");
  }else if(!player.isAlive()&&!entered){finish(level,"FAIL","PLAYER_DIED_BEFORE_RELIABILITY_ENTRY");}
 }
 private void append(String name,JsonObject value)throws IOException{Files.writeString(output.resolve(name),value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private void finish(ServerLevel level,String status,String reason)throws IOException{
  done=true;if(output==null)return;Files.createDirectories(output);var result=new JsonObject();result.addProperty("scope","EXPLICIT_ACTUAL_BOSS_DOMAIN_ABORT_INTEGRATION_NOT_NATURAL_ADMISSION");result.addProperty("status",status);result.addProperty("reason",reason);result.addProperty("nonce",System.getenv("KNEEKURA_DEBUG_DOMAIN_BOSS_NONCE"));result.addProperty("samples",samples);result.addProperty("startGameTime",startGameTime);result.addProperty("endGameTime",level.getGameTime());result.addProperty("entered",entered);result.addProperty("combatTicks",combatTicks);result.addProperty("abortRequested",abortRequested);result.addProperty("duplicateRejected",duplicateRejected);result.addProperty("sawGround",sawGround);result.addProperty("sawTakeoff",sawTakeoff);result.add("journals",DomainObservationAudit.snapshot(level));var errors=new JsonArray();failures.forEach(errors::add);result.add("failures",errors);Files.writeString(output.resolve("result.json"),result+"\n",StandardOpenOption.CREATE_NEW);
 }
}

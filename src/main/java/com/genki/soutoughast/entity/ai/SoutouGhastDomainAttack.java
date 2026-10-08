package com.genki.soutoughast.entity.ai;

import java.io.IOException;
import java.util.UUID;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.domain.*;
import com.genki.soutoughast.entity.ai.flight.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Standalone gated Domain: world writer and Ground produce intents, never direct boss movement. */
public final class SoutouGhastDomainAttack {
 private final SoutouGhast ghast;private final DomainLifecycle life=new DomainLifecycle();
 private DomainPreparation prepared;private DomainGeometry.Plan plan;private DomainCoordinator coordinator;private DomainCoordinator.Handle handle;
 private UUID subject;private long regionGeneration;private int retry;private boolean failed;private String decision="AUTONOMY_GATED";
 public SoutouGhastDomainAttack(SoutouGhast ghast){this.ghast=ghast;}
 public boolean active(){return life.active();}
 public DomainLifecycle.State state(){return life.state();}
 public String decision(){return decision;}
 public boolean tryBegin(LivingEntity target,boolean visible,CombatAnchor.Region region,boolean busy){
  if(!DomainRuntime.released()){prepared=null;decision="AUTONOMY_GATED";return false;}
  if(active()||failed)return false;
  if(!(ghast.level() instanceof ServerLevel level)||!(target instanceof ServerPlayer player)||!visible||region==null||!player.isAlive()
    ||ghast.getOverheadAttack().active()||ghast.getGroundCombat().state().phase()==GroundCombat.Phase.LANDING||ghast.getGroundCombat().state().phase()==GroundCombat.Phase.TAKEOFF){prepared=null;decision="PARTICIPANT_OR_MODE";return false;}
  var writer=DomainRuntime.current(level.getServer());if(writer==null||!writer.ready()){prepared=null;decision="RECONCILIATION_PENDING";return false;}
  try{
   if(prepared!=null&&!prepared.participant().equals(player.getUUID()))prepared=null;
   if(prepared==null){if(retry>0){retry--;return false;}prepared=new DomainPreparation(ghast,player,region);}
   if(!prepared.complete())prepared.step(region,visible);else prepared.validate(region,visible);
   if(!prepared.complete()){decision="READ_ONLY_PREPARATION";return false;}
   var director=ghast.getMajorDirector();
   if(!director.shouldBeginDomain(true,busy,ghast.getRandom().nextDouble())){decision=director.decision();return false;}
   var changes=prepared.commit(region,visible);
   var admission=writer.begin(UUID.randomUUID(),ghast.getUUID(),level.dimension().location().toString(),prepared.plan(),changes);
   if(admission.outcome()!=DomainCoordinator.Outcome.STARTED){decision=admission.outcome().name();prepared=null;retry=20;return false;}
   coordinator=writer;handle=admission.handle();plan=prepared.plan();subject=player.getUUID();regionGeneration=region.generation();prepared=null;
   life.begin();director.began();ghast.getStandardAttack().reset();ghast.getGroundCombat().hold();decision="RESERVED_TELL";
   level.levelEvent(null,1015,ghast.blockPosition(),0);return true;
  }catch(IOException|RuntimeException error){
   prepared=null;retry=20;decision=reason(error);
   // Once a reservation exists, a later failed entry cannot silently return to ordinary offense.
   if(handle!=null){
    if(!life.active())life.begin();
    try{apply(life.abort("ENTRY_FAILED"));}catch(IOException|RuntimeException unresolved){failed=true;}
    return true;
   }
   return false;
  }
 }
 public GroundCombat.State tick(LivingEntity target,boolean visible,CombatAnchor.Region region,MobilityContext.Sample sample){
  ghast.getMajorDirector().tick(false);
  if(failed){ghast.setCharging(false);return ghast.getGroundCombat().hold();}
  boolean observed=target instanceof ServerPlayer player&&player.isAlive()&&!player.isRemoved()&&!player.isCreative()&&!player.isSpectator()
    &&player.getUUID().equals(subject)&&visible&&player.level()==ghast.level()&&ghast.getSensing().hasLineOfSight(player);
  boolean geometry=observed&&region!=null&&region.generation()==regionGeneration&&ghast.level() instanceof ServerLevel level
    &&DomainPreparation.participantsFit(ghast,(ServerPlayer)target,level,plan);
  var ground=ghast.getGroundCombat();boolean movingFailed=ground.state().phase()==GroundCombat.Phase.SAFE_HOLD
    &&(life.state().phase()==DomainLifecycle.Phase.LANDING||life.state().phase()==DomainLifecycle.Phase.COMBAT||life.state().phase()==DomainLifecycle.Phase.TAKEOFF);
  try{
   var overlay=handle.phase();
   if((overlay==DomainOverlay.Phase.RESTORING||overlay==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY||handle.terminal())
      &&(life.state().phase()==DomainLifecycle.Phase.START||life.state().phase()==DomainLifecycle.Phase.PLACING||life.state().phase()==DomainLifecycle.Phase.LANDING||life.state().phase()==DomainLifecycle.Phase.COMBAT))apply(life.abort("OVERLAY_ABORTED"));
   var s=life.step(new DomainLifecycle.Input(observed,geometry,handle.phase()==DomainOverlay.Phase.ACTIVE,
     ground.grounded()&&geometry,handle.terminal(),ground.state().phase()==GroundCombat.Phase.AIR,movingFailed));
   apply(s);
   if(s.beginLanding()&&!ground.beginDomainLanding(new FlightVector(ghast.getX(),plan.floorY(),ghast.getZ())))apply(life.abort("LANDING_REJECTED"));
   boolean offense=life.state().offenseAllowed()&&coordinator.offenseAllowed(handle);
   if(life.state().offenseAllowed()&&!offense)apply(life.abort("OFFENSE_AUTHORITY_LOST"));
   s=life.state();decision=s.reason();
   if(!active()){
    ghast.getMajorDirector().finished();plan=null;handle=null;coordinator=null;subject=null;retry=20;ghast.setCharging(false);return ground.hold();
   }
   if(s.phase()==DomainLifecycle.Phase.START){ground.hold();ghast.setCharging(true);return ground.state();}
   boolean exit=handle.terminal()&&(s.phase()==DomainLifecycle.Phase.TAKEOFF||s.phase()==DomainLifecycle.Phase.EXIT_HOLD);
   if(s.phase()==DomainLifecycle.Phase.PLACING||!exit&&(s.phase()==DomainLifecycle.Phase.ENDING||s.phase()==DomainLifecycle.Phase.EXIT_HOLD)){ghast.setCharging(false);return ground.hold();}
   var domainRegion=new CombatAnchor.Region(new FlightVector(plan.centerX()+.5,plan.floorY(),plan.centerZ()+.5),new FlightVector(17,8,17),regionGeneration,CombatAnchor.Reason.ACQUIRED);
   return ground.tick(target,observed,domainRegion,sample,offense,exit,offense);
  }catch(IOException|RuntimeException error){
   boolean restoring=DomainEncounterFailure.requestRestore(life,coordinator,handle);
   failed=true;decision=(restoring?"RESTORE_REQUESTED:":"RESTORE_UNRESOLVED:")+reason(error);
   ghast.setCharging(false);return ground.hold();
  }
 }
 private void apply(DomainLifecycle.State state)throws IOException{
  if(state.armPlacement()&&!coordinator.armPlacement(handle)){var aborted=life.abort("ARM_REJECTED");if(aborted.requestRestore())coordinator.requestRestore(handle,aborted.reason());}
  if(state.requestRestore())coordinator.requestRestore(handle,state.reason());
 }
 /** Every point in the complete swept4x4 rectangle must stay in the supported interior. */
 public boolean supportedInteriorRoute(FlightVector from,FlightVector to){
  if(plan==null||handle==null||handle.terminal())return true;
  return DomainPreparation.inside(plan,new AABB(Math.min(from.x(),to.x())-2,Math.min(from.y(),to.y()),Math.min(from.z(),to.z())-2,
    Math.max(from.x(),to.x())+2,Math.max(from.y(),to.y())+4,Math.max(from.z(),to.z())+2));
 }
 public void abortForStop(){
  prepared=null;if(!active())return;
  try{apply(life.abort("GOAL_STOPPING"));}catch(IOException|RuntimeException error){failed=true;decision=reason(error);}
  ghast.getGroundCombat().hold();
 }
 private static String reason(Exception error){String value=error instanceof IOException?error.getMessage():error.getClass().getSimpleName();return value==null?"DOMAIN_UNRESOLVED":value.substring(0,Math.min(96,value.length()));}
}

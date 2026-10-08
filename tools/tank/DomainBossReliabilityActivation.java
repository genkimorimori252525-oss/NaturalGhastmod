package com.genki.soutoughast.entity.ai;

import java.io.IOException;
import java.util.UUID;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.domain.*;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import net.minecraft.server.level.ServerPlayer;

/** Private verification-JAR helper only; never packaged in the normal product. */
public final class DomainBossReliabilityActivation {
 private DomainBossReliabilityActivation(){}
 public static DomainCoordinator.Handle activate(SoutouGhast boss,ServerPlayer player,CombatAnchor.Region region,DomainPreparation candidate)throws IOException{
  if(boss.getDomainAttack().active()||boss.getOverheadAttack().active()||boss.getMajorDirector().active())throw new IOException("RELIABILITY_MAJOR_BUSY");
  var writer=DomainRuntime.current(boss.level().getServer());if(writer==null||!writer.ready())throw new IOException("RELIABILITY_WRITER_NOT_READY");
  var changes=candidate.commit(region,boss.getSensing().hasLineOfSight(player));
  var admission=writer.begin(UUID.randomUUID(),boss.getUUID(),boss.level().dimension().location().toString(),candidate.plan(),changes);
  if(admission.outcome()!=DomainCoordinator.Outcome.STARTED)throw new IOException("RELIABILITY_ADMISSION_"+admission.outcome());
  try{boss.getDomainAttack().enterReservedEncounter(writer,admission.handle(),candidate.plan(),player,region);}
  catch(IOException|RuntimeException error){try{writer.requestRestore(admission.handle(),"RELIABILITY_ENTRY_FAILED");}catch(IOException|RuntimeException unresolved){error.addSuppressed(unresolved);}throw error;}
  return admission.handle();
 }
 public static boolean duplicateRejected(SoutouGhast boss,ServerPlayer player,CombatAnchor.Region region,DomainPreparation candidate,DomainCoordinator.Handle handle)throws IOException{
  var before=boss.getDomainAttack().state();var journal=handle.journal();
  try{boss.getDomainAttack().enterReservedEncounter(DomainRuntime.current(boss.level().getServer()),handle,candidate.plan(),player,region);return false;}
  catch(IOException expected){return "DOMAIN_ENTRY_MAJOR_BUSY".equals(expected.getMessage())&&before.equals(boss.getDomainAttack().state())&&journal.equals(handle.journal())&&boss.getMajorDirector().active();}
 }
}

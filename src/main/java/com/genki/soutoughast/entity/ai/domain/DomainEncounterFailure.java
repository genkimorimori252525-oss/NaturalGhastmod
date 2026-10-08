package com.genki.soutoughast.entity.ai.domain;

import java.io.IOException;

/** A held living owner must not leave a healthy coordinator's ACTIVE overlay unexpired. */
public final class DomainEncounterFailure {
 private DomainEncounterFailure(){}
 public static boolean requestRestore(DomainLifecycle life,DomainCoordinator coordinator,DomainCoordinator.Handle handle){
  life.abort("ENCOUNTER_FAILED");
  if(coordinator==null||handle==null)return false;
  try{coordinator.requestRestore(handle,"ENCOUNTER_FAILED");return true;}
  catch(IOException|RuntimeException unresolved){return false;}
 }
}

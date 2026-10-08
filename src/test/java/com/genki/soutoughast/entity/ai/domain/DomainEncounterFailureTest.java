package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** Injected Ground callback failure, healthy coordinator and continuously living simulated owner. */
public final class DomainEncounterFailureTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  var storage=new DomainCoordinatorTest.Storage();var worlds=new DomainCoordinatorTest.Worlds();var coordinator=new DomainCoordinator(storage,worlds);
  var cell=new DomainGeometry.Cell(0,64,0);var admission=coordinator.begin(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",DomainCoordinatorTest.plan(0,64),List.of(new DomainOverlay.Change(cell,"air","blackstone")));
  coordinator.armPlacement(admission.handle());coordinator.tick();check(coordinator.offenseAllowed(admission.handle()),"active healthy writer/living owner");
  var life=new DomainLifecycle();life.begin();for(int i=0;i<60;i++)life.step(new DomainLifecycle.Input(true,true,false,false,false,false,false));
  life.step(new DomainLifecycle.Input(true,true,true,false,false,false,false));life.step(new DomainLifecycle.Input(true,true,true,true,false,false,false));
  check(life.state().offenseAllowed(),"combat before injected callback exception");
  try{groundCallback();throw new AssertionError("injected failure absent");}
  catch(IOException expected){check(DomainEncounterFailure.requestRestore(life,coordinator,admission.handle()),"exception admits restoration before failed hold");}
  check(life.state().phase()==DomainLifecycle.Phase.ENDING&&!life.state().offenseAllowed()&&!coordinator.offenseAllowed(admission.handle()),"both lifecycle and world offense suppressed");
  for(int i=0;i<12&&!admission.handle().terminal();i++)coordinator.tick();
  check(worlds.ownerPresent&&admission.handle().terminal()&&worlds.states.get(cell).equals("air"),"living held owner no longer prevents coordinator restoration");
  admission=coordinator.begin(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",DomainCoordinatorTest.plan(0,64),List.of());storage.fail=true;
  check(!DomainEncounterFailure.requestRestore(life,coordinator,admission.handle())&&!coordinator.ready(),"failed restoration publication remains unresolved/fail closed");
  System.out.println("PASS: "+checks+" encounter callback failure/restoration simulation checks");
 }
 private static void groundCallback()throws IOException{throw new IOException("INJECTED_GROUND_CALLBACK_EXCEPTION");}
}

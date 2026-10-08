package com.genki.soutoughast.entity.ai.domain;

/** Timing/abort/terminal contracts only; no native boss acceptance. */
public final class DomainLifecycleTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static DomainLifecycle.Input input(boolean placed,boolean grounded,boolean terminal){return new DomainLifecycle.Input(true,true,placed,grounded,terminal,false,false);}
 public static void main(String[] args){
  var life=new DomainLifecycle();check(!life.active(),"ordinary movement starts untouched");life.begin();
  for(int i=0;i<59;i++){var s=life.step(input(false,false,false));check(!s.armPlacement()&&!s.offenseAllowed(),"no placement/offense during tell");}
  var s=life.step(input(false,false,false));check(s.phase()==DomainLifecycle.Phase.PLACING&&s.armPlacement(),"arm only after60tick reserved tell");
  s=life.step(input(false,false,false));check(!s.armPlacement()&&!s.beginLanding(),"placement request not repeated");
  s=life.step(input(true,false,false));check(s.phase()==DomainLifecycle.Phase.LANDING&&s.beginLanding()&&!s.offenseAllowed(),"placement before landing/offense");
  for(int i=0;i<25;i++)check(!life.step(input(true,false,false)).offenseAllowed(),"landing does not consume combat timer");
  s=life.step(input(true,true,false));check(s.phase()==DomainLifecycle.Phase.COMBAT&&s.ticks()==0&&s.offenseAllowed(),"supported landing starts400combat");
  for(int i=0;i<399;i++)check(life.step(input(true,true,false)).offenseAllowed(),"combat remains finite");
  s=life.step(input(true,true,false));check(s.phase()==DomainLifecycle.Phase.ENDING&&s.requestRestore()&&!s.offenseAllowed()&&!s.requestTakeoff(),"expiry stops offense before restore");
  for(int i=0;i<401;i++)check(!life.step(input(true,true,false)).requestTakeoff(),"unresolved restore never permits takeoff");
  check(life.state().phase()==DomainLifecycle.Phase.EXIT_HOLD,"bounded unresolved cleanup hold");
  s=life.step(input(true,true,true));check(s.requestTakeoff()&&s.phase()==DomainLifecycle.Phase.TAKEOFF,"durable terminal releases takeoff");
  s=life.step(new DomainLifecycle.Input(false,false,true,true,true,false,true));check(s.phase()==DomainLifecycle.Phase.EXIT_HOLD&&!s.requestRestore(),"blocked exit holds; never restores terminal again");
  check(!life.step(input(true,true,true)).requestTakeoff(),"failed exit request not retriggered each tick");
  s=life.step(new DomainLifecycle.Input(false,false,true,false,true,true,false));check(s.phase()==DomainLifecycle.Phase.AIR&&!life.active(),"later genuine flight completion releases major");
  for(var phase:DomainLifecycle.Phase.values()){
   if(phase!=DomainLifecycle.Phase.START&&phase!=DomainLifecycle.Phase.PLACING&&phase!=DomainLifecycle.Phase.LANDING&&phase!=DomainLifecycle.Phase.COMBAT)continue;
   life=new DomainLifecycle();life.begin();while(life.state().phase()!=phase)life.step(input(phase==DomainLifecycle.Phase.LANDING||phase==DomainLifecycle.Phase.COMBAT,phase==DomainLifecycle.Phase.COMBAT,false));
   s=life.step(new DomainLifecycle.Input(false,true,false,false,false,false,false));check(s.requestRestore()&&!s.offenseAllowed(),"target loss aborts "+phase);
   check(!life.step(input(false,false,false)).requestRestore(),"abort publication requested once");
  }
  life=new DomainLifecycle();life.begin();for(int i=0;i<60+321;i++)s=life.step(input(false,false,false));check(s.phase()==DomainLifecycle.Phase.ENDING&&!s.offenseAllowed(),"placement has bounded timeout");
  life=new DomainLifecycle();life.begin();for(int i=0;i<60;i++)life.step(input(false,false,false));life.step(input(true,false,false));
  for(int i=0;i<161;i++)s=life.step(input(true,false,false));check(s.phase()==DomainLifecycle.Phase.ENDING&&!s.offenseAllowed(),"landing has bounded timeout");
  System.out.println("PASS: "+checks+" Domain lifecycle timing/abort/terminal simulation checks");
 }
}

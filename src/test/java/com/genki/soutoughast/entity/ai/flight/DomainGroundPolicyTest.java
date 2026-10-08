package com.genki.soutoughast.entity.ai.flight;
public final class DomainGroundPolicyTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  var director=new MajorActionDirector();for(int i=0;i<240;i++)director.tick(true);
  check(!director.shouldBegin(MobilityContext.Kind.GROUND_FORCED,true,false,24,1),"Domain does not widen Overhead context");
  check(!director.shouldBeginDomain(false,false,1),"fresh supported candidate required");
  check(!director.shouldBeginDomain(true,true,1),"committed ordinary face/action excludes Domain");
  check(director.shouldBeginDomain(true,false,1),"separate Domain eligibility after full geometry proof");
  director.began();check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,24,1),"Domain occupies shared major slot");
  director.finished();for(int i=0;i<600;i++)director.tick(true);check(!director.shouldBeginDomain(true,false,1),"shared repetition memory retained");
  for(int i=0;i<600;i++)director.tick(true);check(!director.shouldBeginDomain(true,false,.99),"rare Domain threshold not forced");
  check(director.shouldBeginDomain(true,false,1),"shared memory expires naturally");
  var ground=new GroundCombat();var floor=FlightVector.ZERO;var target=new FlightVector(0,0,-12);
  var region=new CombatAnchor.Region(new FlightVector(0,6,0),CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);
  ground.begin(floor,floor,(a,b)->true);ground.step(floor,target,region,false,null,(a,b)->true,(a,b)->true,.5);
  for(int i=0;i<100;i++){
   var s=ground.step(floor,target,region,true,new FlightVector(0,4,0),(a,b)->true,(a,b)->true,.5,false,false);
   check(s.phase()==GroundCombat.Phase.GROUNDED&&s.intent().mode()!=FlightController.Mode.MOVE,"Domain cleanup suppresses scuttle/autotakeoff");
  }
  ground.hold();check(ground.state().intent().mode()!=FlightController.Mode.MOVE,"tell hold clears any retained leg");
  for(int i=0;i<20;i++)ground.step(floor,target,region,true,new FlightVector(0,4,0),(a,b)->true,(a,b)->true,.5,true,false);
  check(ground.state().phase()==GroundCombat.Phase.TAKEOFF,"positive durable release allows ordinary validated takeoff dwell");
  System.out.println("PASS: "+checks+" shared-major/Domain Ground policy simulation checks");
 }
}

package com.genki.soutoughast.entity.ai.domain;
import java.util.*;

/** Original validation and ambiguous interruption must never grant third-party restore authority. */
public final class DomainPlacementAuthorityTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final class Crash extends Error {}
 static final DomainGeometry.Cell A=new DomainGeometry.Cell(0,64,0),B=new DomainGeometry.Cell(1,64,0);
 static final class Store implements DomainOverlay.Store {
  DomainOverlay.Journal saved;boolean crashConflict,crashPending;
  public void persist(DomainOverlay.Journal expected,DomainOverlay.Journal next){
   check(Objects.equals(saved,expected),"exact predecessor");
   if(crashConflict&&next.phase()==DomainOverlay.Phase.RESTORING)throw new Crash();
   saved=next;if(crashPending&&next.reason().equals("PLACEMENT_PENDING"))throw new Crash();
  }
 }
 static final class World implements DomainOverlay.World {
  final Map<DomainGeometry.Cell,String> states=new HashMap<>();int writes;boolean changeLater;
  public boolean loaded(DomainGeometry.Cell c){return true;}
  public String state(DomainGeometry.Cell c){return states.getOrDefault(c,"air");}
  public boolean occupied(DomainGeometry.Cell c,String desired){return false;}
  public void set(DomainGeometry.Cell c,String desired){states.put(c,desired);writes++;if(changeLater&&c.equals(A)&&desired.equals("blackstone"))states.put(B,"blackstone");}
 }
 static DomainOverlay start(Store s)throws Exception{return DomainOverlay.start(new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",0,1),List.of(new DomainOverlay.Change(A,"air","blackstone"),new DomainOverlay.Change(B,"air","blackstone")),s,null);}
 static void restore(DomainOverlay e,World w)throws Exception{for(int i=0;i<4;i++)if(e.journal().phase()==DomainOverlay.Phase.RESTORING)e.restore(w,2);if(e.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY)e.verifyDurability(j->DomainOverlay.DurabilityResult.VERIFIED);}
 public static void main(String[] args)throws Exception{
  var s=new Store();var w=new World();var e=start(s);w.states.put(B,"blackstone");e.place(w,2);
  check(w.writes==0&&e.journal().entries().stream().noneMatch(DomainOverlay.Entry::mutationIntent),"validate whole batch before acquiring any placement intent");
  e=DomainOverlay.recover(s.saved,s);restore(e,w);check(e.terminal()&&w.state(B).equals("blackstone")&&w.writes==0,"pre-intent overlay-equal third-party survives restart/cancellation");
  for(boolean crashBeforeClassification:new boolean[]{false,true}){
   s=new Store();w=new World();w.changeLater=true;e=start(s);s.crashConflict=crashBeforeClassification;
   try{e.place(w,2);if(crashBeforeClassification)throw new AssertionError("selected crash absent");}catch(Crash expected){}
   s.crashConflict=false;e=DomainOverlay.recover(s.saved,s);restore(e,w);
   check(w.state(B).equals("blackstone")&&!e.terminal(),"later unattempted overlay-equal edit retained across classification crash="+crashBeforeClassification);
   check(e.journal().entries().get(1).status()==DomainOverlay.Status.DURABILITY_CONFLICT,"explicit ambiguous ownership remains blocked");
   if(crashBeforeClassification)check(w.state(A).equals("blackstone"),"unverified attempted PENDING is ambiguous too");else check(w.state(A).equals("air"),"independently APPLIED first cell restores");
  }
  s=new Store();w=new World();e=start(s);s.crashPending=true;
  try{e.place(w,2);throw new AssertionError("pending crash absent");}catch(Crash expected){}
  s.crashPending=false;w.states.put(A,"blackstone");e=DomainOverlay.recover(s.saved,s);restore(e,w);
  check(w.writes==0&&w.state(A).equals("blackstone")&&!e.terminal(),"published unverified intent is insufficient to replay a later matching block");
  System.out.println("PASS: "+checks+" original-validation/ambiguous-ownership interruption regressions");
 }
}

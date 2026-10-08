package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** Explicit durability/restart negatives; a fake barrier is never native persistence proof. */
public final class DomainDurabilityTest {
 static int checks;
 static void check(boolean result,String why){checks++;if(!result)throw new AssertionError(why);}
 static final class Store implements DomainOverlay.Store {
  DomainOverlay.Journal saved;int writes;
  public void persist(DomainOverlay.Journal expected,DomainOverlay.Journal next){check(Objects.equals(saved,expected),"predecessor");saved=next;writes++;}
 }
 static final class World implements DomainOverlay.World {
  String value="stone";int writes;boolean loaded=true;
  public boolean loaded(DomainGeometry.Cell cell){return loaded;}
  public String state(DomainGeometry.Cell cell){check(loaded,"loaded-only");return value;}
  public boolean occupied(DomainGeometry.Cell cell,String state){return false;}
  public void set(DomainGeometry.Cell cell,String state){value=state;writes++;}
 }
 static DomainOverlay start(Store s)throws Exception{return DomainOverlay.start(new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",0,1),List.of(new DomainOverlay.Change(new DomainGeometry.Cell(0,64,0),"stone","blackstone")),s,null);}
 public static void main(String[] args)throws Exception{
  Store s=new Store();World w=new World();var e=start(s);e.place(w,1);e.requestRestore("EXPIRY");e.restore(w,1);
  check(!e.terminal()&&e.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY,"in-memory restoration is not terminal persistence");
  check(e.journal().entries().get(0).mutationIntent(),"restored entries retain published mutation ownership");
  int persisted=s.writes;e.verifyDurability(j->DomainOverlay.DurabilityResult.PENDING);check(s.writes==persisted&&!e.terminal(),"pending barrier performs no repeated journal writes");
  try{DomainOverlay.start(new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",0,2),List.of(),s,e.journal());throw new AssertionError("reuse pending durability");}catch(IllegalArgumentException expected){checks++;}
  var interrupted=s.saved;w.value="blackstone";e=DomainOverlay.recover(interrupted,s);int mutations=w.writes;e.restore(w,1);
  check(w.writes==mutations&&w.value.equals("blackstone")&&!e.terminal(),"ambiguous restored overlay after restart is preserved");
  check(e.journal().entries().get(0).status()==DomainOverlay.Status.DURABILITY_CONFLICT,"explicit durability conflict remains unresolved");
  w.loaded=false;persisted=s.writes;e.restore(w,1);e.restore(w,1);check(s.writes==persisted,"unloaded durability conflict has no reads or repeated writes");
  w.loaded=true;w.value="stone";e.restore(w,1);e.verifyDurability(j->DomainOverlay.DurabilityResult.VERIFIED);check(e.terminal(),"positive barrier plus original state permits terminal");
  s=new Store();w=new World();e=start(s);e.requestRestore("CANCEL_BEFORE_PLACEMENT");e.restore(w,1);
  check(!e.journal().entries().get(0).mutationIntent(),"RESERVED cancellation never acquires mutation ownership");
  w.value="blackstone";e=DomainOverlay.recover(s.saved,s);mutations=w.writes;e.restore(w,1);e.verifyDurability(j->DomainOverlay.DurabilityResult.VERIFIED);
  check(w.writes==mutations&&w.value.equals("blackstone")&&e.terminal(),"unowned overlay-looking third-party block remains untouched");
  s=new Store();w=new World();e=start(s);e.place(w,1);e.requestRestore("EXPIRY");e.restore(w,1);
  e.verifyDurability(j->DomainOverlay.DurabilityResult.CONFLICT);check(e.journal().phase()==DomainOverlay.Phase.RESTORING&&!e.terminal(),"barrier mismatch returns to unresolved reconciliation");
  e.restore(w,1);try{e.verifyDurability(j->{throw new IOException("IO_FAILURE");});throw new AssertionError("failed barrier");}catch(IOException expected){check(e.halted()&&!e.terminal(),"barrier failure never grants terminal");}
  System.out.println("PASS: "+checks+" pure durability/ownership contracts; native barrier NOT_RUN");
 }
}

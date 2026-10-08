package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** Full-footprint reservation and restart admission tests; no native world or boss acceptance. */
public final class DomainCoordinatorTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 record Key(String dimension,int slot) {}
 static final class Storage implements DomainCoordinator.Storage {
  final Map<Key,DomainOverlay.Journal> rows=new HashMap<>();int writes;boolean fail;
  public List<DomainOverlay.Journal> load(){return List.copyOf(rows.values());}
  public DomainOverlay.Journal read(String dimension,int slot){return rows.get(new Key(dimension,slot));}
  public DomainOverlay.Store store(String dimension,int slot){return (expected,next)->{if(fail)throw new IOException("UNAVAILABLE");var key=new Key(dimension,slot);check(Objects.equals(rows.get(key),expected),"exact predecessor");rows.put(key,next);writes++;};}
 }
 static final class Worlds implements DomainCoordinator.Worlds {
  boolean available=true,ownerPresent=true;int mutations;Map<DomainGeometry.Cell,String> states=new HashMap<>();
  public DomainOverlay.World world(DomainOverlay.Journal j){return !available?null:new DomainOverlay.World(){
   public boolean loaded(DomainGeometry.Cell c){return true;}
   public String state(DomainGeometry.Cell c){return states.getOrDefault(c,"air");}
   public boolean occupied(DomainGeometry.Cell c,String desired){return false;}
   public void set(DomainGeometry.Cell c,String desired){states.put(c,desired);mutations++;}
  };}
  public boolean ownerPresent(DomainOverlay.Identity id){return ownerPresent;}
  public DomainOverlay.Durability durability(DomainOverlay.Journal j){return saved->DomainOverlay.DurabilityResult.VERIFIED;}
 }
 static DomainGeometry.Plan plan(int x,int y){return DomainGeometry.plan(x,y,0,-64,320);}
 static DomainCoordinator.Admission reserve(DomainCoordinator c,UUID owner,int x,int y,List<DomainOverlay.Change> changes)throws Exception{return c.begin(UUID.randomUUID(),owner,"minecraft:overworld",plan(x,y),changes);}
 static DomainCoordinator.Admission begin(DomainCoordinator c,UUID owner,int x,int y,List<DomainOverlay.Change> changes)throws Exception{var a=reserve(c,owner,x,y,changes);if(a.outcome()==DomainCoordinator.Outcome.STARTED)c.armPlacement(a.handle());return a;}
 static DomainCoordinator.Handle started(DomainCoordinator.Admission a){check(a.outcome()==DomainCoordinator.Outcome.STARTED,"admission started");return a.handle();}
 static void settle(DomainCoordinator c,DomainCoordinator.Handle h)throws Exception{for(int i=0;i<12&&!h.terminal();i++)c.tick();check(h.terminal(),"bounded simulated restoration plus explicit fake barrier");}
 public static void main(String[] args)throws Exception{
  var reservedStore=new Storage();var reservedWorld=new Worlds();var reserved=new DomainCoordinator(reservedStore,reservedWorld);
  var reservedCell=new DomainGeometry.Cell(0,64,0);
  var reservation=started(reserve(reserved,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(reservedCell,"air","blackstone"))));int reservedWrites=reservedStore.writes;
  for(int i=0;i<60;i++)reserved.tick();
  check(reservation.phase()==DomainOverlay.Phase.PLACING&&reservedWorld.mutations==0&&reservedStore.writes==reservedWrites,"reservation never places before explicit arm");
  reserved.armPlacement(reservation);reserved.tick();check(reservation.phase()==DomainOverlay.Phase.ACTIVE&&reservedWorld.mutations==1,"explicit arm authorizes later bounded placement");
  reserved.requestRestore(reservation,"TEST_END");settle(reserved,reservation);
  reservation=started(reserve(reserved,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(reservedCell,"air","blackstone"))));
  for(int i=0;i<201;i++)reserved.tick();settle(reserved,reservation);check(reservedWorld.mutations==2&&!reservation.journal().entries().get(0).mutationIntent(),"abandoned reservation expires without world ownership");
  var storage=new Storage();var worlds=new Worlds();var c=new DomainCoordinator(storage,worlds);check(c.ready(),"empty restart ready");
  UUID owner=UUID.randomUUID();var h=started(begin(c,owner,0,64,List.of()));c.tick();check(h.phase()==DomainOverlay.Phase.ACTIVE,"zero-change footprint still reserved");int writes=storage.writes;
  var clash=begin(c,UUID.randomUUID(),40,64,List.of());check(clash.outcome()==DomainCoordinator.Outcome.CLASH&&clash.clashes().equals(List.of(h.identity().domain())),"touching lattice cell clash before incoming mutation");
  check(storage.rows.size()==1&&storage.writes==writes+1&&worlds.mutations==0&&h.phase()==DomainOverlay.Phase.RESTORING,"incoming has no journal; existing offense stopped/restoration requested");
  check(!c.ready()&&begin(c,UUID.randomUUID(),100,64,List.of()).outcome()==DomainCoordinator.Outcome.RECONCILIATION_PENDING,"restoring footprint blocks new activation");settle(c,h);check(c.ready(),"only verified terminal releases admission gate");
  var next=started(begin(c,owner,0,64,List.of()));check(next.identity().generation()==2&&h.terminal(),"slot reuse increments generation without invalidating old terminal handle");
  check(begin(c,owner,100,64,List.of()).outcome()==DomainCoordinator.Outcome.OWNER_BUSY,"one domain per owner");
  started(begin(c,UUID.randomUUID(),41,64,List.of()));started(begin(c,UUID.randomUUID(),0,77,List.of()));started(begin(c,UUID.randomUUID(),123,64,List.of()));
  check(begin(c,UUID.randomUUID(),240,64,List.of()).outcome()==DomainCoordinator.Outcome.SLOT_EXHAUSTED&&storage.rows.size()==4,"four slots without history growth");
  c.requestRestore(next,"OWNER_REQUEST");settle(c,next);check(storage.rows.size()==4,"fixed-slot release does not append history");
  storage=new Storage();worlds=new Worlds();c=new DomainCoordinator(storage,worlds);
  var cell=new DomainGeometry.Cell(0,64,0);h=started(begin(c,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(cell,"air","blackstone"))));c.tick();check(h.phase()==DomainOverlay.Phase.ACTIVE&&worlds.mutations==1,"bounded actual engine integration in simulation");
  worlds.ownerPresent=false;c.tick();check(!c.offenseAllowed(h)&&h.phase()!=DomainOverlay.Phase.ACTIVE,"owner loss suppresses offense before restore");settle(c,h);check(worlds.states.get(cell).equals("air"),"owner loss exact restoration");
  h=started(begin(c,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(cell,"air","blackstone"))));worlds.ownerPresent=true;c.tick();
  worlds.available=false;var restarted=new DomainCoordinator(storage,worlds);check(!restarted.ready()&&h.phase()==DomainOverlay.Phase.ACTIVE,"startup does not depend on old entity handles");writes=storage.writes;restarted.tick();check(storage.writes==writes&&begin(restarted,UUID.randomUUID(),200,64,List.of()).outcome()==DomainCoordinator.Outcome.RECONCILIATION_PENDING,"missing dimension defers without mutation/admission");
  worlds.available=true;for(int i=0;i<8&&!restarted.ready();i++)restarted.tick();check(restarted.ready()&&worlds.states.get(cell).equals("air"),"restart restores before any fresh activation");
  try{begin(restarted,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(new DomainGeometry.Cell(40,64,0),"air","blackstone")));throw new AssertionError("outside reserved footprint");}catch(IllegalArgumentException expected){checks++;}
  storage.fail=true;try{begin(restarted,UUID.randomUUID(),0,64,List.of());throw new AssertionError("failed initial publication");}catch(IOException expected){check(!restarted.ready(),"unknown publication halts admission");}
  storage=new Storage();worlds=new Worlds();worlds.ownerPresent=false;c=new DomainCoordinator(storage,worlds);
  h=started(begin(c,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(cell,"air","blackstone"))));c.tick();settle(c,h);check(worlds.mutations==0&&!h.journal().entries().get(0).mutationIntent(),"owner loss before placement never authorizes a world mutation");
  worlds.ownerPresent=true;h=started(begin(c,UUID.randomUUID(),0,64,List.of(new DomainOverlay.Change(cell,"air","blackstone"))));c.tick();c.shutdown();
  check(h.phase()==DomainOverlay.Phase.RESTORING&&!c.ready()&&!c.offenseAllowed(h),"shutdown abort published before ownership release");
  try{c.tick();throw new AssertionError("tick after shutdown");}catch(IOException expected){checks++;}
  System.out.println("PASS: "+checks+" coordinator/reservation/clash/restart simulation contracts; native gates NOT_RUN");
 }
}

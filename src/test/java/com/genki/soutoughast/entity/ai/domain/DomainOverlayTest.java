package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** Pure crash-boundary contracts, never native restart/world acceptance. */
public final class DomainOverlayTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final class Crash extends Error {}
 static final class Store implements DomainOverlay.Store {
  DomainOverlay.Journal durable;int writes,crashBefore=-1,crashAfter=-1;boolean unavailable;
  public void persist(DomainOverlay.Journal expected,DomainOverlay.Journal next)throws IOException{
   if(unavailable)throw new IOException("storage unavailable");check(Objects.equals(expected,durable),"checked predecessor");
   if(++writes==crashBefore)throw new Crash();durable=next;if(writes==crashAfter)throw new Crash();
  }
 }
 static final class World implements DomainOverlay.World {
  final Store store;final Map<DomainGeometry.Cell,String> states=new HashMap<>();final Set<DomainGeometry.Cell> absent=new HashSet<>(),occupied=new HashSet<>();
  int writes,crashBefore=-1,crashAfter=-1;
  World(Store store){this.store=store;}
  public boolean loaded(DomainGeometry.Cell c){return !absent.contains(c);}
  public String state(DomainGeometry.Cell c){check(loaded(c),"no unloaded lookup");return states.getOrDefault(c,"stone");}
  public boolean occupied(DomainGeometry.Cell c,String state){return occupied.contains(c);}
  public void set(DomainGeometry.Cell c,String state){
   check(loaded(c),"no unloaded mutation");check(store.durable.entries().stream().anyMatch(e->e.cell().equals(c)&&e.status()==DomainOverlay.Status.PENDING),"durable pending before placement/restore mutation");
   if(++writes==crashBefore)throw new Crash();states.put(c,state);if(writes==crashAfter)throw new Crash();
  }
 }
 static DomainOverlay.Identity identity(long generation){return new DomainOverlay.Identity(UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",0,generation);}
 static List<DomainOverlay.Change> changes(){return List.of(new DomainOverlay.Change(new DomainGeometry.Cell(0,0,0),"stone","blackstone"),new DomainOverlay.Change(new DomainGeometry.Cell(1,0,0),"stone","blackstone"));}
 static void finish(DomainOverlay e,World w)throws Exception{for(int i=0;i<10&&!e.terminal();i++){if(e.journal().phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY)e.verifyDurability(j->DomainOverlay.DurabilityResult.VERIFIED);else e.restore(w,1);}check(e.terminal(),"bounded test restoration reaches terminal through explicit fake barrier");check(w.states.values().stream().allMatch("stone"::equals),"exact originals restored");}
 public static void main(String[] args)throws Exception{
  var geometry=DomainGeometry.plan(0,64,0,-64,320);check(geometry.radius()==20&&geometry.height()==12,"broad declared geometry");
  check(geometry.cells().size()<=32768&&geometry.cells().size()>10000,"bounded whole planned volume");check(new HashSet<>(geometry.cells().stream().map(DomainGeometry.Tile::cell).toList()).size()==geometry.cells().size(),"unique cells");
  check(geometry.cells().stream().filter(c->c.role()==DomainGeometry.Role.FLOOR).allMatch(c->c.cell().y()==63),"floor below actor feet");
  try{DomainGeometry.plan(29_999_981,64,0,-64,320);throw new AssertionError("world bounds");}catch(IllegalArgumentException expected){checks++;}
  try{DomainGeometry.plan(0,319,0,-64,320);throw new AssertionError("build bounds");}catch(IllegalArgumentException expected){checks++;}
  Store s=new Store();World w=new World(s);var e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,1);check(w.writes==1&&e.journal().phase()==DomainOverlay.Phase.PLACING,"bounded placement batch");e.place(w,1);check(e.journal().phase()==DomainOverlay.Phase.ACTIVE,"active only after all verified placements");
  e.requestRestore("expiry");finish(e,w);int writes=w.writes;e.restore(w,64);check(w.writes==writes,"terminal restore idempotent");
  var predecessor=e.journal();e=DomainOverlay.start(identity(2),changes(),s,predecessor);check(e.journal().identity().generation()==2,"verified terminal slot reuse");
  for(boolean after:new boolean[]{false,true})for(int boundary=1;boundary<=5;boundary++){
   s=new Store();w=new World(s);if(after)s.crashAfter=boundary;else s.crashBefore=boundary;
   try{e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");e.restore(w,2);}catch(Crash crash){}
   s.crashBefore=s.crashAfter=-1;if(s.durable!=null){e=DomainOverlay.recover(s.durable,s);finish(e,w);}else check(w.writes==0,"no journal means no mutation");
  }
  for(boolean after:new boolean[]{false,true})for(int boundary=1;boundary<=4;boundary++){
   s=new Store();w=new World(s);if(after)w.crashAfter=boundary;else w.crashBefore=boundary;
   try{e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");e.restore(w,2);}catch(Crash crash){}
   w.crashBefore=w.crashAfter=-1;e=DomainOverlay.recover(s.durable,s);finish(e,w);
  }
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");var cell=changes().get(0).cell();w.states.put(cell,"third-party");e.restore(w,2);check(!e.terminal()&&w.states.get(cell).equals("third-party"),"conflict preserved, never falsely terminal");w.states.put(cell,"stone");finish(e,w);
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");w.absent.add(cell);e.restore(w,2);check(!e.terminal(),"unloaded restore stays unresolved");w.absent.clear();finish(e,w);
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);w.occupied.add(cell);e.place(w,2);check(w.writes==0&&e.journal().phase()==DomainOverlay.Phase.RESTORING,"actor safety rechecked before mutation");w.occupied.clear();finish(e,w);
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);s.unavailable=true;try{e.place(w,2);throw new AssertionError("I/O failure");}catch(IOException expected){check(e.halted()&&w.writes==0,"failed durability halts before writes");}
  try{e.place(w,1);throw new AssertionError("resume unsafe engine");}catch(IOException expected){checks++;}s.unavailable=false;e=DomainOverlay.recover(s.durable,s);finish(e,w);
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");w.occupied.add(cell);e.restore(w,2);check(!e.terminal()&&w.states.get(cell).equals("blackstone"),"restoration cannot trap a new actor");w.occupied.clear();finish(e,w);
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);w.states.put(cell,"third-party");e.place(w,2);check(w.writes==0,"placement rechecks original equality");e.restore(w,2);check(!e.terminal()&&w.states.get(cell).equals("third-party"),"pre-placement conflict also preserved");
  for(int budget:new int[]{0,65})try{e.restore(w,budget);throw new AssertionError("batch budget");}catch(IllegalArgumentException expected){checks++;}
  try{DomainOverlay.start(identity(2),changes(),s,e.journal());throw new AssertionError("reuse unresolved slot");}catch(IllegalArgumentException expected){checks++;}
  try{DomainOverlay.start(identity(1),List.of(changes().get(0),changes().get(0)),new Store(),null);throw new AssertionError("duplicate changes");}catch(IllegalArgumentException expected){checks++;}
  var tooMany=new ArrayList<DomainOverlay.Change>();for(int i=0;i<4097;i++)tooMany.add(new DomainOverlay.Change(new DomainGeometry.Cell(i,0,0),"stone","blackstone"));
  try{DomainOverlay.start(identity(1),tooMany,new Store(),null);throw new AssertionError("changed budget");}catch(IllegalArgumentException expected){checks++;}
  s=new Store();w=new World(s);e=DomainOverlay.start(identity(1),changes(),s,null);e.place(w,2);e.requestRestore("expiry");w.absent.addAll(changes().stream().map(DomainOverlay.Change::cell).toList());
  e.restore(w,2);int persisted=s.writes;for(int i=0;i<4;i++)e.restore(w,2);check(s.writes==persisted,"unchanged unloaded deferral must not continuously rewrite journal");
  w.absent.clear();w.occupied.addAll(changes().stream().map(DomainOverlay.Change::cell).toList());e.restore(w,2);persisted=s.writes;for(int i=0;i<4;i++)e.restore(w,2);check(s.writes==persisted,"unchanged actor deferral must not continuously rewrite journal");w.occupied.clear();finish(e,w);
  System.out.println("PASS: "+checks+" pure Domain geometry/write-ahead/conditional restore contracts; native gates NOT_RUN");
 }
}

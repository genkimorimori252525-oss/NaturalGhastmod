package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** One world writer; fixed slots, full-footprint reservations and restart-before-admission. */
public final class DomainCoordinator {
 public enum Outcome {STARTED,RECONCILIATION_PENDING,OWNER_BUSY,CLASH,SLOT_EXHAUSTED,DIMENSION_LIMIT}
 public record Admission(Outcome outcome,Handle handle,List<UUID> clashes){public Admission{clashes=List.copyOf(clashes);}}
 public interface Storage {
  List<DomainOverlay.Journal> load()throws IOException;
  DomainOverlay.Journal read(String dimension,int slot)throws IOException;
  DomainOverlay.Store store(String dimension,int slot)throws IOException;
 }
 public interface Worlds {
  /** Null means the dimension is not currently available; never load it for reconciliation. */
  DomainOverlay.World world(DomainOverlay.Journal journal)throws IOException;
  DomainOverlay.Durability durability(DomainOverlay.Journal journal)throws IOException;
  boolean ownerPresent(DomainOverlay.Identity identity)throws IOException;
 }
 private record Key(String dimension,int slot) {}
 public static final class Handle {
  private final DomainCoordinator coordinator;private final DomainOverlay engine;private final DomainGeometry.Plan footprint;
  private DomainOverlay.World world;private DomainOverlay.Durability durability;private boolean placementArmed;private int unarmedTicks;
  private Handle(DomainCoordinator coordinator,DomainOverlay engine,DomainGeometry.Plan footprint){this.coordinator=coordinator;this.engine=engine;this.footprint=footprint;}
  public DomainOverlay.Identity identity(){return engine.journal().identity();}
  public DomainOverlay.Phase phase(){return engine.journal().phase();}
  public DomainOverlay.Journal journal(){return engine.journal();}
  public boolean terminal(){return engine.terminal();}
 }
 private final Storage storage;private final Worlds worlds;private final Thread writer=Thread.currentThread();
 private final Map<Key,Handle> slots=new LinkedHashMap<>();private int roundRobin;private boolean failed,busy;
 public DomainCoordinator(Storage storage,Worlds worlds)throws IOException{
  this.storage=Objects.requireNonNull(storage);this.worlds=Objects.requireNonNull(worlds);
  List<DomainOverlay.Journal> saved=List.copyOf(storage.load());if(saved.size()>64)throw new IOException("DOMAIN_COORDINATOR_SLOT_LIMIT");
  Set<Key> keys=new HashSet<>();Set<String> dimensions=new HashSet<>();Set<UUID> domains=new HashSet<>();
  for(var j:saved){if(!keys.add(key(j.identity()))||!domains.add(j.identity().domain()))throw new IOException("DOMAIN_COORDINATOR_DUPLICATE_IDENTITY");dimensions.add(j.identity().dimension());}
  if(dimensions.size()>16)throw new IOException("DOMAIN_COORDINATOR_DIMENSION_LIMIT");
  // Validate the whole inventory before any recovery publication. Unknown footprints need no
  // reconstruction: all nonterminal restart records block admission until exact reconciliation.
  for(var j:saved){DomainOverlay engine=DomainOverlay.recover(j,storage.store(j.identity().dimension(),j.identity().slot()));slots.put(key(j.identity()),new Handle(this,engine,null));}
 }
 public boolean ready(){return Thread.currentThread()==writer&&!busy&&reconciled();}
 private boolean reconciled(){return !failed&&slots.values().stream().noneMatch(h->h.engine.halted()||h.phase()==DomainOverlay.Phase.RESTORING||h.phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY);}
 public boolean offenseAllowed(Handle handle){
  if(handle==null||handle.coordinator!=this||failed||busy||Thread.currentThread()!=writer||slots.get(key(handle.identity()))!=handle||handle.phase()!=DomainOverlay.Phase.ACTIVE||handle.engine.halted())return false;
  try{return worlds.ownerPresent(handle.identity());}catch(IOException|RuntimeException error){failed=true;return false;}
 }
 public Admission begin(UUID domain,UUID owner,String dimension,DomainGeometry.Plan footprint,List<DomainOverlay.Change> changes)throws IOException{
  enter();boolean externalAccess=false;try{
   Objects.requireNonNull(domain);Objects.requireNonNull(owner);Objects.requireNonNull(footprint);changes=List.copyOf(changes);
   if(!DomainGeometry.plan(footprint.centerX(),footprint.floorY(),footprint.centerZ(),-2048,2048).equals(footprint))throw new IllegalArgumentException("DOMAIN_COORDINATOR_NONCANONICAL_PLAN");
   new DomainOverlay.Identity(domain,owner,dimension,0,1);Set<DomainGeometry.Cell> cells=new HashSet<>();for(var t:footprint.cells())cells.add(t.cell());
   if(changes.size()>DomainOverlay.MAX_CHANGED||new HashSet<>(changes.stream().map(DomainOverlay.Change::cell).toList()).size()!=changes.size()||changes.stream().anyMatch(c->!cells.contains(c.cell())))throw new IllegalArgumentException("DOMAIN_COORDINATOR_CHANGE_FOOTPRINT");
   if(!reconciled())return denied(Outcome.RECONCILIATION_PENDING);
   for(var h:slots.values())if(!h.terminal()){
    if(h.identity().domain().equals(domain))throw new IllegalArgumentException("DOMAIN_COORDINATOR_DOMAIN_REUSED");
    if(h.identity().owner().equals(owner))return denied(Outcome.OWNER_BUSY);
   }
   List<Handle> overlaps=slots.values().stream().filter(h->!h.terminal()&&h.identity().dimension().equals(dimension)&&h.footprint!=null&&overlap(h.footprint,footprint)).toList();
   if(!overlaps.isEmpty()){
    externalAccess=true;
    for(var h:overlaps)h.engine.requestRestore("CLASH");
    return new Admission(Outcome.CLASH,null,overlaps.stream().map(h->h.identity().domain()).toList());
   }
   Set<String> dimensions=new HashSet<>();slots.keySet().forEach(k->dimensions.add(k.dimension()));if(!dimensions.contains(dimension)&&dimensions.size()>=16)return denied(Outcome.DIMENSION_LIMIT);
   int index=-1;DomainOverlay.Journal previous=null;
   for(int i=0;i<4;i++){
    externalAccess=true;
    var saved=storage.read(dimension,i);var known=slots.get(new Key(dimension,i));
    if(known!=null&&!Objects.equals(known.journal(),saved)||known==null&&saved!=null&&saved.phase()!=DomainOverlay.Phase.VERIFIED_TERMINAL)throw new IOException("DOMAIN_COORDINATOR_EXTERNAL_LEDGER");
    if(saved==null||saved.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL&&saved.identity().generation()<Long.MAX_VALUE){index=i;previous=saved;break;}
   }
   if(index<0)return denied(Outcome.SLOT_EXHAUSTED);
   var identity=new DomainOverlay.Identity(domain,owner,dimension,index,previous==null?1:previous.identity().generation()+1);
   // Admission is synchronous on the exclusive writer thread. No incoming world operation
   // exists until the footprint passed clash checks and START was durably published.
   var handle=new Handle(this,DomainOverlay.start(identity,changes,storage.store(dimension,index),previous),footprint);slots.put(key(identity),handle);
   return new Admission(Outcome.STARTED,handle,List.of());
  }catch(IOException error){failed=true;throw error;}catch(RuntimeException error){if(externalAccess)failed=true;throw error;}finally{busy=false;}
 }
 public void requestRestore(Handle handle,String reason)throws IOException{
  enter();try{owned(handle);handle.engine.requestRestore(reason);}catch(IOException|RuntimeException error){failed=true;throw error;}finally{busy=false;}
 }
 /** Reservation grants no world mutation. The encounter arms only after its complete tell. */
 public boolean armPlacement(Handle handle)throws IOException{
  enter();try{
   owned(handle);if(handle.phase()!=DomainOverlay.Phase.PLACING||handle.placementArmed)return false;
   if(handle.unarmedTicks>200){handle.engine.requestRestore("RESERVATION_EXPIRED");return false;}
   handle.placementArmed=true;return true;
  }catch(IOException error){failed=true;throw error;}finally{busy=false;}
 }
 /** Persist aborts before releasing repository ownership; restart performs remaining work. */
 public void shutdown()throws IOException{
  enter();try{for(var h:slots.values())if(!h.terminal())h.engine.requestRestore("SERVER_STOPPING");}finally{failed=true;busy=false;}
 }
 /** At most one64-cell engine batch or one bounded persistence step across all64 slots. */
 public void tick()throws IOException{
  enter();try{
   var handles=List.copyOf(slots.values());if(handles.isEmpty())return;
   for(var h:handles)if(h.phase()==DomainOverlay.Phase.PLACING&&!h.placementArmed)h.unarmedTicks=Math.min(201,h.unarmedTicks+1);
   for(int scan=0;scan<handles.size();scan++){
    roundRobin=Math.floorMod(roundRobin,handles.size());Handle h=handles.get(roundRobin);roundRobin=(roundRobin+1)%handles.size();if(h.terminal())continue;
    if(h.phase()==DomainOverlay.Phase.ACTIVE||h.phase()==DomainOverlay.Phase.PLACING){
     if(!worlds.ownerPresent(h.identity()))h.engine.requestRestore("OWNER_LOST");
     else if(h.phase()==DomainOverlay.Phase.PLACING&&!h.placementArmed){if(h.unarmedTicks>200)h.engine.requestRestore("RESERVATION_EXPIRED");else continue;}
     else if(h.phase()==DomainOverlay.Phase.ACTIVE)continue;
    }
    if(h.world==null)h.world=worlds.world(h.journal());if(h.world==null)continue;
    if(h.phase()==DomainOverlay.Phase.PLACING)h.engine.place(h.world,64);
    else if(h.phase()==DomainOverlay.Phase.RESTORING){h.durability=null;h.engine.restore(h.world,64);}
    else if(h.phase()==DomainOverlay.Phase.RESTORED_PENDING_DURABILITY){if(h.durability==null)h.durability=worlds.durability(h.journal());if(h.durability!=null)h.engine.verifyDurability(h.durability);}
    return;
   }
  }catch(IOException|RuntimeException error){failed=true;throw new IOException("DOMAIN_COORDINATOR_UNRESOLVED",error);}finally{busy=false;}
 }
 private void enter()throws IOException{if(failed||busy||Thread.currentThread()!=writer)throw new IOException("DOMAIN_COORDINATOR_UNAVAILABLE");busy=true;}
 private void owned(Handle h){if(h==null||h.coordinator!=this||slots.get(key(h.identity()))!=h&&!h.terminal())throw new IllegalArgumentException("DOMAIN_COORDINATOR_FOREIGN_HANDLE");}
 private static Admission denied(Outcome reason){return new Admission(reason,null,List.of());}
 private static Key key(DomainOverlay.Identity id){return new Key(id.dimension(),id.slot());}
 private static boolean overlap(DomainGeometry.Plan a,DomainGeometry.Plan b){
  if(a.floorY()-1>b.floorY()+b.height()-1||b.floorY()-1>a.floorY()+a.height()-1)return false;
  int minX=Math.max(a.centerX()-a.radius(),b.centerX()-b.radius()),maxX=Math.min(a.centerX()+a.radius(),b.centerX()+b.radius());
  int minZ=Math.max(a.centerZ()-a.radius(),b.centerZ()-b.radius()),maxZ=Math.min(a.centerZ()+a.radius(),b.centerZ()+b.radius());
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   long ax=x-a.centerX(),az=z-a.centerZ(),bx=x-b.centerX(),bz=z-b.centerZ();
   if(ax*ax+az*az<=(long)a.radius()*a.radius()&&bx*bx+bz*bz<=(long)b.radius()*b.radius())return true;
  }return false;
 }
}

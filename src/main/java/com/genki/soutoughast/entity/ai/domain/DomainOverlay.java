package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;

/** Conditional write-ahead engine; a Store must force atomic guarded persistence before returning. */
public final class DomainOverlay {
 public static final int MAX_CHANGED=4096,MAX_BATCH=64;
 public enum Phase {PLACING,ACTIVE,RESTORING,RESTORED_PENDING_DURABILITY,VERIFIED_TERMINAL}
 public enum Status {RESERVED,PENDING,APPLIED,RESTORED,DEFERRED,CONFLICT,DURABILITY_CONFLICT}
 public enum DurabilityResult {PENDING,VERIFIED,CONFLICT}
 @FunctionalInterface public interface Durability {DurabilityResult step(Journal journal)throws IOException;}
 public record Identity(UUID domain,UUID owner,String dimension,int slot,long generation) {
  public Identity {Objects.requireNonNull(domain);Objects.requireNonNull(owner);if(dimension==null||dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||slot<0||slot>=4||generation<1)throw new IllegalArgumentException("DOMAIN_IDENTITY");}
 }
 public record Change(DomainGeometry.Cell cell,String original,String overlay) {
  public Change {Objects.requireNonNull(cell);validateState(original);validateState(overlay);if(original.equals(overlay))throw new IllegalArgumentException("DOMAIN_UNCHANGED_ENTRY");}
 }
 public record Entry(DomainGeometry.Cell cell,String original,String overlay,Status status,boolean mutationIntent) {
  public Entry {new Change(cell,original,overlay);Objects.requireNonNull(status);if(status==Status.RESERVED&&mutationIntent||Set.of(Status.PENDING,Status.APPLIED,Status.DURABILITY_CONFLICT).contains(status)&&!mutationIntent)throw new IllegalArgumentException("DOMAIN_MUTATION_OWNERSHIP");}
  public Entry(DomainGeometry.Cell cell,String original,String overlay,Status status){this(cell,original,overlay,status,status!=Status.RESERVED);}
  Entry with(Status next){return new Entry(cell,original,overlay,next,mutationIntent||next==Status.PENDING||next==Status.APPLIED);}
 }
 public record Journal(Identity identity,Phase phase,List<Entry> entries,int cursor,String reason) {
  public Journal {
   Objects.requireNonNull(identity);Objects.requireNonNull(phase);entries=List.copyOf(entries);
   if(entries.size()>MAX_CHANGED||cursor<0||cursor>entries.size()||reason==null||reason.length()>128||new HashSet<>(entries.stream().map(Entry::cell).toList()).size()!=entries.size())throw new IllegalArgumentException("DOMAIN_JOURNAL_BOUNDS");
   if((phase==Phase.VERIFIED_TERMINAL||phase==Phase.RESTORED_PENDING_DURABILITY)&&entries.stream().anyMatch(e->e.status()!=Status.RESTORED))throw new IllegalArgumentException("DOMAIN_UNVERIFIED_TERMINAL");
   if(phase==Phase.ACTIVE&&entries.stream().anyMatch(e->e.status()!=Status.APPLIED))throw new IllegalArgumentException("DOMAIN_UNVERIFIED_ACTIVE");
  }
 }
 public interface Store {void persist(Journal expected,Journal next)throws IOException;}
 public interface World {
  boolean loaded(DomainGeometry.Cell cell)throws IOException;
  String state(DomainGeometry.Cell cell)throws IOException;
  boolean occupied(DomainGeometry.Cell cell,String desired)throws IOException;
  void set(DomainGeometry.Cell cell,String desired)throws IOException;
 }
 private Journal current;private final Store store;private boolean halted;private int scanCursor;
 private DomainOverlay(Journal journal,Store store){current=journal;scanCursor=journal.cursor();this.store=Objects.requireNonNull(store);}
 public Journal journal(){return current;}
 public boolean halted(){return halted;}
 public boolean terminal(){return !halted&&current.phase()==Phase.VERIFIED_TERMINAL;}
 public static DomainOverlay start(Identity identity,List<Change> changes,Store store,Journal previous)throws IOException{
  if(changes.size()>MAX_CHANGED)throw new IllegalArgumentException("DOMAIN_CHANGE_BUDGET");
  if(previous!=null&&(previous.phase()!=Phase.VERIFIED_TERMINAL||previous.identity().slot()!=identity.slot()||!previous.identity().dimension().equals(identity.dimension())||identity.generation()<=previous.identity().generation()))throw new IllegalArgumentException("DOMAIN_SLOT_NOT_REUSABLE");
  List<Entry> entries=changes.stream().map(c->new Entry(c.cell(),c.original(),c.overlay(),Status.RESERVED)).toList();
  var next=new Journal(identity,Phase.PLACING,entries,0,"START");store.persist(previous,next);return new DomainOverlay(next,store);
 }
 /** Interrupted activation never resumes placement: restore every potentially changed entry first. */
 public static DomainOverlay recover(Journal saved,Store store)throws IOException{
  var result=new DomainOverlay(saved,store);
  if(saved.phase()!=Phase.VERIFIED_TERMINAL){
   // A prior in-memory restoration does not prove the native chunk write reached storage.
   List<Entry> entries=saved.entries().stream().map(e->e.status()==Status.RESTORED&&e.mutationIntent()?e.with(Status.DURABILITY_CONFLICT):e).toList();
   result.write(new Journal(saved.identity(),Phase.RESTORING,entries,0,"RESTART_RECONCILIATION"));
  }return result;
 }
 public void requestRestore(String reason)throws IOException{
  requireRunning();if(terminal()||current.phase()==Phase.RESTORING||current.phase()==Phase.RESTORED_PENDING_DURABILITY)return;
  write(new Journal(current.identity(),Phase.RESTORING,current.entries(),0,reason));
  scanCursor=0;
 }
 public void place(World world,int budget)throws IOException{
  requireRunning();requireBudget(budget);if(current.phase()!=Phase.PLACING)return;
  if(current.entries().isEmpty()){write(copy(Phase.ACTIVE,current.entries(),0,"PLACEMENT_VERIFIED"));return;}
  int start=current.cursor(),end=Math.min(current.entries().size(),start+budget);List<Entry> entries=new ArrayList<>(current.entries());
  for(int i=start;i<end;i++)entries.set(i,entries.get(i).with(Status.PENDING));
  write(copy(Phase.PLACING,entries,start,"PLACEMENT_PENDING"));
  try{
   for(int i=start;i<end;i++){
    Entry e=entries.get(i);
    if(!world.loaded(e.cell())||!world.state(e.cell()).equals(e.original())||world.occupied(e.cell(),e.overlay())){
     write(copy(Phase.RESTORING,entries,0,"PLACEMENT_CONTEXT_CHANGED"));return;
    }
    world.set(e.cell(),e.overlay());
    if(!world.loaded(e.cell())||!world.state(e.cell()).equals(e.overlay())){write(copy(Phase.RESTORING,entries,0,"PLACEMENT_READBACK_CHANGED"));return;}
    entries.set(i,e.with(Status.APPLIED));
   }
   write(copy(end==entries.size()?Phase.ACTIVE:Phase.PLACING,entries,end==entries.size()?0:end,"PLACEMENT_VERIFIED"));
  }catch(IOException|RuntimeException error){halted=true;throw error;}
 }
 public void restore(World world,int budget)throws IOException{
  requireRunning();requireBudget(budget);if(terminal())return;if(current.phase()!=Phase.RESTORING)throw new IOException("DOMAIN_RESTORE_NOT_REQUESTED");
  if(current.entries().isEmpty()){write(copy(Phase.RESTORED_PENDING_DURABILITY,current.entries(),0,"RESTORATION_AWAITING_DURABILITY"));return;}
  List<Entry> entries=new ArrayList<>(current.entries());List<Integer> mutations=new ArrayList<>();int size=entries.size(),count=Math.min(size,budget),start=scanCursor%size;
  try{
   for(int n=0;n<count;n++){
    int i=(start+n)%size;Entry e=entries.get(i);if(e.status()==Status.RESTORED)continue;
    if(e.status()==Status.RESERVED){entries.set(i,e.with(Status.RESTORED));continue;}
    if(e.status()==Status.DURABILITY_CONFLICT){
     if(world.loaded(e.cell())&&world.state(e.cell()).equals(e.original()))entries.set(i,e.with(Status.RESTORED));
     continue; // Never overwrite an ambiguous persisted post-restoration/third-party overlay.
    }
    if(!world.loaded(e.cell())){entries.set(i,e.with(Status.DEFERRED));continue;}
    String observed=world.state(e.cell());
    if(observed.equals(e.original())){entries.set(i,e.with(Status.RESTORED));continue;}
    if(!observed.equals(e.overlay())){entries.set(i,e.with(Status.CONFLICT));continue;}
    if(world.occupied(e.cell(),e.original())){entries.set(i,e.with(Status.DEFERRED));continue;}
    entries.set(i,e.with(Status.PENDING));mutations.add(i);
   }
   scanCursor=(start+count)%size;
   if(!mutations.isEmpty()||!entries.equals(current.entries()))write(copy(Phase.RESTORING,entries,scanCursor,"RESTORATION_PENDING"));
   for(int i:mutations){
    Entry e=entries.get(i);
    if(!world.loaded(e.cell())){entries.set(i,e.with(Status.DEFERRED));continue;}
    String observed=world.state(e.cell());
    if(observed.equals(e.original())){entries.set(i,e.with(Status.RESTORED));continue;}
    if(!observed.equals(e.overlay())){entries.set(i,e.with(Status.CONFLICT));continue;}
    if(world.occupied(e.cell(),e.original())){entries.set(i,e.with(Status.DEFERRED));continue;}
    world.set(e.cell(),e.original());
    entries.set(i,e.with(world.loaded(e.cell())&&world.state(e.cell()).equals(e.original())?Status.RESTORED:Status.DEFERRED));
   }
   boolean complete=entries.stream().allMatch(e->e.status()==Status.RESTORED);
   if(complete||!entries.equals(current.entries()))write(copy(complete?Phase.RESTORED_PENDING_DURABILITY:Phase.RESTORING,entries,complete?0:scanCursor,complete?"RESTORATION_AWAITING_DURABILITY":"RESTORATION_UNRESOLVED"));
  }catch(IOException|RuntimeException error){halted=true;throw error;}
 }
 /** Only a positive native write/flush/readback barrier may authorize terminal reuse. */
 public void verifyDurability(Durability barrier)throws IOException{
  requireRunning();if(current.phase()!=Phase.RESTORED_PENDING_DURABILITY)return;
  try{
   DurabilityResult result=Objects.requireNonNull(barrier.step(current));
   if(result==DurabilityResult.VERIFIED)write(copy(Phase.VERIFIED_TERMINAL,current.entries(),0,"RESTORATION_DURABILITY_VERIFIED"));
   else if(result==DurabilityResult.CONFLICT)write(copy(Phase.RESTORING,current.entries().stream().map(e->e.mutationIntent()?e.with(Status.DURABILITY_CONFLICT):e).toList(),0,"DURABILITY_CONTEXT_CHANGED"));
  }catch(IOException|RuntimeException error){halted=true;throw error;}
 }
 private Journal copy(Phase phase,List<Entry> entries,int cursor,String reason){return new Journal(current.identity(),phase,entries,cursor,reason);}
 private void write(Journal next)throws IOException{try{store.persist(current,next);current=next;}catch(IOException|RuntimeException error){halted=true;throw error;}}
 private void requireRunning()throws IOException{if(halted)throw new IOException("DOMAIN_JOURNAL_OUTCOME_UNKNOWN");}
 private static void requireBudget(int budget){if(budget<1||budget>MAX_BATCH)throw new IllegalArgumentException("DOMAIN_BATCH_BUDGET");}
 private static void validateState(String state){if(state==null||state.isBlank()||state.length()>1024)throw new IllegalArgumentException("DOMAIN_BLOCK_STATE");}
}

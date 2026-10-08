package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

/** Four fixed slots per dimension. Only published canonical journals authorize world changes. */
public final class DomainJournalRepository implements AutoCloseable {
 private static final byte[] MARKER="NATURALGHAST_DOMAIN_V1\n".getBytes(StandardCharsets.UTF_8);
 private final Path world,root;private final Thread writer=Thread.currentThread();
 private final FileChannel channel;private final FileLock lock;private final Map<Path,Slot> slots=new HashMap<>();
 private final Object worldKey,rootKey,lockKey;
 private boolean closed,failed;
 private DomainJournalRepository(Path world,Path root,FileChannel channel,FileLock lock)throws IOException{this.world=world;this.root=root;this.channel=channel;this.lock=lock;worldKey=fileKey(world);rootKey=fileKey(root);lockKey=fileKey(root.resolve("writer.lock"));}
 public static DomainJournalRepository open(Path world)throws IOException{
  if(!world.isAbsolute())throw new IOException("DOMAIN_ABSOLUTE_WORLD_REQUIRED");
  Path canonical=world.toRealPath();if(!canonical.equals(world.normalize()))throw new IOException("DOMAIN_WORLD_ALIAS");safeAncestors(canonical);
  Path data=directory(canonical.resolve("data")),root=directory(data.resolve("naturalghast-domain-v1")),marker=root.resolve("owner.txt");
  if(!Files.exists(marker,NOFOLLOW_LINKS)){
   try(var children=Files.list(root)){if(children.findAny().isPresent())throw new IOException("DOMAIN_UNKNOWN_ROOT");}
   forceNew(marker,MARKER);
  }
  if(!Arrays.equals(read(marker),MARKER))throw new IOException("DOMAIN_ROOT_OWNER");
  Path lockPath=root.resolve("writer.lock");if(Files.exists(lockPath,NOFOLLOW_LINKS))regular(lockPath);
  FileChannel channel=FileChannel.open(lockPath,CREATE,WRITE,NOFOLLOW_LINKS);FileLock lock;
  try{lock=channel.tryLock();if(lock==null)throw new IOException("DOMAIN_WRITER_LOCKED");}
  catch(IOException|OverlappingFileLockException error){channel.close();throw new IOException("DOMAIN_WRITER_LOCKED",error);}
  DomainJournalRepository repo;
  try{repo=new DomainJournalRepository(canonical,root,channel,lock);}catch(IOException|RuntimeException error){try{lock.release();}finally{channel.close();}throw error;}
  try{repo.loadAll();return repo;}catch(IOException|RuntimeException error){repo.close();throw error;}
 }
 public Slot slot(String dimension,int index)throws IOException{
  check();if(dimension==null||dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||index<0||index>=4)throw new IllegalArgumentException("DOMAIN_SLOT");
  String hash=DomainJournalCodec.sha(dimension.getBytes(StandardCharsets.UTF_8));Path folder=root.resolve(hash);
  if(!Files.exists(folder,NOFOLLOW_LINKS)){
   try(var children=Files.list(root)){if(children.filter(p->p.getFileName().toString().matches("[a-f0-9]{64}")).count()>=16)throw new IOException("DOMAIN_DIMENSION_LIMIT");}
   directory(folder);
  }
  safeAncestors(folder);Path path=folder.resolve("slot-"+index+".json");Slot existing=slots.get(path);if(existing!=null)return existing;
  var result=new Slot(dimension,index,path);slots.put(path,result);return result;
 }
 public List<DomainOverlay.Journal> loadAll()throws IOException{
  check();List<DomainOverlay.Journal> result=new ArrayList<>();int dimensions=0;
  try(var files=Files.list(root)){
   List<Path> bounded=files.limit(19).toList();if(bounded.size()>18)throw new IOException("DOMAIN_ROOT_ENTRY_LIMIT");for(Path p:bounded){
    String name=p.getFileName().toString();if(name.equals("owner.txt")||name.equals("writer.lock")){regular(p);continue;}
    if(!name.matches("[a-f0-9]{64}")||++dimensions>16)throw new IOException("DOMAIN_UNKNOWN_ROOT_ENTRY");safeAncestors(p);
    Set<Integer> found=new HashSet<>();
    try(var children=Files.list(p)){List<Path> boundedChildren=children.limit(17).toList();if(boundedChildren.size()>16)throw new IOException("DOMAIN_SLOT_ENTRY_LIMIT");for(Path child:boundedChildren){
     regular(child);String n=child.getFileName().toString();if(!n.matches("slot-[0-3](\\.prepared|\\.diagnostic|\\.diagnostic\\.prepared)?\\.json"))throw new IOException("DOMAIN_UNKNOWN_SLOT_ENTRY");found.add(n.charAt(5)-'0');
    }}
    for(int i:found){Path canonical=p.resolve("slot-"+i+".json"),prepared=p.resolve("slot-"+i+".prepared.json");
     byte[] bytes=optional(canonical);if(bytes==null)bytes=optional(prepared);
     if(bytes==null){Path diagnostic=p.resolve("slot-"+i+".diagnostic.json");bytes=optional(p.resolve("slot-"+i+".diagnostic.prepared.json"));if(bytes==null)bytes=optional(diagnostic);
      if(bytes==null)throw new IOException("DOMAIN_EMPTY_SLOT_RECORD");validateDiagnosticBytes(bytes);String dimension=DomainJournalCodec.string(DomainJournalCodec.unwrap(bytes).payload(),"dimension");
      if(!DomainJournalCodec.sha(dimension.getBytes(StandardCharsets.UTF_8)).equals(name)||DomainJournalCodec.number(DomainJournalCodec.unwrap(bytes).payload(),"slot")!=i)throw new IOException("DOMAIN_SLOT_IDENTITY");slot(dimension,i).read();continue;}
     var journal=DomainJournalCodec.decode(bytes);if(journal.identity().slot()!=i||!DomainJournalCodec.sha(journal.identity().dimension().getBytes(StandardCharsets.UTF_8)).equals(name))throw new IOException("DOMAIN_SLOT_IDENTITY");
     Slot slot=slot(journal.identity().dimension(),i);var saved=slot.read();if(saved!=null)result.add(saved);
    }
   }
  }
  return List.copyOf(result);
 }
 public final class Slot implements DomainOverlay.Store {
  private final String dimension;private final int index;private final Path path,prepared,diagnostic,diagnosticPrepared;
  private String observed;
  private Slot(String dimension,int index,Path path)throws IOException{
   this.dimension=dimension;this.index=index;this.path=path;String prefix="slot-"+index;
   prepared=path.resolveSibling(prefix+".prepared.json");diagnostic=path.resolveSibling(prefix+".diagnostic.json");diagnosticPrepared=path.resolveSibling(prefix+".diagnostic.prepared.json");
   reconcile();observed=hash(optional(path));
  }
  public Path path(){return path;}
  public DomainOverlay.Journal read()throws IOException{
   check();validateDiagnostic(diagnostic);byte[] bytes=optional(path);if(!hash(bytes).equals(observed))throw new IOException("DOMAIN_PREDECESSOR_CHANGED");return bytes==null?null:validate(bytes);
  }
  public void persist(DomainOverlay.Journal expected,DomainOverlay.Journal next)throws IOException{
   check();boolean publicationStarted=false;try{
    DomainOverlay.Journal current=read();if(!Objects.equals(current,expected))throw new IOException("DOMAIN_STALE_PREDECESSOR");validateIdentity(next);transition(expected,next);
    if(Files.exists(prepared,NOFOLLOW_LINKS))throw new IOException("DOMAIN_UNRECONCILED_PREPARATION");
    byte[] bytes=DomainJournalCodec.encode(next,observed);publicationStarted=true;forceNew(prepared,bytes);replace(prepared,path,observed,bytes);observed=hash(bytes);
   }catch(IOException|RuntimeException error){if(publicationStarted||Files.exists(prepared,NOFOLLOW_LINKS))failed=true;throw error;}
  }
  private DomainOverlay.Journal validate(byte[] bytes)throws IOException{var journal=DomainJournalCodec.decode(bytes);validateIdentity(journal);return journal;}
  private void validateIdentity(DomainOverlay.Journal j)throws IOException{if(!j.identity().dimension().equals(dimension)||j.identity().slot()!=index)throw new IOException("DOMAIN_SLOT_IDENTITY");}
  private void reconcile()throws IOException{
   // Diagnostic preparation is metadata only; finish it only under its exact predecessor.
   byte[] pendingDiagnostic=optional(diagnosticPrepared);
   if(pendingDiagnostic!=null){validateDiagnosticRecord(pendingDiagnostic);var record=DomainJournalCodec.unwrap(pendingDiagnostic);byte[] canonical=optional(diagnostic);
    if(hash(canonical).equals(hash(pendingDiagnostic)))reclaim(diagnosticPrepared,hash(pendingDiagnostic));
    else replace(diagnosticPrepared,diagnostic,record.predecessor(),pendingDiagnostic);
   }
   validateDiagnostic(diagnostic);byte[] candidate=optional(prepared);if(candidate==null)return;
   var journal=validate(candidate);var record=DomainJournalCodec.unwrap(candidate);byte[] canonical=optional(path);String canonicalHash=hash(canonical),candidateHash=hash(candidate);String classification;
   if(canonicalHash.equals(candidateHash)){validate(canonical);classification="OBSOLETE_PUBLISHED_PREPARATION";}
   else if(canonicalHash.equals(record.predecessor())){transition(canonical==null?null:validate(canonical),journal);classification="ABANDONED_UNPUBLISHED_PREPARATION";}
   else throw new IOException("DOMAIN_AMBIGUOUS_PREPARATION");
   JsonObject payload=new JsonObject();payload.addProperty("kind","PREPARATION_CLASSIFICATION");payload.addProperty("dimension",dimension);payload.addProperty("slot",index);payload.addProperty("classification",classification);payload.addProperty("candidateSha256",candidateHash);payload.addProperty("canonicalSha256",canonicalHash);payload.addProperty("candidatePredecessorSha256",record.predecessor());
   String predecessor=hash(optional(diagnostic));byte[] receipt=DomainJournalCodec.wrap(payload,predecessor);
   forceNew(diagnosticPrepared,receipt);replace(diagnosticPrepared,diagnostic,predecessor,receipt);
   // Persist classification first; unpublished candidates never become restoration authority.
   reclaim(prepared,candidateHash);
  }
  private void validateDiagnostic(Path file)throws IOException{byte[] bytes=optional(file);if(bytes!=null)validateDiagnosticRecord(bytes);}
  private void validateDiagnosticRecord(byte[] bytes)throws IOException{validateDiagnosticBytes(bytes);JsonObject p=DomainJournalCodec.unwrap(bytes).payload();if(!DomainJournalCodec.string(p,"dimension").equals(dimension)||DomainJournalCodec.number(p,"slot")!=index)throw new IOException("DOMAIN_DIAGNOSTIC_IDENTITY");}
 }
 private void check()throws IOException{
  if(closed||failed||Thread.currentThread()!=writer||!lock.isValid())throw new IOException("DOMAIN_REPOSITORY_UNAVAILABLE");
  safeAncestors(world);safeAncestors(root);if(!Arrays.equals(read(root.resolve("owner.txt")),MARKER))throw new IOException("DOMAIN_ROOT_OWNER");
  regular(root.resolve("writer.lock"));if(!Objects.equals(worldKey,fileKey(world))||!Objects.equals(rootKey,fileKey(root))||!Objects.equals(lockKey,fileKey(root.resolve("writer.lock"))))throw new IOException("DOMAIN_ROOT_REPLACED");
 }
 private static void transition(DomainOverlay.Journal previous,DomainOverlay.Journal next)throws IOException{
  if(previous==null||previous.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL){
   if(next.phase()!=DomainOverlay.Phase.PLACING||next.cursor()!=0||next.entries().stream().anyMatch(e->e.status()!=DomainOverlay.Status.RESERVED))throw new IOException("DOMAIN_START_RECORD");
   if(previous!=null&&(previous.identity().slot()!=next.identity().slot()||!previous.identity().dimension().equals(next.identity().dimension())||next.identity().generation()<=previous.identity().generation()))throw new IOException("DOMAIN_SLOT_GENERATION");return;
  }
  if(!previous.identity().equals(next.identity())||previous.entries().size()!=next.entries().size())throw new IOException("DOMAIN_LEDGER_IDENTITY_CHANGED");
  for(int i=0;i<previous.entries().size();i++){var a=previous.entries().get(i);var b=next.entries().get(i);if(!a.cell().equals(b.cell())||!a.original().equals(b.original())||!a.overlay().equals(b.overlay()))throw new IOException("DOMAIN_ORIGINAL_LEDGER_CHANGED");}
  boolean allowed=switch(previous.phase()){
   case PLACING->next.phase()!=DomainOverlay.Phase.VERIFIED_TERMINAL;
   case ACTIVE->next.phase()==DomainOverlay.Phase.RESTORING;
   case RESTORING->next.phase()==DomainOverlay.Phase.RESTORING||next.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL;
   case VERIFIED_TERMINAL->false;
  };if(!allowed)throw new IOException("DOMAIN_PHASE_TRANSITION");
 }
 private static void validateDiagnostic(Path path)throws IOException{byte[] bytes=optional(path);if(bytes!=null)validateDiagnosticBytes(bytes);}
 private static void validateDiagnosticBytes(byte[] bytes)throws IOException{
  JsonObject p=DomainJournalCodec.unwrap(bytes).payload();DomainJournalCodec.keys(p,"kind","dimension","slot","classification","candidateSha256","canonicalSha256","candidatePredecessorSha256");
  if(!DomainJournalCodec.string(p,"dimension").matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||DomainJournalCodec.number(p,"slot")<0||DomainJournalCodec.number(p,"slot")>=4)throw new IOException("DOMAIN_DIAGNOSTIC_IDENTITY");
  if(!DomainJournalCodec.string(p,"kind").equals("PREPARATION_CLASSIFICATION")||!Set.of("OBSOLETE_PUBLISHED_PREPARATION","ABANDONED_UNPUBLISHED_PREPARATION").contains(DomainJournalCodec.string(p,"classification")))throw new IOException("DOMAIN_DIAGNOSTIC_KIND");
  for(String key:List.of("candidateSha256","canonicalSha256","candidatePredecessorSha256")){String hash=DomainJournalCodec.string(p,key);if(!hash.matches("[a-f0-9]{64}")&&!hash.equals(DomainJournalCodec.ABSENT))throw new IOException("DOMAIN_DIAGNOSTIC_HASH");}
 }
 private static void replace(Path prepared,Path target,String predecessor,byte[] bytes)throws IOException{
  if(!hash(optional(target)).equals(predecessor)||!Arrays.equals(read(prepared),bytes))throw new IOException("DOMAIN_ATOMIC_PREDECESSOR_CHANGED");
  safeAncestors(target.getParent());Files.move(prepared,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  if(!Arrays.equals(read(target),bytes))throw new IOException("DOMAIN_ATOMIC_READBACK_CHANGED");
 }
 private static void reclaim(Path path,String expected)throws IOException{if(!hash(read(path)).equals(expected))throw new IOException("DOMAIN_RECLAIM_CHANGED");safeAncestors(path.getParent());Files.delete(path);}
 private static String hash(byte[] bytes){return bytes==null?DomainJournalCodec.ABSENT:DomainJournalCodec.sha(bytes);}
 private static byte[] optional(Path path)throws IOException{return Files.exists(path,NOFOLLOW_LINKS)?read(path):null;}
 private static byte[] read(Path path)throws IOException{
  safeAncestors(path.getParent());regular(path);long size=Files.size(path);if(size>DomainJournalCodec.MAX_BYTES)throw new IOException("DOMAIN_RECORD_SIZE_LIMIT");
  Object key=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class,NOFOLLOW_LINKS).fileKey();
  try(FileChannel channel=FileChannel.open(path,READ,NOFOLLOW_LINKS)){ByteBuffer buffer=ByteBuffer.allocate((int)size+1);while(buffer.hasRemaining()&&channel.read(buffer)!=-1){}if(buffer.position()!=size)throw new IOException("DOMAIN_RECORD_CHANGED");
   if(!Objects.equals(key,Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class,NOFOLLOW_LINKS).fileKey()))throw new IOException("DOMAIN_FILE_REPLACED");return Arrays.copyOf(buffer.array(),(int)size);
  }
 }
 private static void forceNew(Path path,byte[] bytes)throws IOException{
  safeAncestors(path.getParent());try(FileChannel channel=FileChannel.open(path,CREATE_NEW,WRITE,NOFOLLOW_LINKS)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
 }
 private static Path directory(Path path)throws IOException{if(!Files.exists(path,NOFOLLOW_LINKS))Files.createDirectory(path);safeAncestors(path);return path;}
 private static void regular(Path path)throws IOException{if(!Files.isRegularFile(path,NOFOLLOW_LINKS)||Files.isSymbolicLink(path))throw new IOException("DOMAIN_NONREGULAR_FILE");}
 private static Object fileKey(Path path)throws IOException{return Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class,NOFOLLOW_LINKS).fileKey();}
 private static void safeAncestors(Path path)throws IOException{for(Path p=path;p!=null;p=p.getParent())if(!Files.isDirectory(p,NOFOLLOW_LINKS)||Files.isSymbolicLink(p))throw new IOException("DOMAIN_UNSAFE_DIRECTORY");}
 @Override public void close()throws IOException{if(closed)return;closed=true;try{lock.release();}finally{channel.close();}}
}

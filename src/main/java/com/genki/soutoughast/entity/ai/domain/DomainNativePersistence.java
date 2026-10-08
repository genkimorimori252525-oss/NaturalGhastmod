package com.genki.soutoughast.entity.ai.domain;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkDataEvent;

/** The existing native chunk worker only. No other RegionFile opener, reflection or dirty-flag reset. */
public final class DomainNativePersistence implements DomainPersistenceBarrier.Driver {
 private static final Codec<PalettedContainer<BlockState>> STATES=PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY,BlockState.CODEC,PalettedContainer.Strategy.SECTION_STATES,Blocks.AIR.defaultBlockState());
 private final ServerLevel level;private final IOWorker worker;private final Map<DomainGeometry.Cell,BlockState> originals;
 private record Snapshot(DomainPersistenceBarrier.Chunk chunk,CompoundTag tag,int bytes) implements DomainPersistenceBarrier.Snapshot {}
 private DomainNativePersistence(ServerLevel level,DomainOverlay.Journal journal)throws IOException{
  this.level=Objects.requireNonNull(level);thread();
  if(!(level.getChunkSource().chunkMap.chunkScanner() instanceof IOWorker nativeWorker))throw new IOException("DOMAIN_NATIVE_WORKER_UNSUPPORTED");worker=nativeWorker;
  Map<String,BlockState> decoded=new HashMap<>();Map<DomainGeometry.Cell,BlockState> cells=new HashMap<>();
  for(var e:journal.entries())if(e.mutationIntent()){BlockState state=decoded.get(e.original());if(state==null){state=DomainStateCodec.decode(e.original());decoded.put(e.original(),state);}cells.put(e.cell(),state);}
  originals=Map.copyOf(cells);
 }
 public static DomainPersistenceBarrier barrier(ServerLevel level,DomainOverlay.Journal journal)throws IOException{
  return new DomainPersistenceBarrier(journal,new DomainNativePersistence(level,journal),level::getGameTime,System::nanoTime);
 }
 private void thread()throws IOException{if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level)throw new IOException("DOMAIN_PERSISTENCE_SERVER_THREAD_OR_LEVEL");}
 @Override public List<DomainPersistenceBarrier.Chunk> chunks(DomainOverlay.Journal journal)throws IOException{
  thread();if(!level.dimension().location().toString().equals(journal.identity().dimension()))throw new IOException("DOMAIN_PERSISTENCE_DIMENSION");
  return journal.entries().stream().filter(DomainOverlay.Entry::mutationIntent).map(e->new DomainPersistenceBarrier.Chunk(Math.floorDiv(e.cell().x(),16),Math.floorDiv(e.cell().z(),16))).distinct().sorted(Comparator.comparingInt(DomainPersistenceBarrier.Chunk::x).thenComparingInt(DomainPersistenceBarrier.Chunk::z)).toList();
 }
 @Override public boolean originalsCurrent(DomainOverlay.Journal journal)throws IOException{
  thread();for(var e:journal.entries())if(e.mutationIntent()){
   BlockPos p=new BlockPos(e.cell().x(),e.cell().y(),e.cell().z());
   if(!level.hasChunkAt(p)||!level.getBlockState(p).equals(originals.get(e.cell())))return false;
  }return true;
 }
 @Override public DomainPersistenceBarrier.Snapshot capture(DomainPersistenceBarrier.Chunk position,DomainOverlay.Journal journal)throws IOException{
  thread();LevelChunk chunk=level.getChunkSource().getChunkNow(position.x(),position.z());if(chunk==null)throw new IOException("DOMAIN_PERSISTENCE_UNLOADED");
  CompoundTag tag=ChunkSerializer.write(level,chunk);
  MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk,level,tag));
  if(!matches(position,journal,tag))throw new IOException("DOMAIN_PERSISTENCE_SNAPSHOT_CHANGED");
  final int[] size={0};OutputStream bounded=new OutputStream(){
   @Override public void write(int value)throws IOException{add(1);}
   @Override public void write(byte[] bytes,int offset,int length)throws IOException{add(length);}
   private void add(int length)throws IOException{if(length<0||size[0]>DomainPersistenceBarrier.MAX_CHUNK_BYTES-length)throw new IOException("DOMAIN_PERSISTENCE_SERIALIZED_LIMIT");size[0]+=length;}
  };
  try(var output=new DataOutputStream(bounded)){NbtIo.write(tag,output);}
  // Save listeners may retain their event tag. The queued snapshot owns an independent copy.
  return new Snapshot(position,tag.copy(),size[0]);
 }
 @Override public CompletableFuture<Void> store(DomainPersistenceBarrier.Chunk position,DomainPersistenceBarrier.Snapshot snapshot)throws IOException{
  thread();if(!(snapshot instanceof Snapshot owned)||!owned.chunk().equals(position))throw new IOException("DOMAIN_PERSISTENCE_SNAPSHOT_OWNER");
  // Capture and enqueue happen on the same server thread turn. Normal saves serialize current
  // chunks on that thread too; neither this path nor normal ChunkMap queues an old delayed tag.
  return worker.store(new ChunkPos(position.x(),position.z()),owned.tag());
 }
 @Override public CompletableFuture<Void> flush()throws IOException{thread();return worker.synchronize(true);}
 @Override public CompletableFuture<Boolean> read(DomainPersistenceBarrier.Chunk position,DomainOverlay.Journal journal)throws IOException{
  thread();return worker.loadAsync(new ChunkPos(position.x(),position.z())).thenApply(tag->{
   try{return tag.isPresent()&&matches(position,journal,tag.get());}catch(IOException error){throw new java.util.concurrent.CompletionException(error);}
  });
 }
 private boolean matches(DomainPersistenceBarrier.Chunk position,DomainOverlay.Journal journal,CompoundTag tag)throws IOException{
  if(!tag.contains("xPos",Tag.TAG_INT)||!tag.contains("zPos",Tag.TAG_INT)||tag.getInt("xPos")!=position.x()||tag.getInt("zPos")!=position.z()||!tag.contains("sections",Tag.TAG_LIST))return false;
  ListTag sections=tag.getList("sections",Tag.TAG_COMPOUND);if(sections.size()>256)throw new IOException("DOMAIN_PERSISTENCE_SECTION_LIMIT");
  Map<Integer,CompoundTag> byY=new HashMap<>();for(int i=0;i<sections.size();i++){CompoundTag section=sections.getCompound(i);if(!section.contains("Y",Tag.TAG_BYTE)||byY.put((int)section.getByte("Y"),section)!=null)throw new IOException("DOMAIN_PERSISTENCE_SECTION_IDENTITY");}
  Map<Integer,PalettedContainer<BlockState>> decoded=new HashMap<>();
  for(var e:journal.entries())if(e.mutationIntent()&&Math.floorDiv(e.cell().x(),16)==position.x()&&Math.floorDiv(e.cell().z(),16)==position.z()){
   int y=Math.floorDiv(e.cell().y(),16);CompoundTag section=byY.get(y);if(section==null||!section.contains("block_states",Tag.TAG_COMPOUND))return false;
   PalettedContainer<BlockState> states=decoded.get(y);if(states==null){
    CompoundTag blockStates=section.getCompound("block_states");ListTag palette=blockStates.getList("palette",Tag.TAG_COMPOUND);
    if(palette.isEmpty()||palette.size()>4096)throw new IOException("DOMAIN_PERSISTENCE_PALETTE_LIMIT");
    for(int i=0;i<palette.size();i++)DomainStateCodec.decode(DomainJournalCodec.canonical(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE,palette.getCompound(i))));
    states=STATES.parse(NbtOps.INSTANCE,blockStates).result().orElseThrow(()->new IOException("DOMAIN_PERSISTENCE_PALETTE_INVALID"));decoded.put(y,states);
   }
   if(!states.get(Math.floorMod(e.cell().x(),16),Math.floorMod(e.cell().y(),16),Math.floorMod(e.cell().z(),16)).equals(originals.get(e.cell())))return false;
  }return true;
 }
}

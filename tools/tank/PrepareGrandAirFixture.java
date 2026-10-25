package com.genki.soutoughast.tank;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
/** Offline initial-air-space case, never a runtime teleport or edit of an original/finalized world. */
public final class PrepareGrandAirFixture {
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("NEW_AIR_FIXTURE_REQUIRED");
  Path root=Path.of(args[0]).toRealPath(),allowed=Path.of(args[1]).toRealPath();
  if(root.equals(allowed)||!root.startsWith(allowed)||!root.getFileName().toString().startsWith("naturalghast-grand-air-")||Files.exists(root.resolve("air-preparation.json")))throw new IllegalStateException("NEW_PRIVATE_AIR_COPY_ONLY");
  Path world=root.resolve("game/saves/KNEEKURA_DEBUG_WORLD"),entities=world.resolve("entities");
  try(var channel=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()){
   if(lock==null)throw new IllegalStateException("WORLD_RUNNING");
   try(var region=new RegionFile(entities.resolve("r.0.0.mca"),entities,true)){
    var pos=new ChunkPos(1,2);CompoundTag chunk;try(var input=region.getChunkDataInputStream(pos)){if(input==null)throw new IllegalStateException("EXPECTED_SUBJECT_CHUNK");chunk=NbtIo.read(input);}
    var rows=chunk.getList("Entities",Tag.TAG_COMPOUND);int found=0;
    for(var raw:rows){var entity=(CompoundTag)raw;if(!entity.hasUUID("UUID")||!entity.getUUID("UUID").equals(UUID.fromString("67676767-1007-4000-8000-000000000001")))continue;
     var p=entity.getList("Pos",Tag.TAG_DOUBLE);if(p.getDouble(0)!=26||p.getDouble(1)!=228||p.getDouble(2)!=32||entity.getBoolean("NoAI")||entity.getFloat("Health")!=100)throw new IllegalStateException("EXPECTED_UNRUN_SUBJECT");
     p.set(1,DoubleTag.valueOf(234));found++;
    }
    if(found!=1)throw new IllegalStateException("UNIQUE_SUBJECT_REQUIRED");try(var output=region.getChunkDataOutputStream(pos)){NbtIo.write(chunk,output);}region.flush();
   }
   Files.writeString(root.resolve("air-preparation.json"),"{\"initialBossY\":234,\"previousInitialBossY\":228,\"runtimeMutation\":false,\"geometryAndPlayerUnchanged\":true,\"purpose\":\"Separate air-clearance eligibility case, not RNG forcing\"}\n",StandardOpenOption.CREATE_NEW);
  }
 }
}

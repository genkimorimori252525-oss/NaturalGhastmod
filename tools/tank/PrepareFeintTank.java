package com.github.tartaricacid.touhoulittlemaid.sim.debug;

import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Fresh private fixture only: omit the newly-added combat subject for controlled composer actors. */
public final class PrepareFeintTank {
 public static void main(String[] args)throws Exception{
  if(args.length!=3||!args[2].equals("DOMAIN_RELIABILITY"))throw new IllegalArgumentException("FEINT_PRIVATE_FIXTURE_REQUIRED");
  PrepareFlightTank.main(args);
  Path root=Path.of(args[0]).toRealPath(),allowed=Path.of(args[1]).toRealPath();
  if(!root.startsWith(allowed)||root.equals(allowed))throw new IllegalStateException("FRESH_PRIVATE_ROOT_REQUIRED");
  Path world=root.resolve("game/saves/KNEEKURA_DEBUG_WORLD").toRealPath();if(!world.startsWith(root))throw new IllegalStateException("PRIVATE_WORLD_REQUIRED");
  try(FileChannel channel=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()){
   if(lock==null)throw new IllegalStateException("WORLD_RUNNING");Path entities=world.resolve("entities");
   try(RegionFile region=new RegionFile(entities.resolve("r.0.0.mca"),entities,true)){
    ChunkPos position=new ChunkPos(1,2);CompoundTag chunk;
    try(var input=region.getChunkDataInputStream(position)){if(input==null)throw new IllegalStateException("NEW_FIXTURE_SUBJECT_CHUNK_MISSING");chunk=NbtIo.read(input);}
    ListTag list=chunk.getList("Entities",Tag.TAG_COMPOUND);int count=0;
    for(int i=list.size()-1;i>=0;i--){var entity=list.getCompound(i);if(entity.hasUUID("UUID")&&entity.getUUID("UUID").equals(PrepareFlightTank.SUBJECT)){
     if(!entity.getString("id").equals("soutou_ghast:soutou_ghast"))throw new IllegalStateException("SUBJECT_TYPE_MISMATCH");list.remove(i);count++;
    }}
    if(count!=1)throw new IllegalStateException("EXACT_NEW_FIXTURE_SUBJECT_REQUIRED");
    try(var output=region.getChunkDataOutputStream(position)){NbtIo.write(chunk,output);}region.flush();
   }
   Path file=root.resolve("fixture.json");var fixture=JsonParser.parseString(Files.readString(file)).getAsJsonObject();fixture.addProperty("variant","FEINT_RELIABILITY");
   fixture.addProperty("newFixtureCombatSubjectOmitted",true);fixture.addProperty("changes","FRESH_PRIVATE_ONLY: unchanged52x24x52 room; omit newly generated fixture combat boss, retain genuine survival Player, create explicit probe-only composer actors after authenticated dispatch. Not natural AI selection.");
   Files.writeString(file,fixture+"\n");
  }
 }
}

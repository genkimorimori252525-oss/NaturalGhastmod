package com.github.tartaricacid.touhoulittlemaid.sim.debug;

import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import java.io.IOException;
import java.nio.file.*;

/** Optional saved seed entities are not implicit Tank subjects. Caller owns a fresh save lock. */
public final class TankSeedEntities {
    private TankSeedEntities() { }
    public static int removeSeedReimu(ListTag rows) {
        int removed=0;
        for(int i=rows.size()-1;i>=0;i--){
            if(rows.getCompound(i).getString("id").equals("touhou_little_maid:reimu")){
                rows.remove(i);removed++;
            }
        }
        return removed;
    }
    /** At most 25 chunks around the newly prepared room; unchanged chunks are never rewritten. */
    public static int removeSeedReimu(Path world,int maxChunk) throws IOException {
        if(maxChunk<0||maxChunk>3)throw new IllegalArgumentException("FIXTURE_ENTITY_SCOPE_EXCEEDED");
        Path entities=world.resolve("entities");int removed=0;
        for(int x=-1;x<=maxChunk;x++)for(int z=-1;z<=maxChunk;z++){
            Path file=entities.resolve("r."+Math.floorDiv(x,32)+"."+Math.floorDiv(z,32)+".mca");
            if(!Files.exists(file))continue;
            if(Files.isSymbolicLink(file))throw new IOException("FIXTURE_ENTITY_SYMLINK_REJECTED");
            try(RegionFile region=new RegionFile(file,entities,true)){
                ChunkPos pos=new ChunkPos(x,z);CompoundTag chunk;
                try(var input=region.getChunkDataInputStream(pos)){if(input==null)continue;chunk=NbtIo.read(input);}
                int count=removeSeedReimu(chunk.getList("Entities",Tag.TAG_COMPOUND));
                if(count==0)continue;
                try(var output=region.getChunkDataOutputStream(pos)){NbtIo.write(chunk,output);}
                region.flush();removed+=count;
            }
        }
        return removed;
    }
}

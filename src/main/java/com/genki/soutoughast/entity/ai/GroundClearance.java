package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.GroundFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/** Loaded-only conservative full-body routes and full-top integer floor support. */
public final class GroundClearance {
    private final SoutouGhast ghast;
    public GroundClearance(SoutouGhast ghast){this.ghast=ghast;}
    public FlightVector floor(FlightVector position){
        for(int drop=0;drop<=8;drop++){
            var candidate=new FlightVector(position.x(),Math.floor(position.y())-drop,position.z());
            if(position.y()-candidate.y()>8)continue;
            if(support(candidate,candidate)&&body(position,candidate))return candidate;
        }
        return null;
    }
    public boolean supportedRoute(FlightVector from,FlightVector to){return support(from,to)&&body(from,to);}
    public boolean support(FlightVector from,FlightVector to){
        return GroundFootprint.supported(from,to,(x,y,z)->{
            BlockPos pos=new BlockPos(x,y,z);
            if(y<ghast.level().getMinBuildHeight()||y>=ghast.level().getMaxBuildHeight()||!ghast.level().hasChunkAt(pos))return false;
            var state=ghast.level().getBlockState(pos);
            return state.getFluidState().isEmpty()&&Block.isFaceFull(state.getCollisionShape(ghast.level(),pos),Direction.UP);
        });
    }
    public boolean body(FlightVector from,FlightVector to){
        if(from.subtract(to).length()>12.001)return false;
        AABB start=new AABB(from.x()-2,from.y(),from.z()-2,from.x()+2,from.y()+4,from.z()+2);
        AABB sweep=start.expandTowards(to.x()-from.x(),to.y()-from.y(),to.z()-from.z());
        return loaded(sweep)&&ghast.level().noCollision(ghast,sweep);
    }
    public boolean muzzle(net.minecraft.world.phys.Vec3 eye,net.minecraft.world.phys.Vec3 muzzle){
        AABB sweep=new AABB(eye.x-.5,eye.y,eye.z-.5,eye.x+.5,eye.y+1,eye.z+.5)
                .expandTowards(muzzle.subtract(eye));
        return loaded(sweep);
    }
    private boolean loaded(AABB sweep){
        if(sweep.minX< -29999984||sweep.maxX>29999984||sweep.minZ< -29999984||sweep.maxZ>29999984)return false;
        if(sweep.minY<ghast.level().getMinBuildHeight()||sweep.maxY>ghast.level().getMaxBuildHeight())return false;
        // noCollision may look up adjacent shape blocks; check a one-block loaded margin first.
        for(int x=Math.floorDiv((int)Math.floor(sweep.minX)-1,16);x<=Math.floorDiv((int)Math.floor(sweep.maxX)+1,16);x++)
            for(int z=Math.floorDiv((int)Math.floor(sweep.minZ)-1,16);z<=Math.floorDiv((int)Math.floor(sweep.maxZ)+1,16);z++)
                if(!ghast.level().hasChunk(x,z))return false;
        return true;
    }
}

package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.TrajectoryValidation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.ArrayList;
import java.util.List;

/** Loaded conservative swept-body preflight. Native impact still uses its own center ray. */
public final class CommittedPathClearance {
    public record Terminal(BlockPos position,BlockState state) {}
    public record Proof(TrajectoryValidation.Result result,List<Terminal> terminal) {}
    public static AABB sweptBox(FlightVector from,FlightVector to){
        return new AABB(from.x()-.5,from.y(),from.z()-.5,from.x()+.5,from.y()+1,from.z()+.5)
                .expandTowards(to.x()-from.x(),to.y()-from.y(),to.z()-from.z());
    }
    public static List<BlockPos> terminalFootprint(AABB sweep,int surfaceY){
        var cells=new ArrayList<BlockPos>();
        for(int x=Mth.floor(sweep.minX+1e-7);x<=Mth.floor(sweep.maxX-1e-7);x++)
            for(int z=Mth.floor(sweep.minZ+1e-7);z<=Mth.floor(sweep.maxZ-1e-7);z++){
                if(cells.size()==4)return List.of();cells.add(new BlockPos(x,surfaceY,z));
            }
        return List.copyOf(cells);
    }
    public static Proof validate(Level level,Entity projectile,CommittedTrajectory path){
        var declared=new ArrayList<Terminal>();
        var result=TrajectoryValidation.verify(path,(i,from,to,last)->{
            AABB sweep=sweptBox(from,to);
            // Margin includes shape neighbors queried by vanilla collision shapes; check before any lookup.
            for(int cx=Mth.floor(sweep.minX-2)>>4;cx<=(Mth.floor(sweep.maxX+2)>>4);cx++)
                for(int cz=Mth.floor(sweep.minZ-2)>>4;cz<=(Mth.floor(sweep.maxZ+2)>>4);cz++)
                    if(!level.hasChunk(cx,cz))return TrajectoryValidation.Segment.UNLOADED;
            if(sweep.minY<level.getMinBuildHeight()||sweep.maxY>=level.getMaxBuildHeight())return TrajectoryValidation.Segment.BLOCKED;
            if(last&&path.kind().terminalImpact()){
                var positions=terminalFootprint(sweep,Mth.floor(to.y()));if(positions.isEmpty())return TrajectoryValidation.Segment.BLOCKED;
                for(var pos:positions){var state=level.getBlockState(pos);if(!state.getCollisionShape(level,pos,CollisionContext.of(projectile)).isEmpty())declared.add(new Terminal(pos,state));}
                var hit=level.clip(new ClipContext(vec(from),vec(to),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,projectile));
                if(hit.getType()!=HitResult.Type.BLOCK||declared.stream().noneMatch(t->t.position().equals(hit.getBlockPos())))return TrajectoryValidation.Segment.BLOCKED;
            }
            var shape=Shapes.create(sweep);
            for(var pos:BlockPos.betweenClosed(Mth.floor(sweep.minX)-1,Mth.floor(sweep.minY)-1,Mth.floor(sweep.minZ)-1,
                    Mth.floor(sweep.maxX)+1,Mth.floor(sweep.maxY)+1,Mth.floor(sweep.maxZ)+1)){
                var state=level.getBlockState(pos);var collider=state.getCollisionShape(level,pos,CollisionContext.of(projectile)).move(pos.getX(),pos.getY(),pos.getZ());
                if(!collider.isEmpty()&&Shapes.joinIsNotEmpty(shape,collider,BooleanOp.AND)){
                    if(!last||declared.stream().noneMatch(t->t.position().equals(pos)&&t.state()==state))return TrajectoryValidation.Segment.BLOCKED;
                }
            }
            return last&&path.kind().terminalImpact()?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR;
        });
        return new Proof(result,List.copyOf(declared));
    }
    private static Vec3 vec(FlightVector v){return new Vec3(v.x(),v.y(),v.z());}
    private CommittedPathClearance() {}
}

package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public final class CommittedProjectileTest {
    private static int checks;
    public static void main(String[] args){
        var path=CommittedTrajectory.lob(new FlightVector(9.5,232,20),new FlightVector(9.5,223.75,3.5),10);
        var flight=new CommittedTrajectory.Flight(path);for(int i=0;i<8;i++)flight.next();
        CompoundTag tag=CommittedTrajectoryCodec.write(flight);var restored=CommittedTrajectoryCodec.read(tag);
        check(restored.index()==8&&restored.path().equals(path)&&!restored.normalized(),"durable path and clock");
        restored.normalize();check(CommittedTrajectoryCodec.read(CommittedTrajectoryCodec.write(restored)).normalized(),"reload keeps normalization");
        var invalid=tag.copy();invalid.putInt("Index",10000);reject(invalid);
        invalid=tag.copy();invalid.putString("Kind","HOMING");reject(invalid);
        invalid=tag.copy();invalid.getList("Points",10).getCompound(1).putDouble("X",Double.NaN);reject(invalid);
        invalid=tag.copy();invalid.getList("Points",10).getCompound(1).remove("Z");reject(invalid);
        var sweep=CommittedPathClearance.sweptBox(new FlightVector(9.5,224.7,4.1),new FlightVector(9.5,223.75,3.5));
        check(sweep.getXsize()==1&&sweep.getYsize()>1&&sweep.getZsize()>1,"one-body swept clearance separate from pick radius");
        var terminal=CommittedPathClearance.terminalFootprint(sweep,223);
        check(terminal.size()<=4&&terminal.stream().allMatch(p->p.getY()==223),"bounded terminal surface set");
        check(CommittedPathClearance.terminalFootprint(new AABB(0,223,0,3,225,3),223).isEmpty(),"oversized terminal set rejected");
        check(StandardSoutouFireball.class.isAssignableFrom(CommittedSoutouFireball.class),"shared attributed damage and melee policy");
        System.out.println("PASS: "+checks+" mapped committed codec/clearance checks (no world/input acceptance)");
    }
    private static void reject(CompoundTag tag){try{CommittedTrajectoryCodec.read(tag);throw new AssertionError("malformed trajectory accepted");}catch(IllegalArgumentException expected){checks++;}}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

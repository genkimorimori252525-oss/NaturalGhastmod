package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;

/** Bounded path state for save/spawn, not a per-tick full-path channel. */
public final class CommittedTrajectoryCodec {
    public static CompoundTag write(CommittedTrajectory.Flight flight){
        CompoundTag tag=new CompoundTag();tag.putString("Kind",flight.path().kind().name());
        tag.putInt("Index",flight.index());tag.putBoolean("Normalized",flight.normalized());
        ListTag points=new ListTag();for(var point:flight.path().points()){
            CompoundTag row=new CompoundTag();row.putDouble("X",point.x());row.putDouble("Y",point.y());row.putDouble("Z",point.z());points.add(row);
        }
        tag.put("Points",points);return tag;
    }
    public static CommittedTrajectory.Flight read(CompoundTag tag){
        if(!tag.contains("Kind",Tag.TAG_STRING)||!tag.contains("Index",Tag.TAG_INT)||!tag.contains("Normalized",Tag.TAG_BYTE)||!tag.contains("Points",Tag.TAG_LIST))throw new IllegalArgumentException("COMMITTED_DATA_MISSING");
        var rows=tag.getList("Points",Tag.TAG_COMPOUND);if(rows.size()<2||rows.size()>97)throw new IllegalArgumentException("COMMITTED_POINT_LIMIT");
        var points=new ArrayList<FlightVector>();for(Tag raw:rows){
            CompoundTag row=(CompoundTag)raw;for(String key:new String[]{"X","Y","Z"})if(!row.contains(key,Tag.TAG_DOUBLE))throw new IllegalArgumentException("COMMITTED_COORDINATE_MISSING");
            points.add(new FlightVector(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")));
        }
        var path=new CommittedTrajectory(CommittedTrajectory.Kind.valueOf(tag.getString("Kind")),points);
        return new CommittedTrajectory.Flight(path,tag.getInt("Index"),tag.getBoolean("Normalized"));
    }
    private CommittedTrajectoryCodec() {}
}

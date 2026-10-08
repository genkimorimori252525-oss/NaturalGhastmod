package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Profiles share Standard impact/deflection; a missed finite path never becomes an airburst. */
public final class CommittedSoutouFireball extends StandardSoutouFireball {
    private static final EntityDataAccessor<CompoundTag> PROFILE=SynchedEntityData.defineId(CommittedSoutouFireball.class,EntityDataSerializers.COMPOUND_TAG);
    private CommittedTrajectory.Flight flight;
    private boolean invalid;
    private CompoundTag preflight=new CompoundTag();
    public CommittedSoutouFireball(EntityType<? extends CommittedSoutouFireball> type,Level level){super(type,level);}
    public CommittedSoutouFireball(SoutouGhast owner,CommittedTrajectory path){
        super(ModEntities.PROFILE_FIREBALL.get(),owner,vec(path.points().get(0)),vec(path.velocity(0)));
        flight=new CommittedTrajectory.Flight(path);setDeltaMovement(vec(path.velocity(0)));xPower=0;yPower=0;zPower=0;refreshFlightSync();publishProfile();
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PROFILE,new CompoundTag());}
    public CommittedTrajectory.Flight flight(){return flight;}
    public CompoundTag preflight(){return preflight.copy();}
    public void setPreflight(CommittedPathClearance.Proof proof){
        if(!proof.result().clear()||proof.terminal().size()>4)throw new IllegalArgumentException("VERIFIED_FINITE_PREFLIGHT_REQUIRED");
        var data=new CompoundTag();data.putString("Scope","PRE_LAUNCH_LOADED_SWEPT_BODY");data.putLong("GameTime",level().getGameTime());data.putInt("Segments",proof.result().segments());
        var cells=new net.minecraft.nbt.ListTag();for(var terminal:proof.terminal()){
            var row=new CompoundTag();row.putInt("X",terminal.position().getX());row.putInt("Y",terminal.position().getY());row.putInt("Z",terminal.position().getZ());row.putString("State",terminal.state().toString());cells.add(row);
        }
        data.put("Terminal",cells);preflight=data;
    }
    public String phase(){return flight==null?"UNKNOWN":flight.normalized()?"RALLY_NORMALIZED":flight.path().phase(Math.max(0,flight.index()-1)).name();}
    @Override protected float getInertia(){return flight!=null&&!flight.normalized()?1:super.getInertia();}
    @Override public void tick(){
        if(invalid||flight==null){discard();return;}
        if(!flight.normalized()){
            var velocity=flight.next();if(velocity==null){discard();return;}
            setDeltaMovement(vec(velocity));xPower=0;yPower=0;zPower=0;
        }
        super.tick();
    }
    @Override protected void onDeflected(){if(flight!=null){flight.normalize();publishProfile();}}
    @Override protected ParticleOptions getTrailParticle(){
        if(flight==null||flight.normalized())return super.getTrailParticle();
        return switch(flight.path().kind()){
            case BURST -> phase().equals("WARNING")?ParticleTypes.END_ROD:phase().equals("SLOW")?ParticleTypes.SMALL_FLAME:ParticleTypes.FLAME;
            case CURVE -> ParticleTypes.SOUL_FIRE_FLAME;
            case LOB -> phase().equals("DESCEND")?ParticleTypes.LAVA:ParticleTypes.SMOKE;
            case BOMB -> ParticleTypes.LAVA;
        };
    }
    private void publishProfile(){
        if(flight==null)return;
        var data=CommittedTrajectoryCodec.write(flight);data.remove("Index");entityData.set(PROFILE,data);
    }
    private void applyProfile(CompoundTag data,boolean clock){
        try{
            var copy=data.copy();if(!clock)copy.putInt("Index",flight==null?0:flight.index());
            var parsed=CommittedTrajectoryCodec.read(copy);
            if(!clock&&flight!=null&&!flight.path().equals(parsed.path()))throw new IllegalArgumentException("COMMITTED_PATH_REPLACEMENT");
            flight=parsed;
        }catch(IllegalArgumentException error){
            invalid=true;
            com.mojang.logging.LogUtils.getLogger().warn("Discarding invalid committed fireball {}: {}",getUUID(),error.getMessage());
        }
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);if(PROFILE.equals(key)&&level().isClientSide)applyProfile(entityData.get(PROFILE),false);
    }
    @Override public void writeSpawnData(FriendlyByteBuf buffer){
        super.writeSpawnData(buffer);
        // Tracking can request a spawn packet before the first discard tick after an invalid reload.
        buffer.writeNbt(invalid||flight==null?null:CommittedTrajectoryCodec.write(flight));
    }
    @Override public void readSpawnData(FriendlyByteBuf buffer){super.readSpawnData(buffer);var data=buffer.readNbt();if(data==null)invalid=true;else applyProfile(data,true);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(flight!=null)tag.put("CommittedProfile",CommittedTrajectoryCodec.write(flight));tag.put("ProfilePreflight",preflight.copy());}
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);applyProfile(tag.getCompound("CommittedProfile"),true);
        preflight=tag.getCompound("ProfilePreflight").copy();
        if(flight!=null&&!flight.normalized()&&(provenance().playerDeflected()||bossReturns()>0))invalid=true;
        if(flight!=null&&!flight.normalized()){
            var expected=vec(flight.path().points().get(flight.index()));
            if(position().distanceToSqr(expected)>1e-8)invalid=true;
        }
        if(!invalid)publishProfile();
    }
    private static Vec3 vec(com.genki.soutoughast.entity.ai.flight.FlightVector v){return new Vec3(v.x(),v.y(),v.z());}
}

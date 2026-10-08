package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Dedicated baseline; deliberately outside vanilla LargeFireball's1000-damage branch. */
public class StandardSoutouFireball extends Fireball implements IEntityAdditionalSpawnData {
    public static final float VISUAL_SCALE=1.5F, PICK_RADIUS=1.5F, EXPLOSION_RADIUS=1.5F;
    public static final float DIRECT_DAMAGE=9, RETURN_DAMAGE=20;
    private static final EntityDataAccessor<CompoundTag> FLIGHT=SynchedEntityData.defineId(StandardSoutouFireball.class,EntityDataSerializers.COMPOUND_TAG);
    private UUID origin,deflector,directVictim;
    private boolean playerDeflected,bossAttempted;
    private int returns,age;
    private final ClientProjectileOwner<Entity> clientOwner=new ClientProjectileOwner<>();

    public StandardSoutouFireball(EntityType<? extends StandardSoutouFireball> type,Level level){super(type,level);}
    public StandardSoutouFireball(SoutouGhast owner,Vec3 position,Vec3 direction){
        this(ModEntities.STANDARD_FIREBALL.get(),owner,position,direction);
    }
    protected StandardSoutouFireball(EntityType<? extends StandardSoutouFireball> type,SoutouGhast owner,Vec3 position,Vec3 direction){
        this(type,owner.level());
        origin=owner.getUUID();setPos(position);redirect(owner,direction,.5);
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(FLIGHT,new CompoundTag());}
    @Override public float getPickRadius(){return PICK_RADIUS;}
    @Override public Entity getOwner(){
        if(!level().isClientSide)return super.getOwner();
        if(clientOwner==null)return null; // Superclass construction may call virtual methods.
        if(isRemoved()){clientOwner.clear();return null;}
        return clientOwner.resolve(level().getGameTime(),level()::getEntity,
                owner->!owner.isRemoved()&&owner.level()==level()&&owner.getId()==clientOwner.id());
    }
    @Override public void onRemovedFromWorld(){
        if(level().isClientSide)clientOwner.clear();
        super.onRemovedFromWorld();
    }
    public boolean isPlayerDeflected(){return playerDeflected&&getOwner() instanceof Player p&&p.getUUID().equals(deflector);}
    public boolean isOwnReturn(Entity boss){return getOwner() instanceof Player p&&provenance().matchesReturn(boss.getUUID(),p.getUUID());}
    public boolean isAttributedOwnReturn(DamageSource source,Entity boss){return source.getDirectEntity()==this&&source.getEntity()==getOwner()&&isOwnReturn(boss);}
    public boolean canBossReact(Entity boss){return isOwnReturn(boss)&&returns==0&&!bossAttempted;}
    public void markBossAttempt(){bossAttempted=true;}
    public boolean wasDirectVictim(Entity entity){return entity.getUUID().equals(directVictim);}
    public int bossReturns(){return returns;}
    public UUID originUuid(){return origin;}
    public UUID deflectorUuid(){return deflector;}
    public StandardProvenance provenance(){return new StandardProvenance(origin,deflector,playerDeflected,bossAttempted,returns,age);}

    private void redirect(Entity owner,Vec3 direction,double speed){
        Vec3 unit=direction.normalize();if(unit.lengthSqr()<1e-9)return;
        setOwner(owner);setDeltaMovement(unit.scale(speed));xPower=unit.x*.1;yPower=unit.y*.1;zPower=unit.z*.1;
        hasImpulse=true;syncFlight();
    }
    public boolean returnByBoss(SoutouGhast boss,Vec3 lockedDirection){
        if(level().isClientSide||!isOwnReturn(boss)||returns!=0||lockedDirection.lengthSqr()<1e-9)return false;
        onDeflected();returns=1;bossAttempted=true;playerDeflected=false;redirect(boss,lockedDirection,1.15);return true;
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source)||amount<=0||!source.is(DamageTypes.PLAYER_ATTACK)
                ||!(source.getEntity() instanceof Player player)||source.getDirectEntity()!=player
                ||!player.isAlive()||player.isSpectator())return false;
        if(!level().isClientSide){
            onDeflected();deflector=player.getUUID();playerDeflected=true;markHurt();redirect(player,player.getLookAngle(),1.0+returns*.15);
        }
        return true;
    }
    @Override public void tick(){
        if(!level().isClientSide&&++age>400){discard();return;}
        super.tick();
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        super.onHitEntity(hit);
        if(!level().isClientSide){
            Entity victim=hit.getEntity();directVictim=victim.getUUID();
            float damage=victim instanceof SoutouGhast&&isOwnReturn(victim)?RETURN_DAMAGE:directDamage();
            victim.hurt(damageSources().fireball(this,getOwner()),damage);
        }
    }
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);
        if(!level().isClientSide){
            boolean grief=ForgeEventFactory.getMobGriefingEvent(level(),getOwner());
            level().explode(this,getX(),getY(),getZ(),explosionRadius(),grief,Level.ExplosionInteraction.MOB);
            discard();
        }
    }
    protected float directDamage(){return DIRECT_DAMAGE;}
    protected float explosionRadius(){return EXPLOSION_RADIUS;}
    private CompoundTag flightData(){
        CompoundTag data=new CompoundTag();data.putDouble("px",xPower);data.putDouble("py",yPower);data.putDouble("pz",zPower);
        Vec3 v=getDeltaMovement();data.putDouble("vx",v.x);data.putDouble("vy",v.y);data.putDouble("vz",v.z);
        data.putInt("owner",getOwner()==null?0:getOwner().getId());return data;
    }
    private void applyFlight(CompoundTag data){
        xPower=data.getDouble("px");yPower=data.getDouble("py");zPower=data.getDouble("pz");
        setDeltaMovement(data.getDouble("vx"),data.getDouble("vy"),data.getDouble("vz"));
        if(level().isClientSide){
            clientOwner.update(data.getInt("owner"));
            getOwner(); // Ordinary ticking retries if the owner has not arrived yet.
        }else{
            Entity owner=level().getEntity(data.getInt("owner"));if(owner!=null)setOwner(owner);
        }
    }
    private void syncFlight(){entityData.set(FLIGHT,flightData());}
    protected final void refreshFlightSync(){syncFlight();}
    protected void onDeflected() {}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);if(FLIGHT.equals(key)&&level().isClientSide)applyFlight(entityData.get(FLIGHT));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void writeSpawnData(FriendlyByteBuf buffer){buffer.writeNbt(flightData());}
    @Override public void readSpawnData(FriendlyByteBuf buffer){CompoundTag data=buffer.readNbt();if(data!=null)applyFlight(data);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);provenance().write(tag);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);var saved=StandardProvenance.read(tag);
        origin=saved.origin();deflector=saved.deflector();playerDeflected=saved.playerDeflected();
        returns=saved.returns();bossAttempted=saved.bossAttempted();age=saved.age();syncFlight();
    }
}

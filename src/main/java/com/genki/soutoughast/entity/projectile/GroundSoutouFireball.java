package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.entity.ModEntities;
import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lower-damage Ground single; inherited provenance/deflection/lifetime stay authoritative. */
public final class GroundSoutouFireball extends StandardSoutouFireball {
    public static final float GROUND_DAMAGE=3,GROUND_RADIUS=.5F;
    public GroundSoutouFireball(EntityType<? extends GroundSoutouFireball> type,Level level){super(type,level);}
    public GroundSoutouFireball(SoutouGhast owner,Vec3 position,Vec3 direction){
        super(ModEntities.GROUND_FIREBALL.get(),owner,position,direction);
        setDeltaMovement(direction.normalize().scale(1.9));xPower=yPower=zPower=0;refreshFlightSync();
    }
    @Override protected float directDamage(){return GROUND_DAMAGE;}
    @Override protected float explosionRadius(){return GROUND_RADIUS;}
    // Power is included in existing native NBT/spawn/synced flight data. Deflection restores
    // Standard power and inertia; no second unsynchronized or unpersisted flight-mode flag.
    @Override protected float getInertia(){return xPower*xPower+yPower*yPower+zPower*zPower<1e-12?1:super.getInertia();}
    @Override public boolean canBossReact(Entity boss){return false;}
    @Override public boolean returnByBoss(SoutouGhast boss,Vec3 direction){return false;}
}

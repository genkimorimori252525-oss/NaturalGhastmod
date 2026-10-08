package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.StandardSoutouFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;

/** Loaded visible native projectile positions only; rally owns the complete Soutou family. */
public final class SoutouGhastProjectileDodge {
    private final SoutouGhast ghast;
    private final ObservedProjectileDodge dodge=new ObservedProjectileDodge();
    public SoutouGhastProjectileDodge(SoutouGhast ghast){this.ghast=ghast;}
    public boolean active(){return dodge.active();}
    public ObservedProjectileDodge.State state(){return dodge.state();}
    public void reset(){dodge.reset();}
    public MovementPlanner.Plan tick(LivingEntity target,boolean visible,CombatAnchor.Region region,boolean movementFeint){
        var control=(SoutouGhastInertialMoveControl)ghast.getMoveControl();
        boolean eligible=target!=null&&target.isAlive()&&visible&&!movementFeint&&!ghast.getStandardAttack().engaged()
                &&!ghast.getGroundCombat().active()&&!ghast.getDomainAttack().active()&&!ghast.getOverheadAttack().active()
                &&ghast.getBbWidth()==4&&ghast.getBbHeight()==4;
        long tick=ghast.level().getGameTime();
        java.util.List<ObservedProjectileDodge.Observation> observations=null;
        if(eligible&&!dodge.active()&&tick%2==0){
            observations=new ArrayList<>();
            var candidates=ghast.level().getEntitiesOfClass(Projectile.class,ghast.getBoundingBox().inflate(16),p->supported(p,ghast));
            candidates.sort(java.util.Comparator.comparingDouble(ghast::distanceToSqr));
            for(var projectile:candidates.stream().limit(8).toList()){
                if(!ghast.level().hasChunkAt(projectile.blockPosition()))continue;
                var body=projectile.getBoundingBox();var center=body.getCenter();
                if(center.distanceToSqr(ghast.getBoundingBox().getCenter())>256)continue;
                if(ghast.level().clip(new ClipContext(ghast.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()!=HitResult.Type.MISS)continue;
                if(body.getXsize()>4||body.getYsize()>4||body.getZsize()>4)continue;
                observations.add(new ObservedProjectileDodge.Observation(projectile.getUUID(),SoutouGhastInertialMoveControl.from(center),new FlightVector(body.getXsize()/2,body.getYsize()/2,body.getZsize()/2)));
            }
        }
        double variation=observations!=null&&!observations.isEmpty()?ghast.getRandom().nextDouble():0;
        return dodge.step(tick,target==null?null:target.getUUID(),region,SoutouGhastInertialMoveControl.from(ghast.position()),
                SoutouGhastInertialMoveControl.from(ghast.getDeltaMovement()),control.getMobilityContext(),eligible,observations,variation,
                (from,to)->Math.min(from.y(),to.y())>=ghast.level().getMinBuildHeight()
                        &&Math.max(from.y(),to.y())+ghast.getBbHeight()<ghast.level().getMaxBuildHeight()&&control.hasManeuverClearance(from,to));
    }
    static boolean supported(Projectile projectile,SoutouGhast ghast){
        if(!projectile.isAlive()||!supportedType(projectile.getClass()))return false;
        var owner=projectile.getOwner();
        return owner instanceof LivingEntity living&&living.isAlive()&&owner!=ghast
                &&!owner.getUUID().equals(ghast.getUUID())&&!ghast.isAlliedTo(owner)&&!owner.isAlliedTo(ghast);
    }
    static boolean supportedType(Class<?> type){
        return !StandardSoutouFireball.class.isAssignableFrom(type)
                &&(AbstractArrow.class.isAssignableFrom(type)||SmallFireball.class.isAssignableFrom(type)||LargeFireball.class.isAssignableFrom(type));
    }
}

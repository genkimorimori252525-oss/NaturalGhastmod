package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.GroundSoutouFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** World adapter: loaded support and sole-controller intents; no position/velocity writer. */
public final class SoutouGhastGroundCombat {
    private final SoutouGhast ghast;
    private final GroundCombat movement=new GroundCombat();
    private final GroundAttack attack=new GroundAttack();
    private final GroundClearance clearance;
    private UUID targetUuid;
    private UUID lastProjectileUuid;
    private int constrained,retry,fired;
    public SoutouGhastGroundCombat(SoutouGhast ghast){this.ghast=ghast;clearance=new GroundClearance(ghast);}
    public GroundCombat.State state(){return movement.state();}
    public GroundAttack.State attackState(){return attack.state();}
    public boolean active(){return movement.active();}
    public boolean grounded(){return movement.grounded();}
    public int firedCount(){return fired;}
    public UUID lastProjectileUuid(){return lastProjectileUuid;}
    public void reset(){movement.reset();attack.reset();targetUuid=null;constrained=retry=0;ghast.setCharging(false);}
    public boolean tryBegin(MobilityContext.Sample sample,boolean busy){
        constrained=!sample.clear(8)?Math.min(6,constrained+1):0;
        if(busy||constrained<6)return false;
        var position=from(ghast.position());var floor=clearance.floor(position);
        if(!movement.begin(position,floor,clearance::body))return false;
        attack.reset();return true;
    }
    public GroundCombat.State tick(LivingEntity target,boolean visible,CombatAnchor.Region region,MobilityContext.Sample sample){
        var position=from(ghast.position());
        boolean observed=visible&&target!=null&&target.isAlive();
        UUID next=target!=null&&target.isAlive()?target.getUUID():null;
        if(!java.util.Objects.equals(next,targetUuid)){attack.invalidateTarget();movement.invalidateTarget();targetUuid=next;}
        if(movement.state().phase()==GroundCombat.Phase.SAFE_HOLD&&retry--<=0){
            retry=20;var floor=clearance.floor(position);movement.begin(position,floor,clearance::body);
        }
        boolean open=sample.clear(8)&&Integer.bitCount(sample.clearMask()&255)>=3;
        var ascent=open?position.add(new FlightVector(0,4,0)):null;
        var state=movement.step(position,observed?from(target.position()):null,region,open,ascent,
                clearance::body,clearance::supportedRoute,ghast.getRandom().nextDouble());
        if(!movement.grounded()){attack.invalidateTarget();}
        boolean eligible=movement.grounded()&&observed&&clearance.supportedRoute(position,position)
                &&ghast.distanceToSqr(target)>=16&&ghast.distanceToSqr(target)<=2304;
        Vec3 offset=observed?target.getEyePosition().subtract(ghast.getEyePosition()):null;
        var firing=attack.step(offset==null?null:from(offset),eligible);
        ghast.setCharging(firing.face());
        if(firing.fire()){
            Vec3 direction=to(firing.direction()),eye=ghast.getEyePosition(),muzzle=eye.add(direction.scale(3));
            var shot=new GroundSoutouFireball(ghast,muzzle,direction);
            // Loaded projectile muzzle corridor, not an unchecked shape lookup/forward spawn.
            boolean loaded=clearance.muzzle(eye,muzzle);
            if(loaded&&ghast.level().clip(new ClipContext(eye,muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS
                    &&ghast.level().noCollision(shot,shot.getBoundingBox())&&ghast.level().addFreshEntity(shot)){
                fired++;lastProjectileUuid=shot.getUUID();ghast.level().levelEvent(null,1016,ghast.blockPosition(),0);
            }
        }else if(firing.phase()==GroundAttack.Phase.CHARGE&&firing.ticks()==0){ghast.level().levelEvent(null,1015,ghast.blockPosition(),0);}
        Vec3 look=firing.face()?to(firing.direction()):offset;
        var control=(SoutouGhastFlightLookControl)ghast.getLookControl();if(look==null)control.clearIntent();else control.setIntent(look);
        return state;
    }
    /** The same sole velocity writer also validates inertia's stopping sweep before moving. */
    public boolean safeMotion(Vec3 displacement){
        var position=from(ghast.position());var end=position.add(from(displacement));
        return movement.grounded()?clearance.supportedRoute(position,end):clearance.body(position,end);
    }
    private static FlightVector from(Vec3 v){return SoutouGhastInertialMoveControl.from(v);}
    private static Vec3 to(FlightVector v){return SoutouGhastInertialMoveControl.to(v);}
}

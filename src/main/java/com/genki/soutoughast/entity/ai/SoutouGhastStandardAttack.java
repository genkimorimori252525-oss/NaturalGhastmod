package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.RallyReaction;
import com.genki.soutoughast.entity.ai.flight.StandardAttack;
import com.genki.soutoughast.entity.projectile.StandardSoutouFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Attack composition supplies look only; the accepted move controller still owns all boss velocity. */
public final class SoutouGhastStandardAttack {
    private final SoutouGhast ghast;
    private final StandardAttack attack=new StandardAttack();
    private final RallyReaction rally=new RallyReaction();
    private StandardSoutouFireball incoming;
    private UUID targetUuid;
    private int fired;
    public SoutouGhastStandardAttack(SoutouGhast ghast){this.ghast=ghast;}
    public StandardAttack.State state(){return attack.state();}
    public RallyReaction.State rallyState(){return rally.state();}
    public int firedCount(){return fired;}
    public boolean engaged(){return attack.state().face()||rally.state().face();}
    public void reset(){attack.reset();rally.reset();incoming=null;targetUuid=null;ghast.setCharging(false);}
    public Vec3 tick(LivingEntity target,boolean visible,boolean movementFeint){
        boolean present=target!=null&&target.isAlive();
        if(!present||!target.getUUID().equals(targetUuid)){
            attack.invalidateTarget();rally.reset();incoming=null;targetUuid=present?target.getUUID():null;
        }
        if(!present){
            var committed=attack.step(null,false);ghast.setCharging(committed.face());
            return committed.face()?to(committed.direction()):null;
        }
        Vec3 aim=null;
        if(visible){
            Vec3 offset=target.getEyePosition().subtract(ghast.getEyePosition());
            double leadTicks=Math.min(8,offset.length()/1.5);
            Vec3 lead=target.getDeltaMovement().scale(leadTicks);
            if(lead.length()>2)lead=lead.normalize().scale(2);
            aim=offset.add(lead).normalize();
        }
        if(incoming==null&&!attack.state().face()&&!movementFeint&&visible){
            var candidates=ghast.level().getEntitiesOfClass(StandardSoutouFireball.class,ghast.getBoundingBox().inflate(14),p->p.canBossReact(ghast));
            candidates.sort(java.util.Comparator.comparingDouble(ghast::distanceToSqr));
            for(var candidate:candidates.stream().limit(16).toList()){
                if(!observedIncoming(candidate))continue;
                candidate.markBossAttempt();
                if(rally.begin(candidate.getId(),from(aim),ghast.getRandom().nextDouble())){incoming=candidate;break;}
            }
        }
        Vec3 look=null;
        if(incoming!=null){
            boolean observed=incoming.isAlive()&&incoming.isOwnReturn(ghast)&&observedIncoming(incoming);
            boolean reach=incoming.getBoundingBox().inflate(1.5).intersects(ghast.getBoundingBox());
            var reaction=rally.step(observed,reach);look=to(reaction.direction());
            if(reaction.fire())incoming.returnByBoss(ghast,look);
            if(!reaction.face())incoming=null;
            // Reaction is the sole offensive tell this tick; regular charge waits.
            attack.step(null,false);ghast.setCharging(reaction.face()||reaction.fire());return look;
        }
        boolean eligible=visible&&!movementFeint&&ghast.distanceToSqr(target)>=64&&ghast.distanceToSqr(target)<=4096;
        var state=attack.step(aim==null?null:from(aim),eligible);
        if(state.face())look=to(state.direction());
        if(state.fire()){
            Vec3 muzzle=ghast.getEyePosition().add(look.scale(3));
            // No firing through a nearby wall or a muzzle inside another collider.
            boolean clear=ghast.level().clip(new ClipContext(ghast.getEyePosition(),muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS;
            var shot=new StandardSoutouFireball(ghast,muzzle,look);
            if(clear&&ghast.level().noCollision(shot,shot.getBoundingBox())&&ghast.level().addFreshEntity(shot)){
                fired++;ghast.level().levelEvent(null,1016,ghast.blockPosition(),0);
            }
        }else if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0){ghast.level().levelEvent(null,1015,ghast.blockPosition(),0);}
        ghast.setCharging(state.face());return look;
    }
    private boolean observedIncoming(StandardSoutouFireball projectile){
        if(!ghast.level().hasChunkAt(projectile.blockPosition()))return false;
        Vec3 offset=projectile.position().subtract(ghast.getEyePosition());
        if(!RallyReaction.incoming(from(offset),from(projectile.getDeltaMovement())))return false;
        return ghast.level().clip(new ClipContext(ghast.getEyePosition(),projectile.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS;
    }
    private static FlightVector from(Vec3 v){return new FlightVector(v.x,v.y,v.z);}
    private static Vec3 to(FlightVector v){return new Vec3(v.x(),v.y(),v.z());}
}

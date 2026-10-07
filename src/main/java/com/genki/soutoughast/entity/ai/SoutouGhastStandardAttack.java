package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.RallyReaction;
import com.genki.soutoughast.entity.ai.flight.StandardAttack;
import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.flight.ProjectileSelector;
import com.genki.soutoughast.entity.ai.flight.ObservedStationarity;
import com.genki.soutoughast.entity.ai.flight.MobilityContext;
import com.genki.soutoughast.entity.projectile.StandardSoutouFireball;
import com.genki.soutoughast.entity.projectile.CommittedSoutouFireball;
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
    private final ProjectileSelector selector=new ProjectileSelector();
    private ProjectileSelector.Choice choice=ProjectileSelector.Choice.STANDARD,lastFired=ProjectileSelector.Choice.STANDARD;
    private Vec3 lockedEye,lockedLanding;
    private final ObservedStationarity stationarity=new ObservedStationarity();
    private double selectionVariation;
    private CommittedPathClearance.Proof lastProof;
    private String lastCandidate="NONE",lastRejection="NONE";
    public SoutouGhastStandardAttack(SoutouGhast ghast){this.ghast=ghast;}
    public StandardAttack.State state(){return attack.state();}
    public RallyReaction.State rallyState(){return rally.state();}
    public int firedCount(){return fired;}
    public ProjectileSelector.Choice lastFiredProfile(){return lastFired;}
    public CommittedPathClearance.Proof lastPreflight(){return lastProof;}
    public ProjectileSelector.Choice selectedProfile(){return choice;}
    public String lastCandidate(){return lastCandidate;}
    public String lastRejection(){return lastRejection;}
    public int stationaryTicks(){return stationarity.ticks();}
    public double observedDisplacement(){return stationarity.displacement();}
    public boolean engaged(){return attack.state().face()||rally.state().face();}
    public void reset(){attack.reset();rally.reset();incoming=null;targetUuid=null;selector.reset();stationarity.reset();lockedEye=null;lockedLanding=null;ghast.setCharging(false);}
    public Vec3 tick(LivingEntity target,boolean visible,boolean movementFeint){
        selector.tick();
        boolean present=target!=null&&target.isAlive();
        if(!present||!target.getUUID().equals(targetUuid)){
            attack.invalidateTarget();rally.reset();incoming=null;targetUuid=present?target.getUUID():null;
            selector.reset();stationarity.reset();lockedEye=null;lockedLanding=null;
        }
        if(!present){
            var committed=attack.step(null,false);ghast.setCharging(committed.face());
            return committed.face()?to(committed.direction()):null;
        }
        Vec3 aim=null,aimPoint=null,observedVelocity=null;
        if(visible){
            Vec3 offset=target.getEyePosition().subtract(ghast.getEyePosition());
            double leadTicks=Math.min(8,offset.length()/1.5);
            observedVelocity=target.getDeltaMovement();Vec3 lead=observedVelocity.scale(leadTicks);
            if(lead.length()>2)lead=lead.normalize().scale(2);
            aimPoint=target.getEyePosition().add(lead);aim=offset.add(lead).normalize();
            stationarity.observe(from(target.position()));
        }else{stationarity.observe(null);}
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
        if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()<20&&visible){
            lockedEye=aimPoint;lockedLanding=new Vec3(aimPoint.x,target.getY()-.25,aimPoint.z);
            curveSide=observedVelocity.dot(new Vec3(-aim.z,0,aim.x))>=0?-1:1;
        }
        if(state.face())look=to(state.direction());
        if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0){
            lastProof=null;lastCandidate="NONE";lastRejection="NONE";
            selectionVariation=ghast.getRandom().nextDouble();
            choice=selector.choose(context(),ghast.getEyePosition().distanceTo(aimPoint),from(observedVelocity),stationarity.ticks(),selectionVariation);
            // A rejected candidate never commits a special tell. Standard remains a baseline alternative.
            if(choice!=ProjectileSelector.Choice.STANDARD&&profileShot(ghast.getEyePosition().add(look.scale(3)))==null){
                selector.record(choice);choice=ProjectileSelector.Choice.STANDARD;
            }
        }
        if(state.fire()){
            Vec3 muzzle=ghast.getEyePosition().add(look.scale(3));
            // No firing through a nearby wall or a muzzle inside another collider.
            boolean clear=ghast.level().clip(new ClipContext(ghast.getEyePosition(),muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS;
            var shot=choice==ProjectileSelector.Choice.STANDARD?new StandardSoutouFireball(ghast,muzzle,look):profileShot(muzzle);
            if(clear&&shot!=null&&ghast.level().noCollision(shot,shot.getBoundingBox())&&ghast.level().addFreshEntity(shot)){
                fired++;lastFired=choice;selector.record(choice);ghast.level().levelEvent(null,1016,ghast.blockPosition(),0);
            }else if(choice!=ProjectileSelector.Choice.STANDARD){
                // Dynamic geometry can invalidate a committed special; cancel, never silently replace it.
                selector.record(choice);
            }
        }else if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0){ghast.level().levelEvent(null,1015,ghast.blockPosition(),0);}
        ghast.setCharging(state.face());return look;
    }
    private MobilityContext.Kind context(){return ((SoutouGhastInertialMoveControl)ghast.getMoveControl()).getMobilityContext();}
    private CommittedSoutouFireball profileShot(Vec3 muzzle){
        if(lockedEye==null||lockedLanding==null||context()==MobilityContext.Kind.GROUND_FORCED)return null;
        var paths=new java.util.ArrayList<CommittedTrajectory>();
        try{
            switch(choice){
                case BURST -> {if(context()!=MobilityContext.Kind.CONFINED)paths.add(CommittedTrajectory.burst(from(muzzle),from(lockedEye)));}
                case CURVE -> {
                    int side=selectionVariation<.7?1:-1;
                    // Locked observed strafe, deliberately not an always-optimal counter.
                    if(curveSide<0)side=-side;
                    var strengths=context()==MobilityContext.Kind.CONFINED?new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.SHALLOW}:
                        context()==MobilityContext.Kind.OPEN_AIR&&selectionVariation>.85?new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.DEEP,CommittedTrajectory.Strength.NORMAL,CommittedTrajectory.Strength.SHALLOW}:
                            new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.NORMAL,CommittedTrajectory.Strength.SHALLOW};
                    for(var strength:strengths)for(int direction:new int[]{side,-side})paths.add(CommittedTrajectory.curve(from(muzzle),from(lockedEye),strength,direction));
                }
                case LOB -> paths.addAll(CommittedTrajectory.lobCandidates(from(muzzle),from(lockedLanding),context()==MobilityContext.Kind.OPEN_AIR?new double[]{10,8,6}:new double[]{6,4}));
                default -> {return null;}
            }
        }catch(IllegalArgumentException invalidRange){
            if(!"COMMITTED_RANGE_INVALID".equals(invalidRange.getMessage())&&!"COMMITTED_SEGMENT_SPEED_BOUND".equals(invalidRange.getMessage())&&!"HORIZONTAL_CURVE_REQUIRED".equals(invalidRange.getMessage()))throw invalidRange;
            lastRejection=invalidRange.getMessage();return null;
        }
        if(paths.isEmpty()){lastCandidate=choice.name();lastRejection="NO_VALID_CANDIDATE_GEOMETRY";}
        int ordinal=0;
        for(var path:paths){
            lastCandidate=path.kind().name()+":"+ordinal++;
            var shot=new CommittedSoutouFireball(ghast,path);
            lastProof=CommittedPathClearance.validate(ghast.level(),shot,path);
            lastRejection=lastProof.result().clear()?"NONE":lastProof.result().reason();
            if(lastProof.result().clear()){shot.setPreflight(lastProof);return shot;}
        }
        return null;
    }
    private int curveSide=1;
    private boolean observedIncoming(StandardSoutouFireball projectile){
        if(!ghast.level().hasChunkAt(projectile.blockPosition()))return false;
        Vec3 offset=projectile.position().subtract(ghast.getEyePosition());
        if(!RallyReaction.incoming(from(offset),from(projectile.getDeltaMovement())))return false;
        return ghast.level().clip(new ClipContext(ghast.getEyePosition(),projectile.position(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS;
    }
    private static FlightVector from(Vec3 v){return new FlightVector(v.x,v.y,v.z);}
    private static Vec3 to(FlightVector v){return new Vec3(v.x(),v.y(),v.z());}
}

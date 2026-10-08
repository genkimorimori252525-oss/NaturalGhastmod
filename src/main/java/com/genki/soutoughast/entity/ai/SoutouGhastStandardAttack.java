package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.RallyReaction;
import com.genki.soutoughast.entity.ai.flight.StandardAttack;
import com.genki.soutoughast.entity.ai.flight.CommittedTrajectory;
import com.genki.soutoughast.entity.ai.flight.CommittedProfile;
import com.genki.soutoughast.entity.ai.flight.ProjectileSelector;
import com.genki.soutoughast.entity.ai.flight.ObservedStationarity;
import com.genki.soutoughast.entity.ai.flight.ObservedChargeResponse;
import com.genki.soutoughast.entity.ai.flight.RememberedCoverLob;
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
    private final ObservedChargeResponse chargeResponse=new ObservedChargeResponse();
    private final RememberedCoverLob cover=new RememberedCoverLob();
    private double selectionVariation;
    private CommittedPathClearance.Proof lastProof;
    private CommittedProfile committedProfile;
    private long profileCueTick=-1;
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
    public CommittedProfile committedProfile(){return committedProfile;}
    public long profileCueTick(){return profileCueTick;}
    public boolean engaged(){return attack.state().face()||rally.state().face();}
    public void reset(){attack.reset();rally.reset();incoming=null;targetUuid=null;selector.reset();stationarity.reset();chargeResponse.reset();cover.reset();lockedEye=null;lockedLanding=null;committedProfile=null;ghast.setCharging(false);}
    public Vec3 tick(LivingEntity target,boolean visible,boolean movementFeint){
        selector.tick();
        boolean present=target!=null&&target.isAlive();
        if(!present||!target.getUUID().equals(targetUuid)){
            attack.invalidateTarget();rally.reset();incoming=null;targetUuid=present?target.getUUID():null;
            selector.reset();stationarity.reset();chargeResponse.reset();cover.reset();lockedEye=null;lockedLanding=null;committedProfile=null;
        }
        chargeResponse.tick(ghast.level().getGameTime());
        if(!present){
            var committed=attack.step(null,false);chargeResponse.observe(null,committed,false);ghast.setCharging(committed.face());
            return committed.face()?to(committed.direction()):null;
        }
        Vec3 aim=null,aimPoint=null,observedVelocity=null;
        FlightVector observedPosition=null;
        FlightVector observedEye=null,observedLanding=null;
        if(visible){
            Vec3 offset=target.getEyePosition().subtract(ghast.getEyePosition());
            double leadTicks=Math.min(8,offset.length()/1.5);
            observedVelocity=target.getDeltaMovement();Vec3 lead=observedVelocity.scale(leadTicks);
            if(lead.length()>2)lead=lead.normalize().scale(2);
            aimPoint=target.getEyePosition().add(lead);aim=offset.add(lead).normalize();
            observedPosition=from(target.position());stationarity.observe(observedPosition);
            observedEye=from(target.getEyePosition());observedLanding=from(target.position().add(0,-.25,0));
        }else{stationarity.observe(null);}
        boolean wasCover=cover.active();
        cover.observe(targetUuid,ghast.level().getGameTime(),observedEye,observedLanding,context(),movementFeint);
        if(wasCover&&!cover.active())cancelCover();
        // A visible charge cannot donate its age/recipe to a newly hidden cover attack.
        if(!visible&&!cover.active()&&attack.state().phase()==StandardAttack.Phase.CHARGE){attack.step(null,false);committedProfile=null;}
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
            chargeResponse.observe(observedPosition,attack.step(null,false),false);ghast.setCharging(reaction.face()||reaction.fire());return look;
        }
        boolean coverStarted=false;
        if(!visible&&!movementFeint&&attack.state().phase()==StandardAttack.Phase.IDLE&&!cover.active()){
            var remembered=cover.attempt();
            if(remembered!=null){
                double variation=ghast.getRandom().nextDouble();
                if(selector.chooseCover(context(),ghast.getEyePosition().distanceTo(to(remembered.eye())),variation)==ProjectileSelector.Choice.LOB){
                    choice=ProjectileSelector.Choice.LOB;selectionVariation=variation;committedProfile=null;profileCueTick=-1;
                    lockedEye=to(remembered.eye());lockedLanding=to(remembered.landing());
                    Vec3 direction=lockedEye.subtract(ghast.getEyePosition()).normalize();
                    if(coveredEndpoint()&&prepareProfile(ghast.getEyePosition().add(direction.scale(3)))&&cover.begin(remembered)&&attack.beginCover(from(direction)))coverStarted=true;
                    else{selector.record(ProjectileSelector.Choice.LOB);cancelCover();}
                }
            }
        }
        boolean hiddenCover=cover.active();
        if(hiddenCover){
            aimPoint=lockedEye;aim=lockedEye.subtract(ghast.getEyePosition()).normalize();
            // Frozen recipe/endpoint only: no hidden target geometry, alternates or fallback.
            Vec3 direction=attack.state().ticks()<19?aim:to(attack.state().direction());
            if(!coveredEndpoint()||!coverStarted&&verifiedProfile(committedProfile,ghast.getEyePosition().add(direction.scale(3)))==null){
                selector.record(ProjectileSelector.Choice.LOB);cancelCover();hiddenCover=false;coverStarted=false;aim=null;
            }
        }
        boolean eligible=!movementFeint&&(visible?ghast.distanceToSqr(target)>=64&&ghast.distanceToSqr(target)<=4096:hiddenCover);
        var state=coverStarted?attack.state():attack.step(aim==null?null:from(aim),eligible);
        if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()<20&&visible){
            lockedEye=aimPoint;lockedLanding=new Vec3(aimPoint.x,target.getY()-.25,aimPoint.z);
            curveSide=observedVelocity.dot(new Vec3(-aim.z,0,aim.x))>=0?-1:1;
        }
        if(state.face())look=to(state.direction());
        if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0&&!hiddenCover){
            lastProof=null;lastCandidate="NONE";lastRejection="NONE";
            committedProfile=null;profileCueTick=-1;choice=ProjectileSelector.Choice.STANDARD;
            selectionVariation=ghast.getRandom().nextDouble();
        }
        if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==19){
            if(!hiddenCover){
            choice=selector.choose(context(),ghast.getEyePosition().distanceTo(aimPoint),from(observedVelocity),stationarity.ticks(),selectionVariation,chargeResponse.repeatedTiming());
            // Commit attack recipe/aim at the distinct cue; physical trajectory starts at launch.
            if(choice!=ProjectileSelector.Choice.STANDARD&&!prepareProfile(ghast.getEyePosition().add(look.scale(3)))){
                selector.record(choice);choice=ProjectileSelector.Choice.STANDARD;
            }
            }
            if(committedProfile!=null&&ghast.level() instanceof net.minecraft.server.level.ServerLevel server){
                var cue=switch(choice){case BURST -> net.minecraft.core.particles.ParticleTypes.END_ROD;case CURVE -> net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME;default -> net.minecraft.core.particles.ParticleTypes.LAVA;};
                Vec3 point=ghast.getEyePosition().add(look.scale(3));server.sendParticles(cue,point.x,point.y,point.z,8,.35,.35,.35,.02);
                profileCueTick=server.getGameTime();
            }
        }
        boolean emitted=false;
        if(state.fire()){
            Vec3 muzzle=ghast.getEyePosition().add(look.scale(3));
            // No firing through a nearby wall or a muzzle inside another collider.
            boolean clear=ghast.level().clip(new ClipContext(ghast.getEyePosition(),muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.MISS;
            var shot=choice==ProjectileSelector.Choice.STANDARD?new StandardSoutouFireball(ghast,muzzle,look):verifiedProfile(committedProfile,muzzle);
            if(clear&&shot!=null&&ghast.level().noCollision(shot,shot.getBoundingBox())&&ghast.level().addFreshEntity(shot)){
                emitted=true;fired++;lastFired=choice;selector.record(choice);ghast.level().levelEvent(null,1016,ghast.blockPosition(),0);
            }else if(choice!=ProjectileSelector.Choice.STANDARD){
                // Dynamic geometry can invalidate a committed special; cancel, never silently replace it.
                selector.record(choice);
            }
            cover.finish();
        }else if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0){ghast.level().levelEvent(null,1015,ghast.blockPosition(),0);}
        chargeResponse.observe(observedPosition,state,emitted);ghast.setCharging(state.face());return look;
    }
    private void cancelCover(){cover.finish();attack.reset();committedProfile=null;choice=ProjectileSelector.Choice.STANDARD;lockedEye=lockedLanding=null;}
    private boolean coveredEndpoint(){
        Vec3 eye=ghast.getEyePosition();
        return CommittedPathClearance.loadedRay(ghast.level(),eye,lockedEye)
                &&ghast.level().clip(new ClipContext(eye,lockedEye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()==HitResult.Type.BLOCK;
    }
    private MobilityContext.Kind context(){return ((SoutouGhastInertialMoveControl)ghast.getMoveControl()).getMobilityContext();}
    private boolean prepareProfile(Vec3 muzzle){
        if(lockedEye==null||lockedLanding==null||context()==MobilityContext.Kind.GROUND_FORCED)return false;
        var recipes=new java.util.ArrayList<CommittedProfile>();
            switch(choice){
                case BURST -> {if(context()!=MobilityContext.Kind.CONFINED)recipes.add(new CommittedProfile(CommittedTrajectory.Kind.BURST,null,0,0,from(lockedEye)));}
                case CURVE -> {
                    int side=selectionVariation<.7?1:-1;
                    // Locked observed strafe, deliberately not an always-optimal counter.
                    if(curveSide<0)side=-side;
                    var strengths=context()==MobilityContext.Kind.CONFINED?new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.SHALLOW}:
                        context()==MobilityContext.Kind.OPEN_AIR&&selectionVariation>.85?new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.DEEP,CommittedTrajectory.Strength.NORMAL,CommittedTrajectory.Strength.SHALLOW}:
                            new CommittedTrajectory.Strength[]{CommittedTrajectory.Strength.NORMAL,CommittedTrajectory.Strength.SHALLOW};
                    for(var strength:strengths)for(int direction:new int[]{side,-side})recipes.add(new CommittedProfile(CommittedTrajectory.Kind.CURVE,strength,direction,0,from(lockedEye)));
                }
                case LOB -> {for(double height:context()==MobilityContext.Kind.OPEN_AIR?new double[]{10,8,6}:new double[]{6,4})recipes.add(new CommittedProfile(CommittedTrajectory.Kind.LOB,null,0,height,from(lockedLanding)));}
                default -> {return false;}
            }
        for(var recipe:recipes){
            if(verifiedProfile(recipe,muzzle)!=null){committedProfile=recipe;return true;}
        }
        return false;
    }
    private CommittedSoutouFireball verifiedProfile(CommittedProfile recipe,Vec3 muzzle){
        if(recipe==null||context()==MobilityContext.Kind.GROUND_FORCED)return null;
        if(recipe.kind()==CommittedTrajectory.Kind.BURST&&context()==MobilityContext.Kind.CONFINED)return null;
        if(recipe.strength()==CommittedTrajectory.Strength.DEEP&&context()!=MobilityContext.Kind.OPEN_AIR)return null;
        lastCandidate=recipe.toString();lastProof=null;
        try{
            var shot=new CommittedSoutouFireball(ghast,recipe.pathFrom(from(muzzle)));
            lastProof=CommittedPathClearance.validate(ghast.level(),shot,shot.flight().path());
            lastRejection=lastProof.result().clear()?"NONE":lastProof.result().reason();
            if(lastProof.result().clear()){shot.setPreflight(lastProof);return shot;}
        }catch(IllegalArgumentException invalidRange){
            if(!"COMMITTED_RANGE_INVALID".equals(invalidRange.getMessage())&&!"COMMITTED_SEGMENT_SPEED_BOUND".equals(invalidRange.getMessage())&&!"HORIZONTAL_CURVE_REQUIRED".equals(invalidRange.getMessage()))throw invalidRange;
            lastRejection=invalidRange.getMessage();return null;
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

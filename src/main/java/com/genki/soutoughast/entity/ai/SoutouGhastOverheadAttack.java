package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.CommittedSoutouFireball;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Major produces bounded intents and projectiles; the existing controller alone moves the boss. */
public final class SoutouGhastOverheadAttack {
    private final SoutouGhast ghast;
    private final MajorActionDirector director;
    private final OverheadBombing art=new OverheadBombing();
    private UUID subject;
    private int sequences,bombs;
    private CommittedPathClearance.Proof lastBombProof;
    private String lastDecision="NONE";
    public SoutouGhastOverheadAttack(SoutouGhast ghast){this.ghast=ghast;director=ghast.getMajorDirector();}
    public boolean active(){return art.active();}
    public OverheadBombing.State state(){return art.state();}
    public int sequenceCount(){return sequences;}
    public int bombCount(){return bombs;}
    public String lastDecision(){return lastDecision;}
    public int quietTicks(){return director.quietTicks();}
    public int recentTicks(){return director.recentTicks();}
    public CommittedPathClearance.Proof lastBombProof(){return lastBombProof;}
    public void ordinaryTick(boolean observedCombat){director.tick(observedCombat);}
    private SoutouGhastInertialMoveControl control(){return (SoutouGhastInertialMoveControl)ghast.getMoveControl();}
    public boolean tryBegin(LivingEntity target,boolean visible,CombatAnchor.Region region,boolean busy){
        if(target==null||!target.isAlive()||!visible||region==null){lastDecision="TARGET_OR_REGION_UNOBSERVED";return false;}
        double range=ghast.position().distanceTo(target.position());
        if(!director.shouldBegin(control().getMobilityContext(),true,busy,range,ghast.getRandom().nextDouble())){lastDecision=director.decision();return false;}
        if(!bodyRoute(from(target.position()).add(new FlightVector(0,12,0)))){lastDecision="OVERHEAD_BODY_ROUTE_BLOCKED";director.rejected();return false;}
        if(!art.begin(from(ghast.position()),from(target.position()),region.center(),this::bodyRoute)){lastDecision="WITHDRAW_BODY_ROUTE_BLOCKED";director.rejected();return false;}
        lastDecision="STARTED";
        subject=target.getUUID();director.began();sequences++;lastBombProof=null;
        ghast.level().levelEvent(null,1015,ghast.blockPosition(),0);return true;
    }
    public OverheadBombing.State tick(LivingEntity target,boolean visible){
        director.tick(false);
        boolean observed=target!=null&&target.isAlive()&&visible&&target.getUUID().equals(subject);
        if(art.active()&&art.state().phase()!=OverheadBombing.Phase.RECOVER
                &&(control().getMobilityContext()==MobilityContext.Kind.GROUND_FORCED||control().getMobilityContext()==MobilityContext.Kind.CONFINED))art.abort("MOBILITY_CONSTRAINED");
        var state=art.step(from(ghast.position()),observed?from(target.position()):null,this::bodyRoute);
        if(state.releaseRequested()){
            if(release(state.releasePoint()))art.onFired();else art.abort("BOMB_CORRIDOR_INVALID");
        }
        if(!art.active()){director.finished();subject=null;}
        ghast.setCharging(art.state().face());return art.state();
    }
    public Vec3 look(LivingEntity target,boolean visible){
        if(art.state().downward())return new Vec3(0,-1,0);
        return target!=null&&visible&&target.getUUID().equals(subject)?target.getEyePosition().subtract(ghast.getEyePosition()):null;
    }
    private boolean bodyRoute(FlightVector goal){
        if(goal==null)return false;Vec3 offset=to(goal).subtract(ghast.position());
        if(offset.length()>64)return false;
        AABB sweep=ghast.getBoundingBox().expandTowards(offset);
        return loaded(sweep)&&ghast.level().noCollision(ghast,sweep);
    }
    private boolean loaded(AABB box){
        if(box.minY<ghast.level().getMinBuildHeight()||box.maxY>=ghast.level().getMaxBuildHeight())return false;
        for(int x=Mth.floor(box.minX-2)>>4;x<=(Mth.floor(box.maxX+2)>>4);x++)
            for(int z=Mth.floor(box.minZ-2)>>4;z<=(Mth.floor(box.maxZ+2)>>4);z++)if(!ghast.level().hasChunk(x,z))return false;
        return true;
    }
    private boolean release(FlightVector observedPoint){
        Vec3 start=ghast.getEyePosition().add(0,-3,0),floorStart=to(observedPoint).add(0,.25,0),floorEnd=floorStart.add(0,-24,0);
        if(!loaded(new AABB(floorStart,floorEnd)))return false;
        var floor=ghast.level().clip(new ClipContext(floorStart,floorEnd,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast));
        if(floor.getType()!=HitResult.Type.BLOCK)return false;
        Vec3 endpoint=new Vec3(observedPoint.x(),floor.getLocation().y-.25,observedPoint.z());
        CommittedTrajectory path;
        try{path=CommittedTrajectory.bomb(from(start),from(endpoint));}
        catch(IllegalArgumentException rejected){
            if(!"COMMITTED_RANGE_INVALID".equals(rejected.getMessage())&&!"VERTICAL_BOMB_CORRIDOR_REQUIRED".equals(rejected.getMessage())&&!"COMMITTED_SEGMENT_SPEED_BOUND".equals(rejected.getMessage()))throw rejected;
            return false;
        }
        var shot=new CommittedSoutouFireball(ghast,path);
        lastBombProof=CommittedPathClearance.validate(ghast.level(),shot,path);
        if(!lastBombProof.result().clear())return false;
        if(!loaded(ghast.getBoundingBox())||ghast.level().clip(new ClipContext(ghast.getEyePosition(),start,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,ghast)).getType()!=HitResult.Type.MISS
                ||!ghast.level().noCollision(shot,shot.getBoundingBox()))return false;
        shot.setPreflight(lastBombProof);
        if(!ghast.level().addFreshEntity(shot))return false;
        bombs++;ghast.level().levelEvent(null,1016,ghast.blockPosition(),0);return true;
    }
    public void reset(){if(art.active())director.finished();art.reset();subject=null;lastBombProof=null;lastDecision="NONE";}
    private static FlightVector from(Vec3 point){return SoutouGhastInertialMoveControl.from(point);}
    private static Vec3 to(FlightVector point){return SoutouGhastInertialMoveControl.to(point);}
}

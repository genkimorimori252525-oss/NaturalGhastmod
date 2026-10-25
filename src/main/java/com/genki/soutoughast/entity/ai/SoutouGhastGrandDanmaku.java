package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.danmaku.*;
import com.genki.soutoughast.danmaku.core.*;
import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.*;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import java.util.*;

/** One exclusive server-owned authored swarm. All entity movement still belongs to the flight controller. */
public final class SoutouGhastGrandDanmaku {
    public record State(String phase,int age,FlightController.Intent intent,Vec3 look,boolean face,String reason) {}
    private final SoutouGhast boss;
    private GrandDanmakuAssets.Pattern pattern;
    private WorldDanmakuSession swarm;
    private DanmakuNativeCollision collision;
    private DanmakuFrame frame;
    private UUID session,subject;
    private Vec3 relative;
    private long regionGeneration;
    private int sequence,lastPattern=-1,lastEmitted,lastPeak,lastContacts,lastDamage;
    private String lastReason="NONE";
    private final HashMap<UUID,Boolean> watchers=new HashMap<>();
    public SoutouGhastGrandDanmaku(SoutouGhast boss){this.boss=boss;}
    public boolean active(){return swarm!=null;}
    public int patternIndex(){return pattern==null?lastPattern:pattern.index();}
    public int age(){return swarm==null?-1:swarm.age();}
    public boolean targetAcknowledged(){return subject!=null&&Boolean.TRUE.equals(watchers.get(subject));}
    public int watcherCount(){return watchers.size();}
    public int liveCount(){return swarm==null?0:swarm.liveCount();}
    public int totalEmitted(){return swarm==null?lastEmitted:swarm.totalEmitted();}
    public int peakLive(){return swarm==null?lastPeak:swarm.peakLive();}
    public int contacts(){return collision==null?lastContacts:collision.blockContacts()+collision.entityContacts();}
    public int damageAttempts(){return collision==null?lastDamage:collision.damageAttempts();}
    public String lastReason(){return lastReason;}
    public boolean tryBegin(LivingEntity target,boolean visible,CombatAnchor.Region region,boolean busy){
        if(active()||!(boss.level() instanceof ServerLevel level)||!(target instanceof ServerPlayer player)||player instanceof FakePlayer
                ||!visible||!target.isAlive()||player.isCreative()||player.isSpectator()||region==null||!region.contains(from(boss.position()))
                ||busy||boss.getMajorDirector().active()||boss.getMajorDirector().quietTicks()>0||boss.getMajorDirector().recentTicks()>0
                ||control().getMobilityContext()!=MobilityContext.Kind.OPEN_AIR||boss.getDomainAttack().active()||boss.getGroundCombat().active()
                ||boss.getOverheadAttack().active()||!GrandDanmakuAssets.ready())return false;
        double range=boss.distanceTo(player);if(range<22||range>34||!muzzleClear(player))return false;
        if(!boss.getMajorDirector().shouldBeginGrandDanmaku(true,busy,range,boss.getRandom().nextDouble()))return false;
        UUID id=UUID.randomUUID();if(!DanmakuWorldBudget.acquire(level,id)){boss.getMajorDirector().rejected();lastReason="LEVEL_SESSION_BUDGET";return false;}
        try{
            int selected=boss.getRandom().nextInt(3);if(selected==lastPattern)selected=(selected+1+boss.getRandom().nextInt(2))%3;
            pattern=GrandDanmakuAssets.get(selected);lastPattern=selected;session=id;subject=player.getUUID();regionGeneration=region.generation();relative=boss.position().subtract(player.position());
            swarm=new WorldDanmakuSession(pattern.score());collision=new DanmakuNativeCollision(boss,this::acknowledged);sequence=0;frame=currentFrame(player,null);watchers.clear();lastReason="PRELUDE";
            boss.getMajorDirector().began();boss.getStandardAttack().reset();boss.setCharging(true);level.levelEvent(null,1015,boss.blockPosition(),0);ensureWatchers(level);return true;
        }catch(RuntimeException error){if(active())finish(true,"ADMISSION_FAILED");else DanmakuWorldBudget.release(level,id);LogUtils.getLogger().error("Grand Danmaku admission failed closed",error);return false;}
    }
    public State tick(LivingEntity target,boolean visible,CombatAnchor.Region region){
        if(!active())return idle();boss.getMajorDirector().tick(false);
        if(!(target instanceof ServerPlayer player)||player instanceof FakePlayer||!player.getUUID().equals(subject)||!player.isAlive()||player.isCreative()||player.isSpectator()
                ||player.level()!=boss.level()||!visible||!boss.getSensing().hasLineOfSight(player)||region==null||region.generation()!=regionGeneration
                ||boss.distanceTo(player)<12||boss.distanceTo(player)>64||control().getMobilityContext()!=MobilityContext.Kind.OPEN_AIR
                ||boss.getDomainAttack().active()||boss.getOverheadAttack().active()||boss.getGroundCombat().active()||!muzzleClear(player)){
            finish(true,"OBSERVATION_OR_MODE_LOST");return idle();
        }
        var level=(ServerLevel)boss.level();Vec3 desired=player.position().add(relative);
        if(!region.contains(from(desired))||!control().hasManeuverClearance(from(boss.position()),from(desired))){finish(true,"RELATIVE_LOCK_CLEARANCE");return idle();}
        try{
            ensureWatchers(level);
            if(swarm.nextTick()>=pattern.preludeTicks()-4&&!acknowledged(player)){finish(true,"TARGET_RENDER_NOT_ACKNOWLEDGED");return idle();}
            frame=currentFrame(player,frame);collision.prepare(swarm);
            var previous=new HashMap<Long,WorldDanmakuSession.Bullet>();for(var b:swarm.bullets())previous.put(b.id(),b);
            var batch=swarm.step(frame,collision::remove);
            var removed=batch.removed().stream().map(id->new DanmakuPlayback.Removal(id,collision.removalPoint(previous.get(id),batch.age()))).toList();
            if(swarm.liveCount()>0||!batch.bursts().isEmpty()||!removed.isEmpty()||batch.age()%5==0){
                var update=new DanmakuPlayback.Delta(session,++sequence,batch.age(),level.getGameTime(),frame,batch.bursts(),removed);
                broadcast(GrandDanmakuNetwork.Packet.step(boss.getId(),boss.getUUID(),update));
            }
            if(swarm.finished()){finish(false,"NORMAL_DRAIN_COMPLETE");return idle();}
            boolean face=batch.age()<pattern.score().durationTicks();boss.setCharging(face);
            Vec3 wanted=player.getDeltaMovement().add(desired.subtract(boss.position()).scale(.18));double speed=Math.min(.65,wanted.length());
            var intent=speed<.005?FlightController.Intent.hold():FlightController.Intent.move(from(wanted),speed);
            String phase=swarm.age()<pattern.preludeTicks()?"PRELUDE":face?"FIRING":"DRAIN";lastReason=phase;
            return new State(phase,swarm.age(),intent,player.getEyePosition().subtract(boss.getEyePosition()),face,lastReason);
        }catch(RuntimeException error){LogUtils.getLogger().error("Grand Danmaku session canceled safely",error);finish(true,"SESSION_ERROR");return idle();}
    }
    private State idle(){return new State("IDLE",-1,FlightController.Intent.brake(),null,false,lastReason);}
    private DanmakuFrame currentFrame(ServerPlayer player,DanmakuFrame prior){
        Vec3 forward=player.getEyePosition().subtract(boss.getEyePosition()).normalize(),mouth=boss.getEyePosition().add(forward.scale(3));return DanmakuFrame.toward(vector(mouth),vector(player.getEyePosition()),prior);
    }
    private boolean muzzleClear(ServerPlayer player){
        Vec3 eye=boss.getEyePosition(),muzzle=eye.add(player.getEyePosition().subtract(eye).normalize().scale(3));
        return CommittedPathClearance.loadedRay(boss.level(),eye,muzzle)&&boss.level().clip(new ClipContext(eye,muzzle,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getType()==HitResult.Type.MISS;
    }
    private void ensureWatchers(ServerLevel level){
        watchers.entrySet().removeIf(e->{var p=level.getServer().getPlayerList().getPlayer(e.getKey());return p==null||p.level()!=level||!p.isAlive()||p.distanceToSqr(boss)>96*96;});
        for(var player:level.players())if(player.isAlive()&&player.distanceToSqr(boss)<=96*96&&!watchers.containsKey(player.getUUID()))watch(player);
    }
    public void watch(ServerPlayer player){
        if(!active()||player.level()!=boss.level()||player.distanceToSqr(boss)>96*96||watchers.size()>=GrandDanmakuAssets.MAX_WATCHERS&&!watchers.containsKey(player.getUUID()))return;
        watchers.put(player.getUUID(),false);
        var snapshot=new DanmakuPlayback.Sync(session,boss.getUUID(),boss.getId(),boss.level().dimension().location().toString(),pattern.index(),pattern.hash(),sequence,swarm.age(),boss.level().getGameTime(),swarm.bullets());
        GrandDanmakuNetwork.send(player,GrandDanmakuNetwork.Packet.sync(snapshot));
    }
    public void unwatch(ServerPlayer player){if(active()&&watchers.remove(player.getUUID())!=null)GrandDanmakuNetwork.send(player,GrandDanmakuNetwork.Packet.end(boss.getId(),boss.getUUID(),session,true,"TRACKING_ENDED"));}
    private boolean acknowledged(ServerPlayer player){return Boolean.TRUE.equals(watchers.get(player.getUUID()));}
    public void acknowledge(ServerPlayer player,UUID id,String hash,boolean accepted){
        if(!active()||!session.equals(id)||player.level()!=boss.level()||!watchers.containsKey(player.getUUID()))return;
        boolean correct=accepted&&pattern.hash().equals(hash);watchers.put(player.getUUID(),correct);
        if(!correct&&player.getUUID().equals(subject))finish(true,"TARGET_RESOURCE_MISMATCH");
    }
    private void broadcast(GrandDanmakuNetwork.Packet packet){
        var server=boss.level().getServer();for(var id:List.copyOf(watchers.keySet())){var p=server.getPlayerList().getPlayer(id);if(p!=null&&p.level()==boss.level())GrandDanmakuNetwork.send(p,packet);}
    }
    public void abort(String reason){if(active())finish(true,reason);}
    private void finish(boolean hard,String reason){
        if(!active())return;lastReason=reason;lastEmitted=swarm.totalEmitted();lastPeak=swarm.peakLive();lastContacts=contacts();lastDamage=damageAttempts();
        try{broadcast(GrandDanmakuNetwork.Packet.end(boss.getId(),boss.getUUID(),session,hard,reason));}
        catch(RuntimeException failure){LogUtils.getLogger().warn("Grand Danmaku end delivery failed; server ownership is still released",failure);}
        finally{swarm.cancel();if(boss.level() instanceof ServerLevel level)DanmakuWorldBudget.release(level,session);swarm=null;collision=null;frame=null;session=subject=null;watchers.clear();boss.setCharging(false);if(boss.getMajorDirector().active())boss.getMajorDirector().finished();}
    }
    private SoutouGhastInertialMoveControl control(){return (SoutouGhastInertialMoveControl)boss.getMoveControl();}
    private static FlightVector from(Vec3 v){return new FlightVector(v.x,v.y,v.z);}
    private static DanmakuFrame.Vec vector(Vec3 v){return new DanmakuFrame.Vec(v.x,v.y,v.z);}
}

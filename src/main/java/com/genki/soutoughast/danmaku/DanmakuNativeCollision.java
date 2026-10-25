package com.genki.soutoughast.danmaku;

import com.genki.soutoughast.danmaku.core.*;
import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.function.Predicate;

/** Loaded-only earliest solid/entity swept contacts. Never creates explosions or changes blocks. */
public final class DanmakuNativeCollision {
    private final SoutouGhast owner;private final ServerLevel level;private final Predicate<ServerPlayer> acknowledged;
    private List<LivingEntity> candidates=List.of();
    private final HashMap<Long,DanmakuFrame.Vec> points=new HashMap<>();
    private int blockContacts,entityContacts,damageAttempts,unloadedRemovals;
    public DanmakuNativeCollision(SoutouGhast owner,Predicate<ServerPlayer> acknowledged){this.owner=owner;this.level=(ServerLevel)owner.level();this.acknowledged=acknowledged;}
    public void prepare(WorldDanmakuSession session){
        points.clear();var bullets=session.bullets();if(bullets.isEmpty()){candidates=List.of();return;}
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        for(var b:bullets){var from=b.at(session.nextTick()-b.bornTick()-1);var to=b.at(session.nextTick()-b.bornTick());double r=b.radius()*.48;
            minX=Math.min(minX,Math.min(from.x(),to.x())-r);minY=Math.min(minY,Math.min(from.y(),to.y())-r);minZ=Math.min(minZ,Math.min(from.z(),to.z())-r);
            maxX=Math.max(maxX,Math.max(from.x(),to.x())+r);maxY=Math.max(maxY,Math.max(from.y(),to.y())+r);maxZ=Math.max(maxZ,Math.max(from.z(),to.z())+r);}
        if(maxX-minX>256||maxY-minY>256||maxZ-minZ>256)throw new IllegalStateException("DANMAKU_ENTITY_QUERY_BOUNDS");
        var observed=new ArrayList<LivingEntity>(GrandDanmakuAssets.MAX_CANDIDATES);
        var budget=new DanmakuCandidateBudget<LivingEntity>(GrandDanmakuAssets.MAX_CANDIDATES,e->true);
        level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(minX,minY,minZ,maxX,maxY,maxZ).inflate(1),budget,observed,GrandDanmakuAssets.MAX_CANDIDATES+1);
        candidates=observed.stream().filter(this::eligible).toList();
    }
    private boolean eligible(LivingEntity e){return e!=owner&&e.isAlive()&&!e.isSpectator()&&!(e instanceof SoutouGhast)&&!owner.isAlliedTo(e)&&!e.isAlliedTo(owner)&&(!(e instanceof Player p)||!p.isCreative());}
    public boolean remove(WorldDanmakuSession.Bullet bullet,DanmakuFrame.Vec from,DanmakuFrame.Vec to){
        double radius=bullet.radius()*.48,earliest=Double.POSITIVE_INFINITY;LivingEntity victim=null;
        int minX=(int)Math.floor(Math.min(from.x(),to.x())-radius),maxX=(int)Math.floor(Math.max(from.x(),to.x())+radius),
            minY=(int)Math.floor(Math.min(from.y(),to.y())-radius),maxY=(int)Math.floor(Math.max(from.y(),to.y())+radius),
            minZ=(int)Math.floor(Math.min(from.z(),to.z())-radius),maxZ=(int)Math.floor(Math.max(from.z(),to.z())+radius);
        if(maxX-minX>10||maxY-minY>10||maxZ-minZ>10)throw new IllegalStateException("DANMAKU_SWEEP_VOLUME_BUDGET");
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            var pos=new BlockPos(x,y,z);
            if(y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunkAt(pos)){
                unloadedRemovals++;points.put(bullet.id(),from);return true;
            }
            for(var box:DanmakuWorldBudget.shapes(level,pos))earliest=Math.min(earliest,hit(from,to,radius,box));
        }
        for(var candidate:candidates){if(!eligible(candidate))continue;double t=hit(from,to,radius,candidate.getBoundingBox());if(t<earliest){earliest=t;victim=candidate;}}
        if(!Double.isFinite(earliest))return false;
        points.put(bullet.id(),from.add(to.subtract(from).scale(earliest)));
        if(victim==null){blockContacts++;return true;}
        entityContacts++;
        if(victim instanceof ServerPlayer player&&!acknowledged.test(player))return true;
        if(DanmakuWorldBudget.hurtAllowed(level,victim.getUUID())){damageAttempts++;victim.hurt(owner.damageSources().mobProjectile(owner,owner),1f);}
        return true;
    }
    private static double hit(DanmakuFrame.Vec from,DanmakuFrame.Vec to,double radius,AABB b){return DanmakuSweep.hit(from,to,radius,b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);}
    public DanmakuFrame.Vec removalPoint(WorldDanmakuSession.Bullet bullet,int age){return points.getOrDefault(bullet.id(),bullet.at(age-bullet.bornTick()));}
    public int blockContacts(){return blockContacts;}
    public int entityContacts(){return entityContacts;}
    public int damageAttempts(){return damageAttempts;}
    public int unloadedRemovals(){return unloadedRemovals;}
}

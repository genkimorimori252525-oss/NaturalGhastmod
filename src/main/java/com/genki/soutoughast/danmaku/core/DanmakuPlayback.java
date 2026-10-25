package com.genki.soutoughast.danmaku.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Bounded visual-only replica. It cannot collide, hurt, move entities or choose server births. */
public final class DanmakuPlayback {
    public record Removal(long id,DanmakuFrame.Vec point) {}
    public record Ghost(WorldDanmakuSession.Bullet bullet,DanmakuFrame.Vec point,double removedAt) {}
    public record Sync(UUID session,UUID owner,int entityId,String dimension,int pattern,String scoreHash,int sequence,int age,
                       long gameTick,List<WorldDanmakuSession.Bullet> bullets) {
        public Sync {bullets=List.copyOf(bullets);if(session==null||owner==null||entityId<0||dimension==null||dimension.length()>128
                ||pattern<0||pattern>2||scoreHash==null||!scoreHash.matches("[a-f0-9]{64}")||sequence<0||sequence>10000||age<0||age>2400||gameTick<0
                ||bullets.size()>GrandDanmakuScore.MAX_LIVE_BULLETS)throw new IllegalArgumentException("DANMAKU_SYNC_BOUND");}
    }
    public record Delta(UUID session,int sequence,int age,long gameTick,DanmakuFrame frame,
                        List<GrandDanmakuScore.BurstDescriptor> bursts,List<Removal> removals) {
        public Delta {bursts=List.copyOf(bursts);removals=List.copyOf(removals);if(session==null||sequence<1||sequence>10000||age<0||age>2400||gameTick<0
                ||frame==null||bursts.size()>32||removals.size()>GrandDanmakuScore.MAX_LIVE_BULLETS)throw new IllegalArgumentException("DANMAKU_DELTA_BOUND");}
    }
    private Sync identity;
    private GrandDanmakuScore.Config score;
    private int sequence,serverAge;
    private long receivedTick,lastServerTick;
    private final LinkedHashMap<Long,WorldDanmakuSession.Bullet> bullets=new LinkedHashMap<>();
    private final LinkedHashMap<Long,Ghost> ghosts=new LinkedHashMap<>();
    private boolean valid,ended;
    public Sync identity(){return identity;}
    public boolean valid(){return valid;}
    public boolean ended(){return ended;}
    public void finish(boolean hard,long clientTick){
        if(hard){clear();return;}if(!valid)return;
        for(var bullet:bullets.values()){if(ghosts.size()>=GrandDanmakuScore.MAX_LIVE_BULLETS)ghosts.remove(ghosts.keySet().iterator().next());ghosts.put(bullet.id(),new Ghost(bullet,bullet.at(Math.max(0,serverAge-bullet.bornTick())),serverAge));}
        bullets.clear();ended=true;receivedTick=clientTick;
    }
    public int liveCount(){return bullets.size();}
    public int sequence(){return sequence;}
    public List<WorldDanmakuSession.Bullet> bullets(){return List.copyOf(bullets.values());}
    public List<Ghost> ghosts(){return List.copyOf(ghosts.values());}
    public boolean sync(Sync message,GrandDanmakuScore.Config expected,String expectedHash,String currentDimension,long clientTick){
        if(!message.scoreHash().equals(expectedHash)||!message.dimension().equals(currentDimension)){clear();return false;}
        if(identity!=null&&identity.session().equals(message.session())&&message.sequence()<sequence)return false;
        var checked=new LinkedHashMap<Long,WorldDanmakuSession.Bullet>();
        try{for(var bullet:message.bullets()){verify(bullet,expected,message.age());if(checked.put(bullet.id(),bullet)!=null)throw new IllegalArgumentException("DANMAKU_DUPLICATE_SNAPSHOT_ID");}}
        catch(IllegalArgumentException invalid){clear();return false;}
        clear();identity=message;score=expected;sequence=message.sequence();serverAge=message.age();receivedTick=clientTick;lastServerTick=message.gameTick();bullets.putAll(checked);valid=true;return true;
    }
    public boolean delta(Delta message,long clientTick){
        if(!valid||!identity.session().equals(message.session()))return false;
        if(message.sequence()<=sequence)return false;
        if(message.sequence()!=sequence+1||message.age()<serverAge||message.gameTick()<lastServerTick){clear();return false;}
        try{
            for(var removed:message.removals()){
                if(removed.point()==null)throw new IllegalArgumentException("DANMAKU_REMOVAL_POINT");
                var previous=bullets.remove(removed.id());if(previous!=null){if(ghosts.size()>=GrandDanmakuScore.MAX_LIVE_BULLETS)ghosts.remove(ghosts.keySet().iterator().next());ghosts.put(previous.id(),new Ghost(previous,removed.point(),message.age()));}
            }
            for(var burst:message.bursts()){
                if(burst.bornTick()!=message.age())throw new IllegalArgumentException("DANMAKU_BIRTH_AGE");
                var canonical=GrandDanmakuScore.burstsAt(score,burst.bornTick());if(!canonical.contains(burst))throw new IllegalArgumentException("DANMAKU_UNSCHEDULED_BIRTH");
                for(var spec:GrandDanmakuScore.expandBurst(score,burst)){
                    if(bullets.size()>=GrandDanmakuScore.MAX_LIVE_BULLETS)throw new IllegalArgumentException("DANMAKU_CLIENT_LIVE_CAP");
                    var bullet=new WorldDanmakuSession.Bullet(spec.id(),spec.trackIndex(),spec.bornTick(),spec.lifetimeTicks(),message.frame().birth(spec),message.frame().velocity(spec),spec.radius());
                    if(bullets.putIfAbsent(bullet.id(),bullet)!=null)throw new IllegalArgumentException("DANMAKU_DUPLICATE_CLIENT_BIRTH");
                }
            }
        }catch(IllegalArgumentException invalid){clear();return false;}
        sequence=message.sequence();serverAge=message.age();lastServerTick=message.gameTick();receivedTick=clientTick;prune(serverAge);return true;
    }
    private static void verify(WorldDanmakuSession.Bullet b,GrandDanmakuScore.Config score,int age){
        if(b.trackIndex()<0||b.trackIndex()>=score.tracks().size()||b.origin()==null||b.velocity()==null)throw new IllegalArgumentException("DANMAKU_SNAPSHOT_TRACK");
        var track=score.tracks().get(b.trackIndex());int local=b.bornTick()-track.startTick();long ordinal=b.id()&0xfffffL;
        if(local<0||b.bornTick()>=track.endTick()||local%track.pattern().intervalTicks()!=0||b.bornTick()>age||age-b.bornTick()>=b.lifetimeTicks()
                ||b.id()>>>48!=b.trackIndex()||((b.id()>>>20)&0xfffffffL)!=local/track.pattern().intervalTicks()||ordinal>=track.pattern().bullets()
                ||b.lifetimeTicks()!=track.pattern().lifetimeTicks()||b.radius()!=track.radius()
                ||Math.abs(b.origin().x())>30000000||Math.abs(b.origin().z())>30000000||Math.abs(b.origin().y())>4096||b.velocity().length()>9)throw new IllegalArgumentException("DANMAKU_SNAPSHOT_BULLET");
    }
    /** At most one tick of smooth visual prediction; never remove a core before server authority. */
    public double renderAge(double clientTick){return serverAge+Math.max(0,Math.min(ended?14:1,clientTick-receivedTick));}
    public boolean stale(long clientTick){return !valid||clientTick-receivedTick>40||clientTick<receivedTick;}
    public void prune(double age){ghosts.entrySet().removeIf(e->age-e.getValue().removedAt()>=14);}
    public void clear(){valid=false;ended=false;identity=null;score=null;bullets.clear();ghosts.clear();sequence=serverAge=0;receivedTick=lastServerTick=0;}
}

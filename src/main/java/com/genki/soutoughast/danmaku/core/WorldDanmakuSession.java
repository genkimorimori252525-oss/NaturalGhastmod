package com.genki.soutoughast.danmaku.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Server-independent world swarm. The adapter owns collision/damage; the score owns every birth. */
public final class WorldDanmakuSession {
    public record Bullet(long id,int trackIndex,int bornTick,int lifetimeTicks,DanmakuFrame.Vec origin,DanmakuFrame.Vec velocity,double radius) {
        public DanmakuFrame.Vec at(double age){return origin.add(velocity.scale(age));}
    }
    public record Batch(int age,DanmakuFrame frame,List<GrandDanmakuScore.BurstDescriptor> bursts,List<Long> removed) {}
    @FunctionalInterface public interface Collision {boolean remove(Bullet bullet,DanmakuFrame.Vec from,DanmakuFrame.Vec to);}
    private final GrandDanmakuScore.Config score;
    private final LinkedHashMap<Long,Bullet> bullets=new LinkedHashMap<>();
    private int nextTick,totalEmitted,peakLive;
    private boolean canceled;
    public WorldDanmakuSession(GrandDanmakuScore.Config score){if(score==null)throw new IllegalArgumentException("DANMAKU_SCORE_REQUIRED");this.score=score;}
    public Batch step(DanmakuFrame frame,Collision collision){
        if(canceled)throw new IllegalStateException("DANMAKU_SESSION_CANCELED");
        if(frame==null||collision==null)throw new IllegalArgumentException("DANMAKU_STEP_CONTEXT");
        var removed=new ArrayList<Long>();
        for(var iterator=bullets.values().iterator();iterator.hasNext();){
            var bullet=iterator.next();int age=nextTick-bullet.bornTick();
            if(age>=bullet.lifetimeTicks()||collision.remove(bullet,bullet.at(age-1),bullet.at(age))){removed.add(bullet.id());iterator.remove();}
        }
        var bursts=nextTick<=score.durationTicks()?GrandDanmakuScore.burstsAt(score,nextTick):List.<GrandDanmakuScore.BurstDescriptor>of();
        for(var burst:bursts)for(var spec:GrandDanmakuScore.expandBurst(score,burst)){
            if(bullets.size()>=GrandDanmakuScore.MAX_LIVE_BULLETS)throw new IllegalStateException("DANMAKU_LIVE_BUDGET");
            var bullet=new Bullet(spec.id(),spec.trackIndex(),spec.bornTick(),spec.lifetimeTicks(),frame.birth(spec),frame.velocity(spec),spec.radius());
            if(bullets.putIfAbsent(bullet.id(),bullet)!=null)throw new IllegalStateException("DANMAKU_DUPLICATE_BIRTH");
            totalEmitted++;
        }
        peakLive=Math.max(peakLive,bullets.size());return new Batch(nextTick++,frame,List.copyOf(bursts),List.copyOf(removed));
    }
    public List<Bullet> bullets(){return List.copyOf(bullets.values());}
    public int age(){return Math.max(0,nextTick-1);}
    public int nextTick(){return nextTick;}
    public int liveCount(){return bullets.size();}
    public int totalEmitted(){return totalEmitted;}
    public int peakLive(){return peakLive;}
    public boolean emissionFinished(){return nextTick>=score.durationTicks();}
    public boolean finished(){return canceled||nextTick>=score.durationTicks()+120&&bullets.isEmpty();}
    public void cancel(){canceled=true;bullets.clear();}
}

package com.genki.soutoughast.danmaku.core;

import java.nio.file.*;
import java.util.*;

public final class DanmakuPlaybackTest {
    private static int checks;
    private static final UUID SESSION=new UUID(0,1),OWNER=new UUID(0,2);
    private static final String HASH="0".repeat(64),DIM="minecraft:overworld";
    private static void check(boolean pass,String reason){checks++;if(!pass)throw new AssertionError(reason);}
    private static DanmakuPlayback.Sync sync(WorldDanmakuSession world,int sequence){return new DanmakuPlayback.Sync(SESSION,OWNER,1,DIM,0,HASH,sequence,world.age(),1000+world.age(),world.bullets());}
    public static void main(String[] args)throws Exception{
        var score=GrandDanmakuScoreJson.read(Files.readString(Path.of(args[0],"seisou_tenmaku_grand_danmaku_v2.json")));
        var f=DanmakuFrame.toward(new DanmakuFrame.Vec(26,228,32),new DanmakuFrame.Vec(26,225,12),null);var world=new WorldDanmakuSession(score);var client=new DanmakuPlayback();
        for(int i=0;i<100;i++)world.step(f,(b,a,z)->false);
        check(!client.sync(sync(world,0),score,"f".repeat(64),DIM,100),"hash mismatch fails visual acceptance");check(!client.valid()&&client.liveCount()==0,"mismatch cannot retain a hidden stale swarm");
        check(!client.sync(sync(world,0),score,HASH,"minecraft:the_nether",100),"wrong dimension cannot contaminate cache");
        check(client.sync(sync(world,0),score,HASH,DIM,100),"late tracker receives only actual surviving bullets");check(client.liveCount()==world.liveCount(),"late snapshot exact live count");
        int seq=0;for(int i=100;i<220;i++){
            final boolean hit=i%11==0;var removed=new HashMap<Long,DanmakuFrame.Vec>();var previous=new HashMap<Long,WorldDanmakuSession.Bullet>();for(var b:world.bullets())previous.put(b.id(),b);
            var batch=world.step(f,(b,a,z)->{if(hit)removed.put(b.id(),z);return hit;});
            var removals=batch.removed().stream().map(id->new DanmakuPlayback.Removal(id,removed.getOrDefault(id,previous.get(id).at(batch.age()-previous.get(id).bornTick())))).toList();
            var delta=new DanmakuPlayback.Delta(SESSION,++seq,batch.age(),1000+batch.age(),f,batch.bursts(),removals);
            check(client.delta(delta,i),"ordered authoritative delta accepted");check(client.liveCount()==world.liveCount(),"client reconstructed exact surviving world swarm");
            check(!client.delta(delta,i),"duplicate cannot create duplicate births");check(client.valid(),"duplicate delivery does not invalidate correct state");
            var actual=client.bullets();check(actual.equals(world.bullets()),"all IDs, frozen origins and velocities agree");
        }
        check(client.ghosts().size()<=3000,"ghost trail memory bounded");client.prune(world.age()+14);check(client.ghosts().isEmpty(),"removed light trails expire without permanent history");
        double before=client.renderAge(219),after=client.renderAge(10000);check(after-before<=1.000001,"stale data cannot extrapolate indefinitely");check(client.stale(10000),"lost session becomes stale");
        var gap=new DanmakuPlayback.Delta(SESSION,seq+2,world.age()+1,2000,f,List.of(),List.of());check(!client.delta(gap,220)&&!client.valid(),"sequence gap invalidates rather than permit unseen damaging births");
        check(client.sync(sync(world,seq),score,HASH,DIM,220),"new authoritative snapshot recovers cache");client.clear();check(client.liveCount()==0&&client.ghosts().isEmpty()&&!client.valid(),"world unload/hard cancel clears all visual ownership");
        var finishing=new DanmakuPlayback();check(finishing.sync(sync(world,seq),score,HASH,DIM,220),"normal tail test starts from genuine live state");
        finishing.finish(false,220);check(finishing.liveCount()==0&&finishing.ended()&&!finishing.ghosts().isEmpty(),"normal completion keeps light alone, never a damaging core");
        finishing.prune(finishing.renderAge(235));check(finishing.ghosts().isEmpty(),"normal-end light finishes fading without future server packets");
        var gate=new DanmakuHurtGate();var target=new UUID(1,1);check(gate.allow(target,1000),"first contact allowed");check(!gate.allow(target,1000),"simultaneous second swarm cannot stack damage");
        for(int t=1001;t<1010;t++)check(!gate.allow(target,t),"shared per-target ten tick cap");check(gate.allow(target,1010),"damage can resume after ten ticks");
        for(int i=0;i<300;i++)gate.allow(new UUID(2,i),1010);check(gate.retainedTargets()<=256,"bounded shared hurt memory");check(gate.allow(new UUID(3,1),1611),"expired contact memory releases capacity");
        var bounded=new DanmakuCandidateBudget<Integer>(128,n->true);int produced=0,collected=0;boolean overflow=false;
        try{for(int i=0;i<1000000;i++){produced++;if(bounded.test(i))collected++;}}catch(DanmakuCandidateBudget.Exceeded expected){overflow=true;}
        check(overflow&&produced==129&&collected==128&&bounded.visited()==129,"dense lazy population aborted at129, never fully materialized or silently truncated");
        var filtered=new DanmakuCandidateBudget<Integer>(128,n->false);produced=0;overflow=false;
        try{for(int i=0;i<1000000;i++){produced++;filtered.test(i);}}catch(DanmakuCandidateBudget.Exceeded expected){overflow=true;}
        check(overflow&&produced==129,"ineligible/allied population cannot evade the raw query budget");
        System.out.println("Danmaku playback checks: "+checks);
    }
}

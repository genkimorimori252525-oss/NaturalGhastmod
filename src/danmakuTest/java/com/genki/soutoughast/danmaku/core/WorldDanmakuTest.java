package com.genki.soutoughast.danmaku.core;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldDanmakuTest {
    private static int checks;
    private static final DanmakuFrame.Vec O=new DanmakuFrame.Vec(21,228,30);
    private static void check(boolean pass,String reason){checks++;if(!pass)throw new AssertionError(reason);}
    private static void near(double a,double b,String reason){check(Math.abs(a-b)<1e-7,reason);}
    public static void main(String[] args)throws Exception{
        var shortPattern=new DanmakuPattern.Config(DanmakuPattern.Kind.FAN,1,.1,2,0,0,0,10,4);
        var shortTrack=new GrandDanmakuScore.Track("short",0,4,shortPattern,GrandDanmakuScore.Frame.PLAYER_VIEW,.2,0,0,.1);
        var boundaryScore=new GrandDanmakuScore.Config(20,java.util.List.of(shortTrack));
        check(GrandDanmakuScore.liveCount(shortTrack,5)==2,"live budget retains bullets after their emission track ends");
        check(GrandDanmakuScore.at(boundaryScore,5).size()==GrandDanmakuScore.liveCount(shortTrack,5),"analytic geometry and live budget agree across track boundary");
        var f=DanmakuFrame.toward(O,O.add(new DanmakuFrame.Vec(0,0,20)),null);near(f.right().x(),1,"south-facing emission is player-view right");near(f.up().y(),1,"screen-up is world-up");
        var spec=new GrandDanmakuScore.SpawnSpec(1,0,0,0,2,0,.2,.3,.4,100,0,.12);check(f.birth(spec).equals(O),"local emitter offset removed exactly once");check(f.velocity(spec).equals(new DanmakuFrame.Vec(.2,.3,.4)),"world velocity basis has correct handedness");
        var previous=f;for(int i=0;i<=2000;i++){double angle=(i-1000)*.0001;var next=DanmakuFrame.toward(O,O.add(new DanmakuFrame.Vec(0,Math.cos(angle),Math.sin(angle))),previous);check(next.right().dot(previous.right())>.99,"near-vertical crossing has no frame flip");near(next.forward().dot(next.up()),0,"transport remains orthogonal");previous=next;}
        near(DanmakuSweep.hit(new DanmakuFrame.Vec(0,0,0),new DanmakuFrame.Vec(10,0,0),.1,4,-1,-1,5,1,1),.39,"sweep finds earliest fast contact");
        check(Double.isInfinite(DanmakuSweep.hit(new DanmakuFrame.Vec(0,2,0),new DanmakuFrame.Vec(10,2,0),.1,4,-1,-1,5,1,1)),"parallel outside slab misses");
        near(DanmakuSweep.hit(O,O,.1,20,227,29,22,229,31),0,"initial overlap contacts at zero");
        int[] expected={813,690,3065},peaks={226,193,1020};String[] files={"kouengeki_grand_danmaku_v1.json","kokuseki_kairo_grand_danmaku_v1.json","seisou_tenmaku_grand_danmaku_v2.json"};
        for(int n=0;n<3;n++){
            var score=GrandDanmakuScoreJson.read(Files.readString(Path.of(args[0],files[n])));var world=new WorldDanmakuSession(score);var local=new GrandDanmakuSession(score);
            WorldDanmakuSession.Bullet first=null;DanmakuFrame.Vec firstVelocity=null,firstOrigin=null;
            int maxAge=score.durationTicks()+1200;
            for(int tick=0;!world.finished()&&tick<maxAge;tick++){
                var moving=DanmakuFrame.toward(O.add(new DanmakuFrame.Vec(tick*.01,0,0)),O.add(new DanmakuFrame.Vec(10,0,20)),f);
                var batch=world.step(moving,(b,a,z)->false);local.tick();
                check(world.liveCount()==local.activeCount(),"world lifetime and emission match unchanged pure engine");
                check(batch.bursts().equals(local.drainBurstBatch()),"birth descriptors exactly match canonical core");
                for(var bullet:world.bullets()){
                    if(bullet.bornTick()==tick)check(bullet.origin().subtract(moving.origin()).length()<1e-8,"every bullet starts at this burst's actual mouth");
                    if(first==null){first=bullet;firstVelocity=bullet.velocity();firstOrigin=bullet.origin();}
                }
                if(first!=null){check(first.velocity().equals(firstVelocity)&&first.origin().equals(firstOrigin),"later moving target/mouth cannot bend an existing bullet");}
            }
            check(world.finished()&&world.liveCount()==0,"normal completion drains residual lifetimes");check(world.totalEmitted()==expected[n],"no score bullets silently thinned");check(world.peakLive()==peaks[n],"full supplied peak supported");
        }
        var score=GrandDanmakuScoreJson.read(Files.readString(Path.of(args[0],files[2])));var cancel=new WorldDanmakuSession(score);for(int i=0;i<100;i++)cancel.step(f,(b,a,z)->false);check(cancel.liveCount()>0,"cancel test has real live swarm");cancel.cancel();check(cancel.finished()&&cancel.liveCount()==0,"hard cancellation releases all bullets");
        var hit=new WorldDanmakuSession(score);for(int i=0;i<100;i++)hit.step(f,(b,a,z)->true);check(hit.liveCount()<=68,"collision removals never respawn or duplicate bullets");
        System.out.println("World Danmaku checks: "+checks);
    }
}

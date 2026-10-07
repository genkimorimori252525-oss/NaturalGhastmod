package com.genki.soutoughast.entity.ai.flight;

public final class OverheadBombingTest {
    private static int checks;
    public static void main(String[] args){
        var director=new MajorActionDirector();
        check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,24,1),"initial ordinary interval");
        check(director.decision().equals("QUIET_INTERVAL")&&director.quietTicks()==240,"latest quiet reason is observable without changing timer");
        for(int i=0;i<240;i++)director.tick(true);
        check(!director.shouldBegin(MobilityContext.Kind.CONFINED,true,false,24,1),"confined suppresses overhead");
        check(director.decision().equals("MOBILITY_CONTEXT"),"latest context rejection retained");
        check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,true,24,1),"ordinary/rally/feint tell excludes major");
        check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,false,false,24,1),"unobserved or unsafe excludes major");
        check(director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,24,1),"eligible observed open encounter");
        check(director.decision().equals("ELIGIBLE")&&director.quietTicks()==0,"eligibility diagnostic does not alter selection");
        director.began();director.finished();for(int i=0;i<600;i++)director.tick(true);
        check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,24,1),"strong repetition memory outlasts ordinary quiet");
        check(director.decision().equals("RECENT_MAJOR")&&director.recentTicks()==600,"latest repetition reason uses actual memory");
        for(int i=0;i<600;i++)director.tick(true);
        check(director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,24,1),"finite major memory");
        var art=new OverheadBombing();var boss=new FlightVector(0,6,20);var target=FlightVector.ZERO;var region=new FlightVector(0,6,28);
        check(art.begin(boss,target,region,p->true),"clear visible withdrawal begins");
        var physical=new FlightController();var velocity=FlightVector.ZERO;int releases=0,lastRelease=-1;boolean returned=false,down=false;int total=0;
        for(;total<800&&art.active();total++){
            var state=art.step(boss,target,p->true);
            down|=state.downward();returned|=state.phase()==OverheadBombing.Phase.RETURN;
            if(state.releaseRequested()){
                check(Math.hypot(boss.x()-target.x(),boss.z()-target.z())<=.75,"bombs only from actual overhead acquisition");
                check(lastRelease<0||total-lastRelease==24,"bounded24tick cadence");lastRelease=total;releases++;
                var locked=state.releasePoint();art.onFired();target=target.add(new FlightVector(.01,0,0));
                check(locked.x()!=target.x(),"release-time point does not follow later observed target");
            }
            velocity=physical.step(velocity,state.intent());check(velocity.length()<=FlightController.MAX_SPEED+1e-9,"existing sole-controller speed cap");boss=boss.add(velocity);
        }
        check(returned&&down&&releases==3&&!art.active(),"finite withdrawal/return/downward triple-release/recovery");
        check(boss.subtract(region).length()<2,"physical return to retained region");
        check(!art.begin(boss,target,region,p->false),"blocked withdrawal never starts");
        var awayFromRegion=new FlightVector(0,6,20);
        check(art.begin(awayFromRegion,target,region,p->true),"new clear state begins");
        var lost=art.step(awayFromRegion,null,p->true);check(lost.phase()==OverheadBombing.Phase.RECOVER&&!lost.releaseRequested(),"LOS loss cancels future releases and safely recovers");
        var blocked=new OverheadBombing();blocked.begin(new FlightVector(0,6,20),target,region,p->true);
        check(blocked.step(new FlightVector(0,6,20),target,p->false).phase()==OverheadBombing.Phase.RECOVER,"new geometry aborts offensive phase");
        System.out.println("PASS: "+checks+" overhead/director checks, simulatedTicks="+total);
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

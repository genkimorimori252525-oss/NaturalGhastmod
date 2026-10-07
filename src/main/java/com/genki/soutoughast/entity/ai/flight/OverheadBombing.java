package com.genki.soutoughast.entity.ai.flight;

import java.util.function.Predicate;

/** Finite visible major exception. Produces intent; never integrates or teleports the boss. */
public final class OverheadBombing {
    public enum Phase { IDLE, WITHDRAW, RETURN, ALIGN, BOMBING, RECOVER }
    public record State(Phase phase,int ticks,FlightVector goal,FlightController.Intent intent,
                        boolean downward,boolean face,boolean releaseRequested,FlightVector releasePoint,String reason) {}
    private Phase phase=Phase.IDLE;
    private int ticks,aligned,releases,faceRemaining;
    private FlightVector withdrawal,retainedCenter,lastBoss;
    private String reason="NONE";
    private State state=new State(Phase.IDLE,0,null,FlightController.Intent.hold(),false,false,false,null,"NONE");
    public State state(){return state;}
    public boolean active(){return phase!=Phase.IDLE;}
    public boolean begin(FlightVector boss,FlightVector visibleTarget,FlightVector regionCenter,Predicate<FlightVector> clear){
        if(active()||boss==null||visibleTarget==null||regionCenter==null)return false;
        var offset=boss.subtract(visibleTarget);var away=new FlightVector(offset.x(),0,offset.z()).normalized();
        if(away.length()<1e-9)away=new FlightVector(0,0,1);
        var destination=boss.add(away.scale(16)).add(new FlightVector(0,4,0));
        if(!clear.test(destination))return false;
        withdrawal=destination;retainedCenter=regionCenter;lastBoss=boss;phase=Phase.WITHDRAW;ticks=aligned=releases=faceRemaining=0;reason="NONE";
        state=publish(destination,false,null);return true;
    }
    public State step(FlightVector boss,FlightVector visibleTarget,Predicate<FlightVector> clear){
        lastBoss=boss;if(faceRemaining>0)faceRemaining--;
        if(!active())return publish(null,false,null);
        if(phase!=Phase.RECOVER&&visibleTarget==null)abort("TARGET_UNOBSERVED");
        ticks++;
        var goal=goal(visibleTarget);
        if(phase!=Phase.RECOVER&&!clear.test(goal)){abort("BODY_ROUTE_BLOCKED");goal=retainedCenter;}
        boolean release=false;FlightVector releasePoint=null;
        switch(phase){
            case WITHDRAW -> {
                if(boss.subtract(withdrawal).length()<=1.5)transition(Phase.RETURN);
                else if(ticks>=160)abort("WITHDRAW_TIMEOUT");
            }
            case RETURN -> {
                if(overhead(boss,visibleTarget))transition(Phase.ALIGN);
                else if(ticks>=180)abort("RETURN_TIMEOUT");
            }
            case ALIGN -> {
                aligned=overhead(boss,visibleTarget)?aligned+1:0;
                if(ticks>=30&&aligned>=5)transition(Phase.BOMBING);
                else if(ticks>=90)abort("ALIGN_TIMEOUT");
            }
            case BOMBING -> {
                if(ticks==12+releases*24){
                    if(!overhead(boss,visibleTarget)){abort("OVERHEAD_ALIGNMENT_LOST");}
                    else{release=true;releasePoint=visibleTarget;releases++;}
                }else if(releases==3)transition(Phase.RECOVER);
            }
            case RECOVER -> {
                if(boss.subtract(retainedCenter).length()<=1.5&&faceRemaining==0)transition(Phase.IDLE);
                else if(ticks>=200){reason="RECOVERY_TIMEOUT";transition(Phase.IDLE);}
            }
            case IDLE -> {}
        }
        goal=active()?goal(visibleTarget):null;
        var published=publish(goal,release,releasePoint);
        if(phase==Phase.RECOVER&&!clear.test(goal))state=new State(phase,ticks,goal,FlightController.Intent.brake(),false,published.face(),false,null,reason);
        return state;
    }
    public void onFired(){
        if(!state.releaseRequested())throw new IllegalStateException("DECLARED_RELEASE_REQUIRED");
        faceRemaining=8;state=new State(state.phase(),state.ticks(),state.goal(),state.intent(),state.downward(),true,true,state.releasePoint(),state.reason());
    }
    public void abort(String cause){reason=cause;transition(Phase.RECOVER);state=publish(retainedCenter,false,null);}
    public void reset(){phase=Phase.IDLE;ticks=aligned=releases=faceRemaining=0;withdrawal=retainedCenter=lastBoss=null;reason="NONE";state=publish(null,false,null);}
    private void transition(Phase next){phase=next;ticks=0;aligned=0;}
    private static boolean overhead(FlightVector boss,FlightVector target){
        return target!=null&&Math.hypot(boss.x()-target.x(),boss.z()-target.z())<=.75&&Math.abs(boss.y()-target.y()-12)<=1;
    }
    private FlightVector goal(FlightVector target){return switch(phase){
        case IDLE -> null;
        case WITHDRAW -> withdrawal;
        case RECOVER -> retainedCenter;
        default -> target.add(new FlightVector(0,12,0));
    };}
    private State publish(FlightVector goal,boolean release,FlightVector point){
        var intent=FlightController.Intent.hold();
        if(goal!=null&&lastBoss!=null){
            var error=goal.subtract(lastBoss);double limit=phase==Phase.WITHDRAW?.35:phase==Phase.RETURN?FlightController.MAX_SPEED:.4;
            if(error.length()>.25)intent=FlightController.Intent.move(error,Math.min(limit,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.25))));
        }
        boolean preface=phase==Phase.BOMBING&&ticks>=12+releases*24-8&&ticks<12+releases*24;
        return state=new State(phase,ticks,goal,intent,phase==Phase.ALIGN||phase==Phase.BOMBING,faceRemaining>0||preface,release,point,reason);
    }
}

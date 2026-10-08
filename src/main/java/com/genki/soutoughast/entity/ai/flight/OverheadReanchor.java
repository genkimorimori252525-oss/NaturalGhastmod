package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;
import java.util.function.BiPredicate;

/** Rare frozen transit; only the caller may publish its freshly guarded commit. */
public final class OverheadReanchor {
    public enum Phase { IDLE, TELL, CLIMB, CROSS, BRAKE, ABORT, DONE }
    public record State(Phase phase,FlightController.Intent intent,boolean commit) {}
    private Phase phase=Phase.IDLE;
    private UUID subject;
    private long generation;
    private FlightVector overhead,destination;
    private double routeLength;
    private int age,ticks;
    public boolean active(){return phase!=Phase.IDLE&&phase!=Phase.DONE;}
    public Phase phase(){return phase;}
    public FlightVector overhead(){return overhead;}
    public FlightVector destination(){return destination;}
    public double routeLength(){return routeLength;}
    public void reset(){phase=Phase.IDLE;subject=null;overhead=destination=null;age=ticks=0;}
    public boolean begin(UUID identity,FlightVector target,double targetTop,FlightVector boss,
            CombatAnchor.Region region,BiPredicate<FlightVector,FlightVector> route){
        if(active()||identity==null||target==null||boss==null||region==null||!region.contains(boss)||route==null||!Double.isFinite(targetTop)||targetTop<target.y())return false;
        var delta=target.subtract(boss);var toward=new FlightVector(delta.x(),0,delta.z());double distance=toward.length();
        if(distance<8||distance>16)return false;
        // Arrival margin keeps the actual body bottom >=6 above the frozen target top.
        double height=Math.max(boss.y(),targetTop+6+.75);
        var above=new FlightVector(boss.x(),height,boss.z());
        var beyond=new FlightVector(target.x(),height,target.z()).add(toward.normalized().scale(8));
        double length=above.subtract(boss).length()+beyond.subtract(above).length();
        if(length>24||!route.test(boss,above)||!route.test(above,beyond))return false;
        subject=identity;generation=region.generation();overhead=above;destination=beyond;routeLength=length;age=ticks=0;phase=Phase.TELL;return true;
    }
    public void abort(){if(active()&&phase!=Phase.ABORT){phase=Phase.ABORT;ticks=0;}}
    public State step(UUID identity,boolean visible,CombatAnchor.Region region,FlightVector boss,
            FlightVector velocity,BiPredicate<FlightVector,FlightVector> route){
        if(!active())return new State(phase,FlightController.Intent.hold(),false);
        if(phase!=Phase.ABORT&&(!subject.equals(identity)||!visible||region==null||region.generation()!=generation||age>=148))abort();
        if(phase!=Phase.ABORT){
            var point=phase==Phase.TELL||phase==Phase.CLIMB?overhead:destination;
            if(route==null||!route.test(boss,point))abort();
        }
        boolean commit=false;
        if(phase==Phase.CLIMB&&boss.subtract(overhead).length()<=.7){phase=Phase.CROSS;ticks=0;}
        if(phase==Phase.CROSS&&boss.subtract(destination).length()<=.7){phase=Phase.BRAKE;ticks=0;}
        // A phase transition must validate the newly selected leg before producing its intent.
        if((phase==Phase.CROSS||phase==Phase.BRAKE)&&!route.test(boss,destination))abort();
        if(phase==Phase.BRAKE&&velocity.length()<=.04){
            if(boss.subtract(destination).length()<=.7&&route.test(boss,boss)){phase=Phase.DONE;commit=true;}
            else abort();
        }
        var emittedPhase=phase;
        FlightController.Intent intent;
        if(phase==Phase.TELL||phase==Phase.CLIMB||phase==Phase.CROSS){
            var point=phase==Phase.CROSS?destination:overhead;var offset=point.subtract(boss);
            double speed=Math.min(phase==Phase.TELL?.1:.32,Math.sqrt(2*FlightController.BRAKING*Math.max(0,offset.length()-.4)));
            intent=FlightController.Intent.move(offset,speed);
        }else intent=FlightController.Intent.brake();
        age++;ticks++;
        if(phase==Phase.TELL&&ticks>=12){phase=Phase.CLIMB;ticks=0;}
        else if(phase==Phase.ABORT&&ticks>=12)phase=Phase.DONE;
        return new State(emittedPhase,intent,commit);
    }
}

package com.genki.soutoughast.entity.ai.flight;

import java.util.Objects;
import java.util.UUID;

/** Boss-owned world-space region. Only observed encounter events may relocate it. */
public final class CombatAnchor {
    public static final double MIN_RANGE=22,MAX_RANGE=34;
    public static final FlightVector RADII=new FlightVector(20,8,20);
    public enum Range { TOO_CLOSE, COMFORTABLE, TOO_FAR }
    public enum Reason { ACQUIRED, REPLACED, DISENGAGED, SPACE_BLOCKED }
    public record Region(FlightVector center,FlightVector radii,long generation,Reason reason) {
        public double radius(FlightVector point){
            FlightVector d=point.subtract(center);
            return new FlightVector(d.x()/radii.x(),d.y()/radii.y(),d.z()/radii.z()).length();
        }
        public boolean contains(FlightVector point){return radius(point)<=1;}
    }
    public record Evaluation(Range range,boolean inRegion,FlightVector point,FlightController.Intent intent) { }
    private Region region;
    private UUID subject;
    private long generation;
    private int cooldown,disengaged,blocked,targetless;
    public Region region(){return region;}
    public void clear(){region=null;subject=null;cooldown=disengaged=blocked=targetless=0;}

    public void observe(UUID identity,FlightVector target,FlightVector boss,boolean noUsableRoute){
        Objects.requireNonNull(identity);targetless=0;
        if(region==null||!identity.equals(subject)){
            select(identity,target,boss,region==null?Reason.ACQUIRED:Reason.REPLACED);return;
        }
        if(cooldown>0)cooldown--;
        disengaged=target.subtract(region.center()).length()>60?disengaged+1:0;
        blocked=noUsableRoute?blocked+1:0;
        if(cooldown==0&&(disengaged>=40||blocked>=40)){
            Reason reason=disengaged>=40?Reason.DISENGAGED:Reason.SPACE_BLOCKED;
            // A blocked return needs a locally feasible fallback, not the same unreachable projection.
            FlightVector candidate=reason==Reason.SPACE_BLOCKED?boss:centerFor(target,boss);
            if(candidate.subtract(region.center()).length()>=2)
                selectAt(identity,candidate,reason);
            else{cooldown=100;disengaged=blocked=0;}
        }
    }
    public void unobserved(boolean targetPresent){
        disengaged=blocked=0;if(cooldown>0)cooldown--;
        targetless=targetPresent?0:targetless+1;
        if(targetless>=200){region=null;subject=null;}
    }
    private void select(UUID identity,FlightVector target,FlightVector boss,Reason reason){
        selectAt(identity,centerFor(target,boss),reason);
    }
    private void selectAt(UUID identity,FlightVector center,Reason reason){
        region=new Region(center,RADII,++generation,reason);subject=identity;
        cooldown=100;disengaged=blocked=targetless=0;
    }
    private static FlightVector centerFor(FlightVector target,FlightVector boss){
        FlightVector delta=boss.subtract(target);
        FlightVector bearing=new FlightVector(delta.x(),0,delta.z()).normalized();
        if(bearing.length()<1e-9)bearing=new FlightVector(0,0,1);
        return target.add(bearing.scale(Math.sqrt(28*28-6*6))).add(new FlightVector(0,6,0));
    }
    /** Facing is deliberately ignored; retained signature permits the internal migration. */
    public Evaluation evaluate(FlightVector playerPosition,FlightVector facing,FlightVector bossPosition){
        if(region==null)select(new UUID(0,0),playerPosition,bossPosition,Reason.ACQUIRED);
        double distance=bossPosition.subtract(playerPosition).length();
        Range range=distance<MIN_RANGE?Range.TOO_CLOSE:distance>MAX_RANGE?Range.TOO_FAR:Range.COMFORTABLE;
        boolean inside=region.contains(bossPosition);
        FlightVector point=inside?region.center():region.center().add(bossPosition.subtract(region.center()).scale(.85/region.radius(bossPosition)));
        FlightVector error=point.subtract(bossPosition);
        FlightController.Intent intent=inside?FlightController.Intent.hold():FlightController.Intent.move(error,
                Math.min(FlightController.MAX_SPEED,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.5))));
        return new Evaluation(range,inside,point,intent);
    }
}

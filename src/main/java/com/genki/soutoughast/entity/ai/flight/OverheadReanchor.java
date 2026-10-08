package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;
import java.util.function.BiPredicate;

/** Rare frozen transit; only the caller may publish its freshly guarded commit. */
public final class OverheadReanchor {
    public enum Phase { IDLE, TELL, CLIMB, ALIGN, CROSS, BRAKE, TURN, DESCEND, ABORT, HANDOFF, DONE }
    public record State(Phase phase,FlightController.Intent intent,FlightVector look,boolean commit) {}
    private Phase phase=Phase.IDLE;
    private UUID subject;
    private long generation;
    private FlightVector overhead,beyond,destination,candidateCenter,heading;
    private double routeLength;
    private int age,ticks,budget;
    public boolean active(){return phase!=Phase.IDLE&&phase!=Phase.DONE&&phase!=Phase.HANDOFF;}
    public Phase phase(){return phase;}
    public FlightVector overhead(){return overhead;}
    public FlightVector destination(){return destination;}
    public FlightVector beyond(){return beyond;}
    public FlightVector candidateCenter(){return candidateCenter;}
    public FlightVector heading(){return heading;}
    public int budget(){return budget;}
    public double routeLength(){return routeLength;}
    public void reset(){phase=Phase.IDLE;subject=null;overhead=beyond=destination=candidateCenter=heading=null;age=ticks=0;}
    public boolean begin(UUID identity,FlightVector target,double targetTop,FlightVector boss,
            CombatAnchor.Region region,BiPredicate<FlightVector,FlightVector> route){
        if(active()||identity==null||target==null||boss==null||region==null||!region.contains(boss)||route==null||!Double.isFinite(targetTop)||targetTop<target.y())return false;
        var delta=target.subtract(boss);var toward=new FlightVector(delta.x(),0,delta.z());double distance=toward.length();
        if(distance<8||distance>16)return false;
        double centerY=boss.y()>=targetTop+4?boss.y():targetTop+6;
        // Rise well above the future region, leaving an actual gentle descent before handoff.
        double height=Math.max(centerY+12,targetTop+16+.75);
        var above=new FlightVector(boss.x(),height,boss.z());
        var crossed=new FlightVector(target.x(),height,target.z()).add(toward.normalized().scale(8));
        var center=new FlightVector(crossed.x(),centerY,crossed.z());
        var entry=center.add(new FlightVector(0,CombatAnchor.RADII.y()*.85,0));
        double climb=above.subtract(boss).length(),cross=crossed.subtract(above).length();
        double length=climb+cross+crossed.subtract(entry).length();
        int conservative=12+Math.max((int)Math.ceil(climb/.12)+5,48)+(int)Math.ceil(cross/.24)+5
                +48+(int)Math.ceil((height-centerY-CombatAnchor.RADII.y()*.95)/.12)+5+12;
        if(length>48||conservative>320||!route.test(boss,above)||!route.test(above,crossed)||!route.test(crossed,entry))return false;
        subject=identity;generation=region.generation();overhead=above;beyond=crossed;candidateCenter=center;destination=entry;heading=toward.normalized();budget=conservative;
        routeLength=length;age=ticks=0;phase=Phase.TELL;return true;
    }
    public void abort(){if(active()&&phase!=Phase.ABORT){phase=Phase.ABORT;ticks=0;}}
    public State step(UUID identity,boolean visible,CombatAnchor.Region region,FlightVector boss,
            FlightVector velocity,float yaw,float pitch,FlightVector observedLook,BiPredicate<FlightVector,FlightVector> route){
        if(!active())return new State(phase,FlightController.Intent.hold(),null,false);
        if(phase!=Phase.ABORT&&(!subject.equals(identity)||!visible||observedLook==null||region==null||region.generation()!=generation||age>=308))abort();
        if(phase!=Phase.ABORT){
            if(route==null||!route.test(boss,point()))abort();
        }
        boolean commit=false;
        if(phase==Phase.CLIMB&&boss.subtract(overhead).length()<=.7){phase=Phase.ALIGN;ticks=0;}
        if(phase==Phase.ALIGN){if(aligned(yaw,pitch,heading,15)){phase=Phase.CROSS;ticks=0;}else if(ticks>=48)abort();}
        if(phase==Phase.CROSS&&boss.subtract(beyond).length()<=.7){phase=Phase.BRAKE;ticks=0;}
        if(phase==Phase.BRAKE&&velocity.length()<=.04){
            if(boss.subtract(beyond).length()<=.7){phase=Phase.TURN;ticks=0;}
            else abort();
        }
        if(phase==Phase.TURN){if(aligned(yaw,pitch,observedLook,8)){phase=Phase.DESCEND;ticks=0;}else if(ticks>=48)abort();}
        // Validate the newly selected leg before its movement, including a blocked descent.
        if(phase!=Phase.ABORT&&!route.test(boss,point()))abort();
        if(phase==Phase.DESCEND){
            var candidate=new CombatAnchor.Region(candidateCenter,CombatAnchor.RADII,generation+1,CombatAnchor.Reason.TACTICAL_RELOCATION);
            if(candidate.radius(boss)<=.95&&route.test(boss,boss)){phase=Phase.HANDOFF;commit=true;}
        }
        var emittedPhase=phase;
        FlightController.Intent intent;
        if(phase==Phase.TELL||phase==Phase.CLIMB||phase==Phase.CROSS||phase==Phase.DESCEND||phase==Phase.HANDOFF){
            var offset=point().subtract(boss);
            double speed=Math.min(phase==Phase.TELL?.1:phase==Phase.CROSS?.24:.12,Math.sqrt(2*FlightController.BRAKING*Math.max(0,offset.length()-.4)));
            intent=FlightController.Intent.move(offset,speed);
        }else intent=FlightController.Intent.brake();
        var look=phase==Phase.TURN||phase==Phase.DESCEND||phase==Phase.HANDOFF?observedLook:phase==Phase.ABORT?null:heading;
        age++;ticks++;
        if(phase==Phase.TELL&&ticks>=12){phase=Phase.CLIMB;ticks=0;}
        else if(phase==Phase.ABORT&&ticks>=12)phase=Phase.DONE;
        return new State(emittedPhase,intent,look,commit);
    }
    private FlightVector point(){return switch(phase){case TELL,CLIMB,ALIGN -> overhead;case CROSS,BRAKE,TURN -> beyond;default -> destination;};}
    private static boolean aligned(float yaw,float pitch,FlightVector direction,float limit){
        if(direction==null||direction.length()<1e-9)return false;
        float desiredYaw=(float)Math.toDegrees(-Math.atan2(direction.x(),direction.z()));
        float desiredPitch=(float)Math.toDegrees(-Math.atan2(direction.y(),Math.hypot(direction.x(),direction.z())));
        return Math.abs(FlightOrientation.approachDegrees(yaw,desiredYaw,360)-yaw)<=limit
                &&Math.abs(FlightOrientation.approachDegrees(pitch,desiredPitch,360)-pitch)<=limit;
    }
}

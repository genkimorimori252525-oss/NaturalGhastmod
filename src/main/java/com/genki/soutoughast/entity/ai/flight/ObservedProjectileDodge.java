package com.genki.soutoughast.entity.ai.flight;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Finite, fallible observed-motion evasion. No trajectory, target-input or velocity writes. */
public final class ObservedProjectileDodge {
    public record Observation(UUID id,FlightVector center,FlightVector halfSize) {
        public Observation {
            if(id==null||center==null||halfSize==null||halfSize.x()<0||halfSize.y()<0||halfSize.z()<0
                    ||halfSize.x()>2||halfSize.y()>2||halfSize.z()>2)throw new IllegalArgumentException("Invalid projectile observation");
        }
    }
    public enum Phase { IDLE, EVADE, RECOVER, ABORT }
    public record State(Phase phase,UUID projectile,FlightVector waypoint,long startTick,double impactTicks) {}
    private State state=new State(Phase.IDLE,null,null,-1,Double.NaN);
    private final Map<UUID,Observation> previous=new LinkedHashMap<>();
    private final Map<UUID,Long> attempted=new LinkedHashMap<>();
    private UUID subject;
    private long generation=-1,lastTick=-1,sampleTick=-1,quietUntil;
    public State state(){return state;}
    public boolean active(){return state.phase()==Phase.EVADE;}
    public void reset(){previous.clear();attempted.clear();subject=null;generation=lastTick=sampleTick=-1;quietUntil=0;state=new State(Phase.IDLE,null,null,-1,Double.NaN);}

    /** Null observations denote an unsampled tick, not permission to reuse hidden geometry. */
    public MovementPlanner.Plan step(long tick,UUID identity,CombatAnchor.Region region,FlightVector boss,
            FlightVector velocity,MobilityContext.Kind context,boolean eligible,List<Observation> observations,
            double variation,BiPredicate<FlightVector,FlightVector> route){
        if(tick<0||!Double.isFinite(variation)||variation<0||variation>=1
                ||observations!=null&&observations.size()>8)throw new IllegalArgumentException("Invalid dodge sample");
        boolean wasActive=active();
        if(lastTick>=0&&tick!=lastTick+1){previous.clear();sampleTick=-1;if(wasActive)return cancel(tick,boss);}
        lastTick=tick;
        if(region==null||identity==null||!identity.equals(subject)||region.generation()!=generation){
            reset();lastTick=tick;subject=identity;generation=region==null?-1:region.generation();
            if(wasActive)return cancel(tick,boss);
        }
        if(!eligible||region==null||context==MobilityContext.Kind.GROUND_FORCED||!region.contains(boss)){
            previous.clear();sampleTick=-1;return active()?cancel(tick,boss):null;
        }
        if(active()){
            if(route==null||!route.test(boss,state.waypoint()))return cancel(tick,boss);
            if(tick-state.startTick()>=24||state.waypoint().subtract(boss).length()<=.7){
                state=new State(Phase.RECOVER,state.projectile(),state.waypoint(),state.startTick(),state.impactTicks());
                previous.clear();sampleTick=-1;return null;
            }
            return moving(boss,context);
        }
        if(observations==null)return null;
        attempted.entrySet().removeIf(e->tick-e.getValue()>600);
        Observation threat=null;FlightVector motion=null;double earliest=Double.POSITIVE_INFINITY;
        if(sampleTick>=0&&tick-sampleTick>=1&&tick-sampleTick<=2&&tick>=quietUntil){
            for(var current:observations){
                var before=previous.get(current.id());if(before==null||attempted.containsKey(current.id()))continue;
                var observed=current.center().subtract(before.center()).scale(1.0/(tick-sampleTick));
                if(observed.length()<.05||observed.length()>4)continue;
                var relative=observed.subtract(velocity);
                double impact=impactTime(current.center().subtract(boss.add(new FlightVector(0,2,0))),relative,current.halfSize());
                if(Double.isFinite(impact)&&impact<earliest){earliest=impact;threat=current;motion=relative;}
            }
        }
        previous.clear();for(var observation:observations)previous.put(observation.id(),observation);sampleTick=tick;
        if(threat==null)return null;
        if(attempted.size()>=32)attempted.remove(attempted.keySet().iterator().next());
        attempted.put(threat.id(),tick);quietUntil=tick+120;
        // One attempt per observed threat, including a deliberate miss or unavailable route.
        if(variation>=.7||route==null)return null;
        var lateral=new FlightVector(-motion.z(),0,motion.x()).normalized();
        if(lateral.length()<1e-9)lateral=new FlightVector(1,0,0);
        double length=context==MobilityContext.Kind.CONFINED?2:4;
        int side=variation<.35?-1:1;
        for(int direction:new int[]{side,-side}){
            var waypoint=boss.add(lateral.scale(length*direction));
            if(region.radius(waypoint)>.9||!route.test(boss,waypoint))continue;
            state=new State(Phase.EVADE,threat.id(),waypoint,tick,earliest);previous.clear();sampleTick=-1;
            return moving(boss,context);
        }
        return null;
    }

    /** Swept point against observed projectile size + full4x4boss; linear12tick hypothesis only. */
    public static double impactTime(FlightVector offset,FlightVector relative,FlightVector projectileHalf){
        if(offset.dot(relative)>=0)return Double.NaN;
        double enter=0,exit=12;
        double[] p={offset.x(),offset.y(),offset.z()},v={relative.x(),relative.y(),relative.z()},
                half={2.25+projectileHalf.x(),2.25+projectileHalf.y(),2.25+projectileHalf.z()};
        boolean outside=false;
        for(int i=0;i<3;i++){
            outside|=Math.abs(p[i])>half[i];
            if(Math.abs(v[i])<1e-9){if(Math.abs(p[i])>half[i])return Double.NaN;continue;}
            double a=(-half[i]-p[i])/v[i],b=(half[i]-p[i])/v[i];
            enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));
            if(enter>exit)return Double.NaN;
        }
        return outside&&enter>0&&enter<=12&&exit>=enter?enter:Double.NaN;
    }
    private MovementPlanner.Plan moving(FlightVector boss,MobilityContext.Kind context){
        var error=state.waypoint().subtract(boss);
        double speed=Math.min(context==MobilityContext.Kind.CONFINED?.25:.42,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.4)));
        return new MovementPlanner.Plan(MovementPrimitive.STRAFE,FlightController.Intent.move(error,speed),state.waypoint(),CombatAnchor.Range.COMFORTABLE,true);
    }
    private MovementPlanner.Plan cancel(long tick,FlightVector boss){
        previous.clear();sampleTick=-1;lastTick=tick;quietUntil=Math.max(quietUntil,tick+120);
        state=new State(Phase.ABORT,state.projectile(),state.waypoint(),state.startTick(),state.impactTicks());
        return new MovementPlanner.Plan(MovementPrimitive.BRAKE,FlightController.Intent.brake(),boss,CombatAnchor.Range.COMFORTABLE,false);
    }
}

package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Rare learned return-face dodge. Never redirects a projectile or writes actual motion. */
public final class RallyFeint {
    public enum Phase { IDLE, TELL, EVADE, RECOVER, ABORT }
    private record Success(UUID projectile,long tick) {}
    public record State(Phase phase,UUID projectile,FlightVector waypoint,long started,long generation) {}
    private final ArrayDeque<Success> successes=new ArrayDeque<>();
    private UUID subject,lastProjectile;
    private long quietUntil,lastTick=-1;
    private State state=new State(Phase.IDLE,null,null,-1,-1);
    public State state(){return state;}
    public boolean active(){return state.phase()==Phase.TELL||state.phase()==Phase.EVADE;}
    public boolean face(){return state.phase()==Phase.TELL;}
    public void reset(){successes.clear();subject=lastProjectile=null;quietUntil=0;lastTick=-1;state=new State(Phase.IDLE,null,null,-1,-1);}
    public void observeSubject(UUID identity,long tick){
        if(tick<0)throw new IllegalArgumentException("RALLY_FEINT_TICK");
        if(identity==null||!identity.equals(subject)||lastTick>tick){reset();subject=identity;}
        lastTick=tick;expire(tick);
    }
    /** Only call after the native ordinary return operation actually succeeded. */
    public void ordinaryReturn(UUID projectile,long tick){
        if(subject==null||projectile==null||tick!=lastTick)throw new IllegalArgumentException("RALLY_FEINT_SUCCESS_CONTEXT");
        expire(tick);if(successes.stream().anyMatch(s->s.projectile().equals(projectile)))return;
        if(successes.size()==2)successes.removeFirst();successes.addLast(new Success(projectile,tick));
    }
    private void expire(long tick){successes.removeIf(s->tick-s.tick()>600);}
    public boolean learned(long tick){expire(tick);return subject!=null&&successes.size()==2&&tick>=quietUntil;}
    public boolean begin(UUID projectile,long tick,FlightVector boss,FlightVector incomingVelocity,
            CombatAnchor.Region region,MobilityContext.Kind context,double variation,BiPredicate<FlightVector,FlightVector> route){
        if(!Double.isFinite(variation)||variation<0||variation>=1)throw new IllegalArgumentException("RALLY_FEINT_VARIATION");
        if(active()||tick!=lastTick||!learned(tick)||projectile==null||projectile.equals(lastProjectile)||variation>=.1
                ||region==null||!region.contains(boss)||context==MobilityContext.Kind.GROUND_FORCED
                ||incomingVelocity==null||incomingVelocity.length()<.05||route==null)return false;
        var lateral=new FlightVector(-incomingVelocity.z(),0,incomingVelocity.x()).normalized();
        if(lateral.length()<1e-9)return false;
        double distance=context==MobilityContext.Kind.CONFINED?2:4;
        int side=variation<.05?-1:1;
        for(int direction:new int[]{side,-side}){
            var waypoint=boss.add(lateral.scale(distance*direction));
            if(region.radius(waypoint)>.9||!route.test(boss,waypoint))continue;
            state=new State(Phase.TELL,projectile,waypoint,tick,region.generation());lastProjectile=projectile;quietUntil=tick+600;return true;
        }
        return false;
    }
    /** TELL preserves ordinary drift; the revealed frozen dodge has a finite24-tick leg. */
    public MovementPlanner.Plan step(long tick,boolean eligible,boolean observedIncoming,CombatAnchor.Region region,
            FlightVector boss,MobilityContext.Kind context,BiPredicate<FlightVector,FlightVector> route){
        if(!active())return null;
        if(tick!=lastTick||!eligible||region==null||state.generation()!=region.generation()||!region.contains(boss)
                ||context==MobilityContext.Kind.GROUND_FORCED||route==null||!route.test(boss,state.waypoint())
                ||state.phase()==Phase.TELL&&!observedIncoming)return cancel(boss);
        long age=tick-state.started();
        if(age<0||age>=30)return cancel(boss);
        if(age<6)return null;
        state=new State(Phase.EVADE,state.projectile(),state.waypoint(),state.started(),state.generation());
        var error=state.waypoint().subtract(boss);
        if(error.length()<=.7){state=new State(Phase.RECOVER,state.projectile(),state.waypoint(),state.started(),state.generation());return null;}
        double speed=Math.min(context==MobilityContext.Kind.CONFINED?.375:.63,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.4)));
        return new MovementPlanner.Plan(MovementPrimitive.STRAFE,FlightController.Intent.move(error,speed),state.waypoint(),CombatAnchor.Range.COMFORTABLE,true);
    }
    private MovementPlanner.Plan cancel(FlightVector boss){state=new State(Phase.ABORT,state.projectile(),state.waypoint(),state.started(),state.generation());return new MovementPlanner.Plan(MovementPrimitive.BRAKE,FlightController.Intent.brake(),boss,CombatAnchor.Range.COMFORTABLE,false);}
}

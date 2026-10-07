package com.genki.soutoughast.entity.ai.flight;

/** Observed-only charge grammar. No movement, world access or projectile mutation. */
public final class StandardAttack {
    public enum Phase { IDLE, CHARGE, RECOVER }
    public record State(Phase phase,int ticks,FlightVector direction,boolean fire,boolean face) {}
    private Phase phase=Phase.IDLE;
    private int ticks,quiet=60;
    private FlightVector direction=FlightVector.ZERO;
    private State state=new State(Phase.IDLE,0,FlightVector.ZERO,false,false);
    public State state(){return state;}
    public void invalidateTarget(){if(phase!=Phase.RECOVER)reset();}
    public void reset(){phase=Phase.IDLE;ticks=0;quiet=60;direction=FlightVector.ZERO;publish(false);}
    public State step(FlightVector observedAim,boolean eligible){
        boolean valid=eligible&&observedAim!=null&&observedAim.length()>1e-9;
        if(phase==Phase.RECOVER){if(++ticks>=20){phase=Phase.IDLE;ticks=0;quiet=80;}return publish(false);}
        if(phase==Phase.CHARGE){
            if(!valid){reset();return state;}
            if(ticks<19)direction=observedAim.normalized();
            if(++ticks==30){phase=Phase.RECOVER;ticks=0;return publish(true);}
            return publish(false);
        }
        if(quiet>0)quiet--;
        if(valid&&quiet==0){phase=Phase.CHARGE;ticks=0;direction=observedAim.normalized();}
        return publish(false);
    }
    private State publish(boolean fire){return state=new State(phase,ticks,direction,fire,phase!=Phase.IDLE);}
}

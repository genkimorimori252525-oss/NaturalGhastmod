package com.genki.soutoughast.entity.ai.flight;

/** Independent Ground single-shot grammar; normal air Standard timing is untouched. */
public final class GroundAttack {
    public enum Phase { IDLE, CHARGE, RECOVER }
    public record State(Phase phase,int ticks,FlightVector direction,boolean fire,boolean face) {}
    private Phase phase=Phase.IDLE;
    private int ticks,quiet=20;
    private FlightVector direction=FlightVector.ZERO;
    private State state=new State(phase,0,direction,false,false);
    public State state(){return state;}
    public void reset(){phase=Phase.IDLE;ticks=0;quiet=20;direction=FlightVector.ZERO;publish(false);}
    public void invalidateTarget(){if(phase!=Phase.RECOVER)reset();}
    public State step(FlightVector aim,boolean eligible){
        boolean valid=eligible&&aim!=null&&aim.length()>1e-9;
        if(phase==Phase.RECOVER){if(++ticks>=6){phase=Phase.IDLE;ticks=0;quiet=20;}return publish(false);}
        if(phase==Phase.CHARGE){
            if(!valid){reset();return state;}
            if(ticks<4)direction=aim.normalized();
            if(++ticks==8){phase=Phase.RECOVER;ticks=0;return publish(true);}
            return publish(false);
        }
        if(quiet>0)quiet--;
        if(valid&&quiet==0){phase=Phase.CHARGE;ticks=0;direction=aim.normalized();}
        return publish(false);
    }
    private State publish(boolean fire){return state=new State(phase,ticks,direction,fire,phase!=Phase.IDLE);}
}

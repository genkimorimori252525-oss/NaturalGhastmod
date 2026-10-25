package com.genki.soutoughast.entity.ai.flight;

/** Observed-only charge grammar. No movement, world access or projectile mutation. */
public final class StandardAttack {
    public enum Phase { IDLE, CHARGE, RECOVER, DECLINE }
    public record State(Phase phase,int ticks,FlightVector direction,boolean fire,boolean face) {}
    private Phase phase=Phase.IDLE;
    private int ticks,quiet=60,decisions;
    private boolean coverCycle;
    private StandardShotRhythm.Cycle cycle=StandardShotRhythm.DEFAULT;
    private StandardShotRhythm.Outcome outcome=StandardShotRhythm.Outcome.UNDECIDED;
    private FlightVector direction=FlightVector.ZERO;
    private State state=new State(Phase.IDLE,0,FlightVector.ZERO,false,false);
    public State state(){return state;}
    public boolean readyForCycle(){return phase==Phase.IDLE&&quiet<=1;}
    public int decisionsEvaluated(){return decisions;}
    public StandardShotRhythm.Outcome lastOutcome(){return outcome;}
    public boolean coverCycle(){return coverCycle;}
    public boolean aiming(){return !coverCycle&&(phase==Phase.CHARGE&&ticks>=14||phase==Phase.RECOVER&&ticks<6||phase==Phase.DECLINE);}
    public void delayInitial(int value){if(phase!=Phase.IDLE||value<30||value>60)throw new IllegalArgumentException("INITIAL_SHOT_DELAY");quiet=value;}
    public void invalidateTarget(){if(phase!=Phase.RECOVER)reset();}
    public void reset(){phase=Phase.IDLE;ticks=0;quiet=60;decisions=0;coverCycle=false;cycle=StandardShotRhythm.DEFAULT;outcome=StandardShotRhythm.Outcome.UNDECIDED;direction=FlightVector.ZERO;publish(false);}
    /** Cancel without consuming a second tick or rerolling the existing opportunity deadline. */
    public void cancelCharge(){if(phase==Phase.CHARGE){phase=Phase.IDLE;ticks=0;coverCycle=false;direction=FlightVector.ZERO;outcome=StandardShotRhythm.Outcome.SAFETY_CANCEL;publish(false);}}
    /** Caller validates a fresh cover Lob first; preserve its separate full grammar. */
    public boolean beginCover(FlightVector observedAim){
        if(phase!=Phase.IDLE||observedAim==null||observedAim.length()<=1e-9)return false;
        phase=Phase.CHARGE;ticks=0;quiet=130;coverCycle=true;direction=observedAim.normalized();publish(false);return true;
    }
    public State step(FlightVector observedAim,boolean eligible){return step(observedAim,eligible,StandardShotRhythm.DEFAULT,StandardShotRhythm.CLEAR);}
    public State step(FlightVector observedAim,boolean eligible,StandardShotRhythm.Cycle candidate,StandardShotRhythm.Context context){
        if(quiet>0)quiet--;
        boolean valid=eligible&&observedAim!=null&&observedAim.length()>1e-9;
        if(phase==Phase.RECOVER||phase==Phase.DECLINE){
            if(++ticks>=(phase==Phase.RECOVER?20:4)){phase=Phase.IDLE;ticks=0;coverCycle=false;}
            return publish(false);
        }
        if(phase==Phase.CHARGE){
            if(!valid){cancelCharge();return state;}
            if(ticks<19)direction=observedAim.normalized();
            if(++ticks==19&&!coverCycle){
                decisions++;outcome=StandardShotRhythm.decide(cycle,context);
                if(outcome==StandardShotRhythm.Outcome.SAFETY_CANCEL){cancelCharge();return state;}
                if(outcome==StandardShotRhythm.Outcome.DECLINE){phase=Phase.DECLINE;ticks=0;return publish(false);}
            }
            if(ticks==30){phase=Phase.RECOVER;ticks=0;return publish(true);}
            return publish(false);
        }
        if(valid&&quiet==0){
            cycle=candidate==null?StandardShotRhythm.DEFAULT:candidate;quiet=cycle.interval();coverCycle=false;
            outcome=StandardShotRhythm.Outcome.UNDECIDED;phase=Phase.CHARGE;ticks=0;direction=observedAim.normalized();
        }
        return publish(false);
    }
    private State publish(boolean fire){return state=new State(phase,ticks,direction,fire,phase!=Phase.IDLE);}
}

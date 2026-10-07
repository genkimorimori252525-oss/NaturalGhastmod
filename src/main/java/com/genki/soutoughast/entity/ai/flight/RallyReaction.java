package com.genki.soutoughast.entity.ai.flight;

/** Short fallible reaction, locked aim and finite reach window. */
public final class RallyReaction {
    public record State(int projectileId,int ticks,FlightVector direction,boolean fire,boolean face) {}
    private State state=new State(-1,0,FlightVector.ZERO,false,false);
    public State state(){return state;}
    public void reset(){state=new State(-1,0,FlightVector.ZERO,false,false);}
    public boolean begin(int id,FlightVector direction,double variation){
        if(state.face()||id==state.projectileId()||!Double.isFinite(variation)||variation>=.65
                ||direction==null||direction.length()<1e-9)return false;
        state=new State(id,0,direction.normalized(),false,true);return true;
    }
    public State step(boolean visibleIncoming,boolean inReach){
        if(!state.face())return state=new State(state.projectileId(),state.ticks(),state.direction(),false,false);
        int age=state.ticks()+1;
        boolean fire=visibleIncoming&&inReach&&age>=6&&age<=12;
        state=new State(state.projectileId(),age,state.direction(),fire,visibleIncoming&&!fire&&age<12);
        return state;
    }
    public static boolean incoming(FlightVector offset,FlightVector velocity){
        return velocity.length()>.05&&offset.dot(velocity)<-.05;
    }
}

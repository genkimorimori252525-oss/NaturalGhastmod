package com.genki.soutoughast.entity.ai.domain;

/** Reserved tell -> placement -> supported landing -> combat -> durable restore -> exit. */
public final class DomainLifecycle {
 public enum Phase {AIR,START,PLACING,LANDING,COMBAT,ENDING,TAKEOFF,EXIT_HOLD}
 public record Input(boolean targetValid,boolean geometryValid,boolean placed,boolean grounded,
                     boolean durableTerminal,boolean flightDone,boolean movementFailed) {}
 public record State(Phase phase,int ticks,String reason,boolean armPlacement,boolean beginLanding,
                     boolean requestRestore,boolean offenseAllowed,boolean requestTakeoff) {}
 private Phase phase=Phase.AIR;private int ticks;private boolean exitStarted;
 private State state=publish("NONE",false,false,false,false);
 public State state(){return state;}
 public boolean active(){return phase!=Phase.AIR;}
 public void begin(){if(active())throw new IllegalStateException("DOMAIN_ALREADY_ACTIVE");phase=Phase.START;ticks=0;exitStarted=false;publish("RESERVED_TELL",false,false,false,false);}
 public State abort(String reason){
  if(phase==Phase.START||phase==Phase.PLACING||phase==Phase.LANDING||phase==Phase.COMBAT){phase=Phase.ENDING;ticks=0;return publish(reason,false,false,true,false);}
  return publish(reason,false,false,false,false);
 }
 public State step(Input in){
  if(!active())return state;
  if(phase==Phase.START||phase==Phase.PLACING||phase==Phase.LANDING||phase==Phase.COMBAT){
   if(!in.targetValid()||!in.geometryValid()||in.movementFailed())return abort("PARTICIPANT_OR_GEOMETRY_INVALID");
  }
  ticks=Math.min(10000,ticks+1);
  switch(phase){
   case START->{if(ticks>=60){phase=Phase.PLACING;ticks=0;return publish("TELL_COMPLETE",true,false,false,false);}}
   case PLACING->{
    if(in.placed()){phase=DomainLifecycle.Phase.LANDING;ticks=0;return publish("PLACED",false,true,false,false);}
    if(ticks>320)return abort("PLACEMENT_TIMEOUT");
   }
   case LANDING->{
    if(in.grounded()){phase=Phase.COMBAT;ticks=0;return publish("SUPPORTED_LANDING",false,false,false,false);}
    if(ticks>160)return abort("LANDING_TIMEOUT");
   }
   case COMBAT->{if(!in.grounded())return abort("SUPPORT_LOST");if(ticks>=400)return abort("COMBAT_EXPIRED");}
   case ENDING,EXIT_HOLD->{
    if(in.durableTerminal()&&!exitStarted){exitStarted=true;phase=Phase.TAKEOFF;ticks=0;return publish("DURABLE_EXIT",false,false,false,true);}
    if(exitStarted&&in.durableTerminal()&&in.flightDone()){phase=Phase.AIR;ticks=0;return publish("EXIT_COMPLETE",false,false,false,false);}
    if(phase==Phase.ENDING&&ticks>400){phase=Phase.EXIT_HOLD;ticks=0;}
   }
   case TAKEOFF->{
    if(in.durableTerminal()&&in.flightDone()){phase=Phase.AIR;ticks=0;return publish("EXIT_COMPLETE",false,false,false,false);}
    if(in.movementFailed()||ticks>160){phase=Phase.EXIT_HOLD;ticks=0;return publish("EXIT_BLOCKED",false,false,false,false);}
   }
   default->{}
  }
  return publish(state.reason(),false,false,false,false);
 }
 private State publish(String reason,boolean arm,boolean land,boolean restore,boolean takeoff){return state=new State(phase,ticks,reason,arm,land,restore,phase==Phase.COMBAT,takeoff);}
}

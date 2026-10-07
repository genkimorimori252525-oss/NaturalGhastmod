package com.genki.soutoughast.entity.ai.flight;

import java.util.function.BiPredicate;

/** Confirmed support -> bounded landing/legs/takeoff. Never writes position or velocity. */
public final class GroundCombat {
    public enum Phase { AIR, LANDING, GROUNDED, TAKEOFF, SAFE_HOLD }
    public record State(Phase phase,int ticks,FlightController.Intent intent,String reason,boolean taunt) {}
    private Phase phase=Phase.AIR;
    private int ticks,pause,clearSamples,legTicks,memory,tauntLegs;
    private FlightVector goal;
    private boolean left;
    private State state=new State(phase,0,FlightController.Intent.hold(),"NONE",false);
    public State state(){return state;}
    public boolean active(){return phase!=Phase.AIR;}
    public boolean grounded(){return phase==Phase.GROUNDED;}
    public void invalidateTarget(){goal=phase==Phase.GROUNDED?null:goal;tauntLegs=0;pause=12;}
    /** Query-only landing contact allowance. Actual velocity/native collision stay unchanged. */
    public FlightVector landingSweep(FlightVector position,FlightVector prediction,
            BiPredicate<FlightVector,FlightVector> loadedBody,BiPredicate<FlightVector,FlightVector> freshSupport){
        if(phase!=Phase.LANDING||goal==null||position.y()<goal.y()||prediction.y()>=0||Math.abs(Math.rint(goal.y())-goal.y())>1e-6)return prediction;
        var clipped=new FlightVector(prediction.x(),Math.max(prediction.y(),goal.y()-position.y()),prediction.z());
        var floorStart=new FlightVector(position.x(),goal.y(),position.z());
        var floorEnd=floorStart.add(new FlightVector(prediction.x(),0,prediction.z()));
        return freshSupport.test(floorStart,floorEnd)&&loadedBody.test(position,position.add(clipped))?clipped:prediction;
    }
    public void reset(){phase=Phase.AIR;goal=null;ticks=pause=clearSamples=legTicks=memory=tauntLegs=0;publish(FlightController.Intent.hold(),"RESET");}
    public boolean begin(FlightVector position,FlightVector floor,BiPredicate<FlightVector,FlightVector> descent){
        if(active()&&phase!=Phase.SAFE_HOLD||floor==null)return false;
        var delta=position.subtract(floor);
        if(Math.hypot(delta.x(),delta.z())>1e-6||delta.y()<-.05||delta.y()>8||!descent.test(position,floor))return false;
        phase=Phase.LANDING;goal=floor;ticks=clearSamples=0;publish(FlightController.Intent.hold(),"VERIFIED_DESCENT");return true;
    }
    public State step(FlightVector position,FlightVector observedTarget,CombatAnchor.Region region,boolean clearForFlight,
                      FlightVector ascent,BiPredicate<FlightVector,FlightVector> bodyRoute,
                      BiPredicate<FlightVector,FlightVector> supportedRoute,double variation){
        if(!active())return state;
        ticks++;if(memory>0)memory--;
        if(phase==Phase.LANDING){
            if(ticks>160||!bodyRoute.test(position,goal)||!supportedRoute.test(goal,goal))return safe("LANDING_INVALID_OR_TIMEOUT");
            if(position.subtract(goal).length()<=.02){phase=Phase.GROUNDED;ticks=0;pause=12;goal=null;return publish(FlightController.Intent.brake(),"LANDED");}
            return publish(toward(position,goal,.3),"LANDING");
        }
        if(phase==Phase.TAKEOFF){
            if(ticks>120||!bodyRoute.test(position,goal))return safe("TAKEOFF_INVALID_OR_TIMEOUT");
            if(position.subtract(goal).length()<=.2){reset();return state;}
            return publish(toward(position,goal,.35),"TAKEOFF");
        }
        boolean usableAscent=clearForFlight&&ascent!=null&&ascent.y()>position.y()
                &&ascent.subtract(position).length()<=4.001&&bodyRoute.test(position,ascent);
        clearSamples=usableAscent?clearSamples+1:0;
        if(clearSamples>=20){phase=Phase.TAKEOFF;ticks=clearSamples=0;goal=ascent;return publish(FlightController.Intent.brake(),"CLEAR_DWELL_COMPLETE");}
        if(phase==Phase.SAFE_HOLD)return publish(FlightController.Intent.brake(),"SAFE_HOLD");
        if(!supportedRoute.test(position,position))return safe("SUPPORT_LOST");
        if(observedTarget==null||region==null){goal=null;tauntLegs=0;pause=12;return publish(FlightController.Intent.brake(),"TARGET_UNOBSERVED");}
        if(pause>0){pause--;return publish(FlightController.Intent.brake(),"READABLE_PAUSE");}
        if(goal!=null){
            if(!supportedRoute.test(position,goal)){goal=null;tauntLegs=0;pause=12;return publish(FlightController.Intent.brake(),"SWEEP_INVALID");}
            if(++legTicks>100||position.subtract(goal).length()<.7){goal=null;pause=10;return publish(FlightController.Intent.brake(),"LEG_COMPLETE");}
            return publish(toward(position,goal,tauntLegs>0&&legTicks<=12?.65:.55),"SCUTTLE");
        }
        var bearing=observedTarget.subtract(position);bearing=new FlightVector(bearing.x(),0,bearing.z()).normalized();
        if(bearing.length()<1e-9)bearing=new FlightVector(0,0,1);
        var side=new FlightVector(-bearing.z(),0,bearing.x());
        if(memory==0&&variation>.97){tauntLegs=3;memory=600;}
        for(int attempt=0;attempt<3;attempt++){
            double sign=(left?1:-1)*(attempt==1?-1:1),distance=attempt==2?4:8;
            var candidate=position.add(side.scale(sign*distance));
            if(!insideXZ(region,candidate)){
                var center=new FlightVector(region.center().x(),position.y(),region.center().z());
                candidate=position.add(center.subtract(position).limited(distance));
            }
            if(!insideXZ(region,candidate)||!supportedRoute.test(position,candidate))continue;
            goal=candidate;left=!left;legTicks=0;if(tauntLegs>0)tauntLegs--;
            return publish(toward(position,goal,tauntLegs>0?.65:.55),"VERIFIED_LEG");
        }
        goal=null;tauntLegs=0;pause=12;return publish(FlightController.Intent.brake(),"NO_SUPPORTED_LEG");
    }
    private static boolean insideXZ(CombatAnchor.Region region,FlightVector p){
        double x=(p.x()-region.center().x())/region.radii().x(),z=(p.z()-region.center().z())/region.radii().z();return x*x+z*z<=1;
    }
    private State safe(String reason){phase=Phase.SAFE_HOLD;goal=null;clearSamples=tauntLegs=0;return publish(FlightController.Intent.brake(),reason);}
    private State publish(FlightController.Intent intent,String reason){return state=new State(phase,ticks,intent,reason,tauntLegs>0);}
    private static FlightController.Intent toward(FlightVector from,FlightVector to,double cap){
        var error=to.subtract(from);if(error.length()<.005)return FlightController.Intent.brake();
        return FlightController.Intent.move(error,Math.min(cap,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.005))));
    }
}

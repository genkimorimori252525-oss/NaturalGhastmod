package com.genki.soutoughast.entity.ai.flight;

/** Damps only ordinary swim intent; no waypoint, position or actual-velocity ownership. */
public final class StandardAimingMotion {
    private double cap=FlightController.MAX_SPEED;
    public void reset(){cap=FlightController.MAX_SPEED;}
    public MovementPlanner.Plan apply(MovementPlanner.Plan plan,boolean aiming){
        if(!plan.inRegion()||plan.intent().mode()!=FlightController.Mode.MOVE||
                plan.primitive()!=MovementPrimitive.DRIFT&&plan.primitive()!=MovementPrimitive.HOLD){reset();return plan;}
        if(aiming)cap=Math.min(cap,plan.intent().speed());
        double target=aiming?.10:FlightController.MAX_SPEED,step=aiming?.04:.025;
        cap+=Math.max(-step,Math.min(step,target-cap));
        double speed=Math.min(plan.intent().speed(),cap);
        if(speed>=plan.intent().speed())return plan;
        return new MovementPlanner.Plan(plan.primitive(),FlightController.Intent.move(plan.intent().direction(),speed),plan.waypoint(),plan.range(),plan.inRegion());
    }
}

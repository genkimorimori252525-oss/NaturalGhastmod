package com.genki.soutoughast.entity.ai.flight;

/** Detects context flicker, unsafe selection, lost quiet rhythm and stale maneuver state. */
public final class MovementMobilityTest {
    private static int checks;
    private static final FlightVector FORWARD = new FlightVector(0, 0, 1);
    private static final FlightVector INSIDE = new FlightVector(0, 6, 28);
    public static void main(String[] args) {
        MobilityContext context = new MobilityContext();
        var open = new MobilityContext.Sample(1023);
        var ground = new MobilityContext.Sample(0);
        for (int i=0;i<19;i++) context.update(open);
        check(context.current()==MobilityContext.Kind.CONFINED,"one transient opening does not unlock broad movement");
        context.update(open);
        check(context.current()==MobilityContext.Kind.OPEN_AIR,"sustained opening unlocks air movement");
        for(int i=0;i<5;i++) context.update(ground);
        check(context.current()==MobilityContext.Kind.OPEN_AIR,"constraint needs sustained evidence");
        context.update(ground);
        check(context.current()==MobilityContext.Kind.GROUND_FORCED,"sustained no-clearance enters safe ground constraint");
        for(int i=0;i<50;i++) context.update(i%2==0?open:ground);
        check(context.current()==MobilityContext.Kind.GROUND_FORCED,"doorway flicker cannot cause repeated takeoffs");
        for(int i=0;i<20;i++) context.update(open);
        check(context.current()==MobilityContext.Kind.OPEN_AIR,"ground recovery also waits sustained open samples");
        context.reset();
        check(context.current()==MobilityContext.Kind.CONFINED,"subject reset clears environment history");
        reject(()->new MobilityContext.Sample(1024));
        CombatAnchor anchor=new CombatAnchor();
        MovementPlanner planner=new MovementPlanner();
        var correction=planner.step(anchor,FlightVector.ZERO,new FlightVector(0,6,8),FORWARD,
                MobilityContext.Kind.OPEN_AIR,open,0.1);
        check(correction.primitive()==MovementPrimitive.WITHDRAW&&correction.intent().mode()==FlightController.Mode.MOVE,
                "too-close correction opens range without replacing the anchor");
        var blocked=planner.step(anchor,FlightVector.ZERO,new FlightVector(0,6,8),FORWARD,
                MobilityContext.Kind.CONFINED,ground,0.9);
        check(blocked.intent().mode()!=FlightController.Mode.MOVE,"unavailable correction does not issue blind thrust");
        planner.reset();
        var hold=planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,0.1);
        check(hold.primitive()==MovementPrimitive.HOLD,"quiet movement remains common");
        int quiet=0;
        for(int i=0;i<20;i++) if(planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,0.99).intent().mode()!=FlightController.Mode.MOVE) quiet++;
        check(quiet==20,"new variation cannot cancel committed quiet interval every tick");
        planner.reset();
        var drift=planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,0.7);
        check(drift.intent().mode()==FlightController.Mode.MOVE,"feasible drift can leave stillness");
        check(drift.waypoint().subtract(INSIDE).length()<=4.00001,"drift is bounded around the current region");
        check(anchor.evaluate(FlightVector.ZERO,FORWARD,drift.waypoint()).inRegion(),"drift endpoint remains frontal and in range");
        var shifted=planner.step(anchor,new FlightVector(0,0,50),INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,0.7);
        check(shifted.primitive()==MovementPrimitive.RETURN,"target displacement abandons stale drift and restores frontal region");
        planner.reset();
        var confined=planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.CONFINED,open,0.7);
        check(confined.waypoint().subtract(INSIDE).length()<=1.50001,"confinement uses compact positive movement");
        var groundPlan=planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.GROUND_FORCED,open,0.7);
        check(groundPlan.intent().mode()!=FlightController.Mode.MOVE,"ground constraint suppresses airborne maneuvers");
        planner.reset();
        check(planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,ground,0.7).intent().mode()!=FlightController.Mode.MOVE,
                "variation cannot override directional clearance");
        reject(()->planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,Double.NaN));
        reject(()->planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,1));
        planner.reset();
        FlightVector tangent=new FlightVector(2,6,Math.sqrt(444.1));
        var tangentDrift=planner.step(anchor,FlightVector.ZERO,tangent,FORWARD,MobilityContext.Kind.OPEN_AIR,open,.85);
        FlightVector firstVelocity=new FlightController().step(FlightVector.ZERO,tangentDrift.intent());
        check(anchor.evaluate(FlightVector.ZERO,FORWARD,tangent.add(firstVelocity)).inRegion(),
                "drift chord cannot cross the inner range hole");
        check(!new MobilityContext.Sample(769).allows(new FlightVector(3,0,8)),
                "one clear cardinal ray cannot certify an unsampled diagonal");
        planner.reset();
        int[] calls={0};
        var denied=planner.step(anchor,FlightVector.ZERO,INSIDE,FORWARD,MobilityContext.Kind.OPEN_AIR,open,.7,
                displacement->{calls[0]++;return false;});
        check(denied.intent().mode()!=FlightController.Mode.MOVE,"actual clearance rejection cannot issue drift thrust");
        check(calls[0]==2,"actual candidate queries stay within the bounded budget");
        System.out.println("PASS: "+checks+" movement/mobility assertions");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void reject(Runnable action){checks++;try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("invalid input accepted");}
}

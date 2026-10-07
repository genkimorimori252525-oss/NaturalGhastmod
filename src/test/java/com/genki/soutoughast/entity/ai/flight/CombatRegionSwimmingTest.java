package com.genki.soutoughast.entity.ai.flight;

/** Physical simulation of the approved region, not a visual-quality certificate. */
public final class CombatRegionSwimmingTest {
    private static int checks;
    private static final FlightVector FORWARD=new FlightVector(0,0,1);
    private static final MobilityContext.Sample OPEN=new MobilityContext.Sample(1023);
    public static void main(String[]args){
        CombatAnchor anchor=new CombatAnchor();
        FlightVector target=FlightVector.ZERO,boss=new FlightVector(0,6,28);
        var initial=anchor.evaluate(target,FORWARD,boss);
        check(anchor.evaluate(target,FORWARD.scale(-1),boss).point().equals(initial.point()),"camera reversal must not relocate the region");
        check(anchor.evaluate(new FlightVector(5,0,4),new FlightVector(0,1,0),boss).point().equals(initial.point()),"ordinary Player motion must not drag the region");
        check(anchor.evaluate(target,FORWARD,boss.add(new FlightVector(15,0,0))).inRegion(),"region allows broad horizontal freedom");
        check(anchor.evaluate(target,FORWARD,boss.add(new FlightVector(0,7,0))).inRegion(),"region allows vertical freedom");
        check(!anchor.evaluate(target,FORWARD,boss.add(new FlightVector(21,0,0))).inRegion(),"region has a finite boundary");
        check(anchor.evaluate(boss,FORWARD,boss).inRegion(),"soft range cannot evict an in-region swimmer");
        lifecycle();
        blockedReturn();
        MovementPlanner planner=new MovementPlanner();FlightController controller=new FlightController();
        FlightVector position=boss,velocity=FlightVector.ZERO;double minX=position.x(),maxX=minX,minY=position.y(),maxY=minY,minZ=position.z(),maxZ=minZ;
        int rest=0;FlightVector firstWaypoint=null;
        for(int tick=0;tick<4000;tick++){
            double variation=((tick*37)%997)/997.0;
            var plan=planner.step(anchor,target,position,tick%2==0?FORWARD:FORWARD.scale(-1),MobilityContext.Kind.OPEN_AIR,OPEN,variation,displacement->true);
            if(tick==0)firstWaypoint=plan.waypoint();
            if(tick<30)check(plan.waypoint().equals(firstWaypoint),"new variation cannot cancel a committed swim every tick");
            FlightVector next=controller.step(velocity,plan.intent());
            check(next.length()<=FlightController.MAX_SPEED+1e-9,"speed remains capped");
            if(tick>20)check(next.subtract(velocity).length()<=.02,"ambient intent eases turns and reversals");
            position=position.add(next);velocity=next;
            check(anchor.evaluate(target,FORWARD,position).inRegion(),"actual swimming remains within the retained region tick="+tick+" radius="+anchor.region().radius(position)+" position="+position+" waypoint="+plan.waypoint());
            if(tick>20&&next.length()<.005)rest++;
            minX=Math.min(minX,position.x());maxX=Math.max(maxX,position.x());minY=Math.min(minY,position.y());maxY=Math.max(maxY,position.y());minZ=Math.min(minZ,position.z());maxZ=Math.max(maxZ,position.z());
        }
        check(Math.max(maxX-minX,maxZ-minZ)>20,"actual swimming spans far more than a10-block neighborhood");
        check(maxY-minY>3,"actual swimming includes vertical motion");
        check(rest<80,"normal swimming does not repeatedly stop");
        int[] queries={0};planner.reset();
        var denied=planner.step(anchor,target,boss,FORWARD,MobilityContext.Kind.OPEN_AIR,OPEN,.8,d->{queries[0]++;return false;});
        check(denied.intent().mode()!=FlightController.Mode.MOVE,"blocked swimming cannot issue thrust");
        check(queries[0]<=2,"candidate checks remain bounded");
        check(planner.step(anchor,target,boss,FORWARD,MobilityContext.Kind.GROUND_FORCED,OPEN,.8).intent().mode()!=FlightController.Mode.MOVE,"ground-forced safety takes priority");
        System.out.println("PASS: "+checks+" region/swimming assertions; span="+Math.max(maxX-minX,maxZ-minZ)+" vertical="+(maxY-minY)+" rest="+rest);
    }
    private static void lifecycle(){
        var anchor=new CombatAnchor();var first=new java.util.UUID(1,1);var second=new java.util.UUID(2,2);
        FlightVector boss=new FlightVector(0,6,28);
        anchor.observe(first,FlightVector.ZERO,boss,false);var original=anchor.region();
        check(original.radii().equals(new FlightVector(20,8,20)),"open region is40x16x40");
        for(int i=0;i<120;i++)anchor.observe(first,new FlightVector(4,0,3),boss,false);
        check(anchor.region().equals(original),"normal displacement preserves exact center and generation");
        for(int i=0;i<100;i++)anchor.unobserved(true);
        check(anchor.region().equals(original),"LOS interruption retains the region without target geometry");
        for(int i=0;i<39;i++)anchor.observe(first,new FlightVector(100,0,0),boss,false);
        check(anchor.region().equals(original),"disengagement requires40 consistent observations");
        anchor.observe(first,new FlightVector(100,0,0),boss,false);
        check(anchor.region().generation()==original.generation()+1&&anchor.region().reason()==CombatAnchor.Reason.DISENGAGED,"sustained disengagement records a reselection reason");
        var relocated=anchor.region();
        for(int i=0;i<99;i++)anchor.observe(first,new FlightVector(-100,0,0),boss,false);
        check(anchor.region().equals(relocated),"cooldown prevents repeated region changes");
        anchor.observe(first,new FlightVector(-100,0,0),boss,false);
        check(anchor.region().generation()==relocated.generation()+1,"cooldown eventually permits necessary relocation");
        anchor.observe(second,FlightVector.ZERO,boss,false);
        check(anchor.region().reason()==CombatAnchor.Reason.REPLACED,"new visible target explicitly replaces encounter");
        var replaced=anchor.region();for(int i=0;i<199;i++)anchor.unobserved(false);
        check(anchor.region().equals(replaced),"short targetless interval retains combat region");
        anchor.unobserved(false);check(anchor.region()==null,"200 targetless ticks clear stale encounter");
        anchor.observe(first,FlightVector.ZERO,boss,false);
        check(anchor.region().generation()>replaced.generation(),"new encounter never reuses an old generation");
        var reacquired=anchor.region();
        for(int i=0;i<100;i++)anchor.observe(first,FlightVector.ZERO,boss,true);
        check(anchor.region().equals(reacquired),"blocked retries cannot reset an identical region");
        for(int i=0;i<100;i++)anchor.observe(first,FlightVector.ZERO,new FlightVector(15,6,28),true);
        check(anchor.region().reason()==CombatAnchor.Reason.SPACE_BLOCKED&&anchor.region().generation()==reacquired.generation()+1,"sustained obstruction can select a different region");
    }
    private static void blockedReturn(){
        var anchor=new CombatAnchor();var planner=new MovementPlanner();var controller=new FlightController();
        var identity=new java.util.UUID(3,3);FlightVector boss=new FlightVector(0,6,4),initial=boss,velocity=FlightVector.ZERO;
        boolean blocked=false;int moving=0;
        for(int tick=0;tick<500;tick++){
            anchor.observe(identity,FlightVector.ZERO,boss,blocked);
            var plan=planner.step(anchor,FlightVector.ZERO,boss,FORWARD,MobilityContext.Kind.OPEN_AIR,OPEN,.8,d->d.z()<=0);
            blocked=plan.primitive()==MovementPrimitive.BRAKE;
            if(plan.intent().mode()==FlightController.Mode.MOVE)moving++;
            velocity=controller.step(velocity,plan.intent());boss=boss.add(velocity);
        }
        check(moving>100&&boss.subtract(initial).length()>2,"blocked RETURN must recover through feasible local swimming instead of permanent braking");
        check(anchor.region().reason()==CombatAnchor.Reason.SPACE_BLOCKED&&anchor.region().contains(boss),"blocked fallback retains a feasible boss-owned region");
    }
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}

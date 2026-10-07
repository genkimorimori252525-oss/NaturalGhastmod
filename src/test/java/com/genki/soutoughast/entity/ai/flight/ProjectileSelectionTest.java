package com.genki.soutoughast.entity.ai.flight;

public final class ProjectileSelectionTest {
    private static int checks;
    public static void main(String[] args){
        var selector=new ProjectileSelector();
        check(selector.choose(MobilityContext.Kind.GROUND_FORCED,50,new FlightVector(.2,0,0),80,.5)==ProjectileSelector.Choice.STANDARD,"ground suppresses special");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,50,FlightVector.ZERO,0,.5)==ProjectileSelector.Choice.BURST,"long range creates burst reaction budget");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,30,new FlightVector(.2,0,0),0,.5)==ProjectileSelector.Choice.CURVE,"observed movement favors lateral problem");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,30,new FlightVector(0,.2,0),0,.5)==ProjectileSelector.Choice.STANDARD,"vertical-only motion is not observed strafe");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,24,FlightVector.ZERO,60,.5)==ProjectileSelector.Choice.LOB,"observed stationarity favors area displacement");
        selector.record(ProjectileSelector.Choice.LOB);
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,24,FlightVector.ZERO,60,.5)==ProjectileSelector.Choice.STANDARD,"recent special is reduced");
        for(int i=0;i<600;i++)selector.tick();
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,24,FlightVector.ZERO,60,.5)==ProjectileSelector.Choice.LOB,"finite memory expires");
        var stationarity=new ObservedStationarity();var position=new FlightVector(9.5,224,3.5);
        check(stationarity.observe(position)==0,"first position has no measured interval");
        // A standing Player may retain downward physics velocity; it is not measured displacement.
        for(int i=0;i<40;i++)stationarity.observe(position);
        check(stationarity.ticks()==40&&stationarity.displacement()==0,"unchanged visible position qualifies independently of gravity velocity");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,24,new FlightVector(0,-.078,0),stationarity.ticks(),.5)==ProjectileSelector.Choice.LOB,"stationary grounded target can favor Lob");
        check(stationarity.observe(position.add(new FlightVector(0,.1,0)))==0,"actual vertical displacement breaks stationarity");
        check(stationarity.observe(null)==0&&stationarity.displacement()==-1,"LOS gap clears history and counter");
        check(stationarity.observe(position)==0,"reacquisition never bridges an unseen gap");
        for(int i=0;i<100;i++)stationarity.observe(position);check(stationarity.ticks()==80,"bounded counter");
        stationarity.reset();check(stationarity.observe(position)==0,"target change has no inherited intervals");
        var path=CommittedTrajectory.lob(new FlightVector(0,8,0),new FlightVector(0,0,30),8);
        check(TrajectoryValidation.verify(path,(i,a,b,last)->last?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR).clear(),"declared final terminal allowed");
        check(!TrajectoryValidation.verify(path,(i,a,b,last)->TrajectoryValidation.Segment.CLEAR).clear(),"Lob requires intended surface");
        for(var invalid:new TrajectoryValidation.Segment[]{TrajectoryValidation.Segment.BLOCKED,TrajectoryValidation.Segment.UNLOADED,TrajectoryValidation.Segment.EXPECTED_TERMINAL})
            check(!TrajectoryValidation.verify(path,(i,a,b,last)->i==1?invalid:last?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR).clear(),"earlier obstruction is never permitted");
        var curve=CommittedTrajectory.curve(new FlightVector(0,8,0),new FlightVector(0,2,30),CommittedTrajectory.Strength.SHALLOW,1);
        check(!TrajectoryValidation.verify(curve,(i,a,b,last)->last?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR).clear(),"undeclared curve block endpoint rejected");
        System.out.println("PASS: "+checks+" projectile selection/finite clearance checks");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

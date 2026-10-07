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

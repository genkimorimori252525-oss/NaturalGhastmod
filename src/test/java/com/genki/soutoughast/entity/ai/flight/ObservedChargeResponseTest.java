package com.genki.soutoughast.entity.ai.flight;

/** Actual helper + selector contracts; no assertion of intent, input or native readability. */
public final class ObservedChargeResponseTest {
    private static int checks;
    private static long clock;
    private static final FlightVector ORIGIN=new FlightVector(0,0,0);
    public static void main(String[] args){
        try{Class.forName("com.genki.soutoughast.entity.ai.flight.ObservedChargeResponse");}
        catch(ClassNotFoundException e){throw new AssertionError("MISSING_OBSERVED_CHARGE_RESPONSE",e);}
        thresholds();lifecycle();selection();
        System.out.println("PASS: "+checks+" observed charge-response assertions");
    }
    private static void thresholds(){
        var m=new ObservedChargeResponse();clock=0;
        episode(m,10,2,.2,4,true);check(m.count()==1&&!m.repeatedTiming(),"one response never learns a deterministic counter");
        episode(m,10,4,.2,4,true);check(m.count()==2&&m.repeatedTiming(),"two completed similarly timed departures provide bounded preference");
        episode(m,10,9,.2,4,true);episode(m,10,10,.2,4,true);check(m.count()==3&&m.repeatedTiming(),"at most three observations; onset10 can finish at13");
        var shortBaseline=new ObservedChargeResponse();episode(shortBaseline,9,2,.2,4,true);check(shortBaseline.count()==0,"ten prior intervals required");
        var exact=new ObservedChargeResponse();episode(exact,10,2,.15,4,true);check(exact.count()==1,"exact .15 speed and .6 displacement thresholds admit four samples");
        for(double step:new double[]{.149,.15}){
            var weak=new ObservedChargeResponse();episode(weak,10,2,step,3,true);check(weak.count()==0,"three samples alone without .6 displacement do not qualify");
        }
        var late=new ObservedChargeResponse();episode(late,10,11,.25,4,true);check(late.count()==0,"onset after10 is not early charge response");
        var canceled=new ObservedChargeResponse();episode(canceled,10,2,.2,4,false);check(canceled.count()==0,"unemitted charge supplies no completed history");
        var different=new ObservedChargeResponse();episode(different,10,1,.2,4,true);episode(different,10,9,.2,4,true);check(!different.repeatedTiming(),"different timing is not a repeated pattern");
        var strafe=new ObservedChargeResponse();for(int i=0;i<11;i++)sample(strafe,new FlightVector(i*.2,0,0),StandardAttack.Phase.IDLE,0,false,false);
        for(int i=0;i<=30;i++)sample(strafe,new FlightVector((i+11)*.2,0,0),i==30?StandardAttack.Phase.RECOVER:StandardAttack.Phase.CHARGE,i==30?0:i,i==30,i==30);
        check(strafe.count()==0,"constant strafe is not reaction timing");
        var vertical=new ObservedChargeResponse();baseline(vertical,10);
        for(int i=0;i<=30;i++)sample(vertical,new FlightVector(0,i*.2,0),i==30?StandardAttack.Phase.RECOVER:StandardAttack.Phase.CHARGE,i==30?0:i,i==30,i==30);
        check(vertical.count()==0,"vertical motion cannot masquerade as horizontal departure");
    }
    private static void lifecycle(){
        var gap=new ObservedChargeResponse();baseline(gap,10);
        sample(gap,ORIGIN,StandardAttack.Phase.CHARGE,0,false,false);sample(gap,null,StandardAttack.Phase.CHARGE,1,false,false);
        for(int i=2;i<=30;i++)sample(gap,new FlightVector(i*.2,0,0),i==30?StandardAttack.Phase.RECOVER:StandardAttack.Phase.CHARGE,i==30?0:i,i==30,i==30);
        check(gap.count()==0,"LOS gap invalidates pending observation");
        var interrupted=new ObservedChargeResponse();baseline(interrupted,10);sample(interrupted,ORIGIN,StandardAttack.Phase.CHARGE,0,false,false);clock+=2;
        for(int i=1;i<=30;i++)sample(interrupted,new FlightVector(i*.2,0,0),i==30?StandardAttack.Phase.RECOVER:StandardAttack.Phase.CHARGE,i==30?0:i,i==30,i==30);
        check(interrupted.count()==0,"native sampling discontinuity cannot bridge ten visible intervals");
        var history=new ObservedChargeResponse();episode(history,10,2,.2,4,true);episode(history,10,3,.2,4,true);check(history.repeatedTiming(),"completed history present");
        sample(history,null,StandardAttack.Phase.IDLE,0,false,false);check(history.count()==2,"LOS clears pending geometry, not valid past same-target records");
        history.tick(clock+600);check(history.count()==0&&!history.repeatedTiming(),"600 actual server ticks expire history even during suspended ordinary attacks");
        episode(history,10,2,.2,4,true);history.reset();check(history.count()==0,"target/lifecycle reset clears all inherited evidence");
        episode(history,10,2,.2,4,true);history.tick(0);check(history.count()==0,"world clock rollback clears observations");
    }
    private static void selection(){
        var selector=new ProjectileSelector();selector.record(ProjectileSelector.Choice.BURST);for(int i=0;i<280;i++)selector.tick();
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,50,FlightVector.ZERO,0,.25)==ProjectileSelector.Choice.STANDARD,"existing neutral selection remains unchanged");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,50,FlightVector.ZERO,0,.25,true)==ProjectileSelector.Choice.BURST,"repeated timing adds bounded context preference");
        check(selector.choose(MobilityContext.Kind.OPEN_AIR,50,FlightVector.ZERO,0,.5,true)==ProjectileSelector.Choice.STANDARD,"variation retains ordinary choice");
        for(var context:MobilityContext.Kind.values())for(double range:new double[]{10,30,46.99})check(selector.choose(context,range,FlightVector.ZERO,0,.25,true)!=ProjectileSelector.Choice.BURST,"response cannot bypass actual range budget");
        selector.record(ProjectileSelector.Choice.BURST);check(selector.choose(MobilityContext.Kind.OPEN_AIR,50,FlightVector.ZERO,0,.25,true)==ProjectileSelector.Choice.STANDARD,"fresh repetition penalty remains authoritative");
        check(selector.choose(MobilityContext.Kind.CONFINED,50,FlightVector.ZERO,0,.25,true)!=ProjectileSelector.Choice.BURST,"confined Burst suppression remains");
        check(selector.choose(MobilityContext.Kind.GROUND_FORCED,50,FlightVector.ZERO,0,.25,true)==ProjectileSelector.Choice.STANDARD,"ground excludes all aerial specials");
    }
    private static void episode(ObservedChargeResponse m,int prior,int onset,double step,int count,boolean emitted){
        baseline(m,prior);double x=0;
        for(int i=0;i<=30;i++){if(i>=onset&&i<onset+count)x+=step;sample(m,new FlightVector(x,0,0),i==30?StandardAttack.Phase.RECOVER:StandardAttack.Phase.CHARGE,i==30?0:i,i==30,i==30&&emitted);}
    }
    private static void baseline(ObservedChargeResponse m,int intervals){sample(m,ORIGIN,StandardAttack.Phase.IDLE,0,false,false);for(int i=0;i<intervals;i++)sample(m,ORIGIN,StandardAttack.Phase.IDLE,0,false,false);}
    private static void sample(ObservedChargeResponse m,FlightVector position,StandardAttack.Phase phase,int tick,boolean fire,boolean emitted){m.tick(clock++);m.observe(position,new StandardAttack.State(phase,tick,new FlightVector(0,0,1),fire,phase!=StandardAttack.Phase.IDLE),emitted);}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}

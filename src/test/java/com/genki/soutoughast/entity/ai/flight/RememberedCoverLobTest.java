package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;

/** Observed-only memory/admission contracts; no native hidden-target/input claim. */
public final class RememberedCoverLobTest {
    private static int checks;
    private static final UUID TARGET=UUID.fromString("00000000-0000-0000-0000-000000000001"),OTHER=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final FlightVector EYE=new FlightVector(30,2,0),LANDING=new FlightVector(30,-.25,0);
    public static void main(String[] args){
        try{StandardAttack.class.getMethod("beginCover",FlightVector.class);}catch(ReflectiveOperationException e){throw new AssertionError("MISSING_FULL_TELL_REMEMBERED_COVER_LOB",e);}
        memory();cancellation();grammar();preference();
        System.out.println("PASS: "+checks+" remembered cover/charge/selection assertions");
    }
    private static void observe(RememberedCoverLob m,UUID id,long tick,boolean visible,MobilityContext.Kind context,boolean blocked){m.observe(id,tick,visible?EYE:null,visible?LANDING:null,context,blocked);}
    private static RememberedCoverLob fresh(){var m=new RememberedCoverLob();observe(m,TARGET,100,true,MobilityContext.Kind.OPEN_AIR,false);return m;}
    private static void memory(){
        var m=fresh();check(m.attempt()==null,"visible target never cover admission");
        observe(m,TARGET,101,false,MobilityContext.Kind.OPEN_AIR,false);var snapshot=m.attempt();
        check(snapshot!=null&&snapshot.eye().equals(EYE)&&snapshot.landing().equals(LANDING)&&snapshot.tick()==100,"only previous visible snapshot");
        check(m.attempt()==null,"single attempt per loss episode");check(m.begin(snapshot)&&m.active(),"admit frozen observed endpoint");
        for(int tick=102;tick<=140;tick++){observe(m,TARGET,tick,false,MobilityContext.Kind.OPEN_AIR,false);check(m.active()&&m.frozen()==snapshot,"hidden ticks never refresh endpoint/clock");}
        observe(m,TARGET,141,false,MobilityContext.Kind.OPEN_AIR,false);check(!m.active()&&m.attempt()==null,"entire attempt expires40ticks from observation");
        observe(m,TARGET,142,true,MobilityContext.Kind.OPEN_AIR,false);observe(m,TARGET,143,false,MobilityContext.Kind.OPEN_AIR,false);check(m.attempt()!=null,"visible reacquisition starts fresh episode");
        m=fresh();for(int tick=101;tick<=110;tick++)observe(m,TARGET,tick,false,MobilityContext.Kind.OPEN_AIR,false);check(m.attempt()!=null,"age10 admitted");
        m=fresh();for(int tick=101;tick<=111;tick++)observe(m,TARGET,tick,false,MobilityContext.Kind.OPEN_AIR,false);check(m.attempt()==null,"age11 cannot admit");
        m=fresh();observe(m,TARGET,101,false,MobilityContext.Kind.OPEN_AIR,false);var rejected=m.attempt();check(rejected!=null,"eligible snapshot for rejected route");m.finish();check(m.attempt()==null,"rejected route cannot reroll same episode");
    }
    private static void cancellation(){
        for(int reason=0;reason<7;reason++){
            var m=fresh();observe(m,TARGET,101,false,MobilityContext.Kind.OPEN_AIR,false);check(m.begin(m.attempt()),"begin cancellation fixture");
            switch(reason){
                case 0 -> observe(m,TARGET,102,true,MobilityContext.Kind.OPEN_AIR,false);
                case 1 -> observe(m,OTHER,102,false,MobilityContext.Kind.OPEN_AIR,false);
                case 2 -> observe(m,null,102,false,MobilityContext.Kind.OPEN_AIR,false);
                case 3 -> observe(m,TARGET,100,false,MobilityContext.Kind.OPEN_AIR,false);
                case 4 -> observe(m,TARGET,103,false,MobilityContext.Kind.OPEN_AIR,false);
                case 5 -> observe(m,TARGET,102,false,MobilityContext.Kind.SEMI_OPEN,false);
                case 6 -> observe(m,TARGET,102,false,MobilityContext.Kind.OPEN_AIR,true);
            }
            check(!m.active(),"reacquire/identity/death/clock/gap/context/ownership cancellation"+reason);
            if(reason!=0)check(m.attempt()==null,"hidden discontinuity cannot reuse snapshot"+reason);
        }
        var m=fresh();observe(m,TARGET,101,false,MobilityContext.Kind.GROUND_FORCED,false);check(m.attempt()==null,"Ground suppressed");
        m.reset();check(m.snapshot()==null&&!m.active(),"stop clears memory");
    }
    private static void grammar(){
        var attack=new StandardAttack();for(int i=0;i<80;i++)attack.step(EYE,true);check(attack.state().phase()==StandardAttack.Phase.CHARGE&&attack.state().ticks()==20,"interrupted visible charge fixture");
        attack.step(null,false);check(attack.beginCover(EYE)&&attack.state().ticks()==0,"restart complete tell, no inherited visible charge");
        for(int tick=1;tick<=30;tick++){var state=attack.step(EYE,true);check(state.fire()==(tick==30),"full30tick cover charge"+tick);if(tick==19)check(state.ticks()==19&&!state.fire(),"same cue19 boundary");}
        check(!attack.beginCover(EYE),"never bypass firing recovery");
        attack.step(null,false);check(attack.state().phase()==StandardAttack.Phase.RECOVER,"existing20tick recovery survives loss");
        attack.reset();check(!attack.beginCover(null)&&!attack.beginCover(FlightVector.ZERO),"invalid aim cannot start");
        check(attack.beginCover(EYE),"guarded initial cover begin");attack.step(null,false);check(attack.state().phase()==StandardAttack.Phase.IDLE&&!attack.state().fire(),"cover cancellation never hidden fallback");
        var memory=fresh();for(int i=101;i<=110;i++)observe(memory,TARGET,i,false,MobilityContext.Kind.OPEN_AIR,false);check(memory.begin(memory.attempt()),"latest permitted admission");
        for(int i=111;i<=140;i++)observe(memory,TARGET,i,false,MobilityContext.Kind.OPEN_AIR,false);check(memory.active(),"last permitted30tick launch freshness");
    }
    private static void preference(){
        var selector=new ProjectileSelector();
        for(double variation:new double[]{0,.1,.4,.7,.99})check(selector.chooseCover(MobilityContext.Kind.OPEN_AIR,24,variation)==ProjectileSelector.Choice.LOB,"actual cover increases Lob value");
        selector.record(ProjectileSelector.Choice.LOB);for(double v:new double[]{0,.1,.4,.7,.99})check(selector.chooseCover(MobilityContext.Kind.OPEN_AIR,24,v)==ProjectileSelector.Choice.STANDARD,"600tick repetition suppresses immediate cover");
        for(int i=0;i<600;i++)selector.tick();check(selector.chooseCover(MobilityContext.Kind.SEMI_OPEN,24,.5)==ProjectileSelector.Choice.LOB,"existing finite penalty expires");
        for(double r:new double[]{0,11.99,64.01,Double.NaN,Double.POSITIVE_INFINITY})check(selector.chooseCover(MobilityContext.Kind.OPEN_AIR,r,.5)==ProjectileSelector.Choice.STANDARD,"bounded observed range"+r);
        check(selector.chooseCover(MobilityContext.Kind.GROUND_FORCED,24,.5)==ProjectileSelector.Choice.STANDARD,"Ground cannot select normal Lob");
        check(selector.chooseCover(MobilityContext.Kind.OPEN_AIR,24,Double.NaN)==ProjectileSelector.Choice.STANDARD,"invalid variation fail closed");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

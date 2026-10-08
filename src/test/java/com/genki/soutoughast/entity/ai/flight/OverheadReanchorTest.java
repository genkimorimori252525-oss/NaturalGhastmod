package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;

/** State/controller contracts; does not imply natural admission or native/human readability. */
public final class OverheadReanchorTest {
    private static int checks;
    private static final UUID TARGET=UUID.fromString("00000000-0000-0000-0000-000000000001"),OTHER=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final FlightVector OBSERVED=FlightVector.ZERO,START=new FlightVector(0,6,12);
    public static void main(String[] args){
        if(java.util.Arrays.stream(CombatAnchor.Reason.values()).noneMatch(x->x.name().equals("TACTICAL_RELOCATION")))throw new AssertionError("MISSING_PLANNED_OVERHEAD_REANCHOR");
        geometry();simulation();abort();commitGuards();director();
        System.out.println("PASS: "+checks+" overhead re-anchor/controller assertions");
    }
    private static CombatAnchor anchor(){var a=new CombatAnchor();a.observe(TARGET,OBSERVED,START,false);return a;}
    private static OverheadReanchor transit(CombatAnchor a){var m=new OverheadReanchor();check(m.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->true),"admit bounded frozen route");return m;}
    private static void geometry(){
        var a=anchor();var m=transit(a);check(m.overhead().y()>=7.8&&m.destination().z()==-8,"body bottom overhead and beyond frozen plane");check(m.routeLength()<=24,"total preflight route bound");
        check(!m.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->true),"active admission cannot replace route");
        var blocked=new OverheadReanchor();var calls=new int[]{0};check(!blocked.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->++calls[0]<2)&&calls[0]==2,"blocked cross rejected before action");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,new FlightVector(0,6,7),a.region(),(x,y)->true),"below minimum horizontal distance");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,new FlightVector(0,6,17),a.region(),(x,y)->true),"above maximum horizontal distance");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,10,START,a.region(),(x,y)->true),"too much climb cannot exceed24total route");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,START,a.region(),null),"coarse context is insufficient for route proof");
    }
    private static void simulation(){
        var a=anchor();var original=a.region();var m=transit(a);var controller=new FlightController();var p=START;var v=FlightVector.ZERO;boolean crossed=false,committed=false;int ticks=0;
        while(m.active()&&ticks++<170){
            var s=m.step(TARGET,true,a.region(),p,v,(x,y)->true);
            check(a.region().equals(original),"region retained throughout transit before guarded commit");
            if(s.commit()){check(v.length()<=.04&&p.subtract(m.destination()).length()<=.7,"commit only after braked physical arrival");check(a.commitRelocation(TARGET,original.generation(),p),"one authoritative relocation");committed=true;}
            var next=controller.step(v,s.intent());check(next.length()<=.32+1e-9,"bounded controller speed");check(next.subtract(v).length()<=Math.hypot(FlightController.ACCELERATION,FlightController.LATERAL_ACCELERATION)+1e-9,"same controller acceleration");
            v=next;p=p.add(v);if(p.z()<0){crossed=true;check(p.y()>=7.1,"cross only above observed top plus clearance with arrival margin");}
        }
        check(committed&&crossed&&ticks<=160&&!m.active(),"physical crossing and one finite commit");check(a.region().reason()==CombatAnchor.Reason.TACTICAL_RELOCATION&&a.region().radii().equals(CombatAnchor.RADII),"new explicit reason preserves broad region");
        check(!m.step(TARGET,true,a.region(),p,v,(x,y)->true).commit(),"completed action cannot repeat publication");
    }
    private static void abort(){
        for(int reason=0;reason<4;reason++){
            var a=anchor();var old=a.region();var m=transit(a);final int kind=reason;
            var s=m.step(reason==0?OTHER:TARGET,reason!=1,reason==2?new CombatAnchor.Region(old.center(),old.radii(),old.generation()+1,old.reason()):old,START,FlightVector.ZERO,(x,y)->kind!=3);
            check(!s.commit()&&s.phase()==OverheadReanchor.Phase.ABORT&&s.intent().mode()==FlightController.Mode.BRAKE,"identity/LOS/generation/body changes abort");
            for(int i=0;i<20;i++)check(!m.step(TARGET,true,old,START,FlightVector.ZERO,(x,y)->true).commit(),"abort cannot commit later");
            check(!m.active()&&a.region().equals(old),"bounded abort retains original anchor");
        }
        var a=anchor();var m=transit(a);for(int i=0;i<175;i++)check(!m.step(TARGET,true,a.region(),START,FlightVector.ZERO,(x,y)->true).commit(),"stalled motion cannot fabricate arrival");check(!m.active(),"global timeout abort is bounded");
    }
    private static void commitGuards(){
        var a=anchor();long g=a.region().generation();check(!a.commitRelocation(OTHER,g,new FlightVector(0,8,-8)),"wrong subject rejected");check(!a.commitRelocation(TARGET,g+1,new FlightVector(0,8,-8)),"wrong generation rejected");
        check(a.commitRelocation(TARGET,g,new FlightVector(0,8,-8)),"guarded fresh commit succeeds");check(!a.commitRelocation(TARGET,g,new FlightVector(0,8,-8)),"old generation cannot commit twice");
        long next=a.region().generation();for(int i=0;i<80;i++)a.observe(TARGET,OBSERVED,new FlightVector(0,8,-8),false);check(a.region().generation()==next,"ordinary observed target does not drag new anchor");
    }
    private static void director(){
        var d=new MajorActionDirector();check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,.999),"initial shared quiet");for(int i=0;i<240;i++)d.tick(true);
        int selected=0;for(int i=0;i<10000;i++)if(d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,i/10000.0))selected++;check(selected==49,"rare bounded variation retains ordinary majority");
        check(!d.shouldBeginRelocation(MobilityContext.Kind.SEMI_OPEN,true,false,12,.999),"open context required");check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,false,false,12,.999),"visible geometry required");check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,true,12,.999),"offense/feint busy rejection");
        check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,7,.999)&&!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,17,.999),"distance bound");
        d.began();check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,.999)&&!d.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,20,.999)&&!d.shouldBeginDomain(true,false,.999),"single shared-major reservation");d.finished();for(int i=0;i<1199;i++)d.tick(true);check(!d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,.999),"strong1200tick memory");d.tick(true);check(d.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,.999),"memory expires without permanent suppression");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

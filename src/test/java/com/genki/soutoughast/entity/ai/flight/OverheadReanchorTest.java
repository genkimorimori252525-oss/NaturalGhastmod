package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;

/** State/controller contracts; does not imply natural admission or native/human readability. */
public final class OverheadReanchorTest {
    private static int checks;
    private static final UUID TARGET=UUID.fromString("00000000-0000-0000-0000-000000000001"),OTHER=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final FlightVector OBSERVED=FlightVector.ZERO,START=new FlightVector(0,6,12);
    public static void main(String[] args){
        if(java.util.Arrays.stream(OverheadReanchor.Phase.values()).noneMatch(x->x.name().equals("DESCEND")))throw new AssertionError("MISSING_USER_ASCEND_TRAVEL_FACE_TURN_DESCEND_HANDOFF");
        if(java.util.Arrays.stream(CombatAnchor.Reason.values()).noneMatch(x->x.name().equals("TACTICAL_RELOCATION")))throw new AssertionError("MISSING_PLANNED_OVERHEAD_REANCHOR");
        geometry();simulation();orientationGates();abort();commitGuards();director();
        System.out.println("PASS: "+checks+" overhead re-anchor/controller assertions");
    }
    private static CombatAnchor anchor(){var a=new CombatAnchor();a.observe(TARGET,OBSERVED,START,false);return a;}
    private static OverheadReanchor transit(CombatAnchor a){var m=new OverheadReanchor();check(m.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->true),"admit bounded frozen route");return m;}
    private static void geometry(){
        var a=anchor();var m=transit(a);check(m.overhead().y()>=17.8&&m.destination().z()==-8&&m.candidateCenter().y()==START.y(),"high passage and frozen region altitude");check(m.routeLength()<=48&&m.budget()<=320,"all three legs and conservative time bounded");
        check(!m.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->true),"active admission cannot replace route");
        var blocked=new OverheadReanchor();var calls=new int[]{0};check(!blocked.begin(TARGET,OBSERVED,1.8,START,a.region(),(x,y)->++calls[0]<2)&&calls[0]==2,"blocked cross rejected before action");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,new FlightVector(0,6,7),a.region(),(x,y)->true),"below minimum horizontal distance");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,new FlightVector(0,6,17),a.region(),(x,y)->true),"above maximum horizontal distance");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,10,START,a.region(),(x,y)->true),"too much climb cannot exceed conservative320tick budget");
        check(!new OverheadReanchor().begin(TARGET,OBSERVED,1.8,START,a.region(),null),"coarse context is insufficient for route proof");
    }
    private static void simulation(){
        var a=anchor();var original=a.region();var m=transit(a);var controller=new FlightController();var p=START;var v=FlightVector.ZERO;boolean crossed=false,committed=false,turned=false,descending=false;int ticks=0;float yaw=0,pitch=0;
        while(m.active()&&ticks++<330){
            var s=m.step(TARGET,true,a.region(),p,v,yaw,pitch,OBSERVED.subtract(p),(x,y)->true);
            check(a.region().equals(original),"region retained throughout transit before guarded commit");
            if(s.commit()){check(v.y()<-.05&&s.intent().mode()==FlightController.Mode.MOVE&&p.subtract(m.candidateCenter()).length()>7,"handoff in height band preserves descending motion without center stop");check(a.commitRelocation(TARGET,original.generation(),m.candidateCenter()),"one authoritative relocation");committed=true;}
            var next=controller.step(v,s.intent());check(next.length()<=.32+1e-9,"bounded controller speed");check(next.subtract(v).length()<=Math.hypot(FlightController.ACCELERATION,FlightController.LATERAL_ACCELERATION)+1e-9,"same controller acceleration");
            if(s.look()!=null){float desiredYaw=(float)Math.toDegrees(-Math.atan2(s.look().x(),s.look().z()));float desiredPitch=(float)Math.toDegrees(-Math.atan2(s.look().y(),Math.hypot(s.look().x(),s.look().z())));yaw=FlightOrientation.approachDegrees(yaw,desiredYaw,4);pitch=FlightOrientation.approachDegrees(pitch,desiredPitch,3);}
            if(s.phase()==OverheadReanchor.Phase.CROSS||s.phase()==OverheadReanchor.Phase.BRAKE)check(s.look().equals(m.heading()),"crossing and high braking face travel heading, never Player");
            if(s.phase()==OverheadReanchor.Phase.TURN){turned=true;check(p.z()<-7,"only behind Player may turn toward Player");}
            if(s.phase()==OverheadReanchor.Phase.DESCEND)descending=true;
            v=next;p=p.add(v);if(s.phase()==OverheadReanchor.Phase.CROSS&&p.z()<0){crossed=true;check(p.y()>=17.8,"high physical passage above target top");}
        }
        check(committed&&crossed&&turned&&descending&&ticks<=320&&!m.active(),"physical crossing and one finite commit");check(a.region().reason()==CombatAnchor.Reason.TACTICAL_RELOCATION&&a.region().radii().equals(CombatAnchor.RADII),"new explicit reason preserves broad region");
        check(!m.step(TARGET,true,a.region(),p,v,yaw,pitch,OBSERVED.subtract(p),(x,y)->true).commit(),"completed action cannot repeat publication");
        var ordinary=new MovementPlanner().step(a,OBSERVED,p,FlightVector.ZERO,MobilityContext.Kind.OPEN_AIR,new MobilityContext.Sample(1023),.3,(x)->true);
        check(controller.step(v,ordinary.intent()).length()>.01,"ordinary swim resumes with inherited physical momentum");
    }
    private static void orientationGates(){
        var a=anchor();var m=transit(a);var point=m.overhead();
        for(int i=0;i<12;i++)m.step(TARGET,true,a.region(),point,FlightVector.ZERO,0,0,OBSERVED.subtract(point),(x,y)->true);
        var waiting=m.step(TARGET,true,a.region(),point,FlightVector.ZERO,0,0,OBSERVED.subtract(point),(x,y)->true);
        check(waiting.phase()==OverheadReanchor.Phase.ALIGN&&waiting.intent().mode()==FlightController.Mode.BRAKE,"cannot cross backward before actual travel-facing alignment");
        check(m.step(TARGET,true,a.region(),point,FlightVector.ZERO,180,0,OBSERVED.subtract(point),(x,y)->true).phase()==OverheadReanchor.Phase.CROSS,"actual travel alignment admits crossing");
        point=m.beyond();var toward=OBSERVED.subtract(point);
        var turning=m.step(TARGET,true,a.region(),point,FlightVector.ZERO,180,0,toward,(x,y)->true);
        check(turning.phase()==OverheadReanchor.Phase.TURN&&turning.look().equals(toward)&&turning.intent().mode()==FlightController.Mode.BRAKE,"behind arrival turns toward Player before descent");
        float pitch=(float)Math.toDegrees(-Math.atan2(toward.y(),Math.hypot(toward.x(),toward.z())));
        var descending=m.step(TARGET,true,a.region(),point,FlightVector.ZERO,0,pitch,toward,(x,y)->true);
        check(descending.phase()==OverheadReanchor.Phase.DESCEND&&descending.intent().direction().y()<0,"actual target-facing alignment admits gentle descent");
        var blocked=m.step(TARGET,true,a.region(),point,FlightVector.ZERO,0,pitch,toward,(x,y)->!y.equals(m.destination()));
        check(blocked.phase()==OverheadReanchor.Phase.ABORT&&!blocked.commit(),"blocked descent cannot publish a region");
    }
    private static void abort(){
        for(int reason=0;reason<4;reason++){
            var a=anchor();var old=a.region();var m=transit(a);final int kind=reason;
            var s=m.step(reason==0?OTHER:TARGET,reason!=1,reason==2?new CombatAnchor.Region(old.center(),old.radii(),old.generation()+1,old.reason()):old,START,FlightVector.ZERO,180,0,OBSERVED.subtract(START),(x,y)->kind!=3);
            check(!s.commit()&&s.phase()==OverheadReanchor.Phase.ABORT&&s.intent().mode()==FlightController.Mode.BRAKE,"identity/LOS/generation/body changes abort");
            for(int i=0;i<20;i++)check(!m.step(TARGET,true,old,START,FlightVector.ZERO,180,0,OBSERVED.subtract(START),(x,y)->true).commit(),"abort cannot commit later");
            check(!m.active()&&a.region().equals(old),"bounded abort retains original anchor");
        }
        var a=anchor();var m=transit(a);for(int i=0;i<330;i++)check(!m.step(TARGET,true,a.region(),START,FlightVector.ZERO,180,0,OBSERVED.subtract(START),(x,y)->true).commit(),"stalled motion cannot fabricate arrival");check(!m.active(),"global timeout abort is bounded");
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

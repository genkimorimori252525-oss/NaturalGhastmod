package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;

public final class RallyFeintTest {
    private static int checks;
    private static final UUID SUBJECT=new UUID(0,1),A=new UUID(0,2),B=new UUID(0,3),SHOT=new UUID(0,4);
    private static final FlightVector BOSS=FlightVector.ZERO,IN=new FlightVector(0,0,-1);
    private static final CombatAnchor.Region REGION=new CombatAnchor.Region(BOSS,CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);
    private static void check(boolean pass,String name){checks++;if(!pass)throw new AssertionError(name);}
    private static RallyFeint learned(){var f=new RallyFeint();f.observeSubject(SUBJECT,0);f.ordinaryReturn(A,0);f.observeSubject(SUBJECT,1);f.ordinaryReturn(B,1);f.observeSubject(SUBJECT,2);return f;}
    private static boolean begin(RallyFeint f,double v,boolean clear){return f.begin(SHOT,2,BOSS,IN,REGION,MobilityContext.Kind.OPEN_AIR,v,(a,b)->clear);}
    private static MovementPlanner.Plan step(RallyFeint f,int t,boolean eligible,boolean observed,boolean clear){f.observeSubject(SUBJECT,t);return f.step(t,eligible,observed,REGION,BOSS,MobilityContext.Kind.OPEN_AIR,(a,b)->clear);}
    public static void main(String[] args){
        var f=new RallyFeint();f.observeSubject(SUBJECT,2);check(!begin(f,0,true),"no unlearned deception");
        f.ordinaryReturn(A,2);check(!begin(f,0,true),"one success insufficient");f.ordinaryReturn(A,2);check(!begin(f,0,true),"same success cannot be counted twice");
        f=learned();check(!begin(f,.1,true),"ordinary returns remain dominant");check(!begin(f,.01,false),"blocked route cannot admit");check(begin(f,.01,true),"learned rare case admitted");
        check(f.face()&&f.active()&&f.state().waypoint().equals(new FlightVector(-4,0,0)),"frozen face and lateral route");
        for(int t=2;t<8;t++){check(step(f,t,true,true,true)==null&&f.face(),"six tick face preserves ordinary drift");}
        var p=step(f,8,true,true,true);check(p!=null&&p.primitive()==MovementPrimitive.STRAFE&&!f.face(),"reveal lateral dodge after face");
        var point=p.waypoint();for(int t=9;t<32;t++){p=step(f,t,true,false,true);check(p!=null&&p.waypoint().equals(point),"frozen leg without hidden projectile chase");}
        p=step(f,32,true,false,true);check(!f.active()&&p.intent().mode()==FlightController.Mode.BRAKE,"finite leg times out through braking");
        f.observeSubject(SUBJECT,33);check(!f.learned(33),"600 tick repetition cooldown");
        f=learned();begin(f,.01,true);p=step(f,3,true,false,true);check(!f.active()&&p.intent().mode()==FlightController.Mode.BRAKE,"incoming lost during face aborts without return");
        for(int reason=0;reason<3;reason++){f=learned();begin(f,.01,true);p=step(f,8,reason!=0,true,reason!=1);if(reason<2)check(!f.active()&&p.intent().mode()==FlightController.Mode.BRAKE,"eligibility or full route loss brakes");else{f.observeSubject(SUBJECT,9);var changed=new CombatAnchor.Region(BOSS,CombatAnchor.RADII,2,CombatAnchor.Reason.TACTICAL_RELOCATION);p=f.step(9,true,true,changed,BOSS,MobilityContext.Kind.OPEN_AIR,(a,b)->true);check(!f.active()&&p.intent().mode()==FlightController.Mode.BRAKE,"region replacement cancels");}}
        f=learned();f.observeSubject(new UUID(0,9),3);check(!f.learned(3),"target replacement clears learning");
        f=learned();f.observeSubject(SUBJECT,602);check(!f.learned(602),"bounded learned evidence expires");
        f=learned();check(!f.begin(SHOT,2,BOSS,IN,REGION,MobilityContext.Kind.GROUND_FORCED,0,(a,b)->true),"ground exclusion");
        check(!f.begin(SHOT,2,BOSS,new FlightVector(0,1,0),REGION,MobilityContext.Kind.OPEN_AIR,0,(a,b)->true),"no invented lateral direction for vertical approach");
        for(int i=0;i<1000;i++){f=learned();boolean admitted=begin(f,(i+.5)/1000,true);check(admitted==(i<100),"ten percent admission without rerolls");}
        f=learned();begin(f,.08,true);var controller=new FlightController();FlightVector position=BOSS,velocity=FlightVector.ZERO;double displacement=0;
        for(int t=2;t<32;t++){
            f.observeSubject(SUBJECT,t);p=f.step(t,true,true,REGION,position,MobilityContext.Kind.OPEN_AIR,(a,b)->true);
            if(p!=null){velocity=controller.step(velocity,p.intent());position=position.add(velocity);displacement=position.length();}
            if(!f.active())break;
        }
        check(displacement>2&&displacement<5,"sole inertial controller actually dodges without teleporting");
        System.out.println("Rally feint checks: "+checks);
    }
}

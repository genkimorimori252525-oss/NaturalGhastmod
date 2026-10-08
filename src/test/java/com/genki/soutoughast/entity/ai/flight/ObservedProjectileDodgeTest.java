package com.genki.soutoughast.entity.ai.flight;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

public final class ObservedProjectileDodgeTest {
    private static final UUID SUBJECT=new UUID(1,1),SHOT=new UUID(2,2),OTHER=new UUID(3,3);
    private static final FlightVector ZERO=FlightVector.ZERO,HALF=new FlightVector(.25,.25,.25);
    private static final CombatAnchor.Region REGION=new CombatAnchor.Region(ZERO,CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);
    private static final BiPredicate<FlightVector,FlightVector> CLEAR=(a,b)->true;
    private static int checks;
    public static void main(String[] args){prediction();observations();bounds();cancellation();physical();System.out.println("Observed projectile dodge assertions passed: "+checks);}
    private static void prediction(){
        check(Math.abs(ObservedProjectileDodge.impactTime(new FlightVector(0,0,12),new FlightVector(0,0,-1),HALF)-9.5)<1e-9,"full4x4body collision horizon");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(new FlightVector(0,0,12),new FlightVector(0,0,1),HALF)),"receding projectile rejected");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(new FlightVector(3,0,12),new FlightVector(0,0,-1),HALF)),"nearby miss is not a threat");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(new FlightVector(0,4,12),new FlightVector(0,0,-1),HALF)),"above full body rejected");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(new FlightVector(0,0,16),new FlightVector(0,0,-1),HALF)),"beyond12tick horizon rejected");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(ZERO,new FlightVector(0,0,-1),HALF)),"already overlapping cannot fabricate preemptive dodge");
        check(Double.isNaN(ObservedProjectileDodge.impactTime(new FlightVector(4,0,8),new FlightVector(0,0,-1),HALF)),"stationary nonintersecting axis rejected");
        check(Double.isFinite(ObservedProjectileDodge.impactTime(new FlightVector(3,0,8),new FlightVector(-.2,0,-1),HALF)),"closing side intersects after entry");
    }
    private static List<ObservedProjectileDodge.Observation> shot(double z){return List.of(new ObservedProjectileDodge.Observation(SHOT,new FlightVector(0,2,z),HALF));}
    private static MovementPlanner.Plan step(ObservedProjectileDodge d,long tick,List<ObservedProjectileDodge.Observation> observations,double variation,boolean eligible){
        return d.step(tick,SUBJECT,REGION,ZERO,ZERO,MobilityContext.Kind.OPEN_AIR,eligible,observations,variation,CLEAR);
    }
    private static ObservedProjectileDodge admitted(){var d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);step(d,1,null,.1,true);check(step(d,2,shot(12),.1,true)!=null&&d.active(),"second visible displacement admits one fallible route");return d;}
    private static void observations(){
        var d=new ObservedProjectileDodge();check(step(d,0,shot(14),.1,true)==null,"one position is insufficient");check(step(d,1,null,.1,true)==null,"unsampled tick does not invent motion");check(step(d,2,shot(12),.1,true)!=null,"two consecutive samples with observed closing motion");
        check(d.state().projectile().equals(SHOT)&&d.state().waypoint().equals(new FlightVector(-4,0,0)),"one frozen lateral route, no target chase");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);step(d,1,null,.1,true);check(step(d,2,shot(14),.1,true)==null,"stationary projectile rejected");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);step(d,1,null,.1,false);check(step(d,2,shot(12),.1,true)==null,"LOS/eligibility loss clears hidden continuity");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);step(d,1,null,.1,true);step(d,2,List.of(),.1,true);step(d,3,null,.1,true);check(step(d,4,shot(10),.1,true)==null,"unseen intermediate sample breaks identity continuity");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);check(step(d,3,shot(11),.1,true)==null,"sampling gap cannot infer unseen trajectory");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.9,true);step(d,1,null,.9,true);check(step(d,2,shot(12),.9,true)==null&&!d.active(),"deliberate fallibility remains");
        for(int i=3;i<=130;i++)check(step(d,i,i%2==0?shot(12):null,.1,true)==null,"same attempted projectile cannot retrigger in memory window");
        d=new ObservedProjectileDodge();step(d,0,shot(14),.1,true);step(d,1,null,.1,true);var changed=List.of(new ObservedProjectileDodge.Observation(OTHER,new FlightVector(0,2,12),HALF));check(step(d,2,changed,.1,true)==null,"new UUID cannot reuse another projectile's position");
    }
    private static void bounds(){
        var d=admitted();var frozen=d.state().waypoint();for(int i=3;i<26;i++){var p=step(d,i,null,.1,true);check(p!=null&&p.waypoint().equals(frozen),"route remains fixed even if no projectile remains visible");}
        check(step(d,26,null,.1,true)==null&&!d.active()&&d.state().phase()==ObservedProjectileDodge.Phase.RECOVER,"24tick bound resumes ordinary recovery");
        for(int i=27;i<122;i++)check(step(d,i,i%2==0?List.of(new ObservedProjectileDodge.Observation(OTHER,new FlightVector(0,2,140-i),HALF)):null,.1,true)==null,"120tick attempt quiet");
        d=new ObservedProjectileDodge();int[] queries={0};for(int i=0;i<=2;i++)d.step(i,SUBJECT,REGION,ZERO,ZERO,MobilityContext.Kind.OPEN_AIR,true,i%2==0?shot(14-i):null,.1,(a,b)->{queries[0]++;return false;});check(!d.active()&&queries[0]==2,"at most two full-body route candidates, blocked routes rejected");
        d=new ObservedProjectileDodge();for(int i=0;i<=2;i++)d.step(i,SUBJECT,REGION,new FlightVector(19,0,0),ZERO,MobilityContext.Kind.OPEN_AIR,true,i%2==0?List.of(new ObservedProjectileDodge.Observation(SHOT,new FlightVector(19,2,14-i),HALF)):null,.1,CLEAR);check(d.active()&&REGION.radius(d.state().waypoint())<=.9,"boundary chooses inward route instead of relocation");
        d=new ObservedProjectileDodge();for(int i=0;i<=2;i++)d.step(i,SUBJECT,REGION,ZERO,ZERO,MobilityContext.Kind.CONFINED,true,i%2==0?shot(14-i):null,.1,CLEAR);check(d.active()&&d.state().waypoint().length()==2,"confined route is compact");
        d=new ObservedProjectileDodge();for(int i=0;i<=2;i++)check(d.step(i,SUBJECT,REGION,ZERO,ZERO,MobilityContext.Kind.GROUND_FORCED,true,i%2==0?shot(14-i):null,.1,CLEAR)==null,"ground suppresses airborne dodge");
        boolean rejected=false;try{step(d,3,java.util.Collections.nCopies(9,shot(1).get(0)),.1,true);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"observation bound enforced");
    }
    private static void cancellation(){
        for(int reason=0;reason<5;reason++){
            var d=admitted();var identity=reason==0?OTHER:SUBJECT;var region=reason==1?new CombatAnchor.Region(ZERO,CombatAnchor.RADII,2,CombatAnchor.Reason.REPLACED):REGION;
            final int kind=reason;var p=d.step(reason==4?5:3,identity,region,ZERO,ZERO,MobilityContext.Kind.OPEN_AIR,reason!=2,null,.1,(a,b)->kind!=3);
            check(!d.active()&&p!=null&&p.intent().mode()==FlightController.Mode.BRAKE,"identity/generation/eligibility/clearance/clock loss cancels with physical braking");
            check(REGION.center().equals(ZERO)&&REGION.generation()==1,"no dodge may publish/move original region");
        }
    }
    private static void physical(){
        var d=new ObservedProjectileDodge();var controller=new FlightController();var anchor=new CombatAnchor();var target=new FlightVector(0,0,-28);anchor.observe(SUBJECT,target,ZERO,false);var retained=anchor.region();
        var position=ZERO;var velocity=new FlightVector(.08,0,0);int moving=0;boolean recovered=false;
        for(int tick=0;tick<40;tick++){
            var plan=d.step(tick,SUBJECT,retained,position,velocity,MobilityContext.Kind.OPEN_AIR,true,tick%2==0?shot(14-tick):null,.1,CLEAR);
            if(plan==null){
                if(d.state().phase()==ObservedProjectileDodge.Phase.RECOVER)recovered=true;
                plan=new MovementPlanner().step(anchor,target,position,ZERO,MobilityContext.Kind.OPEN_AIR,new MobilityContext.Sample(1023),.3,x->true);
            }else moving++;
            var next=controller.step(velocity,plan.intent());check(next.subtract(velocity).length()<=Math.hypot(.11,.045)+1e-9,"sole controller bounds acceleration/inertia");position=position.add(next);velocity=next;
            check(anchor.region().equals(retained),"physical dodge/swim keep boss-owned region");
            if(tick==12)check(Math.abs(position.x())>2.5,"actual controller motion leaves observed incoming body lane");
        }
        check(moving>0&&moving<=24&&recovered&&velocity.length()>.001,"finite evasion then ordinary swim, not exact-center stop");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

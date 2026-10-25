package com.genki.soutoughast.entity.ai.flight;

/** Scheduling, fallible disposition and actual inertial pacing; no fake input or native claims. */
public final class StandardShotRhythmTest {
    private static int checks;
    private static final FlightVector X=new FlightVector(1,0,0);
    private static final StandardShotRhythm.Context CLEAR=new StandardShotRhythm.Context(true,true,true,28,.08,0);
    public static void main(String[] args){cadence();decisions();pacing();cover();System.out.println("PASS: "+checks+" ordinary-shot rhythm/disposition/inertia checks");}
    private static void cadence(){
        check(StandardShotRhythm.sample(0,0).interval()==60,"minimum interval");
        check(StandardShotRhythm.sample(Math.nextDown(1.0),0).interval()==90,"maximum interval");
        check(StandardShotRhythm.initialQuiet(0)==30&&StandardShotRhythm.initialQuiet(Math.nextDown(1.0))==60,"initial first-shot window");
        for(double invalid:new double[]{-1,1,Double.NaN,Double.POSITIVE_INFINITY}){
            try{StandardShotRhythm.sample(invalid,0);throw new AssertionError("invalid timing accepted");}catch(IllegalArgumentException expected){checks++;}
            try{StandardShotRhythm.sample(0,invalid);throw new AssertionError("invalid decision sample accepted");}catch(IllegalArgumentException expected){checks++;}
        }
        for(int interval:new int[]{60,75,90})for(boolean decline:new boolean[]{false,true}){
            var attack=new StandardAttack();attack.delayInitial(interval-30);int previousAdmission=-1,previousShot=-1,admissions=0,shots=0,declines=0;
            for(int tick=1;tick<=650;tick++){
                var c=new StandardShotRhythm.Cycle(interval,decline?.999:0);
                var state=attack.step(X,true,c,CLEAR);
                if(state.phase()==StandardAttack.Phase.CHARGE&&state.ticks()==0){
                    if(previousAdmission>=0)check(tick-previousAdmission==interval,"one opportunity deadline even after withhold");
                    previousAdmission=tick;admissions++;
                }
                if(state.phase()==StandardAttack.Phase.DECLINE&&state.ticks()==0)declines++;
                if(state.fire()){
                    if(previousShot>=0)check(tick-previousShot==interval,"eligible fired cycle stays3-4.5seconds");else check(tick==interval,"first scheduled shot60-90ticks");
                    previousShot=tick;shots++;
                }
            }
            check(admissions>=6,"multiple opportunities exercised");check(decline?shots==0&&declines>=6:shots>=6&&declines==0,"stored disposition drives fire or withhold");
        }
    }
    private static void decisions(){
        check(StandardShotRhythm.decide(new StandardShotRhythm.Cycle(60,.999),CLEAR)==StandardShotRhythm.Outcome.DECLINE,"favorable position never guarantees shooting");
        var unsafe=new StandardShotRhythm.Context(true,true,false,28,.08,0);
        check(StandardShotRhythm.decide(new StandardShotRhythm.Cycle(60,0),unsafe)==StandardShotRhythm.Outcome.SAFETY_CANCEL,"clearance veto separate from deliberate withhold");
        var favorable=new StandardShotRhythm.Cycle(60,.6);
        check(StandardShotRhythm.decide(favorable,CLEAR)==StandardShotRhythm.Outcome.FIRE,"favorable current observations can fire");
        check(StandardShotRhythm.decide(favorable,new StandardShotRhythm.Context(true,false,true,12,.4,.9))==StandardShotRhythm.Outcome.DECLINE,"observed geometry and motion change choice");
        for(boolean decline:new boolean[]{false,true}){
            var a=new StandardAttack();a.delayInitial(30);for(int i=0;i<30;i++)a.step(X,true,new StandardShotRhythm.Cycle(60,decline?.999:0),CLEAR);
            for(int t=1;t<=18;t++)a.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);
            check(a.state().ticks()==18&&a.decisionsEvaluated()==0,"decision waits for current aiming boundary");
            var at19=a.step(new FlightVector(0,0,1),true,StandardShotRhythm.DEFAULT,CLEAR);
            check(a.decisionsEvaluated()==1,"one decision per admitted cycle");
            if(decline){
                check(at19.phase()==StandardAttack.Phase.DECLINE&&at19.ticks()==0&&!at19.fire(),"withhold precedes profile tick19 cue");
                for(int t=1;t<=3;t++){a.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);check(a.aiming(),"brief decline still eases momentum");}
                a.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);check(!a.aiming()&&!a.state().face(),"withhold releases after4ticks");
            }else{
                check(at19.phase()==StandardAttack.Phase.CHARGE&&at19.ticks()==19,"fire path preserves committed profile cue");
                var locked=at19.direction();
                for(int t=20;t<=30;t++){
                    var s=a.step(X,true,new StandardShotRhythm.Cycle(90,.999),unsafe);
                    check(s.direction().equals(locked),"late target motion cannot change frozen aim");check(s.fire()==(t==30),"full30tick tell preserved");
                }
                check(a.decisionsEvaluated()==1&&a.aiming(),"later samples do not reroll committed choice");
                for(int t=1;t<=5;t++)a.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);
                check(a.aiming(),"fired pause lasts through recovery5");a.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);
                check(!a.aiming()&&a.state().face(),"motion resumes before20tick firing face ends");
            }
        }
        var lost=new StandardAttack();lost.delayInitial(30);for(int i=0;i<30;i++)lost.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);
        for(int i=0;i<16;i++)lost.step(X,true,StandardShotRhythm.DEFAULT,CLEAR);
        check(lost.step(null,false,StandardShotRhythm.DEFAULT,CLEAR).phase()==StandardAttack.Phase.IDLE,"LOS loss cancels safely, without an intentional-decline hold");
        check(lost.lastOutcome()==StandardShotRhythm.Outcome.SAFETY_CANCEL,"safety cancellation remains observable");
    }
    private static MovementPlanner.Plan plan(MovementPrimitive primitive,boolean inside,FlightController.Intent intent){return new MovementPlanner.Plan(primitive,intent,new FlightVector(15,6,8),CombatAnchor.Range.COMFORTABLE,inside);}
    private static void pacing(){
        var pacer=new StandardAimingMotion();var original=plan(MovementPrimitive.DRIFT,true,FlightController.Intent.move(X,.4));
        var controller=new FlightController();var v=X.scale(.4);double previous=.4;
        for(int tick=0;tick<30;tick++){
            var slow=pacer.apply(original,true);check(slow.waypoint()==original.waypoint()&&slow.primitive()==original.primitive()&&slow.intent().direction().equals(X),"pacing retains course and thought vocabulary");
            check(slow.intent().speed()<=previous+1e-9&&slow.intent().speed()>=.1-1e-9,"aiming target slows but never becomes a physical stop");
            var next=controller.step(v,slow.intent());check(next.length()>.09&&v.subtract(next).length()<=FlightController.BRAKING+1e-9,"actual inertia is retained while aiming");v=next;previous=slow.intent().speed();
        }
        check(v.length()<.101,"soft aiming actually slows production controller");
        for(int i=0;i<15;i++){var next=pacer.apply(original,false);check(next.intent().speed()-previous<=.025000001,"release speed eases rather than jumps");previous=next.intent().speed();v=controller.step(v,next.intent());}
        check(v.length()>.39,"ordinary swimming resumes");
        for(var p:new MovementPlanner.Plan[]{plan(MovementPrimitive.RETURN,false,FlightController.Intent.move(X,.8)),plan(MovementPrimitive.STRAFE,true,FlightController.Intent.move(X,.5)),plan(MovementPrimitive.BRAKE,true,FlightController.Intent.brake())})check(pacer.apply(p,true)==p,"return/safety/maneuvers bypass aiming cap");
    }
    private static void cover(){
        var a=new StandardAttack();check(a.beginCover(X),"fresh guarded cover begins");
        var veto=new StandardShotRhythm.Context(false,false,false,0,0,0);
        for(int t=1;t<=30;t++){var s=a.step(X,true,new StandardShotRhythm.Cycle(60,.999),veto);check(s.fire()==(t==30)&&!a.aiming(),"cover grammar bypasses ordinary disposition and pacing");}
        check(a.decisionsEvaluated()==0,"cover never consumes ordinary decision");
    }
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}

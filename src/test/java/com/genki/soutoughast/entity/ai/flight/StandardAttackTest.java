package com.genki.soutoughast.entity.ai.flight;

/** Boundary behavior without world, random server state or a fake Player. */
public final class StandardAttackTest {
    private static int checks;
    private static final FlightVector A=new FlightVector(1,0,0), B=new FlightVector(0,0,1);
    public static void main(String[] args) {
        StandardAttack attack=new StandardAttack();
        for(int i=0;i<60;i++) check(!attack.step(A,true).fire(),"initial quiet");
        var start=attack.state();check(start.phase()==StandardAttack.Phase.CHARGE&&start.face(),"face begins charge");
        for(int i=1;i<30;i++) {
            var state=attack.step(i<20?A:B,true);
            check(!state.fire(),"no early fire");
            if(i>=20)check(state.direction().equals(A),"direction locked before late target movement");
        }
        var fired=attack.step(B,true);check(fired.fire()&&fired.face(),"one shot after30 full intervals");
        for(int i=1;i<20;i++)check(attack.step(null,false).face(),"committed face remains after LOS loss");
        check(!attack.step(null,false).face(),"face clears20 ticks after launch");
        for(int i=0;i<200;i++)check(!attack.step(null,false).fire(),"no hidden shots");
        attack.reset();for(int i=0;i<70;i++)attack.step(A,true);
        check(attack.state().face(),"charging before cancellation");
        check(attack.step(null,false).phase()==StandardAttack.Phase.IDLE,"LOS loss aborts charge");
        for(int i=0;i<30;i++)check(!attack.step(B,true).fire(),"cancellation cannot immediate refire");
        for(String reason:new String[]{"target death","null target","target replacement"}){
            attack.reset();for(int i=0;i<90;i++)attack.step(A,true);
            check(attack.state().fire(),"launched before "+reason);
            for(int i=1;i<20;i++){
                attack.invalidateTarget();check(attack.step(reason.equals("target replacement")?B:null,false).face(),"committed recovery survives "+reason);
            }
            attack.invalidateTarget();check(!attack.step(null,false).face(),"recovery still ends on time after "+reason);
        }
        RallyReaction rally=new RallyReaction();
        check(!rally.begin(1,A,0.95),"fallible choice declines");
        check(rally.begin(1,A,.1),"reaction starts");
        for(int i=0;i<5;i++)check(!rally.step(true,false).fire(),"readable reaction before return");
        check(rally.step(true,true).fire(),"sixth tick in reach returns");
        check(!rally.step(true,true).fire(),"one return pulse");
        rally.reset();check(rally.begin(2,B,.1),"second independent projectile");
        for(int i=0;i<14;i++)check(!rally.step(true,false).fire(),"missed reach expires without magic return");
        check(!rally.state().face(),"miss clears face");
        rally.reset();rally.begin(3,A,.1);check(!rally.step(false,true).face(),"lost visible projectile aborts");
        check(!RallyReaction.incoming(A,new FlightVector(1,0,0)),"outgoing ignored");
        check(RallyReaction.incoming(A,new FlightVector(-1,0,0)),"incoming accepted");
        System.out.println("PASS: "+checks+" Standard charge/rally checks");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

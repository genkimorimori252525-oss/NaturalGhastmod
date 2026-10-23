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
        attackFaceTracking();
        System.out.println("PASS: "+checks+" Standard charge/rally checks");
    }
    private static void attackFaceTracking(){
        var attack=new StandardAttack();for(int i=0;i<79;i++)attack.step(A,true);
        var locked=attack.state().direction();float yaw=0,pitch=0;
        for(int i=0;i<10;i++){
            var movingPlayer=new FlightVector(Math.cos(i*.6),Math.sin(i*.4)*.8,Math.sin(i*.6));
            var state=attack.step(movingPlayer,true);
            check(state.direction().equals(locked),"tracking face cannot change committed projectile direction");
            float desiredYaw=(float)Math.toDegrees(-Math.atan2(movingPlayer.x(),movingPlayer.z()));
            float desiredPitch=(float)Math.toDegrees(-Math.atan2(movingPlayer.y(),Math.hypot(movingPlayer.x(),movingPlayer.z())));
            yaw=FlightOrientation.approachDegrees(yaw,desiredYaw,FlightOrientation.yawLimit(true));
            pitch=FlightOrientation.approachDegrees(pitch,desiredPitch,FlightOrientation.pitchLimit(true));
            check(Math.abs(Math.sin(Math.toRadians(yaw-desiredYaw)))<1e-6&&Math.cos(Math.toRadians(yaw-desiredYaw))>.999999,"attack face tracks moving visible target without yaw lag");
            check(Math.abs(pitch-desiredPitch)<1e-5,"attack face tracks target height without pitch lag");
        }
        check(FlightOrientation.approachDegrees(179,-179,FlightOrientation.yawLimit(true))==181,"tracking uses short path across yaw seam");
        check(Math.abs(FlightOrientation.approachDegrees(0,90,FlightOrientation.yawLimit(false)))==7,"ordinary turns retain bounded rate");
        check(Math.abs(FlightOrientation.approachDegrees(0,-60,FlightOrientation.pitchLimit(false)))==5,"ordinary pitch retains bounded rate");
        check(!FlightOrientation.validDirection(0,0,0)&&!FlightOrientation.validDirection(Double.NaN,0,1)&&!FlightOrientation.validDirection(1,Double.POSITIVE_INFINITY,0),"zero and nonfinite face offsets reject");
        check(FlightOrientation.targetYaw(0,0,123)==123,"vertical target preserves yaw rather than snapping north");
        check(FlightOrientation.targetPitch(0,1,0)==-90&&FlightOrientation.targetPitch(0,-1,0)==90,"vertical tracking pitch stays valid");
        float shown=FlightOrientation.attackRenderPitch(0,-35,true);
        check(shown==-35&&FlightOrientation.renderPitch(0,0,shown)==-35,"ordinary attack pitch reaches renderer without an extra tracking delay");
        check(FlightOrientation.attackRenderPitch(shown,0,false)==-30,"attack pose eases back after face release");
        check(FlightOrientation.renderPitch(3,90,-35)==3,"major downward pose takes precedence");
        check(FlightOrientation.renderPitch(0,90,-35)==0,"major pose start does not borrow ordinary attack pitch");
        check(FlightOrientation.renderPitch(3,0,-35)==3,"major pose release keeps its prior interpolation");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

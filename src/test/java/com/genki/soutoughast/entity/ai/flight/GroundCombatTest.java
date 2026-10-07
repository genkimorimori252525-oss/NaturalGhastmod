package com.genki.soutoughast.entity.ai.flight;

public final class GroundCombatTest {
    private static int checks;
    private static final FlightVector FLOOR=new FlightVector(0,0,0);
    private static final CombatAnchor.Region REGION=new CombatAnchor.Region(new FlightVector(0,6,0),CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);
    public static void main(String[] args){
        check(GroundFootprint.supported(FLOOR,new FlightVector(8,0,8),(x,y,z)->true),"bounded full swept footprint");
        check(!GroundFootprint.supported(FLOOR,new FlightVector(8,0,8),(x,y,z)->x!=8||z!=-2),"conservative corner tile gap is not missed between point samples");
        check(!GroundFootprint.supported(FLOOR,new FlightVector(13,0,0),(x,y,z)->true),"support query horizontal bound");
        check(!GroundFootprint.supported(FLOOR,new FlightVector(0,.3,0),(x,y,z)->true),"different floor heights are not fabricated support");
        check(!GroundFootprint.supported(new FlightVector(1e15,0,0),new FlightVector(1e15,0,0),(x,y,z)->true),"out-of-world coordinates fail before integer conversion");
        var contact=new GroundCombat();var near=new FlightVector(0,.085,0);contact.begin(near,FLOOR,(a,b)->true);
        var predicted=new FlightVector(.2,-.3,.1);
        var clipped=contact.landingSweep(near,predicted,(a,b)->true,(a,b)->true);
        check(Math.abs(clipped.y()+.085)<1e-9&&clipped.x()==.2&&clipped.z()==.1,"only validated landing query ends at floor, full XZ preserved");
        check(contact.landingSweep(near,predicted,(a,b)->true,(a,b)->GroundFootprint.supported(a,b,(x,y,z)->x!=0||z!=0)).equals(predicted),"missing floor tile keeps full conservative guard");
        boolean[] support={true};contact.landingSweep(near,predicted,(a,b)->true,(a,b)->support[0]);support[0]=false;
        check(contact.landingSweep(near,predicted,(a,b)->true,(a,b)->support[0]).equals(predicted),"changed support is rechecked, not cached from earlier proof");
        check(contact.landingSweep(near,predicted,(a,b)->b.x()<=.1,(a,b)->true).equals(predicted),"horizontal wall keeps guard");
        check(contact.landingSweep(near,predicted,(a,b)->false,(a,b)->true).equals(predicted),"unloaded body route keeps guard");
        check(contact.landingSweep(new FlightVector(0,-.01,0),predicted,(a,b)->true,(a,b)->true).equals(predicted),"never clip below validated plane");
        var contactVelocity=FlightVector.ZERO;var contactPosition=near;var contactController=new FlightController();
        for(int i=0;i<40&&!contact.grounded();i++){
            var s=contact.step(contactPosition,null,REGION,false,null,(a,b)->true,(a,b)->true,.5);
            var next=contactController.step(contactVelocity,s.intent());
            var sweep=contact.landingSweep(contactPosition,contactController.clearanceSweep(contactVelocity,next),(a,b)->true,(a,b)->true);
            if(contactPosition.y()+sweep.y()< -1e-9)next=contactController.step(contactVelocity,FlightController.Intent.brake());
            contactPosition=contactPosition.add(next);contactVelocity=next;
            if(contactPosition.y()<0){contactPosition=new FlightVector(contactPosition.x(),0,contactPosition.z());contactVelocity=new FlightVector(next.x(),0,next.z());}
        }
        check(contact.grounded()&&contactPosition.y()<.02,"actual .085 hover with predictive stopping guard can land without teleport");
        var ground=new GroundCombat();
        check(!ground.begin(new FlightVector(0,4,0),null,(a,b)->true),"blocked up/down is not support");
        check(!ground.begin(new FlightVector(0,9,0),FLOOR,(a,b)->true),"descent bounded8");
        check(!ground.begin(new FlightVector(0,4,0),FLOOR,(a,b)->false),"blocked descent rejects landing");
        check(ground.begin(new FlightVector(0,4,0),FLOOR,(a,b)->true),"verified support begins landing");
        var controller=new FlightController();var position=new FlightVector(0,4,0);var velocity=FlightVector.ZERO;
        for(int i=0;i<180&&ground.state().phase()==GroundCombat.Phase.LANDING;i++){
            var state=ground.step(position,null,REGION,false,null,(a,b)->true,(a,b)->true,.5);
            velocity=controller.step(velocity,state.intent());position=position.add(velocity);
            check(velocity.length()<=.650001,"same controller cap during landing");
        }
        check(ground.state().phase()==GroundCombat.Phase.GROUNDED&&Math.abs(position.y())<.15,"physical landing without teleport");
        // Actual supported position is supplied by the world adapter, not invented by the state machine.
        position=FLOOR;velocity=FlightVector.ZERO;
        var target=new FlightVector(0,0,-12);
        boolean moved=false,paused=false;
        for(int i=0;i<260;i++){
            var state=ground.step(position,target,REGION,false,null,(a,b)->true,(a,b)->true,.5);
            moved|=state.intent().mode()==FlightController.Mode.MOVE;paused|=state.intent().mode()!=FlightController.Mode.MOVE;
            check(state.intent().speed()<=.55,"ordinary ground scuttle cap");
            velocity=controller.step(velocity,state.intent());position=position.add(velocity);
            check(Math.abs(position.y())<1e-9,"ground intent does not fabricate vertical motion");
        }
        check(moved&&paused,"finite crossing legs and readable pauses");
        var blocked=ground.step(position,target,REGION,false,null,(a,b)->true,(a,b)->false,.5);
        check(blocked.intent().mode()!=FlightController.Mode.MOVE,"ledge or wall invalidates supported sweep");
        check(ground.step(position,null,REGION,false,null,(a,b)->true,(a,b)->true,.5).intent().mode()!=FlightController.Mode.MOVE,"unobserved target cancels crossing");
        ground=new GroundCombat();ground.begin(position,position,(a,b)->true);
        ground.step(position,target,REGION,false,null,(a,b)->true,(a,b)->true,.5);
        var ascent=position.add(new FlightVector(0,4,0));
        for(int i=0;i<19;i++)ground.step(position,target,REGION,true,ascent,(a,b)->true,(a,b)->true,.5);
        check(ground.state().phase()==GroundCombat.Phase.GROUNDED,"19clear samples cannot take off");
        ground.step(position,target,REGION,false,null,(a,b)->true,(a,b)->true,.5);
        for(int i=0;i<19;i++)ground.step(position,target,REGION,true,ascent,(a,b)->true,(a,b)->true,.5);
        check(ground.state().phase()==GroundCombat.Phase.GROUNDED,"transient obstruction resets clear dwell");
        ground.step(position,target,REGION,true,ascent,(a,b)->true,(a,b)->true,.5);
        check(ground.state().phase()==GroundCombat.Phase.TAKEOFF,"20clear samples plus verified ascent");
        ground.step(position,target,REGION,true,ascent,(a,b)->false,(a,b)->true,.5);
        check(ground.state().phase()==GroundCombat.Phase.SAFE_HOLD,"blocked takeoff explicit safe fallback");
        var timeout=new GroundCombat();timeout.begin(new FlightVector(0,4,0),FLOOR,(a,b)->true);
        for(int i=0;i<161;i++)timeout.step(new FlightVector(0,4,0),target,REGION,false,null,(a,b)->true,(a,b)->true,.5);
        check(timeout.state().phase()==GroundCombat.Phase.SAFE_HOLD,"landing cannot pursue forever");
        var attack=new GroundAttack();int fires=0,last=-1;
        for(int i=0;i<150;i++){
            var s=attack.step(new FlightVector(0,0,-1),true);
            if(s.fire()){check(last<0||i-last==34,"8charge+6face+20quiet cadence");last=i;fires++;}
        }
        check(fires>=3,"lower-damage frequent singles");
        while(!attack.state().fire())attack.step(new FlightVector(0,0,-1),true);
        attack.invalidateTarget();
        check(attack.step(null,false).face(),"committed six-tick firing expression survives target loss");
        for(int i=0;i<6;i++)attack.step(null,false);
        check(!attack.state().face()&&!attack.state().fire(),"no new shot after target loss");
        System.out.println("PASS: "+checks+" Ground movement/cadence checks");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

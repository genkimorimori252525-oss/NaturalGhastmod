package com.genki.soutoughast.entity.ai.flight;

public final class RallyAimTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        var target=new FlightVector(26,224.9,20);
        var locked=RallyAim.lock(new FlightVector(25,229,30),target,FlightVector.ZERO);
        check(locked.equals(target),"stationary target freezes exact body center");
        for(var origin:new FlightVector[]{new FlightVector(21,228,28),new FlightVector(29,227,26),new FlightVector(26,225,23)}){
            var direction=RallyAim.direction(origin,locked);
            check(origin.add(direction.scale(target.subtract(origin).length())).subtract(target).length()<1e-9,"ray from actual off-center ball reaches target");
            check(Math.abs(direction.length()-1)<1e-9,"unit return direction");
        }
        var moving=RallyAim.lock(FlightVector.ZERO,target,new FlightVector(.2,0,0));
        check(moving.subtract(target).length()<=2.000000001,"observed movement lead stays bounded");
        var before=moving;target=target.add(new FlightVector(0,0,8));
        check(moving.equals(before),"late dodge cannot change frozen endpoint");
        check(RallyAim.direction(locked,locked).equals(FlightVector.ZERO),"coincident endpoint never yields invalid velocity");
        var reaction=new RallyReaction();check(reaction.begin(3,new FlightVector(0,0,1),.1),"normal fallible reaction remains");
        for(int i=0;i<5;i++)check(!reaction.step(true,true).fire(),"no instant return before six-tick tell");
        check(reaction.step(true,true).fire(),"return remains physically reach gated");
        reaction.reset();check(!reaction.begin(4,new FlightVector(0,0,1),.9),"declined receive remains possible");
        System.out.println("PASS: "+checks+" Rally endpoint/origin and counterplay checks");
    }
}

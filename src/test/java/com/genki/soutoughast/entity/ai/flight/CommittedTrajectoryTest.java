package com.genki.soutoughast.entity.ai.flight;

public final class CommittedTrajectoryTest {
    private static int checks;
    public static void main(String[] args){
        FlightVector start=new FlightVector(0,8,0),end=new FlightVector(0,2,60);
        var burst=CommittedTrajectory.burst(start,end);
        check(burst.points().size()<=97,"finite bounded path");
        for(int i=0;i<16;i++)check(Math.abs(burst.velocity(i).length()-1.14)<1e-9,"declared slow speed");
        double previous=1.14;
        for(int i=16;i<20;i++){double next=burst.velocity(i).length();check(next>previous&&next<=3.04,"smooth acceleration");previous=next;}
        check(Math.abs(burst.velocity(20).length()-3.04)<1e-9,"explicit1.6x settled-reference speed");
        check(burst.phase(12)==CommittedTrajectory.Phase.WARNING&&burst.phase(16)==CommittedTrajectory.Phase.BURST,"cue before acceleration");
        check(burst.points().get(burst.points().size()-1).equals(end),"locked endpoint");
        for(var strength:CommittedTrajectory.Strength.values())for(int side:new int[]{-1,1}){
            var curve=CommittedTrajectory.curve(start,end,strength,side);
            double priorZ=0;double peak=0;
            for(int i=0;i<curve.points().size();i++){
                var p=curve.points().get(i);check(p.z()>=priorZ,"no forward U-turn");priorZ=p.z();peak=Math.max(peak,Math.abs(p.x()));
                if(i<curve.points().size()-1)check(curve.velocity(i).length()<=1.9,"curve bounded reference speed");
            }
            check(peak>strength.amplitude()*.95,"legible declared arc");
            check(curve.points().get(curve.points().size()-1).equals(end),"committed curve exit");
        }
        var lob=CommittedTrajectory.lob(start,new FlightVector(0,0,30),8);
        check(lob.velocity(0).y()>0,"visible upward launch");
        check(lob.velocity(lob.points().size()-2).y()<0,"visible descent");
        check(lob.points().stream().mapToDouble(FlightVector::y).max().orElseThrow()>10,"coherent apex");
        var verticalStart=new FlightVector(0,0,0);var verticalEnd=new FlightVector(0,12,0);
        boolean highRejected=false;try{CommittedTrajectory.lob(verticalStart,verticalEnd,10);}catch(IllegalArgumentException expected){highRejected=true;}
        check(highRejected,"high arc exceeds segment speed bound");
        check(CommittedTrajectory.lobCandidates(verticalStart,verticalEnd,10,8,6).size()==2,"one infeasible high arc must not suppress valid lower arcs");
        var recipe=new CommittedProfile(CommittedTrajectory.Kind.LOB,null,0,8,new FlightVector(0,0,30));
        var movedMuzzle=start.add(new FlightVector(1,0,0));var launched=recipe.pathFrom(movedMuzzle);
        check(launched.points().get(0).equals(movedMuzzle),"actual muzzle is the physical start, not a predicted or old start");
        check(launched.points().get(launched.points().size()-1).equals(recipe.endpoint())&&recipe.lobHeight()==8,"unchanged committed parameters and endpoint");
        check(!TrajectoryValidation.verify(launched,(i,a,b,last)->TrajectoryValidation.Segment.BLOCKED).clear(),"invalid actual launch cancels, without alternate recipe");
        boolean immutable=false;try{launched.points().set(0,start);}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"complete spawned path is immutable");
        for(var path:new CommittedTrajectory[]{burst,lob,CommittedTrajectory.curve(start,end,CommittedTrajectory.Strength.NORMAL,1)}){
            var flight=new CommittedTrajectory.Flight(path);
            check(flight.next()!=null,"initial committed step");flight.normalize();check(flight.next()==null&&flight.normalized(),"deflection immediately removes all special motion");
            var miss=new CommittedTrajectory.Flight(path);for(int i=1;i<path.points().size();i++)check(miss.next()!=null,"finite travel");
            check(miss.next()==null&&!miss.normalized(),"finite miss ends without straight continuation");
        }
        System.out.println("PASS: "+checks+" committed trajectory checks");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

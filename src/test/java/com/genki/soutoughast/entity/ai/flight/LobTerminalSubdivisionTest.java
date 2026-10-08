package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayList;
import java.util.List;

/** Spatial geometry only: no loaded/native impact acceptance. */
public final class LobTerminalSubdivisionTest {
    private static int checks;
    public static void main(String[] args){
        var start=new FlightVector(15,231.8,25.3);var end=new FlightVector(42,223.75,26);
        var raw=CommittedTrajectory.lob(start,end,10);var recipe=new CommittedProfile(CommittedTrajectory.Kind.LOB,null,0,10,end);var refined=recipe.pathFrom(start);
        check(refined.points().size()==raw.points().size()+1,"MISSING_SAFE_LOB_TERMINAL_SUBDIVISION");
        geometry();bounds();
        System.out.println("PASS: "+checks+" Lob terminal spatial/guard assertions");
    }
    private static int cells(FlightVector a,FlightVector b){
        int x=(int)Math.floor(Math.max(a.x(),b.x())+.5-1e-7)-(int)Math.floor(Math.min(a.x(),b.x())-.5+1e-7)+1;
        int z=(int)Math.floor(Math.max(a.z(),b.z())+.5-1e-7)-(int)Math.floor(Math.min(a.z(),b.z())-.5+1e-7)+1;
        return x*z;
    }
    private static void geometry(){
        for(double x:new double[]{42,42.5,42.49999,42.50001,41.999999,-42,-42.5})for(double z:new double[]{26,26.5,26.49999,26.50001,-26,-26.5})for(double y:new double[]{223.65,223.75,223.9})for(double height:new double[]{6,8,10}){
            var start=new FlightVector(x-27,231.8,z-.7);var end=new FlightVector(x,y,z);var raw=CommittedTrajectory.lob(start,end,height);var output=new CommittedProfile(CommittedTrajectory.Kind.LOB,null,0,height,end).pathFrom(start);
            verify(raw,output);
        }
        for(double dx:new double[]{-.9,-.7,.7,.9})for(double dz:new double[]{-.9,-.7,.7,.9})for(double y:new double[]{224.1,224.7,225.1}){
            var end=new FlightVector(42,223.75,26);var from=end.add(new FlightVector(dx,y-end.y(),dz));var raw=new CommittedTrajectory(CommittedTrajectory.Kind.LOB,List.of(from,end));
            verify(raw,LobTerminalSubdivision.refine(raw));
        }
    }
    private static void verify(CommittedTrajectory raw,CommittedTrajectory output){
        var old=raw.points();var points=output.points();var from=old.get(old.size()-2);var end=old.get(old.size()-1);
        check(points.get(points.size()-1).equals(end),"exact observed endpoint");check(points.size()<=97&&points.size()<=old.size()+1,"bounded single subdivision");
        if(points.size()==old.size()){check(output==raw||points.equals(old),"unchanged refusal/clear geometry");return;}
        for(int i=0;i<old.size()-1;i++)check(points.get(i).equals(old.get(i)),"all original preterminal geometry preserved");
        var split=points.get(points.size()-2);double t=(split.y()-from.y())/(end.y()-from.y());
        check(t>0&&t<1&&split.subtract(from.add(end.subtract(from).scale(t))).length()<1e-8,"point on original final line");
        check(split.y()>Math.floor(end.y())+1+1e-7,"split before first permitted fullheight floor contact");
        check(cells(split,end)<=4,"unchanged four collider cap fits");
        for(int i=0;i<points.size()-1;i++)check(output.velocity(i).length()>1e-9&&output.velocity(i).length()<=1.9+1e-9,"positive bounded segments");
        check(TrajectoryValidation.verify(output,(i,a,b,last)->last?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR).clear(),"complete declared path validator retained");
        check(!TrajectoryValidation.verify(output,(i,a,b,last)->i==points.size()-3?TrajectoryValidation.Segment.BLOCKED:last?TrajectoryValidation.Segment.EXPECTED_TERMINAL:TrajectoryValidation.Segment.CLEAR).clear(),"introduced earlier contact still rejects");
    }
    private static void bounds(){
        var end=new FlightVector(42,223.75,26);var below=new FlightVector(41.2,223.9,25.3);var raw=new CommittedTrajectory(CommittedTrajectory.Kind.LOB,List.of(below,end));
        boolean speedRejected=false;try{new CommittedTrajectory(CommittedTrajectory.Kind.LOB,List.of(new FlightVector(42.9,225.5,26.9),end));}catch(IllegalArgumentException expected){speedRejected="COMMITTED_SEGMENT_SPEED_BOUND".equals(expected.getMessage());}
        check(speedRejected,"refinement never receives or rescues invalid original speed");
        check(cells(below,end)>4&&LobTerminalSubdivision.refine(raw)==raw,"no precontact split retains invalid footprint for validator rejection");
        var points=new ArrayList<FlightVector>();for(int i=0;i<96;i++)points.add(new FlightVector(41.2,225+(95-i)*.01,25.3));points.add(end);
        var cap=new CommittedTrajectory(CommittedTrajectory.Kind.LOB,points);check(LobTerminalSubdivision.refine(cap)==cap&&cells(points.get(95),end)>4,"97point cap never bypassed");
        var clear=CommittedTrajectory.lob(new FlightVector(15,231.8,25.3),new FlightVector(42.5,223.75,26.5),10);check(LobTerminalSubdivision.refine(clear)==clear,"halfgrid fitting path has no timing change");
        var other=CommittedTrajectory.curve(new FlightVector(15,231.8,25.3),new FlightVector(42,229,26),CommittedTrajectory.Strength.SHALLOW,1);check(LobTerminalSubdivision.refine(other)==other,"nonLob untouched");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import net.minecraft.util.Mth;
import java.util.List;

/** Actual mapped footprint agreement, not loaded world/native acceptance. */
public final class LobTerminalFixtureTest {
    public static void main(String[] args){
        int checks=0;var start=new FlightVector(15,231.8,25.3);
        for(double x:new double[]{42,42.5,42.49999,42.50001,-42,-42.5})for(double z:new double[]{26,26.5,26.49999,26.50001,-26,-26.5})for(double height:new double[]{6,8,10}){
            var from=new FlightVector(x-27,start.y(),z-.7);var end=new FlightVector(x,223.75,z);var raw=CommittedTrajectory.lob(from,end,height);var refined=LobTerminalSubdivision.refine(raw);
            for(var path:new CommittedTrajectory[]{raw,refined}){
                var a=path.points().get(path.points().size()-2);var terminal=CommittedPathClearance.terminalFootprint(CommittedPathClearance.sweptBox(a,end),Mth.floor(end.y()));
                if(terminal.isEmpty()!=(LobTerminalSubdivision.cells(a,end)>4))throw new AssertionError("MAPPED_TERMINAL_FOOTPRINT_DISAGREEMENT");checks++;
            }
            if(refined.points().size()>raw.points().size()){
                var split=refined.points().get(refined.points().size()-2);var prior=refined.points().get(refined.points().size()-3);
                if(CommittedPathClearance.sweptBox(prior,split).minY<=Math.floor(end.y())+1)throw new AssertionError("EARLIER_FLOOR_CONTACT");checks++;
            }
        }
        var target=LobTerminalProbe.TARGET_START;var end=new FlightVector(target.x,target.y-.25,target.z);
        for(double height:new double[]{10,8,6}){
            var raw=CommittedTrajectory.lob(start,end,height);var path=new CommittedProfile(CommittedTrajectory.Kind.LOB,null,0,height,end).pathFrom(start);var points=path.points();
            if(path.points().size()!=raw.points().size()+1||LobTerminalSubdivision.cells(raw.points().get(raw.points().size()-2),end)<=4||CommittedPathClearance.terminalFootprint(CommittedPathClearance.sweptBox(points.get(points.size()-2),end),Mth.floor(end.y())).isEmpty())throw new AssertionError("NATIVE_INTEGER_FIXTURE_REFINEMENT_REQUIRED");checks++;
        }
        var invalid=new CommittedTrajectory(CommittedTrajectory.Kind.LOB,List.of(new FlightVector(41.2,223.9,25.3),new FlightVector(42,223.75,26)));var refused=LobTerminalSubdivision.refine(invalid);
        if(refused!=invalid||!CommittedPathClearance.terminalFootprint(CommittedPathClearance.sweptBox(refused.points().get(0),refused.points().get(1)),223).isEmpty())throw new AssertionError("UNREFINABLE_STILL_REJECTED");checks++;
        System.out.println("PASS:"+checks+"mapped footprint/earlier-contact/private integer landing checks");
    }
}

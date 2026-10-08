package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
import net.minecraft.util.Mth;

/** Private fixture must fit the unchanged production <=4 exact terminal-collider contract. */
public final class CoverLobFixtureTest {
    public static void main(String[] args){
        var start=new FlightVector(15,231.8,25.3);var target=CoverLobProbe.TARGET_START;
        var end=new FlightVector(target.x,target.y-.25,target.z);
        for(double height:new double[]{10,8,6}){
            var points=CommittedTrajectory.lob(start,end,height).points();var sweep=CommittedPathClearance.sweptBox(points.get(points.size()-2),end);
            if(CommittedPathClearance.terminalFootprint(sweep,Mth.floor(end.y())).isEmpty())throw new AssertionError("STATIC_COVER_FIXTURE_TERMINAL_BOUND: height="+height+",end="+end+",sweep="+sweep);
        }
        System.out.println("PASS:3private landing/unchanged terminal-footprint checks; not native feasibility");
    }
}

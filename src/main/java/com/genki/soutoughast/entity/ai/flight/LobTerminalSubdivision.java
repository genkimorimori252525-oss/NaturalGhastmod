package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayList;

/** At most one point on the existing final Lob line; native full-path validation still owns acceptance. */
public final class LobTerminalSubdivision {
    private static final double EPS=1e-6;
    public static CommittedTrajectory refine(CommittedTrajectory path){
        if(path.kind()!=CommittedTrajectory.Kind.LOB)return path;
        var points=path.points();var from=points.get(points.size()-2);var end=points.get(points.size()-1);
        if(cells(from,end)<=4||points.size()==97)return path;
        double drop=from.y()-end.y(),surface=Math.floor(end.y())+1;
        if(drop<=0||from.y()<=surface+EPS)return path;
        double beforeContact=(from.y()-surface-EPS)/drop;
        double required=Math.max(requiredFraction(from.x(),end.x()),requiredFraction(from.z(),end.z()));
        if(!Double.isFinite(required)||required>=beforeContact||beforeContact<=0||beforeContact>=1)return path;
        // Interior of both constraints, avoiding exact cell/contact boundaries.
        double fraction=(required+beforeContact)*.5;
        var split=from.add(end.subtract(from).scale(fraction));
        if(split.y()<=surface+EPS||cells(split,end)>4||split.subtract(from).length()<=1e-9||end.subtract(split).length()<=1e-9)return path;
        var refined=new ArrayList<>(points);refined.add(points.size()-1,split);
        return new CommittedTrajectory(path.kind(),refined);
    }
    private static double requiredFraction(double from,double end){
        int min=(int)Math.floor(end-.5+1e-7),max=(int)Math.floor(end+.5-1e-7);
        if(max-min==0){if(from<end)min--;else max++;}
        double low=min+.5+EPS,high=max+.5-EPS;
        if(from<low)return (low-from)/(end-from);
        if(from>high)return (high-from)/(end-from);
        return 0;
    }
    public static int cells(FlightVector from,FlightVector to){
        int x=(int)Math.floor(Math.max(from.x(),to.x())+.5-1e-7)-(int)Math.floor(Math.min(from.x(),to.x())-.5+1e-7)+1;
        int z=(int)Math.floor(Math.max(from.z(),to.z())+.5-1e-7)-(int)Math.floor(Math.min(from.z(),to.z())-.5+1e-7)+1;
        return x*z;
    }
    private LobTerminalSubdivision() {}
}

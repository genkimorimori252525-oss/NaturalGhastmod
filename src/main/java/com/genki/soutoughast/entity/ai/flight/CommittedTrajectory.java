package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayList;
import java.util.List;

/** Complete finite world-space trajectory, never reads a target after construction. */
public record CommittedTrajectory(Kind kind,List<FlightVector> points) {
    public enum Kind { BURST, CURVE, LOB, BOMB;
        public boolean terminalImpact(){return this==LOB||this==BOMB;}
    }
    public enum Phase { SLOW, WARNING, BURST, FAST, OUTWARD, SWEEP, ASCEND, DESCEND, DONE }
    public enum Strength {
        SHALLOW(2), NORMAL(4), DEEP(6);
        private final double amplitude;
        Strength(double amplitude){this.amplitude=amplitude;}
        public double amplitude(){return amplitude;}
    }
    public static final double STANDARD_SETTLED_SPEED=1.9, BURST_FAST_SPEED=STANDARD_SETTLED_SPEED*1.6;
    public CommittedTrajectory {
        if(kind==null||points==null||points.size()<2||points.size()>97)throw new IllegalArgumentException("BOUNDED_COMMITTED_PATH_REQUIRED");
        points=List.copyOf(points);
        for(int i=0;i<points.size();i++){
            var point=points.get(i);
            if(Math.abs(point.x())>30_000_000||Math.abs(point.z())>30_000_000||Math.abs(point.y())>2048)throw new IllegalArgumentException("WORLD_PATH_BOUND_EXCEEDED");
            if(i>0){double speed=point.subtract(points.get(i-1)).length();
                if(speed<1e-9||speed>(kind==Kind.BURST?BURST_FAST_SPEED:STANDARD_SETTLED_SPEED)+1e-9)throw new IllegalArgumentException("COMMITTED_SEGMENT_SPEED_BOUND");}
        }
    }
    public FlightVector velocity(int index){return points.get(index+1).subtract(points.get(index));}
    public Phase phase(int index){
        if(index>=points.size()-1)return Phase.DONE;
        return switch(kind){
            case BURST -> index<12?Phase.SLOW:index<16?Phase.WARNING:index<20?Phase.BURST:Phase.FAST;
            case CURVE -> index<(points.size()-1)/2?Phase.OUTWARD:Phase.SWEEP;
            case LOB -> velocity(index).y()>0?Phase.ASCEND:Phase.DESCEND;
            case BOMB -> Phase.DESCEND;
        };
    }
    private static double distance(FlightVector start,FlightVector end,double min){
        double d=end.subtract(start).length();if(d<min||d>64)throw new IllegalArgumentException("COMMITTED_RANGE_INVALID");return d;
    }
    public static CommittedTrajectory burst(FlightVector start,FlightVector end){
        double distance=distance(start,end,44),travel=0;var direction=end.subtract(start).normalized();var points=new ArrayList<FlightVector>();points.add(start);
        for(int tick=0;travel<distance;tick++){
            double speed=tick<16?1.14:tick<20?1.14+(BURST_FAST_SPEED-1.14)*(tick-15)/4:BURST_FAST_SPEED;
            travel=Math.min(distance,travel+speed);points.add(travel==distance?end:start.add(direction.scale(travel)));
        }
        return new CommittedTrajectory(Kind.BURST,points);
    }
    public static CommittedTrajectory curve(FlightVector start,FlightVector end,Strength strength,int side){
        double distance=distance(start,end,12);if(side!=1&&side!=-1)throw new IllegalArgumentException("CURVE_SIDE_REQUIRED");
        var offset=end.subtract(start);var lateral=new FlightVector(-offset.z(),0,offset.x()).normalized();
        if(lateral.length()<1e-9)throw new IllegalArgumentException("HORIZONTAL_CURVE_REQUIRED");
        int steps=(int)Math.ceil(Math.max(distance/1.4,strength.amplitude()*Math.PI));var points=new ArrayList<FlightVector>();
        for(int i=0;i<=steps;i++){
            double t=(double)i/steps;points.add(i==steps?end:start.add(offset.scale(t)).add(lateral.scale(side*strength.amplitude()*Math.sin(Math.PI*t))));
        }
        return new CommittedTrajectory(Kind.CURVE,points);
    }
    public static CommittedTrajectory lob(FlightVector start,FlightVector end,double height){
        double distance=distance(start,end,12);if(height<4||height>12)throw new IllegalArgumentException("LOB_HEIGHT_REQUIRED");
        int steps=(int)Math.ceil(distance/1.2+height*1.2);var offset=end.subtract(start);var points=new ArrayList<FlightVector>();
        for(int i=0;i<=steps;i++){
            double t=(double)i/steps;points.add(i==steps?end:start.add(offset.scale(t)).add(new FlightVector(0,height*Math.sin(Math.PI*t),0)));
        }
        return new CommittedTrajectory(Kind.LOB,points);
    }
    public static List<CommittedTrajectory> lobCandidates(FlightVector start,FlightVector end,double... heights){
        var candidates=new ArrayList<CommittedTrajectory>();
        if(heights.length>3)throw new IllegalArgumentException("BOUNDED_LOB_CANDIDATES_REQUIRED");
        for(double height:heights){
            try{candidates.add(lob(start,end,height));}
            catch(IllegalArgumentException rejected){
                if(!"COMMITTED_SEGMENT_SPEED_BOUND".equals(rejected.getMessage())&&!"COMMITTED_RANGE_INVALID".equals(rejected.getMessage()))throw rejected;
            }
        }
        return List.copyOf(candidates);
    }
    public static CommittedTrajectory bomb(FlightVector start,FlightVector end){
        double distance=distance(start,end,4);var offset=end.subtract(start);
        if(offset.y()>=0||Math.hypot(offset.x(),offset.z())>1)throw new IllegalArgumentException("VERTICAL_BOMB_CORRIDOR_REQUIRED");
        int steps=(int)Math.ceil(distance/1.2);var points=new ArrayList<FlightVector>();
        for(int i=0;i<=steps;i++)points.add(i==steps?end:start.add(offset.scale((double)i/steps)));
        return new CommittedTrajectory(Kind.BOMB,points);
    }
    /** Serializable clock; normalization deliberately ends every special trajectory phase. */
    public static final class Flight {
        private final CommittedTrajectory path;
        private int index;
        private boolean normalized;
        public Flight(CommittedTrajectory path){this(path,0,false);}
        public Flight(CommittedTrajectory path,int index,boolean normalized){
            if(path==null||index<0||index>path.points().size()-1)throw new IllegalArgumentException("COMMITTED_CLOCK_INVALID");
            this.path=path;this.index=index;this.normalized=normalized;
        }
        public CommittedTrajectory path(){return path;}
        public int index(){return index;}
        public boolean normalized(){return normalized;}
        public void normalize(){normalized=true;}
        public FlightVector next(){return normalized||index>=path.points().size()-1?null:path.velocity(index++);}
    }
}

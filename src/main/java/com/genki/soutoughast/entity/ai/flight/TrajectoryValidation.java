package com.genki.soutoughast.entity.ai.flight;

/** Testable finite query order; the mapped adapter supplies actual loaded collider results. */
public final class TrajectoryValidation {
    public enum Segment { CLEAR, EXPECTED_TERMINAL, BLOCKED, UNLOADED }
    @FunctionalInterface public interface Probe {Segment inspect(int index,FlightVector start,FlightVector end,boolean last);}
    public record Result(boolean clear,int segments,String reason) {}
    public static Result verify(CommittedTrajectory path,Probe probe){
        int count=path.points().size()-1;
        for(int i=0;i<count;i++){
            boolean last=i==count-1;var status=probe.inspect(i,path.points().get(i),path.points().get(i+1),last);
            boolean terminal=last&&path.kind()==CommittedTrajectory.Kind.LOB;
            if(status!=(terminal?Segment.EXPECTED_TERMINAL:Segment.CLEAR))return new Result(false,i+1,status==Segment.CLEAR?"DECLARED_TERMINAL_MISSING":status.name());
        }
        return new Result(true,count,"COMPLETE_FINITE_PATH");
    }
    private TrajectoryValidation() {}
}

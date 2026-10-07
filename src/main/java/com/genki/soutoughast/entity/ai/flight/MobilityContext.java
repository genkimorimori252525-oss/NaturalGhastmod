package com.genki.soutoughast.entity.ai.flight;

/** Bounded observed clearance, not biome labels or a promise of ground locomotion. */
public final class MobilityContext {
    public enum Kind { OPEN_AIR, SEMI_OPEN, CONFINED, GROUND_FORCED }
    // Eight horizontal world directions (+Z clockwise), then up/down.
    public record Sample(int clearMask) {
        public Sample { if(clearMask<0||clearMask>1023)throw new IllegalArgumentException("Invalid clearance sample"); }
        public boolean clear(int index){return (clearMask&(1<<index))!=0;}
        public boolean allows(FlightVector direction){
            if(direction.length()<1e-9)return true;
            FlightVector d=direction.normalized();
            if(d.y()>.25&&!clear(8)||d.y()<-.25&&!clear(9))return false;
            if(Math.hypot(d.x(),d.z())<.1)return true;
            int sector=Math.floorMod((int)Math.round(Math.atan2(d.x(),d.z())/(Math.PI/4)),8);
            return clear(sector);
        }
        public Kind kind(){
            int horizontal=Integer.bitCount(clearMask&255);
            if(!clear(8)&&!clear(9))return Kind.GROUND_FORCED;
            if(horizontal>=6&&clear(8)&&clear(9))return Kind.OPEN_AIR;
            if(horizontal>=3&&clear(8)&&clear(9))return Kind.SEMI_OPEN;
            return Kind.CONFINED;
        }
    }
    private Kind current=Kind.CONFINED,pending;
    private int matching;
    public Kind current(){return current;}
    public Kind update(Sample sample){
        Kind observed=sample.kind();
        if(observed==current){pending=null;matching=0;return current;}
        matching=observed==pending?matching+1:1;pending=observed;
        if(matching>=(observed.ordinal()>current.ordinal()?6:20)){current=observed;pending=null;matching=0;}
        return current;
    }
    public void reset(){current=Kind.CONFINED;pending=null;matching=0;}
    public static FlightVector direction(int index){
        if(index==8)return new FlightVector(0,1,0);
        if(index==9)return new FlightVector(0,-1,0);
        if(index<0||index>9)throw new IllegalArgumentException("Invalid clearance direction");
        double angle=index*Math.PI/4;
        return new FlightVector(Math.sin(angle),0,Math.cos(angle));
    }
}

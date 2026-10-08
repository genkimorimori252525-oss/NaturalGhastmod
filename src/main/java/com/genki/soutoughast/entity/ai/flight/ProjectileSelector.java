package com.genki.soutoughast.entity.ai.flight;

/** Observable combat problem and bounded repetition memory, not a random rotation. */
public final class ProjectileSelector {
    public enum Choice { STANDARD, BURST, CURVE, LOB }
    private final int[] recent=new int[Choice.values().length];
    public void tick(){for(int i=0;i<recent.length;i++)if(recent[i]>0)recent[i]--;}
    public void reset(){java.util.Arrays.fill(recent,0);}
    public void record(Choice choice){recent[choice.ordinal()]=600;}
    /** STANDARD means decline, never permission to shoot an unseen target. */
    public Choice chooseCover(MobilityContext.Kind context,double range,double variation){
        if(context==null||context==MobilityContext.Kind.GROUND_FORCED||!Double.isFinite(range)||range<12||range>64||!Double.isFinite(variation))return Choice.STANDARD;
        double score=1.4*(1-.75*recent[Choice.LOB.ordinal()]/600.0)+.12*Math.sin(variation*6.28+Choice.LOB.ordinal()*2);
        return score>1?Choice.LOB:Choice.STANDARD;
    }
    public Choice choose(MobilityContext.Kind context,double range,FlightVector observedVelocity,int stationaryTicks,double variation){
        return choose(context,range,observedVelocity,stationaryTicks,variation,false);
    }
    public Choice choose(MobilityContext.Kind context,double range,FlightVector observedVelocity,int stationaryTicks,double variation,boolean repeatedChargeResponse){
        if(context==MobilityContext.Kind.GROUND_FORCED||observedVelocity==null||range<12)return Choice.STANDARD;
        double[] weights={1,range>=47&&context!=MobilityContext.Kind.CONFINED?1.7:0,Math.hypot(observedVelocity.x(),observedVelocity.z())>.08?1.5:.35,stationaryTicks>=40?1.4:.3};
        if(weights[Choice.BURST.ordinal()]>0&&repeatedChargeResponse)weights[Choice.BURST.ordinal()]+=.3*Math.max(0,Math.sin(variation*6.28));
        Choice best=Choice.STANDARD;double bestScore=1;
        for(Choice candidate:Choice.values()){
            if(candidate==Choice.STANDARD)continue;
            double score=weights[candidate.ordinal()]*(1-.75*recent[candidate.ordinal()]/600.0)+.12*Math.sin(variation*6.28+candidate.ordinal()*2);
            if(score>bestScore){best=candidate;bestScore=score;}
        }
        return best;
    }
}

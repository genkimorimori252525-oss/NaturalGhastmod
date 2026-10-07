package com.genki.soutoughast.entity.ai.flight;

/** Fixed-size recent maneuver weights; no persistent history or player input knowledge. */
public final class TacticalMemory {
    private static final int WINDOW=300;
    private final int[] remaining=new int[TacticalEvaluator.Action.values().length];
    public void tick(){for(int i=0;i<remaining.length;i++)if(remaining[i]>0)remaining[i]--;}
    public void record(TacticalEvaluator.Action action){if(action!=TacticalEvaluator.Action.DRIFT)remaining[action.ordinal()]=WINDOW;}
    public double weight(TacticalEvaluator.Action action){return 1-.75*remaining[action.ordinal()]/WINDOW;}
    public void clear(){java.util.Arrays.fill(remaining,0);}
}

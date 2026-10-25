package com.genki.soutoughast.entity.ai.flight;

/** One sampled opportunity and one current-observation disposition, never a per-tick reroll. */
public final class StandardShotRhythm {
    public enum Outcome { UNDECIDED, FIRE, DECLINE, SAFETY_CANCEL }
    public record Cycle(int interval,double inclination) {
        public Cycle { if(interval<60||interval>90)throw new IllegalArgumentException("SHOT_INTERVAL");variation(inclination); }
    }
    public record Context(boolean visible,boolean inRegion,boolean muzzleClear,double range,double speed,double relativeMotion) {
        public Context { for(double n:new double[]{range,speed,relativeMotion})if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("SHOT_CONTEXT"); }
    }
    public static final Cycle DEFAULT=new Cycle(75,0);
    public static final Context CLEAR=new Context(true,true,true,28,.1,0);
    private StandardShotRhythm(){}
    private static void variation(double n){if(!Double.isFinite(n)||n<0||n>=1)throw new IllegalArgumentException("SHOT_VARIATION");}
    public static Cycle sample(double timing,double disposition){variation(timing);return new Cycle(60+(int)(timing*31),disposition);}
    public static int initialQuiet(double timing){variation(timing);return 30+(int)(timing*31);}
    public static Outcome decide(Cycle cycle,Context context){
        if(!context.visible()||!context.muzzleClear()||context.range()<8||context.range()>64)return Outcome.SAFETY_CANCEL;
        double chance=.55+(context.inRegion()?.12:0)+(context.range()>=22&&context.range()<=34?.1:0)+(context.speed()<=.14?.08:0);
        chance-=Math.min(.2,context.relativeMotion()*.3);
        if(context.range()<16||context.range()>42)chance-=.15;
        if(context.speed()>.24)chance-=.15;
        chance=Math.max(.25,Math.min(.85,chance));
        return cycle.inclination()<chance?Outcome.FIRE:Outcome.DECLINE;
    }
}

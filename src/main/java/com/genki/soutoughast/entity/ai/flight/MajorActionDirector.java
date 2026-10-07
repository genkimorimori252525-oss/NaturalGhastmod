package com.genki.soutoughast.entity.ai.flight;

/** Major spacing is independent of ordinary projectile selection; Grand Danmaku is absent. */
public final class MajorActionDirector {
    private int quiet=240,recent;
    private boolean active;
    public void tick(boolean ordinaryCombat){if(ordinaryCombat&&quiet>0)quiet--;if(recent>0)recent--;}
    public boolean shouldBegin(MobilityContext.Kind context,boolean observedClearance,boolean offensiveBusy,double range,double variation){
        if(active||quiet>0||recent>0||!observedClearance||offensiveBusy||range<16||range>48)return false;
        return context==MobilityContext.Kind.OPEN_AIR&&variation>.97||context==MobilityContext.Kind.SEMI_OPEN&&variation>.99;
    }
    public void began(){if(active)throw new IllegalStateException("ONE_MAJOR_ACTION_ONLY");active=true;recent=1200;}
    public void finished(){active=false;quiet=600;}
    public void rejected(){quiet=Math.max(quiet,60);}
    public void reset(){active=false;quiet=240;recent=0;}
}

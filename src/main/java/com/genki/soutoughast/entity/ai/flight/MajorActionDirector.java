package com.genki.soutoughast.entity.ai.flight;

/** Major spacing is independent of ordinary projectile selection; Grand Danmaku is absent. */
public final class MajorActionDirector {
    private int quiet=240,recent;
    private boolean active;
    private String decision="NONE";
    public String decision(){return decision;}
    public int quietTicks(){return quiet;}
    public int recentTicks(){return recent;}
    public void tick(boolean ordinaryCombat){if(ordinaryCombat&&quiet>0)quiet--;if(recent>0)recent--;}
    public boolean shouldBegin(MobilityContext.Kind context,boolean observedClearance,boolean offensiveBusy,double range,double variation){
        if(active)return reject("MAJOR_ACTIVE");
        if(quiet>0)return reject("QUIET_INTERVAL");
        if(recent>0)return reject("RECENT_MAJOR");
        if(!observedClearance)return reject("OBSERVATION_OR_CLEARANCE");
        if(offensiveBusy)return reject("OFFENSE_BUSY");
        if(range<16||range>48)return reject("RANGE");
        if(context!=MobilityContext.Kind.OPEN_AIR&&context!=MobilityContext.Kind.SEMI_OPEN)return reject("MOBILITY_CONTEXT");
        boolean selected=context==MobilityContext.Kind.OPEN_AIR&&variation>.97||context==MobilityContext.Kind.SEMI_OPEN&&variation>.99;
        decision=selected?"ELIGIBLE":"VARIATION";return selected;
    }
    private boolean reject(String reason){decision=reason;return false;}
    /** Full participant/floor preparation supplies Domain eligibility independently of Overhead. */
    public boolean shouldBeginDomain(boolean prepared,boolean offensiveBusy,double variation){
        if(active)return reject("MAJOR_ACTIVE");if(quiet>0)return reject("QUIET_INTERVAL");if(recent>0)return reject("RECENT_MAJOR");
        if(!prepared)return reject("DOMAIN_PREPARATION");if(offensiveBusy)return reject("OFFENSE_BUSY");
        boolean selected=Double.isFinite(variation)&&variation>.99&&variation<=1;decision=selected?"DOMAIN_ELIGIBLE":"VARIATION";return selected;
    }
    public void began(){if(active)throw new IllegalStateException("ONE_MAJOR_ACTION_ONLY");active=true;recent=1200;}
    public void finished(){active=false;quiet=600;}
    public void rejected(){quiet=Math.max(quiet,60);}
    public void reset(){active=false;quiet=240;recent=0;decision="NONE";}
}

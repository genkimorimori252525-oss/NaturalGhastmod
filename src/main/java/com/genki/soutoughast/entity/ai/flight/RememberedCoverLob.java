package com.genki.soutoughast.entity.ai.flight;

import java.util.UUID;

/** Single observed snapshot/LOS-loss attempt; no world access or hidden target tracking. */
public final class RememberedCoverLob {
    public record Snapshot(UUID subject,long tick,FlightVector eye,FlightVector landing,MobilityContext.Kind context) {}
    private Snapshot snapshot,frozen;
    private UUID subject;
    private long now=-1;
    private boolean visible,attempted;
    public Snapshot snapshot(){return snapshot;}
    public Snapshot frozen(){return frozen;}
    public long age(){return snapshot==null?-1:now-snapshot.tick();}
    public boolean active(){return frozen!=null;}
    public void reset(){snapshot=frozen=null;subject=null;now=-1;visible=false;attempted=false;}
    public void finish(){frozen=null;}
    public void observe(UUID identity,long tick,FlightVector eye,FlightVector landing,MobilityContext.Kind context,boolean blocked){
        boolean seen=eye!=null&&landing!=null;
        if(tick<0||identity==null||context==null||blocked||context==MobilityContext.Kind.GROUND_FORCED){reset();return;}
        if(now>=0&&(tick!=now+1||!identity.equals(subject))||snapshot!=null&&snapshot.context()!=context)reset();
        subject=identity;now=tick;visible=seen;
        if(seen){snapshot=new Snapshot(identity,tick,eye,landing,context);frozen=null;attempted=false;}
        else if(eye!=null||landing!=null){snapshot=frozen=null;attempted=true;}
        else if(snapshot!=null&&age()>40){snapshot=frozen=null;attempted=true;}
    }
    public Snapshot attempt(){
        if(visible||attempted||snapshot==null||age()<1||age()>10)return null;
        attempted=true;return snapshot;
    }
    public boolean begin(Snapshot admitted){
        if(admitted==null||admitted!=snapshot||!attempted||visible||active()||age()<1||age()>10)return false;
        frozen=admitted;return true;
    }
}

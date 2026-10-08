package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayList;
import java.util.List;

/** Visible horizontal departure timing, never inferred input/intent or hidden geometry. */
public final class ObservedChargeResponse {
    private static final double POSITION_ROUNDOFF=1e-8;
    private record Response(int onset,long completedAt) {}
    private final List<Response> recent=new ArrayList<>(3);
    private FlightVector previous,departureOrigin;
    private long now=-1;
    private int stationary,onset=-1,persistent,chargeTick=-1;
    private boolean pending,qualified;
    public void reset(){recent.clear();clearObservation();now=-1;}
    private void clearPending(){pending=false;qualified=false;onset=-1;persistent=0;chargeTick=-1;departureOrigin=null;}
    private void clearObservation(){previous=null;stationary=0;clearPending();}
    public void tick(long gameTime){
        if(gameTime<0)throw new IllegalArgumentException("OBSERVATION_CLOCK_REQUIRED");
        if(now>=0&&gameTime<now)reset();
        else if(now>=0&&gameTime-now>1)clearObservation();
        now=gameTime;recent.removeIf(x->gameTime-x.completedAt()>=600);
    }
    public void observe(FlightVector position,StandardAttack.State state,boolean emitted){
        if(now<0)throw new IllegalStateException("OBSERVATION_TICK_REQUIRED");
        if(position==null){clearObservation();return;}
        double step=previous==null?-1:horizontal(position.subtract(previous));
        if(state.phase()==StandardAttack.Phase.CHARGE){
            if(state.ticks()==0){clearPending();pending=stationary>=10&&step>=0&&step<.025;chargeTick=0;}
            else if(pending){
                if(state.ticks()!=chargeTick+1||!qualified&&state.ticks()>13){clearPending();}
                else{
                    chargeTick=state.ticks();
                    if(!qualified){
                        if(step>=.15-POSITION_ROUNDOFF){
                            if(onset<0&&chargeTick<=10){onset=chargeTick;departureOrigin=previous;}
                            if(onset>=0){persistent++;qualified=persistent>=3&&horizontal(position.subtract(departureOrigin))>=.6-POSITION_ROUNDOFF;}
                        }else{onset=-1;persistent=0;departureOrigin=null;}
                        if(onset<0&&chargeTick>10)clearPending();
                    }
                }
            }
        }else{
            if(state.fire()&&emitted&&pending&&qualified){if(recent.size()==3)recent.remove(0);recent.add(new Response(onset,now));}
            clearPending();
        }
        stationary=step>=0&&step<.025?Math.min(10,stationary+1):0;previous=position;
    }
    public int count(){return recent.size();}
    public int stationaryIntervals(){return stationary;}
    public int pendingOnset(){return onset;}
    public boolean qualified(){return qualified;}
    public boolean repeatedTiming(){
        for(int i=0;i<recent.size();i++)for(int j=i+1;j<recent.size();j++)if(Math.abs(recent.get(i).onset()-recent.get(j).onset())<=3)return true;
        return false;
    }
    private static double horizontal(FlightVector v){return Math.hypot(v.x(),v.z());}
}

package com.genki.soutoughast.entity.ai.flight;

/** Consecutive visible world positions; physics velocity is deliberately not displacement. */
public final class ObservedStationarity {
    private FlightVector previous;
    private int ticks;
    private double displacement=-1;
    public int observe(FlightVector position){
        if(position==null){reset();return 0;}
        displacement=previous==null?-1:position.subtract(previous).length();
        ticks=displacement>=0&&displacement<.025?Math.min(80,ticks+1):0;
        previous=position;return ticks;
    }
    public int ticks(){return ticks;}
    public double displacement(){return displacement;}
    public void reset(){previous=null;ticks=0;displacement=-1;}
}

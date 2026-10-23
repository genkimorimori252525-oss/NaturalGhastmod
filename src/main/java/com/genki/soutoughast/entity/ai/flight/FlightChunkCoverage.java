package com.genki.soutoughast.entity.ai.flight;

import java.util.function.BiPredicate;

/** Finite, non-loading coverage gate for the complete body/stopping sweep. */
public final class FlightChunkCoverage {
    private FlightChunkCoverage() { }
    public static boolean loaded(int minX,int maxX,int minZ,int maxZ,BiPredicate<Integer,Integer> present) {
        long width=(long)maxX-minX+1,depth=(long)maxZ-minZ+1;
        if(width<=0||depth<=0||width>16||depth>16||width*depth>16)return false;
        for(long x=minX;x<=maxX;x++)for(long z=minZ;z<=maxZ;z++)if(!present.test((int)x,(int)z))return false;
        return true;
    }
}

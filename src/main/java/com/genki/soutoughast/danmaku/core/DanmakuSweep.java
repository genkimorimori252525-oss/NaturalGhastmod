package com.genki.soutoughast.danmaku.core;

/** Earliest swept sphere/box contact via expanded AABB, shared by native blocks/entities. */
public final class DanmakuSweep {
    private DanmakuSweep(){}
    public static double hit(DanmakuFrame.Vec from,DanmakuFrame.Vec to,double radius,
            double minX,double minY,double minZ,double maxX,double maxY,double maxZ){
        if(!Double.isFinite(radius)||radius<0||radius>1||!Double.isFinite(minX)||!Double.isFinite(minY)||!Double.isFinite(minZ)
                ||!Double.isFinite(maxX)||!Double.isFinite(maxY)||!Double.isFinite(maxZ)||minX>maxX||minY>maxY||minZ>maxZ)throw new IllegalArgumentException("DANMAKU_SWEEP_BOUNDS");
        double enter=0,exit=1;double[] p={from.x(),from.y(),from.z()},v={to.x()-from.x(),to.y()-from.y(),to.z()-from.z()},lo={minX-radius,minY-radius,minZ-radius},hi={maxX+radius,maxY+radius,maxZ+radius};
        for(int i=0;i<3;i++){
            if(Math.abs(v[i])<1e-12){if(p[i]<lo[i]||p[i]>hi[i])return Double.POSITIVE_INFINITY;continue;}
            double a=(lo[i]-p[i])/v[i],b=(hi[i]-p[i])/v[i];enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));if(enter>exit)return Double.POSITIVE_INFINITY;
        }
        return enter;
    }
}

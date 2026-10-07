package com.genki.soutoughast.entity.ai.flight;

/** Every tile under the conservative swept4x4 rectangle, not just sampled centers. */
public final class GroundFootprint {
    @FunctionalInterface public interface FloorTile { boolean supports(int x,int y,int z); }
    private GroundFootprint() {}
    public static boolean supported(FlightVector from,FlightVector to,FloorTile floor){
        if(Math.abs(from.x())>29999980||Math.abs(from.z())>29999980||Math.abs(to.x())>29999980||Math.abs(to.z())>29999980)return false;
        if(from.subtract(to).length()>12.001||Math.abs(from.y()-to.y())>.02)return false;
        double height=Math.rint(to.y());
        if(Math.abs(from.y()-height)>.02||Math.abs(to.y()-height)>.02||height<-2032||height>2032)return false;
        int minX=(int)Math.floor(Math.min(from.x(),to.x())-2+1e-7),maxX=(int)Math.ceil(Math.max(from.x(),to.x())+2-1e-7)-1;
        int minZ=(int)Math.floor(Math.min(from.z(),to.z())-2+1e-7),maxZ=(int)Math.ceil(Math.max(from.z(),to.z())+2-1e-7)-1;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!floor.supports(x,(int)height-1,z))return false;
        return true;
    }
}

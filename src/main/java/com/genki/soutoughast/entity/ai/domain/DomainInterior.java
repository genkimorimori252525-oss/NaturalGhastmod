package com.genki.soutoughast.entity.ai.domain;

/** Complete conservative rectangular footprint, including the actual shell block geometry. */
public final class DomainInterior {
 private DomainInterior(){}
 public static boolean contains(DomainGeometry.Plan p,double minX,double minY,double minZ,double maxX,double maxY,double maxZ){
  if(!Double.isFinite(minX)||!Double.isFinite(minY)||!Double.isFinite(minZ)||!Double.isFinite(maxX)||!Double.isFinite(maxY)||!Double.isFinite(maxZ)
    ||minX>=maxX||minY>=maxY||minZ>=maxZ||minY<p.floorY()-.02||maxY>p.floorY()+p.height()-1
    ||minX<p.centerX()-p.radius()||maxX>p.centerX()+p.radius()+1||minZ<p.centerZ()-p.radius()||maxZ>p.centerZ()+p.radius()+1)return false;
  int firstX=(int)Math.floor(minX+1e-7),lastX=(int)Math.ceil(maxX-1e-7)-1;
  int firstZ=(int)Math.floor(minZ+1e-7),lastZ=(int)Math.ceil(maxZ-1e-7)-1;
  for(int x=firstX;x<=lastX;x++)for(int z=firstZ;z<=lastZ;z++){
   long dx=x-p.centerX(),dz=z-p.centerZ();if(dx*dx+dz*dz>(long)(p.radius()-1)*(p.radius()-1))return false;
  }
  return true;
 }
}

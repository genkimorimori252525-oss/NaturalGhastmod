package com.genki.soutoughast.entity.ai.domain;
import java.util.*;

/** Finite prototype geometry; native loaded-state, support and actor checks remain mandatory. */
public final class DomainGeometry {
 public static final int RADIUS=20,HEIGHT=12,MAX_PLANNED=32768;
 public enum Role {FLOOR,SHELL,INTERIOR}
 public record Cell(int x,int y,int z) {}
 public record Tile(Cell cell,Role role) {}
 public record Plan(int centerX,int floorY,int centerZ,int radius,int height,List<Tile> cells) {
  public Plan {cells=List.copyOf(cells);if(radius!=RADIUS||height!=HEIGHT||cells.size()>MAX_PLANNED)throw new IllegalArgumentException("DOMAIN_GEOMETRY_LIMIT");}
 }
 private DomainGeometry(){}
 public static Plan plan(int centerX,int floorY,int centerZ,int minY,int maxY){
  if(minY<-2048||maxY>2048||minY>=maxY||floorY<=minY||floorY>maxY-HEIGHT||Math.abs((long)centerX)>29_999_960||Math.abs((long)centerZ)>29_999_960)throw new IllegalArgumentException("DOMAIN_WORLD_OR_BUILD_BOUNDS");
  List<Tile> cells=new ArrayList<>();
  for(int y=-1;y<HEIGHT;y++)for(int x=-RADIUS;x<=RADIUS;x++)for(int z=-RADIUS;z<=RADIUS;z++){
   int distance=x*x+z*z;if(distance>RADIUS*RADIUS)continue;
   Role role=y==-1?Role.FLOOR:y==HEIGHT-1||distance>(RADIUS-1)*(RADIUS-1)?Role.SHELL:Role.INTERIOR;
   cells.add(new Tile(new Cell(centerX+x,floorY+y,centerZ+z),role));
  }
  return new Plan(centerX,floorY,centerZ,RADIUS,HEIGHT,cells);
 }
}

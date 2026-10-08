package com.genki.soutoughast.tank;
import com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFlightTank;
import net.minecraft.nbt.CompoundTag;
import com.genki.soutoughast.entity.ai.domain.DomainGeometry;
import com.genki.soutoughast.entity.ai.domain.DomainPreparation;
import java.util.*;
public final class DomainFixtureTest {
 public static void main(String[] args){
  int checked=0,floor=0;Map<String,CompoundTag> sections=new HashMap<>();
  int planned=DomainGeometry.plan(26,224,26,-64,320).cells().size();
  if((planned+127)/128!=DomainPreparation.PREFLIGHT_TICKS)throw new AssertionError("cooldown scheduling must match actual canonical scan duration");checked++;
  for(int x=6;x<=46;x++)for(int z=6;z<=46;z++)if((x-26)*(x-26)+(z-26)*(z-26)<=400){
   int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);String key=cx+":"+cz;
   CompoundTag section=sections.computeIfAbsent(key,k->PrepareFlightTank.section(13,cx,cz,true,false,true));
   if(!PrepareFlightTank.block(section,Math.floorMod(x,16),15,Math.floorMod(z,16)).equals("minecraft:stone"))throw new AssertionError("full existing simple radius20 floor required");floor++;checked++;
  }
  if(floor!=1257)throw new AssertionError("no silent Domain radius reduction");checked++;
  CompoundTag interior=PrepareFlightTank.section(14,1,1,true,false,true);
  if(!PrepareFlightTank.block(interior,10,4,10).equals("minecraft:stone"))throw new AssertionError("declared initial Domain seal");checked++;
  if(!PrepareFlightTank.block(interior,9,4,10).equals("minecraft:air"))throw new AssertionError("Domain seal must remain single-cell");checked++;
  for(int x=24;x<28;x++)for(int y=228;y<232;y++)for(int z=30;z<34;z++){
   int sy=Math.floorDiv(y,16),cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);String key=sy+":"+cx+":"+cz;
   var section=sections.computeIfAbsent(key,k->PrepareFlightTank.section(sy,cx,cz,true,false,true));
   if(!PrepareFlightTank.block(section,Math.floorMod(x,16),Math.floorMod(y,16),Math.floorMod(z,16)).equals("minecraft:air"))throw new AssertionError("actual4x4boss initial body obstructed");checked++;
  }
  CompoundTag oldWall=PrepareFlightTank.section(14,0,0,true,false,true);
  if(!PrepareFlightTank.block(oldWall,9,4,6).equals("minecraft:air"))throw new AssertionError("Domain does not inherit off-center wall");checked++;
  if(PrepareFlightTank.allocatedCells()!=75816)throw new AssertionError("allocation must remain bounded");checked++;
  System.out.println("PASS: "+checked+" mapped centered Domain fixture contracts; no native Player or boss proof");
 }
}

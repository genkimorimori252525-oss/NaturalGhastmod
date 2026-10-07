package com.genki.soutoughast.tank;
import com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFlightTank;
import net.minecraft.nbt.*;
public final class FlightFixtureTest {
 public static void main(String[] args){
  CompoundTag section=PrepareFlightTank.section(14,0,0);
  if(!PrepareFlightTank.block(section,0,0,0).equals("minecraft:air"))throw new AssertionError("interior replaced by shell");
  CompoundTag floor=PrepareFlightTank.section(13,0,0);
  if(!PrepareFlightTank.block(floor,9,15,9).equals("minecraft:black_concrete"))throw new AssertionError("floor missing");
  CompoundTag edge=PrepareFlightTank.section(14,-1,0);
  if(!PrepareFlightTank.block(edge,15,0,9).equals("minecraft:black_concrete"))throw new AssertionError("negative boundary packing");
  if(!PrepareFlightTank.block(section,9,4,6).equals("minecraft:stone"))throw new AssertionError("initial LOS seal must use the registered Arena palette");
  if(!PrepareFlightTank.block(section,9,5,6).equals("minecraft:air"))throw new AssertionError("aperture must be prepared before runtime activation");
  if(!PrepareFlightTank.block(section,9,2,6).equals("minecraft:air"))throw new AssertionError("distant target eye ray needs the low aperture cells");
  if(!PrepareFlightTank.block(section,9,6,9).equals("minecraft:air"))throw new AssertionError("ghast initial body obstructed");
  if(PrepareFlightTank.allocatedCells()!=75816)throw new AssertionError("swimming fixture must allocate52x24x52 with shell");
  CompoundTag ceiling=PrepareFlightTank.section(15,0,0);
  if(!PrepareFlightTank.block(ceiling,9,7,9).equals("minecraft:air"))throw new AssertionError("vertical swimming and body clearance missing");
  if(!PrepareFlightTank.block(ceiling,9,8,9).equals("minecraft:black_concrete"))throw new AssertionError("new ceiling missing");
  CompoundTag singleSeal=PrepareFlightTank.section(14,0,0,true);
  if(!PrepareFlightTank.block(singleSeal,9,4,6).equals("minecraft:stone"))throw new AssertionError("single-cell fixture must keep initial owner seal");
  if(!PrepareFlightTank.block(singleSeal,7,4,6).equals("minecraft:air"))throw new AssertionError("open tactics fixture must not keep the LOS wall");
  if(!PrepareFlightTank.block(singleSeal,9,10,6).equals("minecraft:air"))throw new AssertionError("open tactics fixture retains no upper wall");
  if(PrepareFlightTank.allocatedCells()!=75816)throw new AssertionError("variant cannot grow allocation");
  CompoundTag ground=PrepareFlightTank.section(14,0,0,true,true);
  if(!PrepareFlightTank.block(ground,9,6,9).equals("minecraft:black_concrete"))throw new AssertionError("Ground low ceiling missing");
  if(!PrepareFlightTank.block(ground,9,2,6).equals("minecraft:stone"))throw new AssertionError("Ground initial eye seal missing");
  if(!PrepareFlightTank.block(ground,9,4,6).equals("minecraft:air"))throw new AssertionError("Ground does not inherit old seal");
  if(!PrepareFlightTank.block(ground,9,1,9).equals("minecraft:air"))throw new AssertionError("Ground initial4x4body clearance");
  System.out.println("PASS: 18 mapped flight fixture assertions");
 }
}

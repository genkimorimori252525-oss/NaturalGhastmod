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
  if(PrepareFlightTank.allocatedCells()!=60552)throw new AssertionError("geometry allocation changed");
  System.out.println("PASS: 8 mapped flight fixture assertions");
 }
}

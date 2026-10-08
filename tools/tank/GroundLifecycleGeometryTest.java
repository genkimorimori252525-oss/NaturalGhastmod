package com.github.tartaricacid.touhoulittlemaid.sim.debug;
public final class GroundLifecycleGeometryTest {
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int checks=0;
  for(int cx=0;cx<4;cx++)for(int cz=0;cz<4;cz++){
   var section=PrepareFlightTank.section(14,cx,cz,false,true,true);
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(cx*16+x<52&&cz*16+z<52){
    if(!PrepareFlightTank.block(section,x,6,z).equals("minecraft:stone"))throw new AssertionError("CEILING");
    if(!PrepareFlightTank.block(section,x,1,z).equals("minecraft:air"))throw new AssertionError("BODY_OR_SEAL");checks+=2;
   }
  }
  var floor=PrepareFlightTank.section(13,1,2,false,true,true);
  if(!PrepareFlightTank.block(floor,10,15,0).equals("minecraft:stone"))throw new AssertionError("FULL_FLOOR");
  System.out.println("Ground lifecycle fixture PASS "+(checks+1));
 }
}

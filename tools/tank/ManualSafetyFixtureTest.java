package com.genki.soutoughast.tank;

import com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFlightTank;
import net.minecraft.nbt.*;
import java.nio.file.Path;

/** Regression: a human must survive noticing the window and respawn inside it. */
public final class ManualSafetyFixtureTest {
 private static int checked;
 private static void check(boolean value,String reason){checked++;if(!value)throw new AssertionError(reason);}
 private static void player(CompoundTag data,CompoundTag p){
  check(data.getInt("SpawnX")==4&&data.getInt("SpawnY")==224&&data.getInt("SpawnZ")==4,"global spawn inside Tank");
  check(data.getCompound("GameRules").getString("spawnRadius").equals("0"),"no random roof respawn");
  check(p.getInt("SpawnX")==4&&p.getInt("SpawnY")==224&&p.getInt("SpawnZ")==4&&p.getBoolean("SpawnForced"),"forced personal spawn inside room");
  check(p.getString("SpawnDimension").equals("minecraft:overworld"),"respawn dimension");
  var pos=p.getList("Pos",Tag.TAG_DOUBLE);check(pos.size()==3&&pos.getDouble(0)==4.5&&pos.getDouble(1)==224&&pos.getDouble(2)==5.5,"initial protected position");
  int resistance=0;for(var raw:p.getList("ActiveEffects",Tag.TAG_COMPOUND)){
   var effect=(CompoundTag)raw;if((effect.getByte("Id")&255)!=11)continue;resistance++;
   check(effect.getByte("Amplifier")==127,"signed-byte reload-safe amplifier");check(effect.getInt("Duration")==-1,"no expiry before human notices");
   check(!effect.getBoolean("ShowParticles")&&effect.getBoolean("ShowIcon"),"no view obstruction, visible effect status");
  }
  check(resistance==1,"exactly one Resistance effect");
 }
 public static void main(String[] args)throws Exception{
  CompoundTag data=new CompoundTag(),p=new CompoundTag();data.put("GameRules",new CompoundTag());
  PrepareFlightTank.manualPlayer(data,p);PrepareFlightTank.manualPlayer(data,p);player(data,p);
  for(int x=1;x<=7;x++)for(int z=1;z<=7;z++)for(int y=224;y<=228;y++){
   String name=PrepareFlightTank.block(PrepareFlightTank.manualSection(y/16,x/16,z/16),x%16,y%16,z%16);
   boolean door=x==4&&z==7&&(y==224||y==225),wall=x==1||x==7||z==1||z==7||y==228;
   if(door)check(name.equals("minecraft:iron_door"),"ordinary manually opened door");
   else if(wall)check(name.equals("minecraft:bedrock"),"protected shell has no projectile opening");
  }
  for(int y=224;y<=225;y++){
   var props=PrepareFlightTank.manualState(4,y,7).getCompound("Properties");
   check(props.getString("facing").equals("south")&&props.getString("open").equals("false")&&props.getString("powered").equals("false"),"door closed at launch");
   check(props.getString("half").equals(y==224?"lower":"upper"),"matching door halves");
  }
  for(int z:new int[]{6,8}){
   var button=PrepareFlightTank.manualState(5,225,z);check(button.getString("Name").equals("minecraft:oak_button"),"entry and return buttons");
   var props=button.getCompound("Properties");check(props.getString("face").equals("wall")&&props.getString("facing").equals(z==6?"north":"south"),"buttons supported on wall beside door");
  }
  check(PrepareFlightTank.manualState(4,224,8).getString("Name").equals("minecraft:air"),"unobstructed arena exit");
  check(PrepareFlightTank.manualState(4,225,8).getString("Name").equals("minecraft:air"),"player head exit clearance");
  check(PrepareFlightTank.manualState(4,223,4).getString("Name").equals("minecraft:stone"),"supported respawn floor");
  for(int x=10;x<52;x++)for(int z=10;z<52;z++)check(PrepareFlightTank.manualState(x,228,z).getString("Name").equals("minecraft:air"),"broad swimming space unchanged outside corner room");
  check(PrepareFlightTank.allocatedCells()==75816,"same bounded world allocation");
  if(args.length==1){var world=Path.of(args[0]);data=NbtIo.readCompressed(world.resolve("level.dat").toFile()).getCompound("Data");p=data.getCompound("Player");player(data,p);player(data,NbtIo.readCompressed(world.resolve("playerdata/"+p.getUUID("UUID")+".dat").toFile()));check(p.getInt("playerGameType")==0&&p.getFloat("Health")==20,"survival20HP remains");}
  System.out.println("PASS: "+checked+" manual safety fixture checks; no runtime input/mutation");
 }
}

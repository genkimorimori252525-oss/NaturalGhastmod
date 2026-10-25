package com.genki.soutoughast.tank;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import com.google.gson.JsonParser;
import net.minecraft.nbt.*;
import com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFlightTank;
/** Initial safety effects only in a new Domain-specific offline fixture. */
public final class PrepareDomainObservation {
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("DOMAIN_NEW_FIXTURE_REQUIRED");
  Path root=Path.of(args[0]).toRealPath(),allowed=Path.of(args[1]).toRealPath();
  if(root.equals(allowed)||!root.startsWith(allowed)||Files.exists(root.resolve("observation-safety.json")))throw new IllegalStateException("DOMAIN_NEW_COPY_ONLY");
  var fixture=JsonParser.parseString(Files.readString(root.resolve("fixture.json"))).getAsJsonObject();
  if(!fixture.get("variant").getAsString().equals("DOMAIN_RELIABILITY"))throw new IllegalStateException("DOMAIN_RELIABILITY_VARIANT_REQUIRED");
  Path world=root.resolve("game/saves/KNEEKURA_DEBUG_WORLD");
  try(var ch=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.WRITE);var lock=ch.tryLock()){
   if(lock==null)throw new IllegalStateException("WORLD_RUNNING");
   var level=NbtIo.readCompressed(world.resolve("level.dat").toFile());var data=level.getCompound("Data");var player=data.getCompound("Player");
   var pos=player.getList("Pos",Tag.TAG_DOUBLE).copy();var rotation=player.getList("Rotation",Tag.TAG_FLOAT).copy();
   if(pos.getDouble(0)!=26||pos.getDouble(1)!=224||pos.getDouble(2)!=20||player.getFloat("Health")!=20)throw new IllegalStateException("DOMAIN_INITIAL_POSE_OR_HEALTH");
   PrepareFlightTank.manualPlayer(data,player);player.put("Pos",pos);player.put("Rotation",rotation);
   NbtIo.writeCompressed(level,world.resolve("level.dat").toFile());Path personal=world.resolve("playerdata/"+player.getUUID("UUID")+".dat");if(Files.exists(personal))NbtIo.writeCompressed(player,personal.toFile());
   Files.writeString(root.resolve("observation-safety.json"),"{\"initialResistance\":127,\"initialNightVision\":0,\"runtimeMutation\":false,\"damageBalanceAcceptance\":false}\n",StandardOpenOption.CREATE_NEW);
  }
 }
}

package com.genki.soutoughast.tank;

import com.google.gson.*;
import net.minecraft.nbt.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Offline-only setup of a NEW copied fixture; real client Player, no runtime controls. */
public final class PrepareAutonomousRhythm {
    private static ListTag vector(double x,double y,double z){var v=new ListTag();for(double n:new double[]{x,y,z})v.add(DoubleTag.valueOf(n));return v;}
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("NEW_WORLD_AND_ALLOWED_FIXTURE_REQUIRED");
        Path world=Path.of(args[0]).toRealPath(),allowed=Path.of(args[1]).toRealPath();
        if(world.equals(allowed)||!world.startsWith(allowed)||!world.endsWith("game/saves/KNEEKURA_DEBUG_WORLD")||Files.exists(allowed.resolve("closed.json"))||Files.exists(allowed.resolve("autonomous-preparation.json")))throw new IllegalStateException("NEW_PRIVATE_COPY_REQUIRED");
        try(var channel=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IllegalStateException("WORLD_RUNNING");
            var level=NbtIo.readCompressed(world.resolve("level.dat").toFile());var player=level.getCompound("Data").getCompound("Player");
            if(player.getFloat("Health")!=20||player.getInt("playerGameType")!=0)throw new IllegalStateException("EXPECTED_UNRUN_SURVIVAL_TEMPLATE");
            boolean resistance=false,night=false;
            for(var e:player.getList("ActiveEffects",Tag.TAG_COMPOUND)){var effect=(CompoundTag)e;resistance|=(effect.getByte("Id")==11||effect.getString("forge:id").equals("minecraft:resistance"))&&effect.getByte("Amplifier")==127;night|=effect.getByte("Id")==16||effect.getString("forge:id").equals("minecraft:night_vision");}
            if(!resistance||!night)throw new IllegalStateException("EXPECTED_INITIAL_SAFETY_EFFECTS");
            // Outside the voluntary room, on existing support, with the saved boss naturally visible.
            player.put("Pos",vector(26,224,5.5));player.put("Motion",vector(0,0,0));var rotation=new ListTag();rotation.add(FloatTag.valueOf(0));rotation.add(FloatTag.valueOf(-12));player.put("Rotation",rotation);
            NbtIo.writeCompressed(level,world.resolve("level.dat").toFile());var playerFile=world.resolve("playerdata/"+player.getUUID("UUID")+".dat");if(Files.exists(playerFile))NbtIo.writeCompressed(player,playerFile.toFile());
            var actual=NbtIo.readCompressed(world.resolve("level.dat").toFile()).getCompound("Data").getCompound("Player").getList("Pos",Tag.TAG_DOUBLE);
            if(actual.getDouble(0)!=26||actual.getDouble(1)!=224||actual.getDouble(2)!=5.5)throw new AssertionError("PRIVATE_POSITION_READBACK");
            var record=new JsonObject();record.addProperty("mode","AUTONOMOUS_STATIC_CLIENT_TECHNICAL");record.addProperty("world",world.toString());record.addProperty("playerUuid",player.getUUID("UUID").toString());record.addProperty("initialPlayerPosition","26,224,5.5");record.addProperty("runtimeMutation",false);record.addProperty("damageBalanceAcceptance",false);record.addProperty("humanAcceptance","NOT_REQUESTED_NOT_INFERRED");Files.writeString(allowed.resolve("autonomous-preparation.json"),record+"\n",StandardOpenOption.CREATE_NEW);
            System.out.println(record);
        }
    }
}

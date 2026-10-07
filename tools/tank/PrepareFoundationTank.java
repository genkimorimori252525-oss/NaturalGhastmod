package com.github.tartaricacid.touhoulittlemaid.sim.debug;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;

/** Offline fixture on an exclusively created private copy; never a live action channel. */
public final class PrepareFoundationTank {
    public static final UUID SUBJECT = UUID.fromString("67676767-1007-4000-8000-000000000001");
    private static ListTag vector(double... values) {
        ListTag tag = new ListTag();
        for (double value : values) tag.add(DoubleTag.valueOf(value));
        return tag;
    }
    private static ListTag rotation(float yaw, float pitch) {
        ListTag tag = new ListTag(); tag.add(FloatTag.valueOf(yaw)); tag.add(FloatTag.valueOf(pitch)); return tag;
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path root = Path.of(args[0]).toRealPath();
        Path allowed = Path.of(args[1]).toRealPath();
        if (!root.startsWith(allowed) || root.equals(allowed) || Files.exists(root.resolve("fixture.json"))) {
            throw new IllegalStateException("NEW_PRIVATE_COPY_REQUIRED");
        }
        Path world = root.resolve("game/saves/KNEEKURA_DEBUG_WORLD").toRealPath();
        if (!world.startsWith(root)) throw new IllegalStateException("PRIVATE_WORLD_REQUIRED");
        try (FileChannel channel = FileChannel.open(world.resolve("session.lock"), StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IllegalStateException("WORLD_RUNNING");
            CompoundTag level = NbtIo.readCompressed(world.resolve("level.dat").toFile());
            CompoundTag data = level.getCompound("Data");
            data.putBoolean("confirmedExperimentalSettings", true);
            CompoundTag player = data.getCompound("Player");
            player.put("Pos", vector(.6, 232.2, .6)); player.put("Motion", vector(0, 0, 0));
            player.put("Rotation", rotation(-45, 35)); player.putInt("playerGameType", 3);
            CompoundTag abilities = player.getCompound("abilities");
            abilities.putBoolean("flying", true); abilities.putBoolean("mayfly", true); abilities.putBoolean("invulnerable", true);
            NbtIo.writeCompressed(level, world.resolve("level.dat").toFile());
            Path playerFile = world.resolve("playerdata/" + player.getUUID("UUID") + ".dat");
            if (Files.exists(playerFile)) NbtIo.writeCompressed(player, playerFile.toFile());
            int seedReimuRemoved=TankSeedEntities.removeSeedReimu(world,1);
            Path entities = world.resolve("entities");
            try (RegionFile region = new RegionFile(entities.resolve("r.0.0.mca"), entities, true)) {
                ChunkPos position = new ChunkPos(0, 0);
                CompoundTag chunk;
                try (var input = region.getChunkDataInputStream(position)) { chunk = NbtIo.read(input); }
                ListTag rows = chunk.getList("Entities", Tag.TAG_COMPOUND);
                for (Tag row : rows) {
                    CompoundTag entity = (CompoundTag)row;
                    if (entity.hasUUID("UUID") && entity.getUUID("UUID").equals(SUBJECT)) throw new IllegalStateException("DUPLICATE_SUBJECT");
                }
                CompoundTag ghast = new CompoundTag();
                ghast.putString("id", "soutou_ghast:soutou_ghast"); ghast.putUUID("UUID", SUBJECT);
                ghast.put("Pos", vector(9.5, 224, 9.5)); ghast.put("Motion", vector(0, 0, 0));
                ghast.put("Rotation", rotation(0, 0)); ghast.putFloat("Health", 10);
                ghast.putBoolean("PersistenceRequired", true); ghast.putBoolean("NoAI", false);
                rows.add(ghast); chunk.put("Entities", rows);
                try (var output = region.getChunkDataOutputStream(position)) { NbtIo.write(chunk, output); }
                region.flush();
            }
            JsonObject scope = new JsonObject(); scope.addProperty("scope", KneekuraDebugArenaController.SCOPE);
            scope.addProperty("dimension", "minecraft:overworld"); JsonArray blocks = new JsonArray();
            for (int x = 7; x < 13; x++) for (int y = 224; y < 229; y++) for (int z = 7; z < 13; z++) {
                JsonArray cell = new JsonArray(); cell.add(x); cell.add(y); cell.add(z); cell.add("minecraft:air"); blocks.add(cell);
            }
            scope.add("blocks", blocks); JsonObject poses = new JsonObject(), pose = new JsonObject();
            pose.addProperty("x", 9.5); pose.addProperty("y", 224d); pose.addProperty("z", 9.5);
            pose.addProperty("yaw", 0f); pose.addProperty("pitch", 0f);
            for (String key : new String[]{"vx", "vy", "vz"}) pose.addProperty(key, 0d);
            poses.add(SUBJECT.toString(), pose); scope.add("subjectPoses", poses);
            JsonObject fixture = new JsonObject();
            fixture.add("scope", scope);
            // Arena receipts use typed Gson numbers (0.0), not Node action-sidecar
            // serialization (0). Reuse the pinned LAB adapter's exact hash contract.
            fixture.addProperty("baselineHash", KneekuraDebugActionJournal.sha256(KneekuraDebugActionJournal.canonical(scope)));
            fixture.addProperty("certainty", "PREDICTED_SCOPE_REQUIRES_NATIVE_OWNER_MATCH");
            fixture.addProperty("subjectUuid", SUBJECT.toString());
            fixture.addProperty("seedReimuRemoved",seedReimuRemoved);
            fixture.addProperty("changes", "PRIVATE_ONLY: spectator camera; seed Reimu excluded from room chunks; active NaturalGhast added. Tank geometry unchanged.");
            Files.writeString(root.resolve("fixture.json"), fixture + "\n", StandardOpenOption.CREATE_NEW);
        }
    }
}

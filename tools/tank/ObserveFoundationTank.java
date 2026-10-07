package com.genki.soutoughast.tank;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.SoutouGhastInertialMoveControl;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.UUID;

/** Finite read-only supplement to LAB canonical evidence; excluded from the product jar. */
@Mod("naturalghast_tank_observer")
public final class ObserveFoundationTank {
    private static int samples;
    private static volatile boolean ready;
    private static Path output() { return Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("evidence/derived/naturalghast-foundation"); }
    private static boolean active() { return "1".equals(System.getenv("KNEEKURA_DEBUG_ENABLED")); }
    private static boolean owned() throws Exception {
        try {
            JsonObject owner = JsonParser.parseString(Files.readString(Path.of(System.getenv("KNEEKURA_DEBUG_RUN_DIR")).resolve("control/owner-status.json"))).getAsJsonObject();
            return "ACTIVE_SCOPED_CONTROL".equals(owner.get("status").getAsString());
        } catch (NoSuchFileException pending) { return false; }
    }
    @Mod.EventBusSubscriber(modid="naturalghast_tank_observer")
    public static final class ServerObserver {
        @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) throws Exception {
            if (!active() || event.phase != TickEvent.Phase.END || samples >= 40 || !owned()) return;
            var entity = event.getServer().overworld().getEntity(UUID.fromString("67676767-1007-4000-8000-000000000001"));
            if (!(entity instanceof SoutouGhast ghast)) return;
            var control = (SoutouGhastInertialMoveControl)ghast.getMoveControl();
            JsonObject row = new JsonObject(); row.addProperty("tick", ghast.level().getGameTime());
            row.addProperty("uuid", ghast.getUUID().toString()); row.addProperty("noAI", ghast.isNoAi());
            row.addProperty("x", ghast.getX()); row.addProperty("y", ghast.getY()); row.addProperty("z", ghast.getZ());
            row.addProperty("speed", ghast.getDeltaMovement().length()); row.addProperty("targetPresent", ghast.getTarget() != null);
            row.addProperty("intent", control.getIntent().mode().name()); row.addProperty("clearanceBlocked", control.isClearanceBlocked());
            row.addProperty("collisionFree", ghast.level().noCollision(ghast, ghast.getBoundingBox()));
            row.addProperty("width", ghast.getBbWidth()); row.addProperty("height", ghast.getBbHeight());
            row.addProperty("health", ghast.getHealth());
            Files.createDirectories(output());
            Files.writeString(output().resolve("idle.jsonl"), row + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            ready = ++samples >= 40;
        }
    }
    @Mod.EventBusSubscriber(modid="naturalghast_tank_observer", value=Dist.CLIENT)
    public static final class ClientObserver {
        private static boolean captured;
        private static int startupTicks;
        private static boolean startupCaptured;
        @SubscribeEvent public static void startup(TickEvent.ClientTickEvent event) throws Exception {
            if (!active() || event.phase != TickEvent.Phase.END || startupCaptured || ++startupTicks < 300) return;
            var mc = Minecraft.getInstance();
            if (mc.level != null) { startupCaptured = true; return; }
            var expected = System.getenv("NATURALGHAST_PRIVATE_GAME_DIR");
            if (expected == null || !mc.gameDirectory.toPath().toRealPath().equals(Path.of(expected).toRealPath())) return;
            startupCaptured = true;
            Files.createDirectories(output());
            JsonObject state = new JsonObject();
            state.addProperty("scope", "PRE_OWNER_PRIVATE_CLIENT_UI_OBSERVATION_ONLY");
            state.addProperty("screenClass", mc.screen == null ? "NONE" : mc.screen.getClass().getName());
            state.addProperty("title", mc.screen == null ? "NONE" : mc.screen.getTitle().getString());
            state.addProperty("integratedServerPresent", mc.getSingleplayerServer() != null);
            JsonArray messages = new JsonArray();
            if (mc.screen != null) for (var child : mc.screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && messages.size() < 16) messages.add(widget.getMessage().getString());
            }
            state.add("widgetLabels", messages);
            Files.writeString(output().resolve("startup.json"), state + "\n", StandardOpenOption.CREATE_NEW);
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output().resolve("startup.png")); }
        }
        @SubscribeEvent public static void frame(RenderLevelStageEvent event) throws Exception {
            if (!active() || !ready || captured || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !owned()) return;
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var entities = mc.level.getEntitiesOfClass(SoutouGhast.class, new net.minecraft.world.phys.AABB(7,224,7,13,229,13));
            if (entities.size() != 1) return;
            captured = true;
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output().resolve("frame.png")); }
            JsonObject frame = new JsonObject(); frame.addProperty("renderer", mc.getEntityRenderDispatcher().getRenderer(entities.get(0)).getClass().getName());
            frame.addProperty("capture", "SUPPLEMENTARY_RAW_FRAMEBUFFER_AFTER_IDLE_WINDOW_NOT_CARDINAL");
            JsonArray mods = new JsonArray(); net.minecraftforge.fml.ModList.get().getMods().forEach(mod -> mods.add(mod.getModId()));
            frame.add("loadedModIds", mods); Files.writeString(output().resolve("frame.json"), frame + "\n", StandardOpenOption.CREATE_NEW);
        }
    }
}

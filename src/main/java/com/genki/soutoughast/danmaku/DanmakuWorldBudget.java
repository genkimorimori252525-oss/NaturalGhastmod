package com.genki.soutoughast.danmaku;

import com.genki.soutoughast.danmaku.core.DanmakuHurtGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-thread-only bounded per-level ownership, collision cache and shared contact cooldown. */
@Mod.EventBusSubscriber(modid="soutou_ghast")
public final class DanmakuWorldBudget {
    private static final WeakHashMap<ServerLevel,State> LEVELS=new WeakHashMap<>();
    private static final class State {
        final Set<UUID> sessions=new HashSet<>();final DanmakuHurtGate hurt=new DanmakuHurtGate();
        final HashMap<BlockPos,List<AABB>> shapes=new HashMap<>();long cacheTick=-1;
    }
    private DanmakuWorldBudget(){}
    private static State state(ServerLevel level){if(!level.getServer().isSameThread())throw new IllegalStateException("DANMAKU_SERVER_THREAD");return LEVELS.computeIfAbsent(level,k->new State());}
    public static boolean acquire(ServerLevel level,UUID id){var s=state(level);return s.sessions.size()<GrandDanmakuAssets.MAX_SESSIONS_PER_LEVEL&&s.sessions.add(id);}
    public static void release(ServerLevel level,UUID id){var s=LEVELS.get(level);if(s!=null)s.sessions.remove(id);}
    public static boolean hurtAllowed(ServerLevel level,UUID target){return state(level).hurt.allow(target,level.getGameTime());}
    public static List<AABB> shapes(ServerLevel level,BlockPos pos){
        var s=state(level);if(s.cacheTick!=level.getGameTime()){s.shapes.clear();s.cacheTick=level.getGameTime();}
        var existing=s.shapes.get(pos);if(existing!=null)return existing;
        if(s.shapes.size()>=8192)throw new IllegalStateException("DANMAKU_BLOCK_QUERY_BUDGET");
        if(!level.hasChunkAt(pos))throw new IllegalStateException("DANMAKU_UNLOADED_BLOCK_QUERY");
        var boxes=level.getBlockState(pos).getCollisionShape(level,pos).toAabbs();if(boxes.size()>32)throw new IllegalStateException("DANMAKU_COMPLEX_SHAPE_BUDGET");
        var result=boxes.stream().map(a->a.move(pos)).toList();s.shapes.put(pos.immutable(),result);return result;
    }
    @SubscribeEvent public static void startTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&event.getTarget() instanceof com.genki.soutoughast.entity.SoutouGhast boss)boss.getGrandDanmaku().watch(player);}
    @SubscribeEvent public static void stopTracking(net.minecraftforge.event.entity.player.PlayerEvent.StopTracking event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&event.getTarget() instanceof com.genki.soutoughast.entity.SoutouGhast boss)boss.getGrandDanmaku().unwatch(player);}
    @SubscribeEvent public static void leaving(net.minecraftforge.event.entity.EntityLeaveLevelEvent event){if(!event.getLevel().isClientSide()&&event.getEntity() instanceof com.genki.soutoughast.entity.SoutouGhast boss)boss.getGrandDanmaku().abort("OWNER_REMOVED");}
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event){if(!event.getEntity().level().isClientSide&&event.getEntity() instanceof com.genki.soutoughast.entity.SoutouGhast boss)boss.getGrandDanmaku().abort("OWNER_DIED");}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)LEVELS.remove(level);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){LEVELS.clear();}
}

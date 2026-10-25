package com.genki.soutoughast.client.danmaku;

import com.genki.soutoughast.danmaku.*;
import com.genki.soutoughast.danmaku.core.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One visual-only cache per received server session, with mandatory score/texture/shader readiness. */
@Mod.EventBusSubscriber(modid="soutou_ghast",value=Dist.CLIENT)
public final class GrandDanmakuClient {
    private static final LinkedHashMap<UUID,DanmakuPlayback> SESSIONS=new LinkedHashMap<>();
    private GrandDanmakuClient(){}
    public static Collection<DanmakuPlayback> sessions(){return List.copyOf(SESSIONS.values());}
    public static void accept(GrandDanmakuNetwork.Packet packet){
        var minecraft=Minecraft.getInstance();if(minecraft.level==null||minecraft.player==null)return;long tick=minecraft.level.getGameTime();
        if(packet.kind()==0){
            var sync=packet.sync();boolean ready=GrandDanmakuAssets.ready()&&GrandDanmakuRenderer.ready();DanmakuPlayback playback=SESSIONS.get(sync.session());
            if(playback==null){
                SESSIONS.entrySet().removeIf(e->{var id=e.getValue().identity();return id!=null&&id.owner().equals(sync.owner());});
                if(SESSIONS.size()>=4)ready=false;playback=new DanmakuPlayback();
            }
            if(ready){var pattern=GrandDanmakuAssets.get(sync.pattern());ready=playback.sync(sync,pattern.score(),pattern.hash(),minecraft.level.dimension().location().toString(),tick);}
            if(ready)SESSIONS.put(sync.session(),playback);else{playback.clear();SESSIONS.remove(sync.session());}
            GrandDanmakuNetwork.acknowledge(new GrandDanmakuNetwork.Ack(sync.entityId(),sync.owner(),sync.session(),sync.scoreHash(),ready));return;
        }
        var playback=SESSIONS.get(packet.session());if(playback==null)return;var identity=playback.identity();
        if(identity==null||!identity.owner().equals(packet.owner())||identity.entityId()!=packet.entityId())return;
        if(packet.kind()==2){playback.finish(packet.hardEnd(),tick);if(packet.hardEnd())SESSIONS.remove(packet.session());return;}
        boolean applied=playback.delta(packet.delta(),tick);
        if(!applied&&!playback.valid()){reject(identity);SESSIONS.remove(packet.session());}
    }
    private static void reject(DanmakuPlayback.Sync id){if(id!=null&&Minecraft.getInstance().getConnection()!=null)GrandDanmakuNetwork.acknowledge(new GrandDanmakuNetwork.Ack(id.entityId(),id.owner(),id.session(),id.scoreHash(),false));}
    public static void invalidateAssets(){for(var p:SESSIONS.values()){reject(p.identity());p.clear();}SESSIONS.clear();}
    public static void renderingFailed(RuntimeException failure){LogUtils.getLogger().error("Grand Danmaku visual session rejected; server damage permission revoked",failure);invalidateAssets();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var level=Minecraft.getInstance().level;if(level==null){SESSIONS.clear();return;}
        long tick=level.getGameTime();var it=SESSIONS.values().iterator();while(it.hasNext()){
            var p=it.next();var id=p.identity();if(p.stale(tick)||id==null||!id.dimension().equals(level.dimension().location().toString())){reject(id);p.clear();it.remove();continue;}
            var owner=level.getEntity(id.entityId());if(owner!=null&&!owner.getUUID().equals(id.owner())){reject(id);p.clear();it.remove();continue;}
            p.prune(p.renderAge(tick));if(p.ended()&&p.ghosts().isEmpty()){p.clear();it.remove();}
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){SESSIONS.clear();}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel().isClientSide())SESSIONS.clear();}
}

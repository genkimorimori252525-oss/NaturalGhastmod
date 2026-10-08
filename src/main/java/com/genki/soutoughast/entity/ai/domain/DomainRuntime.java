package com.genki.soutoughast.entity.ai.domain;

import java.io.IOException;
import java.util.*;
import com.genki.soutoughast.SoutouGhastMod;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One exclusive world writer per server. Development prototype after scoped reliability gates. */
@Mod.EventBusSubscriber(modid=SoutouGhastMod.MODID)
public final class DomainRuntime {
 // Development branch only: natural combat/readability/input acceptance remains separate.
 private static final boolean AUTONOMOUS_RELEASED=true;
 private static final Map<MinecraftServer,DomainNativeCoordinator> writers=new IdentityHashMap<>();
 private static final Set<MinecraftServer> failed=Collections.newSetFromMap(new IdentityHashMap<>());
 private DomainRuntime(){}
 public static boolean released(){return AUTONOMOUS_RELEASED;}
 public static DomainCoordinator current(MinecraftServer server){
  if(!released()||!server.isSameThread()||failed.contains(server))return null;
  var writer=writers.get(server);return writer==null?null:writer.coordinator();
 }
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
  if(!released()||event.phase!=TickEvent.Phase.END)return;var server=event.getServer();if(failed.contains(server))return;
  try{
   var writer=writers.get(server);if(writer==null){writer=DomainNativeCoordinator.open(server);writers.put(server,writer);}
   writer.coordinator().tick();
  }catch(IOException|RuntimeException error){failed.add(server);LogUtils.getLogger().error("Domain writer unresolved; activation blocked until restart",error);}
 }
 @SubscribeEvent public static void stop(ServerStoppingEvent event){
  var server=event.getServer();var writer=writers.remove(server);failed.remove(server);
  if(writer!=null)try{writer.close();}catch(IOException|RuntimeException error){LogUtils.getLogger().error("Domain stop retained unresolved journals",error);}
 }
}

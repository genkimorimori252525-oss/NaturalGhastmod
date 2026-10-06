package com.genki.soutoughast.client;

import com.genki.soutoughast.SoutouGhastMod;
import com.genki.soutoughast.client.model.SoutouGhastArmorModel;
import com.genki.soutoughast.client.renderer.SoutouGhastRenderer;
import com.genki.soutoughast.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoutouGhastMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SoutouGhastArmorModel.LAYER_LOCATION, SoutouGhastArmorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SOUTOU_GHAST.get(), SoutouGhastRenderer::new);
    }
}

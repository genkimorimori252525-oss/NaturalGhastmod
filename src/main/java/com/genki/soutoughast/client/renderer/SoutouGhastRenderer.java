package com.genki.soutoughast.client.renderer;

import com.genki.soutoughast.client.model.SoutouGhastArmorModel;
import com.genki.soutoughast.client.renderer.layer.GhastArmorLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GhastRenderer;

public class SoutouGhastRenderer extends GhastRenderer {

    public SoutouGhastRenderer(EntityRendererProvider.Context context) {
        super(context);

        // Bake and add the armor layer
        // GhastRenderer extends MobRenderer<Ghast, GhastModel<Ghast>>,
        // so GhastArmorLayer uses <Ghast, GhastModel<Ghast>> to match
        SoutouGhastArmorModel armorModel = new SoutouGhastArmorModel(
                context.bakeLayer(SoutouGhastArmorModel.LAYER_LOCATION));
        this.addLayer(new GhastArmorLayer(this, armorModel));
    }
    @Override protected void setupRotations(net.minecraft.world.entity.monster.Ghast entity,com.mojang.blaze3d.vertex.PoseStack pose,float age,float bodyYaw,float partialTick){
        super.setupRotations(entity,pose,age,bodyYaw,partialTick);
        if(entity instanceof com.genki.soutoughast.entity.SoutouGhast boss){
            // Root rotation precedes MobRenderer's inverted model axes: negative pitch faces downward.
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-boss.majorRenderPitch(partialTick)));
        }
    }
}

package com.genki.soutoughast.client.renderer.layer;

import com.genki.soutoughast.SoutouGhastMod;
import com.genki.soutoughast.client.model.SoutouGhastArmorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ghast;

public class GhastArmorLayer extends RenderLayer<Ghast, GhastModel<Ghast>> {
    private static final ResourceLocation ARMOR_TEXTURE =
            new ResourceLocation(SoutouGhastMod.MODID + ":textures/entity/soutou_ghast_armor.png");

    private final SoutouGhastArmorModel armorModel;

    public GhastArmorLayer(RenderLayerParent<Ghast, GhastModel<Ghast>> renderer, SoutouGhastArmorModel armorModel) {
        super(renderer);
        this.armorModel = armorModel;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       Ghast entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();

        // Offset adjustment to align armor with Ghast body
        // The GhastRenderer already applies 4.5x scale, so coordinates are in model space
        // Adjust these values if the armor clips into or floats away from the Ghast body
        poseStack.translate(0.0F, 0.0F, 0.0F);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ARMOR_TEXTURE));
        this.armorModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}

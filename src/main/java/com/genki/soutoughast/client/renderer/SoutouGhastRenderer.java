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
}

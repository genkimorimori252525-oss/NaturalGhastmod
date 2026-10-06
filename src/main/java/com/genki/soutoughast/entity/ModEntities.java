package com.genki.soutoughast.entity;

import com.genki.soutoughast.SoutouGhastMod;
import com.genki.soutoughast.entity.projectile.SoutouGhastFireball;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SoutouGhastMod.MODID);

    public static final RegistryObject<EntityType<SoutouGhast>> SOUTOU_GHAST =
            ENTITY_TYPES.register("soutou_ghast", () ->
                    EntityType.Builder.of(SoutouGhast::new, MobCategory.MONSTER)
                            .sized(4.0F, 4.0F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("soutou_ghast"));

    public static final RegistryObject<EntityType<SoutouGhastFireball>> SOUTOU_GHAST_FIREBALL =
            ENTITY_TYPES.register("soutou_ghast_fireball", () ->
                    EntityType.Builder.<SoutouGhastFireball>of(SoutouGhastFireball::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .fireImmune()
                            .build("soutou_ghast_fireball"));
}

package com.genki.soutoughast.sound;

import com.genki.soutoughast.SoutouGhastMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoutouGhastMod.MODID);

    // 待機音 (Ambient) - 14 sounds
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_AMBIENT =
            SOUND_EVENTS.register("soutou_ghast_ambient",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_ambient")));

    // 被ダメージ音 (Hurt) - 19 sounds
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_HURT =
            SOUND_EVENTS.register("soutou_ghast_hurt",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_hurt")));

    // デス音 (Death) - chikuchou.ogg
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_DEATH =
            SOUND_EVENTS.register("soutou_ghast_death",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_death")));

    // 攻撃音 (Attack voice when shooting) - 15 sounds
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_ATTACK =
            SOUND_EVENTS.register("soutou_ghast_attack",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_attack")));

    // 火球発射音 (Shoot) - same as attack for now
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_SHOOT =
            SOUND_EVENTS.register("soutou_ghast_shoot",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_shoot")));

    // 火の玉着弾音 (Fireball impact) - pen.ogg, attack2.ogg
    public static final RegistryObject<SoundEvent> SOUTOU_GHAST_FIREBALL_IMPACT =
            SOUND_EVENTS.register("soutou_ghast_fireball_impact",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SoutouGhastMod.MODID + ":soutou_ghast_fireball_impact")));
}

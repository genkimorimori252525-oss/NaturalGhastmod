package com.genki.soutoughast.danmaku;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client effects affect only halos/trails. Every authoritative hit core remains visible. */
public final class GrandDanmakuClientConfig {
    public enum Effects { FULL, REDUCED }
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.EnumValue<Effects> EFFECTS=BUILDER.comment("Grand Danmaku glow/trails only; never changes server bullets or hides hit cores.").defineEnum("danmakuEffects",Effects.FULL);
    public static final ForgeConfigSpec SPEC=BUILDER.build();
    private GrandDanmakuClientConfig(){}
    public static boolean reduced(){return EFFECTS.get()==Effects.REDUCED;}
}

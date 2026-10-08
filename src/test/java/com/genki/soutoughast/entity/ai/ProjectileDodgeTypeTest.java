package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.projectile.*;
import net.minecraft.world.entity.projectile.*;

/** Exact mapped class boundary only; no native owner/input/selection claim. */
public final class ProjectileDodgeTypeTest {
    public static void main(String[] args){
        Class<?>[] allowed={Arrow.class,SpectralArrow.class,SmallFireball.class,LargeFireball.class};
        Class<?>[] excluded={StandardSoutouFireball.class,CommittedSoutouFireball.class,GroundSoutouFireball.class,Snowball.class,ThrownEgg.class,FishingHook.class,ThrownEnderpearl.class,Fireball.class};
        for(var type:allowed)if(!SoutouGhastProjectileDodge.supportedType(type))throw new AssertionError("SUPPORTED_CLASS_"+type.getName());
        for(var type:excluded)if(SoutouGhastProjectileDodge.supportedType(type))throw new AssertionError("EXCLUDED_CLASS_"+type.getName());
        System.out.println("PASS:12mapped projectile dodge class boundaries; not native owner/input acceptance");
    }
}

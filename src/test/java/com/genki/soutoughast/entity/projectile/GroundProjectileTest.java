package com.genki.soutoughast.entity.projectile;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;

/** Genuine mapped shape/type policy, not actual world damage/reload/input. */
public final class GroundProjectileTest {
    private static int checks;
    public static void main(String[] args){
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        check(Block.isFaceFull(Shapes.block(),Direction.UP),"full block top supports Ground footprint");
        check(!Block.isFaceFull(Shapes.box(0,0,0,1,.5,1),Direction.UP),"lower slab cannot masquerade as integer-height floor");
        check(!Block.isFaceFull(Shapes.box(0,0,0,.5,1,1),Direction.UP),"partial top cannot support full tile");
        check(!Block.isFaceFull(Shapes.empty(),Direction.UP),"floor gap rejected");
        check(StandardSoutouFireball.class.isAssignableFrom(GroundSoutouFireball.class),"Ground retains native Standard provenance and deflection family");
        check(!net.minecraft.world.entity.projectile.LargeFireball.class.isAssignableFrom(GroundSoutouFireball.class),"Ground cannot enter vanilla1000damage branch");
        System.out.println("PASS: "+checks+" mapped Ground shape/type checks (no actual damage/reload/input acceptance)");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

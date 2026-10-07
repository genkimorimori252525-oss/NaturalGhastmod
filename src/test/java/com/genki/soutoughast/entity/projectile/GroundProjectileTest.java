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
        for(var direction:new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(1,0,0),new net.minecraft.world.phys.Vec3(1,1,1),new net.minecraft.world.phys.Vec3(-3,.2,7)}){
            var velocity=direction.normalize().scale(1.9);
            var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(42,velocity);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{
                packet.write(buffer);var decoded=new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(buffer);
                check(decoded.getId()==42&&decoded.getXa()==(int)(velocity.x*8000)&&decoded.getYa()==(int)(velocity.y*8000)&&decoded.getZa()==(int)(velocity.z*8000),"actual mapped motion packet roundtrip truncates all components");
                double client=new net.minecraft.world.phys.Vec3(decoded.getXa()/8000.0,decoded.getYa()/8000.0,decoded.getZa()/8000.0).length();
                check(client<=1.9+1e-9&&client>=1.9-Math.sqrt(3)/8000-1e-9,"first air client velocity permits only native norm loss");
            }finally{buffer.release();}
        }
        System.out.println("PASS: "+checks+" mapped Ground shape/type checks (no actual damage/reload/input acceptance)");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

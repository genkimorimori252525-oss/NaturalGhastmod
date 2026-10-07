package com.genki.soutoughast.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import java.util.UUID;

/** Mapped dependency/codec verification; no world or Player interaction claims. */
public final class StandardProjectileTest {
    private static int checks;
    public static void main(String[] args){
        check(Fireball.class.isAssignableFrom(StandardSoutouFireball.class),"Ghast-like Fireball family");
        check(!LargeFireball.class.isAssignableFrom(StandardSoutouFireball.class),"cannot enter vanilla reflected1000 branch");
        var origin=UUID.randomUUID();var player=UUID.randomUUID();
        var state=new StandardProvenance(origin,player,true,false,0,123);
        CompoundTag tag=new CompoundTag();state.write(tag);
        check(StandardProvenance.read(tag).equals(state),"save/reload provenance");
        FriendlyByteBuf bytes=new FriendlyByteBuf(Unpooled.buffer());
        try{bytes.writeNbt(tag);check(StandardProvenance.read(bytes.readNbt()).equals(state),"mapped packet NBT roundtrip");}finally{bytes.release();}
        tag.putInt("BossReturns",99);tag.putInt("StandardAge",-20);
        var bounded=StandardProvenance.read(tag);
        check(bounded.returns()==1&&bounded.bossAttempted()&&bounded.age()==0,"reload bounds cannot reopen return budget");
        tag.remove("StandardOrigin");check(!StandardProvenance.read(tag).playerDeflected(),"missing origin disables immunity admission");
        tag.remove("StandardDeflector");check(!StandardProvenance.read(tag).playerDeflected(),"missing deflector disables admission");
        check(state.matchesReturn(origin,player)&&!state.matchesReturn(UUID.randomUUID(),player),"origin identity is required");
        check(!state.matchesReturn(origin,UUID.randomUUID()),"live owner must match deflector");
        check(StandardSoutouFireball.PICK_RADIUS==1.5F&&StandardSoutouFireball.VISUAL_SCALE==1.5F,"independent interaction and visual scales");
        System.out.println("PASS: "+checks+" mapped Standard provenance/type checks (no real-input acceptance)");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

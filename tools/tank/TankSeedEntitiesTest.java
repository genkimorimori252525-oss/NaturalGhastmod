package com.genki.soutoughast.tank;

import com.github.tartaricacid.touhoulittlemaid.sim.debug.TankSeedEntities;
import net.minecraft.nbt.*;

/** Developer fixture policy, not removal of explicitly registered test subjects. */
public final class TankSeedEntitiesTest {
    public static void main(String[] args) {
        ListTag rows=new ListTag();
        CompoundTag ghast=entity("soutou_ghast:soutou_ghast");ghast.putString("CustomName","preserve");
        CompoundTag item=entity("minecraft:item");item.putInt("Age",123);
        CompoundTag similarlyNamed=entity("other_mod:reimu");
        rows.add(entity("touhou_little_maid:reimu"));rows.add(ghast);rows.add(item);
        rows.add(entity("touhou_little_maid:reimu"));rows.add(similarlyNamed);
        if(TankSeedEntities.removeSeedReimu(rows)!=2)throw new AssertionError("all exact seed Reimu must be removed");
        if(rows.size()!=3||rows.get(0)!=ghast||rows.get(1)!=item||rows.get(2)!=similarlyNamed)
            throw new AssertionError("unrelated entity data/order changed");
        if(!ghast.getString("CustomName").equals("preserve")||item.getInt("Age")!=123)
            throw new AssertionError("non-Reimu NBT changed");
        if(TankSeedEntities.removeSeedReimu(rows)!=0)throw new AssertionError("cleanup must be idempotent");
        if(TankSeedEntities.removeSeedReimu(new ListTag())!=0)throw new AssertionError("empty seed must be supported");
        System.out.println("PASS: 5 seed-entity policy regressions");
    }
    private static CompoundTag entity(String type){CompoundTag tag=new CompoundTag();tag.putString("id",type);return tag;}
}

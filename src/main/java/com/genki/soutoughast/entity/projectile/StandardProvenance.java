package com.genki.soutoughast.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Durable origin remains distinct from current ownership across rally/reload. */
public record StandardProvenance(UUID origin,UUID deflector,boolean playerDeflected,boolean bossAttempted,int returns,int age) {
    public StandardProvenance {
        returns=Math.max(0,Math.min(1,returns));age=Math.max(0,Math.min(400,age));
        playerDeflected=playerDeflected&&origin!=null&&deflector!=null;
        bossAttempted=bossAttempted||returns>0;
    }
    public boolean matchesReturn(UUID boss,UUID currentOwner){return playerDeflected&&origin.equals(boss)&&deflector.equals(currentOwner);}
    public void write(CompoundTag tag){
        if(origin!=null)tag.putUUID("StandardOrigin",origin);if(deflector!=null)tag.putUUID("StandardDeflector",deflector);
        tag.putBoolean("PlayerDeflected",playerDeflected);tag.putBoolean("BossAttempted",bossAttempted);
        tag.putInt("BossReturns",returns);tag.putInt("StandardAge",age);
    }
    public static StandardProvenance read(CompoundTag tag){
        return new StandardProvenance(tag.hasUUID("StandardOrigin")?tag.getUUID("StandardOrigin"):null,
                tag.hasUUID("StandardDeflector")?tag.getUUID("StandardDeflector"):null,
                tag.getBoolean("PlayerDeflected"),tag.getBoolean("BossAttempted"),tag.getInt("BossReturns"),tag.getInt("StandardAge"));
    }
}

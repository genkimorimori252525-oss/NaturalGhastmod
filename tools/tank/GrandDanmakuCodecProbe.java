package com.genki.soutoughast.danmaku;

import com.genki.soutoughast.danmaku.core.*;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;

/** Private verification-JAR only. Tests real wire code inside transformed Forge, never sends synthetic gameplay packets. */
public final class GrandDanmakuCodecProbe {
    private static int checks,maximumBytes;
    private static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
    private static void roundtrip(GrandDanmakuNetwork.Packet packet){var b=new FriendlyByteBuf(Unpooled.buffer());try{GrandDanmakuNetwork.encode(packet,b);maximumBytes=Math.max(maximumBytes,b.readableBytes());check(packet.equals(GrandDanmakuNetwork.decode(b)),"real wire roundtrip");}finally{b.release();}}
    private static boolean rejected(FriendlyByteBuf b){try{GrandDanmakuNetwork.decode(b);return false;}catch(IllegalArgumentException expected){return true;}}
    public static JsonObject run(){
        checks=maximumBytes=0;UUID session=UUID.randomUUID(),owner=UUID.randomUUID();var pattern=GrandDanmakuAssets.get(2);
        var frame=DanmakuFrame.toward(new DanmakuFrame.Vec(26,228,32),new DanmakuFrame.Vec(26,225,12),null);var world=new WorldDanmakuSession(pattern.score());
        for(int i=0;i<100;i++)world.step(frame,(b,a,z)->false);
        var sync=new DanmakuPlayback.Sync(session,owner,1,"minecraft:overworld",2,pattern.hash(),0,world.age(),1000,world.bullets());roundtrip(GrandDanmakuNetwork.Packet.sync(sync));
        var batch=world.step(frame,(b,a,z)->false);roundtrip(GrandDanmakuNetwork.Packet.step(1,owner,new DanmakuPlayback.Delta(session,1,batch.age(),1001,frame,batch.bursts(),List.of())));
        roundtrip(GrandDanmakuNetwork.Packet.end(1,owner,session,false,"NORMAL_DRAIN_COMPLETE"));roundtrip(GrandDanmakuNetwork.Packet.end(1,owner,session,true,"OWNER_REMOVED"));
        var synthetic=new ArrayList<WorldDanmakuSession.Bullet>();for(int i=0;i<3000;i++)synthetic.add(new WorldDanmakuSession.Bullet(i,0,0,100,frame.origin(),new DanmakuFrame.Vec(.1,0,.2),.12));
        roundtrip(GrandDanmakuNetwork.Packet.sync(new DanmakuPlayback.Sync(session,owner,1,"minecraft:overworld",2,pattern.hash(),0,0,1000,synthetic)));
        check(maximumBytes<=GrandDanmakuAssets.MAX_PACKET_BYTES,"worst-size wire snapshot within explicit budget");
        var extra=new FriendlyByteBuf(Unpooled.buffer());try{GrandDanmakuNetwork.encode(GrandDanmakuNetwork.Packet.sync(sync),extra);extra.writeByte(1);check(rejected(extra),"unknown trailing fields rejected");}finally{extra.release();}
        var huge=new FriendlyByteBuf(Unpooled.buffer());try{huge.writeZero(GrandDanmakuAssets.MAX_PACKET_BYTES+1);check(rejected(huge),"oversized packet rejected before decode/allocation");}finally{huge.release();}
        for(int count:new int[]{-1,3001}){
            var bad=new FriendlyByteBuf(Unpooled.buffer());try{bad.writeByte(0);bad.writeVarInt(1);bad.writeUUID(owner);bad.writeUUID(session);bad.writeUtf("minecraft:overworld",128);bad.writeByte(2);bad.writeUtf(pattern.hash(),64);bad.writeVarInt(0);bad.writeVarInt(0);bad.writeLong(1000);bad.writeVarInt(count);check(rejected(bad),"invalid snapshot count rejected before population allocation");}finally{bad.release();}
        }
        var result=new JsonObject();result.addProperty("status","PASS");result.addProperty("checks",checks);result.addProperty("maximumPacketBytes",maximumBytes);result.addProperty("scope","ACTUAL_FORGE_WIRE_CODE_ONLY_SYNTHETIC_DATA_NOT_GAMEPLAY_OR_NETWORK_DELIVERY");return result;
    }
}

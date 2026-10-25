package com.genki.soutoughast.danmaku;

import com.genki.soutoughast.danmaku.core.*;
import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

/** Direction-bound v1 transport. No C2S birth, movement, damage or activation operation. */
public final class GrandDanmakuNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("soutou_ghast","grand_danmaku"),()->"1","1"::equals,"1"::equals);
    private static boolean registered;
    public record Packet(int kind,int entityId,UUID owner,UUID session,DanmakuPlayback.Sync sync,DanmakuPlayback.Delta delta,boolean hardEnd,String reason) {
        public Packet {if(kind<0||kind>2||entityId<0||owner==null||session==null||kind==0&&sync==null||kind==1&&delta==null||reason==null||reason.length()>64)throw new IllegalArgumentException("DANMAKU_PACKET_ENVELOPE");}
        public static Packet sync(DanmakuPlayback.Sync s){return new Packet(0,s.entityId(),s.owner(),s.session(),s,null,false,"");}
        public static Packet step(int id,UUID owner,DanmakuPlayback.Delta d){return new Packet(1,id,owner,d.session(),null,d,false,"");}
        public static Packet end(int id,UUID owner,UUID session,boolean hard,String reason){return new Packet(2,id,owner,session,null,null,hard,reason);}
    }
    public record Ack(int entityId,UUID owner,UUID session,String hash,boolean accepted) {
        public Ack {if(entityId<0||owner==null||session==null||hash==null||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("DANMAKU_ACK_BOUND");}
    }
    private GrandDanmakuNetwork(){}
    public static synchronized void register(){
        if(registered)return;registered=true;
        CHANNEL.registerMessage(0,Packet.class,GrandDanmakuNetwork::encode,GrandDanmakuNetwork::decode,GrandDanmakuNetwork::client,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Ack.class,(m,b)->{b.writeVarInt(m.entityId());b.writeUUID(m.owner());b.writeUUID(m.session());b.writeUtf(m.hash(),64);b.writeBoolean(m.accepted());},
                b->new Ack(b.readVarInt(),b.readUUID(),b.readUUID(),b.readUtf(64),b.readBoolean()),GrandDanmakuNetwork::server,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void send(ServerPlayer player,Packet message){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);}
    public static void acknowledge(Ack message){CHANNEL.sendToServer(message);}
    private static void client(Packet message,Supplier<NetworkEvent.Context> supplied){
        var context=supplied.get();context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.genki.soutoughast.client.danmaku.GrandDanmakuClient.accept(message)));context.setPacketHandled(true);
    }
    private static void server(Ack message,Supplier<NetworkEvent.Context> supplied){
        var context=supplied.get();context.enqueueWork(()->{var player=context.getSender();if(player==null)return;var entity=player.serverLevel().getEntity(message.entityId());
            if(entity instanceof SoutouGhast boss&&boss.getUUID().equals(message.owner()))boss.getGrandDanmaku().acknowledge(player,message.session(),message.hash(),message.accepted());});context.setPacketHandled(true);
    }
    private static void vector(FriendlyByteBuf b,DanmakuFrame.Vec v){b.writeDouble(v.x());b.writeDouble(v.y());b.writeDouble(v.z());}
    private static DanmakuFrame.Vec vector(FriendlyByteBuf b){var v=new DanmakuFrame.Vec(b.readDouble(),b.readDouble(),b.readDouble());if(Math.abs(v.x())>30000000||Math.abs(v.y())>30000000||Math.abs(v.z())>30000000)throw new IllegalArgumentException("DANMAKU_VECTOR_WIRE_BOUND");return v;}
    private static void frame(FriendlyByteBuf b,DanmakuFrame f){vector(b,f.origin());vector(b,f.right());vector(b,f.up());vector(b,f.forward());}
    private static DanmakuFrame frame(FriendlyByteBuf b){return new DanmakuFrame(vector(b),vector(b),vector(b),vector(b));}
    private static void bullet(FriendlyByteBuf b,WorldDanmakuSession.Bullet v){b.writeLong(v.id());b.writeVarInt(v.trackIndex());b.writeVarInt(v.bornTick());b.writeVarInt(v.lifetimeTicks());vector(b,v.origin());vector(b,v.velocity());b.writeDouble(v.radius());}
    private static WorldDanmakuSession.Bullet bullet(FriendlyByteBuf b){return new WorldDanmakuSession.Bullet(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),vector(b),vector(b),b.readDouble());}
    private static Packet complete(FriendlyByteBuf buffer,Packet packet){if(buffer.readableBytes()!=0)throw new IllegalArgumentException("DANMAKU_TRAILING_WIRE_BYTES");return packet;}
    private static int count(FriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("DANMAKU_WIRE_COUNT");return n;}
    static void encode(Packet m,FriendlyByteBuf b){
        int start=b.writerIndex();b.writeByte(m.kind());b.writeVarInt(m.entityId());b.writeUUID(m.owner());b.writeUUID(m.session());
        if(m.kind()==0){var s=m.sync();b.writeUtf(s.dimension(),128);b.writeByte(s.pattern());b.writeUtf(s.scoreHash(),64);b.writeVarInt(s.sequence());b.writeVarInt(s.age());b.writeLong(s.gameTick());b.writeVarInt(s.bullets().size());for(var v:s.bullets())bullet(b,v);}
        else if(m.kind()==1){var d=m.delta();b.writeVarInt(d.sequence());b.writeVarInt(d.age());b.writeLong(d.gameTick());frame(b,d.frame());b.writeVarInt(d.bursts().size());for(var x:d.bursts()){b.writeVarInt(x.trackIndex());b.writeVarInt(x.bornTick());}b.writeVarInt(d.removals().size());for(var r:d.removals()){b.writeLong(r.id());vector(b,r.point());}}
        else{b.writeBoolean(m.hardEnd());b.writeUtf(m.reason(),64);}
        if(b.writerIndex()-start>GrandDanmakuAssets.MAX_PACKET_BYTES)throw new IllegalArgumentException("DANMAKU_PACKET_BYTE_BUDGET");
    }
    static Packet decode(FriendlyByteBuf b){
        if(b.readableBytes()>GrandDanmakuAssets.MAX_PACKET_BYTES)throw new IllegalArgumentException("DANMAKU_PACKET_BYTE_BUDGET");
        int kind=b.readUnsignedByte(),entity=b.readVarInt();UUID owner=b.readUUID(),session=b.readUUID();
        if(kind==0){String dimension=b.readUtf(128);int pattern=b.readUnsignedByte();String hash=b.readUtf(64);int sequence=b.readVarInt(),age=b.readVarInt();long gameTick=b.readLong();int n=count(b,3000);var bullets=new ArrayList<WorldDanmakuSession.Bullet>(n);for(int i=0;i<n;i++)bullets.add(bullet(b));return complete(b,Packet.sync(new DanmakuPlayback.Sync(session,owner,entity,dimension,pattern,hash,sequence,age,gameTick,bullets)));}
        if(kind==1){int sequence=b.readVarInt(),age=b.readVarInt();long gameTick=b.readLong();var frame=frame(b);int n=count(b,32);var bursts=new ArrayList<GrandDanmakuScore.BurstDescriptor>(n);for(int i=0;i<n;i++)bursts.add(new GrandDanmakuScore.BurstDescriptor(b.readVarInt(),b.readVarInt()));n=count(b,3000);var removed=new ArrayList<DanmakuPlayback.Removal>(n);for(int i=0;i<n;i++)removed.add(new DanmakuPlayback.Removal(b.readLong(),vector(b)));return complete(b,Packet.step(entity,owner,new DanmakuPlayback.Delta(session,sequence,age,gameTick,frame,bursts,removed)));}
        if(kind==2)return complete(b,Packet.end(entity,owner,session,b.readBoolean(),b.readUtf(64)));
        throw new IllegalArgumentException("DANMAKU_PACKET_KIND");
    }
}

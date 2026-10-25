package com.genki.soutoughast.client.danmaku;

import com.genki.soutoughast.danmaku.*;
import com.genki.soutoughast.danmaku.core.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

/** Original world sprites and analytic finite ribbons; no particles/entities or damaging cosmetics. */
@Mod.EventBusSubscriber(modid="soutou_ghast",value=Dist.CLIENT)
public final class GrandDanmakuRenderer {
    private static ShaderInstance shader;private static boolean assetsReady;
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(2097152));
    private static long frames;private static int lastCores,lastTrails,lastGhosts;private static long lastNanos;
    public record Diagnostics(boolean ready,long frames,int cores,int trails,int ghosts,long frameNanos) {}
    public static Diagnostics diagnostics(){return new Diagnostics(ready(),frames,lastCores,lastTrails,lastGhosts,lastNanos);}
    public static boolean ready(){return shader!=null&&assetsReady;}
    static ShaderInstance shader(){return shader;}
    private record Draw(DanmakuFrame.Vec point,DanmakuFrame.Vec velocity,double radius,int age,int trailTicks,double glow,GrandDanmakuAssets.Material material,float fade,boolean ghost) {}
    private GrandDanmakuRenderer(){}
    @Mod.EventBusSubscriber(modid="soutou_ghast",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderRegistration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent event){
            GrandDanmakuClient.invalidateAssets();assetsReady=false;shader=null;
            try{
                for(var material:GrandDanmakuAssets.Material.values()){
                    byte[] expected=GrandDanmakuAssets.resource(material.resource(),2097152);
                    var location=new ResourceLocation("soutou_ghast","textures/danmaku/"+material.texture+".png");
                    try(var actual=event.getResourceProvider().getResource(location).orElseThrow().open()){
                        if(!DanmakuResourceIntegrity.matches(expected,actual.readNBytes(2097153),false))throw new IOException("DANMAKU_TEXTURE_RESOURCE_MISMATCH");
                    }
                }
                for(String extension:new String[]{"json","vsh","fsh"}){
                    String file="shaders/core/danmaku."+extension;byte[] expected=GrandDanmakuAssets.resource("assets/soutou_ghast/"+file,65536);
                    try(var actual=event.getResourceProvider().getResource(new ResourceLocation("soutou_ghast",file)).orElseThrow().open()){
                        if(!DanmakuResourceIntegrity.matches(expected,actual.readNBytes(65537),true))throw new IOException("DANMAKU_SHADER_RESOURCE_MISMATCH");
                    }
                }
                event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("soutou_ghast","danmaku"),DefaultVertexFormat.POSITION_COLOR_TEX),value->{shader=value;assetsReady=true;});
            }catch(IOException|RuntimeException invalid){LogUtils.getLogger().error("Grand Danmaku shader/texture readiness failed closed",invalid);}
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!ready())return;
        var minecraft=Minecraft.getInstance();if(minecraft.level==null)return;long began=System.nanoTime();lastCores=lastTrails=lastGhosts=0;
        try{
            boolean reduced=GrandDanmakuClientConfig.reduced();double clientTick=minecraft.level.getGameTime()+event.getPartialTick();var draws=new ArrayList<Draw>();
            for(var playback:GrandDanmakuClient.sessions()){
                var id=playback.identity();if(!playback.valid()||id==null)continue;var pattern=GrandDanmakuAssets.get(id.pattern());double now=playback.renderAge(clientTick);
                for(var bullet:playback.bullets()){
                    int track=bullet.trackIndex(),tail=reduced?Math.min(4,pattern.trailTicks().get(track)):pattern.trailTicks().get(track);
                    double age=Math.max(0,Math.min(bullet.lifetimeTicks()-.001,now-bullet.bornTick()));
                    draws.add(new Draw(bullet.at(age),bullet.velocity(),bullet.radius(),(int)Math.floor(age),tail,pattern.glowScales().get(track),pattern.materials().get(track),1,false));
                }
                for(var ghost:playback.ghosts()){
                    var bullet=ghost.bullet();int track=bullet.trackIndex(),tail=reduced?Math.min(4,pattern.trailTicks().get(track)):pattern.trailTicks().get(track);double elapsed=now-ghost.removedAt();
                    if(tail<=0||elapsed<0||elapsed>=tail)continue;float fade=(float)Math.pow(1-elapsed/tail,2);
                    draws.add(new Draw(ghost.point(),bullet.velocity(),bullet.radius(),Math.max(0,(int)(ghost.removedAt()-bullet.bornTick())),tail,pattern.glowScales().get(track),pattern.materials().get(track),fade,true));
                }
            }
            if(draws.size()>24000)throw new IllegalStateException("DANMAKU_RENDER_ITEM_BUDGET");
            Vec3 camera=event.getCamera().getPosition();var right=new Vector3f(1,0,0).rotate(event.getCamera().rotation());var up=new Vector3f(0,1,0).rotate(event.getCamera().rotation());
            draws.sort(Comparator.comparingDouble((Draw d)->distance(d.point(),camera)).reversed());
            var pose=event.getPoseStack().last().pose();
            for(var material:GrandDanmakuAssets.Material.values()){
                var body=BUFFERS.getBuffer(GrandDanmakuRenderTypes.body(material));
                for(var draw:draws)if(draw.material()==material&&!draw.ghost()){billboard(body,pose,camera,draw.point(),right,up,draw.radius(),1,1,1,1,0);lastCores++;}
                BUFFERS.endBatch(GrandDanmakuRenderTypes.body(material));
                var light=BUFFERS.getBuffer(GrandDanmakuRenderTypes.light(material));
                for(var draw:draws)if(draw.material()==material){
                    if(!draw.ghost()&&!reduced&&material.additive)billboard(light,pose,camera,draw.point(),right,up,draw.radius()*draw.glow(),material.r,material.g,material.b,.10f,2);
                    if(draw.trailTicks()>0&&draw.age()>0){ribbon(light,pose,camera,draw,right,reduced);lastTrails++;if(draw.ghost())lastGhosts++;}
                }
                BUFFERS.endBatch(GrandDanmakuRenderTypes.light(material));
            }
            frames++;lastNanos=System.nanoTime()-began;
        }catch(RuntimeException failure){try{BUFFERS.endBatch();}finally{GrandDanmakuClient.renderingFailed(failure);}}
    }
    private static double distance(DanmakuFrame.Vec p,Vec3 camera){double x=p.x()-camera.x,y=p.y()-camera.y,z=p.z()-camera.z;return x*x+y*y+z*z;}
    private static void billboard(VertexConsumer b,Matrix4f pose,Vec3 camera,DanmakuFrame.Vec p,Vector3f right,Vector3f up,double radius,float r,float g,float blue,float alpha,int mode){
        vertex(b,pose,camera,p,right,up,-radius,-radius,r,g,blue,alpha,mode,1);vertex(b,pose,camera,p,right,up,radius,-radius,r,g,blue,alpha,mode+1,1);
        vertex(b,pose,camera,p,right,up,radius,radius,r,g,blue,alpha,mode+1,0);vertex(b,pose,camera,p,right,up,-radius,radius,r,g,blue,alpha,mode,0);
    }
    private static void vertex(VertexConsumer b,Matrix4f pose,Vec3 camera,DanmakuFrame.Vec p,Vector3f right,Vector3f up,double x,double y,float r,float g,float blue,float alpha,float u,float v){
        b.vertex(pose,(float)(p.x()-camera.x+right.x*x+up.x*y),(float)(p.y()-camera.y+right.y*x+up.y*y),(float)(p.z()-camera.z+right.z*x+up.z*y)).color(r,g,blue,alpha).uv(u,v).endVertex();
    }
    private static void ribbon(VertexConsumer b,Matrix4f pose,Vec3 camera,Draw d,Vector3f fallback,boolean reduced){
        var velocity=d.velocity();int history=Math.min(d.age(),d.trailTicks());var tail=d.point().subtract(velocity.scale(history));
        var eye=new DanmakuFrame.Vec(camera.x-d.point().x(),camera.y-d.point().y(),camera.z-d.point().z());var side=velocity.cross(eye);
        if(side.length()<1e-8)side=new DanmakuFrame.Vec(fallback.x,fallback.y,fallback.z);else side=side.unit();
        double headWidth=d.radius()*(reduced?.65:.9),tailWidth=headWidth*.08;float alpha=(reduced?.18f:.28f)*d.fade();var m=d.material();
        ribbonVertex(b,pose,camera,tail.subtract(side.scale(tailWidth)),m,alpha,-2,0);ribbonVertex(b,pose,camera,tail.add(side.scale(tailWidth)),m,alpha,-1,0);
        ribbonVertex(b,pose,camera,d.point().add(side.scale(headWidth)),m,alpha,-1,1);ribbonVertex(b,pose,camera,d.point().subtract(side.scale(headWidth)),m,alpha,-2,1);
    }
    private static void ribbonVertex(VertexConsumer b,Matrix4f pose,Vec3 camera,DanmakuFrame.Vec p,GrandDanmakuAssets.Material m,float alpha,float u,float v){b.vertex(pose,(float)(p.x()-camera.x),(float)(p.y()-camera.y),(float)(p.z()-camera.z)).color(m.r,m.g,m.b,alpha).uv(u,v).endVertex();}
}

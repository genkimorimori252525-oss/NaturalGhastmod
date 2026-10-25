package com.genki.soutoughast.client.danmaku;

import com.genki.soutoughast.danmaku.GrandDanmakuAssets.Material;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.EnumMap;

/** Own render batches: depth-tested, no depth writes, scoped blend/shader/texture restoration. */
final class GrandDanmakuRenderTypes extends RenderType {
    private static final EnumMap<Material,RenderType> BODY=new EnumMap<>(Material.class),LIGHT=new EnumMap<>(Material.class);
    private GrandDanmakuRenderTypes(){super("unused",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    static RenderType body(Material m){return BODY.computeIfAbsent(m,k->make(k,false));}
    static RenderType light(Material m){return LIGHT.computeIfAbsent(m,k->make(k,true));}
    private static RenderType make(Material m,boolean light){
        return create("soutou_danmaku_"+m.texture+(light?"_light":"_body"),DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,262144,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(GrandDanmakuRenderer::shader))
                        .setTextureState(new TextureStateShard(new ResourceLocation("soutou_ghast","textures/danmaku/"+m.texture+".png"),false,false))
                        .setTransparencyState(light&&m.additive?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    }
}

package com.genki.soutoughast.tank;
import com.genki.soutoughast.danmaku.GrandDanmakuAssets;
/** Actual production loader acceptance/rejection; not a Forge world/render test. */
public final class DanmakuAssetTest {
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("EXPECTED_VALID_OR_REJECT");
  if(args[0].equals("reject")){
   if(GrandDanmakuAssets.ready())throw new AssertionError("malformed authored resources must not admit invisible attacks");
   try{GrandDanmakuAssets.get(0);throw new AssertionError("rejected catalog still usable");}catch(IllegalStateException expected){}
   System.out.println("PASS: actual catalog fails closed for rejected resources");return;
  }
  if(!args[0].equals("valid")||!GrandDanmakuAssets.ready())throw new AssertionError("valid supplied resources rejected: "+GrandDanmakuAssets.failure());
  int tracks=0;for(int i=0;i<3;i++){var p=GrandDanmakuAssets.get(i);if(p.index()!=i||p.materials().size()!=p.score().tracks().size()||p.preludeTicks()<30)throw new AssertionError("canonical score/material/charge binding");tracks+=p.materials().size();}
  for(var material:GrandDanmakuAssets.Material.values()){byte[] raw=GrandDanmakuAssets.resource(material.resource(),2097152);if(raw.length<8||raw[0]!=(byte)137||raw[1]!=80)throw new AssertionError("original texture PNG missing");}
  byte[] shader=GrandDanmakuAssets.resource("assets/soutou_ghast/shaders/core/danmaku.fsh",65536);
  if(!com.genki.soutoughast.danmaku.DanmakuResourceIntegrity.matches(shader,shader,true))throw new AssertionError("unaltered shader rejected");
  byte[] invisible="#version 150\nout vec4 fragColor;\nvoid main(){fragColor=vec4(0.0);}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  if(com.genki.soutoughast.danmaku.DanmakuResourceIntegrity.matches(shader,invisible,true))throw new AssertionError("valid transparent resource-pack shader must be rejected before damage ACK");
  byte[] dependency="#version 150\n#moj_import <unverified.glsl>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  if(com.genki.soutoughast.danmaku.DanmakuResourceIntegrity.matches(dependency,dependency,true))throw new AssertionError("unreviewed shader dependency must fail closed");
  if(tracks!=78)throw new AssertionError("all supplied tracks must have original-material mapping");
  System.out.println("PASS: actual production loader accepts3scores/78tracks/4original PNGs and bounded visual metadata");
 }
}

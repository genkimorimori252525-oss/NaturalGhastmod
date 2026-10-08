package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Actual mapped registry codec. This does not create a ServerLevel or prove restart behavior. */
public final class DomainStateCodecTest {
 private static int checks;
 private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 private static void reject(String state)throws Exception{try{DomainStateCodec.decode(state);throw new AssertionError("accepted invalid state: "+state);}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  for(BlockState state:new BlockState[]{Blocks.AIR.defaultBlockState(),Blocks.STONE.defaultBlockState(),Blocks.GRASS_BLOCK.defaultBlockState(),Blocks.OAK_STAIRS.defaultBlockState(),Blocks.RED_NETHER_BRICKS.defaultBlockState()}){
   String encoded=DomainStateCodec.encode(state);check(DomainStateCodec.decode(encoded).equals(state),"exact registry state roundtrip");
  }
  reject("{\"Name\":\"missing:unknown\"}");
  reject("{\"Name\":\"minecraft:stone\",\"Properties\":{\"unknown\":\"true\"}}");
  reject("{\"Name\":\"minecraft:grass_block\",\"Properties\":{\"snowy\":\"invalid\"}}");
  reject("{\"Name\":\"minecraft:grass_block\"}");
  reject(" {\"Name\":\"minecraft:stone\"}");
  reject("{\"Name\":\"minecraft:stone\",\"Name\":\"minecraft:stone\"}");
  reject("{\"Name\":\"minecraft:stone\",\"extra\":true}");
  reject("{".repeat(1100));reject("null");
  System.out.println("PASS: "+checks+" mapped Domain registry codec checks; native mutation/restart NOT_RUN");
 }
}

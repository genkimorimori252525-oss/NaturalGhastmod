package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import net.minecraft.world.level.block.state.BlockState;

/** Exact registry decoding: reject partial/defaulted/dropped properties before any mutation. */
public final class DomainStateCodec {
 private DomainStateCodec(){}
 public static String encode(BlockState state)throws IOException{
  JsonElement json=BlockState.CODEC.encodeStart(JsonOps.INSTANCE,state).result().orElseThrow(()->new IOException("DOMAIN_STATE_ENCODE"));
  String canonical=DomainJournalCodec.canonical(json);if(canonical.length()>1024)throw new IOException("DOMAIN_STATE_SIZE");return canonical;
 }
 public static BlockState decode(String text)throws IOException{
  if(text==null||text.isBlank()||text.length()>1024)throw new IOException("DOMAIN_STATE_SIZE");
  try{
   BlockState state=BlockState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(text)).result().orElseThrow(()->new IOException("DOMAIN_STATE_REGISTRY"));
   if(!encode(state).equals(text))throw new IOException("DOMAIN_STATE_NONCANONICAL_OR_DEFAULTED");return state;
  }catch(RuntimeException error){throw new IOException("DOMAIN_STATE_INVALID",error);}
 }
}

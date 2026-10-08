package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Bounded canonical records; reject duplicate keys, checksum drift and ambiguous numeric forms. */
public final class DomainJournalCodec {
 public static final int MAX_BYTES=4*1024*1024;
 public static final String ABSENT="ABSENT";
 public record Record(JsonObject payload,String predecessor) {}
 private DomainJournalCodec(){}
 public static byte[] encode(DomainOverlay.Journal journal)throws IOException{return encode(journal,ABSENT);}
 public static byte[] encode(DomainOverlay.Journal journal,String predecessor)throws IOException{
  JsonObject p=new JsonObject();p.addProperty("kind","DOMAIN_JOURNAL");p.add("journal",journalJson(journal));return wrap(p,predecessor);
 }
 public static DomainOverlay.Journal decode(byte[] bytes)throws IOException{return journal(unwrap(bytes).payload());}
 static byte[] wrap(JsonObject payload,String predecessor)throws IOException{
  validateHash(predecessor);JsonObject body=new JsonObject();body.addProperty("predecessorSha256",predecessor);body.add("payload",payload);
  JsonObject record=new JsonObject();record.addProperty("schemaVersion",1);record.add("body",body);record.addProperty("bodySha256",sha(canonical(body).getBytes(StandardCharsets.UTF_8)));
  byte[] bytes=(canonical(record)+"\n").getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("DOMAIN_RECORD_SIZE_LIMIT");return bytes;
 }
 static Record unwrap(byte[] bytes)throws IOException{
  if(bytes.length>MAX_BYTES)throw new IOException("DOMAIN_RECORD_SIZE_LIMIT");
  try{
   String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
   JsonReader reader=new JsonReader(new StringReader(text));reader.setLenient(false);JsonObject root=object(value(reader,0));if(reader.peek()!=JsonToken.END_DOCUMENT)throw new IOException("DOMAIN_TRAILING_JSON");
   keys(root,"schemaVersion","body","bodySha256");if(number(root,"schemaVersion")!=1)throw new IOException("DOMAIN_RECORD_SCHEMA");JsonObject body=object(root.get("body"));keys(body,"predecessorSha256","payload");
   String predecessor=string(body,"predecessorSha256");validateHash(predecessor);if(!sha(canonical(body).getBytes(StandardCharsets.UTF_8)).equals(string(root,"bodySha256")))throw new IOException("DOMAIN_RECORD_CHECKSUM");
   Record result=new Record(object(body.get("payload")),predecessor);if(!Arrays.equals(bytes,wrap(result.payload(),predecessor)))throw new IOException("DOMAIN_NONCANONICAL_RECORD");return result;
  }catch(RuntimeException error){throw new IOException("DOMAIN_RECORD_INVALID",error);}
 }
 static DomainOverlay.Journal journal(JsonObject payload)throws IOException{
  try{
   keys(payload,"kind","journal");if(!string(payload,"kind").equals("DOMAIN_JOURNAL"))throw new IOException("DOMAIN_RECORD_KIND");
   JsonObject j=object(payload.get("journal"));keys(j,"identity","phase","entries","cursor","reason");JsonObject id=object(j.get("identity"));keys(id,"domain","owner","dimension","slot","generation");
   var identity=new DomainOverlay.Identity(uuid(string(id,"domain")),uuid(string(id,"owner")),string(id,"dimension"),Math.toIntExact(number(id,"slot")),number(id,"generation"));
   JsonArray rows=j.getAsJsonArray("entries");if(rows.size()>DomainOverlay.MAX_CHANGED)throw new IOException("DOMAIN_ENTRY_LIMIT");List<DomainOverlay.Entry> entries=new ArrayList<>();
   for(JsonElement raw:rows){JsonObject e=object(raw);keys(e,"cell","original","overlay","status");JsonArray cell=e.getAsJsonArray("cell");if(cell.size()!=3)throw new IOException("DOMAIN_CELL_LENGTH");
    entries.add(new DomainOverlay.Entry(new DomainGeometry.Cell(integer(cell.get(0)),integer(cell.get(1)),integer(cell.get(2))),string(e,"original"),string(e,"overlay"),DomainOverlay.Status.valueOf(string(e,"status"))));}
   return new DomainOverlay.Journal(identity,DomainOverlay.Phase.valueOf(string(j,"phase")),entries,Math.toIntExact(number(j,"cursor")),string(j,"reason"));
  }catch(RuntimeException error){throw new IOException("DOMAIN_JOURNAL_INVALID",error);}
 }
 private static JsonObject journalJson(DomainOverlay.Journal j){
  JsonObject id=new JsonObject();id.addProperty("domain",j.identity().domain().toString());id.addProperty("owner",j.identity().owner().toString());id.addProperty("dimension",j.identity().dimension());id.addProperty("slot",j.identity().slot());id.addProperty("generation",j.identity().generation());
  JsonObject body=new JsonObject();body.add("identity",id);body.addProperty("phase",j.phase().name());body.addProperty("cursor",j.cursor());body.addProperty("reason",j.reason());JsonArray rows=new JsonArray();
  for(var e:j.entries()){JsonObject row=new JsonObject();JsonArray cell=new JsonArray();cell.add(e.cell().x());cell.add(e.cell().y());cell.add(e.cell().z());row.add("cell",cell);row.addProperty("original",e.original());row.addProperty("overlay",e.overlay());row.addProperty("status",e.status().name());rows.add(row);}body.add("entries",rows);return body;
 }
 private static JsonElement value(JsonReader r,int depth)throws IOException{
  if(depth>12)throw new IOException("DOMAIN_JSON_DEPTH");
  return switch(r.peek()){
   case BEGIN_OBJECT->{r.beginObject();JsonObject o=new JsonObject();while(r.hasNext()){String name=r.nextName();if(name.length()>64||o.has(name)||o.size()>=16)throw new IOException("DOMAIN_JSON_KEYS");o.add(name,value(r,depth+1));}r.endObject();yield o;}
   case BEGIN_ARRAY->{r.beginArray();JsonArray a=new JsonArray();while(r.hasNext()){if(a.size()>=4096)throw new IOException("DOMAIN_JSON_ARRAY_LIMIT");a.add(value(r,depth+1));}r.endArray();yield a;}
   case STRING->{String s=r.nextString();if(s.length()>2048)throw new IOException("DOMAIN_JSON_STRING_LIMIT");yield new JsonPrimitive(s);}
   case NUMBER->{String n=r.nextString();if(n.length()>32||!n.matches("-?(0|[1-9][0-9]*)"))throw new IOException("DOMAIN_JSON_INTEGER_FORM");yield new JsonPrimitive(new BigDecimal(n));}
   case BOOLEAN->new JsonPrimitive(r.nextBoolean());
   case NULL->{r.nextNull();yield JsonNull.INSTANCE;}
   default->throw new IOException("DOMAIN_JSON_TOKEN");
  };
 }
 static String canonical(JsonElement e){
  if(e.isJsonObject()){List<String> keys=new ArrayList<>(e.getAsJsonObject().keySet());Collections.sort(keys);List<String> fields=new ArrayList<>();for(String k:keys)fields.add(new JsonPrimitive(k)+":"+canonical(e.getAsJsonObject().get(k)));return "{"+String.join(",",fields)+"}";}
  if(e.isJsonArray()){List<String> values=new ArrayList<>();for(var v:e.getAsJsonArray())values.add(canonical(v));return "["+String.join(",",values)+"]";}return e.toString();
 }
 static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
 static void keys(JsonObject o,String... expected)throws IOException{if(!o.keySet().equals(Set.of(expected)))throw new IOException("DOMAIN_RECORD_FIELDS");}
 static JsonObject object(JsonElement e)throws IOException{if(e==null||!e.isJsonObject())throw new IOException("DOMAIN_OBJECT_REQUIRED");return e.getAsJsonObject();}
 static String string(JsonObject o,String key)throws IOException{JsonElement e=o.get(key);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IOException("DOMAIN_STRING_REQUIRED");return e.getAsString();}
 static long number(JsonObject o,String key)throws IOException{try{JsonElement e=o.get(key);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw new IOException("DOMAIN_INTEGER_REQUIRED");return e.getAsBigDecimal().longValueExact();}catch(ArithmeticException error){throw new IOException("DOMAIN_INTEGER_REQUIRED",error);}}
 private static int integer(JsonElement e)throws IOException{JsonObject o=new JsonObject();o.add("value",e);return Math.toIntExact(number(o,"value"));}
 private static UUID uuid(String text)throws IOException{UUID id=UUID.fromString(text);if(!id.toString().equals(text))throw new IOException("DOMAIN_UUID_CANONICAL");return id;}
 private static void validateHash(String hash)throws IOException{if(hash==null||!hash.equals(ABSENT)&&!hash.matches("[a-f0-9]{64}"))throw new IOException("DOMAIN_PREDECESSOR_HASH");}
}

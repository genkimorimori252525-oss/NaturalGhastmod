package com.github.tartaricacid.touhoulittlemaid.sim.debug;

import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Offline bounded fixture in an exclusively created save copy. No runtime mutation API. */
public final class PrepareFlightTank {
 public static final UUID SUBJECT=UUID.fromString("67676767-1007-4000-8000-000000000001");
 private static final int SIZE=52,Y=224,HEIGHT=24;
 public static int allocatedCells(){return (SIZE+2)*(SIZE+2)*(HEIGHT+2);}
 private static boolean allocated(int x,int y,int z){return x>=-1&&x<=SIZE&&z>=-1&&z<=SIZE&&y>=Y-1&&y<=Y+HEIGHT;}
 private static String fixtureBlock(int x,int y,int z){return fixtureBlock(x,y,z,false);}
 private static String fixtureBlock(int x,int y,int z,boolean singleCellSeal){
  return fixtureBlock(x,y,z,singleCellSeal,false);
 }
 private static String fixtureBlock(int x,int y,int z,boolean singleCellSeal,boolean ground){
  return fixtureBlock(x,y,z,singleCellSeal,ground,false);
 }
 private static String fixtureBlock(int x,int y,int z,boolean singleCellSeal,boolean ground,boolean domain){
  boolean shell=x==-1||x==SIZE||z==-1||z==SIZE||y==Y-1||y==Y+HEIGHT;
  if(domain)return y==Y-1?"minecraft:stone":shell?"minecraft:black_concrete":singleCellSeal&&x==26&&y==228&&z==26?"minecraft:stone":"minecraft:air";
  boolean wall=z==6&&x>=7&&x<=12&&y>=224&&y<=237;
  boolean aperture=z==6&&x>=8&&x<=10&&y>=225&&y<=231&&!(x==9&&y==228);
  return shell?"minecraft:black_concrete":ground&&y==230|| (singleCellSeal?x==9&&y==(ground?226:228)&&z==6:wall&&!aperture)?"minecraft:stone":"minecraft:air";
 }
 private static CompoundTag state(String name){CompoundTag s=new CompoundTag();s.putString("Name",name);return s;}
 private static CompoundTag cell(CompoundTag section,int x,int y,int z){
  CompoundTag states=section.getCompound("block_states");ListTag palette=states.getList("palette",Tag.TAG_COMPOUND);
  if(palette.isEmpty())return state("minecraft:air");
  int index=0;
  if(palette.size()>1){int bits=Math.max(4,32-Integer.numberOfLeadingZeros(palette.size()-1)),per=64/bits,offset=(y<<8)|(z<<4)|x;
   long[] data=states.getLongArray("data");if(data.length!=(4096+per-1)/per)throw new IllegalStateException("INVALID_SECTION_PALETTE_LENGTH");
   index=(int)((data[offset/per]>>>((offset%per)*bits))&((1L<<bits)-1));}
  if(index>=palette.size())throw new IllegalStateException("INVALID_SECTION_PALETTE_INDEX");return palette.getCompound(index).copy();
 }
 public static String block(CompoundTag section,int x,int y,int z){return cell(section,x,y,z).getString("Name");}
 public static CompoundTag section(int sectionY,int chunkX,int chunkZ){return section(new CompoundTag(),sectionY,chunkX,chunkZ);}
 public static CompoundTag section(int sectionY,int chunkX,int chunkZ,boolean singleCellSeal){return section(new CompoundTag(),sectionY,chunkX,chunkZ,singleCellSeal);}
 public static CompoundTag section(int sectionY,int chunkX,int chunkZ,boolean singleCellSeal,boolean ground){return section(new CompoundTag(),sectionY,chunkX,chunkZ,singleCellSeal,ground);}
 public static CompoundTag section(int sectionY,int chunkX,int chunkZ,boolean singleCellSeal,boolean ground,boolean domain){return section(new CompoundTag(),sectionY,chunkX,chunkZ,singleCellSeal,ground,domain);}
 private static CompoundTag section(CompoundTag previous,int sectionY,int chunkX,int chunkZ){
  return section(previous,sectionY,chunkX,chunkZ,false);
 }
 private static CompoundTag section(CompoundTag previous,int sectionY,int chunkX,int chunkZ,boolean singleCellSeal){
  return section(previous,sectionY,chunkX,chunkZ,singleCellSeal,false);
 }
 private static CompoundTag section(CompoundTag previous,int sectionY,int chunkX,int chunkZ,boolean singleCellSeal,boolean ground){
  return section(previous,sectionY,chunkX,chunkZ,singleCellSeal,ground,false);
 }
 private static CompoundTag section(CompoundTag previous,int sectionY,int chunkX,int chunkZ,boolean singleCellSeal,boolean ground,boolean domain){
  ListTag palette=new ListTag();Map<String,Integer> indices=new LinkedHashMap<>();int[] cells=new int[4096];
  for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
   int wx=chunkX*16+x,wy=sectionY*16+y,wz=chunkZ*16+z;
   CompoundTag value=allocated(wx,wy,wz)?state(fixtureBlock(wx,wy,wz,singleCellSeal,ground,domain)):cell(previous,x,y,z);
   String key=value.toString();Integer index=indices.get(key);if(index==null){index=palette.size();indices.put(key,index);palette.add(value);}
   cells[(y<<8)|(z<<4)|x]=index;
  }
  CompoundTag output=previous.copy(),states=new CompoundTag();output.putByte("Y",(byte)sectionY);states.put("palette",palette);
  if(palette.size()>1){int bits=Math.max(4,32-Integer.numberOfLeadingZeros(palette.size()-1)),per=64/bits;long[] data=new long[(4096+per-1)/per];
   for(int i=0;i<4096;i++)data[i/per]|=(long)cells[i]<<((i%per)*bits);states.putLongArray("data",data);}
  output.put("block_states",states);output.remove("BlockLight");output.remove("SkyLight");
  if(!output.contains("biomes")){CompoundTag biomes=new CompoundTag();ListTag p=new ListTag();p.add(StringTag.valueOf("minecraft:the_void"));biomes.put("palette",p);output.put("biomes",biomes);}
  return output;
 }
 private static ListTag vector(double... values){ListTag t=new ListTag();for(double v:values)t.add(DoubleTag.valueOf(v));return t;}
 private static ListTag rotation(float yaw,float pitch){ListTag t=new ListTag();t.add(FloatTag.valueOf(yaw));t.add(FloatTag.valueOf(pitch));return t;}
 public static void main(String[] args)throws Exception{
  if(args.length!=2&&(args.length!=3||!Set.of("SINGLE_CELL_SEAL","STANDARD_FIREBALL","OVERHEAD_BOMBING","GROUND_COMBAT","DOMAIN_COMBAT","DOMAIN_RELIABILITY").contains(args[2])))throw new IllegalArgumentException("PRIVATE_FIXTURE_VARIANT_REQUIRED");
  boolean reliability=args.length==3&&args[2].equals("DOMAIN_RELIABILITY"),singleCellSeal=args.length==3&&!reliability,overhead=args.length==3&&args[2].equals("OVERHEAD_BOMBING"),ground=args.length==3&&args[2].equals("GROUND_COMBAT"),domain=args.length==3&&(args[2].equals("DOMAIN_COMBAT")||reliability),standard=args.length==3&&(args[2].equals("STANDARD_FIREBALL")||overhead||ground||domain);
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  Path root=Path.of(args[0]).toRealPath(),allowed=Path.of(args[1]).toRealPath();
  if(!root.startsWith(allowed)||root.equals(allowed)||Files.exists(root.resolve("fixture.json")))throw new IllegalStateException("NEW_PRIVATE_COPY_REQUIRED");
  Path world=root.resolve("game/saves/KNEEKURA_DEBUG_WORLD").toRealPath();if(!world.startsWith(root))throw new IllegalStateException("PRIVATE_WORLD_REQUIRED");
  if(allocatedCells()>100000||SIZE*SIZE*HEIGHT>65536)throw new IllegalStateException("FIXTURE_VOLUME_EXCEEDED");
  try(FileChannel channel=FileChannel.open(world.resolve("session.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()){
   if(lock==null)throw new IllegalStateException("WORLD_RUNNING");
   CompoundTag level=NbtIo.readCompressed(world.resolve("level.dat").toFile()),data=level.getCompound("Data"),player=data.getCompound("Player");
   data.putBoolean("confirmedExperimentalSettings",true);data.putByte("Difficulty",(byte)2);data.putInt("GameType",0);
   if(standard)data.getCompound("GameRules").putString("mobGriefing","false");
   player.put("Pos",domain?vector(26,224,20):vector(9.5,224,3.5));player.put("Motion",vector(0,0,0));player.put("Rotation",rotation(0,overhead?-90:-18));player.putInt("playerGameType",0);player.putFloat("Health",20);
   CompoundTag abilities=player.getCompound("abilities");for(String key:new String[]{"flying","mayfly","invulnerable","instabuild"})abilities.putBoolean(key,false);
   NbtIo.writeCompressed(level,world.resolve("level.dat").toFile());Path playerFile=world.resolve("playerdata/"+player.getUUID("UUID")+".dat");if(Files.exists(playerFile))NbtIo.writeCompressed(player,playerFile.toFile());
   Path regions=world.resolve("region");CompoundTag seed;
   try(RegionFile region=new RegionFile(regions.resolve("r.0.0.mca"),regions,true);var input=region.getChunkDataInputStream(new ChunkPos(0,0))){if(input==null)throw new IllegalStateException("SEED_CHUNK_MISSING");seed=NbtIo.read(input);}
   for(int cx=-1;cx<=3;cx++)for(int cz=-1;cz<=3;cz++){
    ChunkPos pos=new ChunkPos(cx,cz);Path file=regions.resolve("r."+Math.floorDiv(cx,32)+"."+Math.floorDiv(cz,32)+".mca");
    try(RegionFile region=new RegionFile(file,regions,true)){
     CompoundTag chunk;try(var input=region.getChunkDataInputStream(pos)){chunk=input==null?seed.copy():NbtIo.read(input);}
     if(chunk.getInt("xPos")!=cx||chunk.getInt("zPos")!=cz){chunk.put("block_entities",new ListTag());chunk.put("block_ticks",new ListTag());chunk.put("fluid_ticks",new ListTag());}
     if(!chunk.getList("block_entities",Tag.TAG_COMPOUND).isEmpty())throw new IllegalStateException("FIXTURE_BLOCK_ENTITY_PRESENT");
     chunk.putInt("xPos",cx);chunk.putInt("zPos",cz);ListTag sections=chunk.getList("sections",Tag.TAG_COMPOUND);
     for(int sy=13;sy<=15;sy++){
      CompoundTag previous=new CompoundTag();int found=-1;
      for(int i=0;i<sections.size();i++)if(sections.getCompound(i).getByte("Y")==sy){previous=sections.getCompound(i);found=i;break;}
      CompoundTag replacement=section(previous,sy,cx,cz,singleCellSeal,ground,domain);if(found<0)sections.add(replacement);else sections.set(found,replacement);
     }
     chunk.put("sections",sections);chunk.remove("Heightmaps");chunk.putBoolean("isLightOn",false);
     try(var output=region.getChunkDataOutputStream(pos)){NbtIo.write(chunk,output);}region.flush();
    }
   }
   int seedReimuRemoved=TankSeedEntities.removeSeedReimu(world,3);
   Path entities=world.resolve("entities");
   try(RegionFile region=new RegionFile(entities.resolve("r.0.0.mca"),entities,true)){
    ChunkPos pos=domain?new ChunkPos(1,2):new ChunkPos(0,0);CompoundTag chunk;try(var input=region.getChunkDataInputStream(pos)){if(input==null){if(!domain)throw new IllegalStateException("ENTITY_CHUNK_MISSING");chunk=new CompoundTag();chunk.putInt("DataVersion",3465);chunk.putIntArray("Position",new int[]{pos.x,pos.z});chunk.put("Entities",new ListTag());}else chunk=NbtIo.read(input);}
    ListTag rows=chunk.getList("Entities",Tag.TAG_COMPOUND);for(Tag t:rows){CompoundTag e=(CompoundTag)t;if(e.hasUUID("UUID")&&e.getUUID("UUID").equals(SUBJECT))throw new IllegalStateException("DUPLICATE_SUBJECT");}
    CompoundTag ghast=new CompoundTag();ghast.putString("id","soutou_ghast:soutou_ghast");ghast.putUUID("UUID",SUBJECT);ghast.put("Pos",domain?vector(26,228,32):vector(9.5,ground?225:230,9.5));ghast.put("Motion",vector(0,0,0));ghast.put("Rotation",rotation(0,0));ghast.putFloat("Health",100);ghast.putBoolean("PersistenceRequired",true);ghast.putBoolean("NoAI",false);rows.add(ghast);chunk.put("Entities",rows);
    try(var output=region.getChunkDataOutputStream(pos)){NbtIo.write(chunk,output);}region.flush();
   }
   JsonObject scope=new JsonObject();scope.addProperty("scope",KneekuraDebugArenaController.SCOPE);scope.addProperty("dimension","minecraft:overworld");JsonArray blocks=new JsonArray();
   for(int x=domain?24:7;x<(domain?30:13);x++)for(int y=224;y<235;y++)for(int z=domain?26:6;z<(domain?34:13);z++){JsonArray b=new JsonArray();b.add(x);b.add(y);b.add(z);b.add(fixtureBlock(x,y,z,singleCellSeal,ground,domain));blocks.add(b);}scope.add("blocks",blocks);
   JsonObject poses=new JsonObject(),pose=new JsonObject();pose.addProperty("x",domain?26d:9.5d);pose.addProperty("y",domain?228d:ground?225d:230d);pose.addProperty("z",domain?32d:9.5d);pose.addProperty("yaw",0f);pose.addProperty("pitch",0f);for(String key:new String[]{"vx","vy","vz"})pose.addProperty(key,0d);poses.add(SUBJECT.toString(),pose);scope.add("subjectPoses",poses);
   JsonObject fixture=new JsonObject();fixture.add("scope",scope);fixture.addProperty("baselineHash",KneekuraDebugActionJournal.sha256(KneekuraDebugActionJournal.canonical(scope)));fixture.addProperty("subjectUuid",SUBJECT.toString());fixture.addProperty("playerUuid",player.getUUID("UUID").toString());
   fixture.addProperty("seedReimuRemoved",seedReimuRemoved);
   fixture.addProperty("variant",reliability?"DOMAIN_RELIABILITY":domain?"DOMAIN_COMBAT":ground?"GROUND_COMBAT":overhead?"OVERHEAD_BOMBING":standard?"STANDARD_FIREBALL":singleCellSeal?"SINGLE_CELL_SEAL":"OPAQUE_LOS_WALL");
   if(domain){fixture.addProperty("domainFloorY",224);fixture.addProperty("subjectInitialY",228);fixture.addProperty("entityChunkX",1);fixture.addProperty("entityChunkZ",2);}
   if(ground){fixture.addProperty("groundFloorY",224);fixture.addProperty("groundCeilingY",230);fixture.addProperty("subjectInitialY",225);}
   fixture.addProperty("offlinePlayerCameraPitch",overhead?-90:-18);
   fixture.addProperty("subjectHealth",100);fixture.addProperty("playerHealth",20);fixture.addProperty("mobGriefingDisabled",standard);
   fixture.addProperty("changes","PRIVATE_ONLY: offline52x24x52 bounded Tank shell, survival Player observation fixture, "+(reliability?"open Domain reliability baseline without ScopedOwner authority":singleCellSeal?"single-cell initial seal":"opaque LOS wall")+", active ghast; seed Reimu excluded from room chunks. "+(reliability?"Explicit reliability activation in private verification JAR; no natural-selection proof.":"Runtime window opening only through registered owner block actions."));
   Files.writeString(root.resolve("fixture.json"),fixture+"\n",StandardOpenOption.CREATE_NEW);
   JsonObject owner=JsonParser.parseString(Files.readString(world.resolve("kneekura-tank-owner.json"))).getAsJsonObject(),recipe=owner.getAsJsonObject("recipe");
   recipe.addProperty("preset","custom");JsonObject sizes=recipe.getAsJsonObject("dimensions");sizes.addProperty("width",SIZE);sizes.addProperty("height",HEIGHT);sizes.addProperty("depth",SIZE);owner.addProperty("recipeHash","sha256:"+KneekuraDebugActionJournal.sha256(KneekuraDebugActionJournal.canonical(recipe)));
   Files.writeString(world.resolve("kneekura-tank-owner.json"),owner+"\n");Files.writeString(root.resolve("tank-owner.json"),owner+"\n",StandardOpenOption.CREATE_NEW);
  }
 }
}

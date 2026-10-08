package com.genki.soutoughast.entity.ai.domain;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.*;

/** Explicit private native reliability sidecar; no mock Player, natural selector or camera control. */
@GameTestHolder("natural_domain_probe") @PrefixGameTestTemplate(false)
public final class DomainNativeProbe {
 @GameTest(template="empty",timeoutTicks=1200)
 public static void reliability(GameTestHelper helper)throws Exception{
  var probe=new Trial(helper);if(Set.of("crash","recover").contains(probe.scenario)){DomainCrashProbe.run(probe);return;}helper.onEachTick(probe::tick);
 }
 static final class Trial {
  final GameTestHelper helper;final ServerLevel level;final Path root,world;final String nonce,source,scenario,dimension;
  final DomainGeometry.Plan plan;final List<String> claims=new ArrayList<>();
  DomainJournalRepository repository;DomainWorldAdapter.Preflight preflight;DomainOverlay engine;DomainWorldAdapter adapter;
  int stage,fixtureCursor,verifyCursor,activeTicks,changedCells,checks;boolean finished;long started;
  Trial(GameTestHelper helper)throws Exception{
   this.helper=helper;level=helper.getLevel();root=Path.of(required("root")).toRealPath();nonce=required("nonce");source=required("source");scenario=required("scenario");
   if(!nonce.matches("[a-f0-9-]{36}")||!source.matches("[a-f0-9]{40}")||!Set.of("baseline","reopen-terminal","crash","recover").contains(scenario)||!root.getFileName().toString().startsWith("domain-probe-"))throw new IOException("DOMAIN_PROBE_AUTHORITY");
   if(!Files.readString(root.resolve("probe-owner.txt")).equals(nonce+"\n"+source+"\n"))throw new IOException("DOMAIN_PROBE_OWNER");
   Path expected=root.resolve("universe/domain-owned");safeDirectories(expected);
   world=level.getServer().getWorldPath(LevelResource.ROOT).toRealPath();if(!world.equals(expected))throw new IOException("DOMAIN_PROBE_WORLD");
   check(net.minecraft.SharedConstants.getCurrentVersion().getName().equals("1.20.1")&&net.minecraftforge.versions.forge.ForgeVersion.getVersion().equals("47.4.10")&&Runtime.version().feature()==17,"TANK_CORE_VERSION");
   check(level.players().isEmpty(),"NO_PLAYER_OR_MOCK_INPUT");dimension=level.dimension().location().toString();plan=DomainGeometry.plan(64,16,64,level.getMinBuildHeight(),level.getMaxBuildHeight());started=level.getGameTime();
   level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,level.getServer());
   // Fixture-only explicit preload: bounded16 planned chunks plus one kernel chunk, never inside adapter.
   for(int x=2;x<=5;x++)for(int z=2;z<=5;z++)level.getChunk(x,z);level.getChunk(7,4);
   if(scenario.equals("reopen-terminal")){
    check(Files.exists(root.resolve("baseline-native.json")),"BASELINE_RECEIPT_EXISTS");repository=DomainJournalRepository.open(world);
    check(repository.loadAll().size()==4&&repository.loadAll().stream().allMatch(j->j.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL),"PERSISTED_TERMINAL_FOUR_SLOTS");stage=5;
   }
  }
  void tick(){
   if(finished)return;
   try{
    if(stage==0){
     for(int end=Math.min(plan.cells().size(),fixtureCursor+128);fixtureCursor<end;fixtureCursor++){
      var tile=plan.cells().get(fixtureCursor);BlockState wanted=tile.role()==DomainGeometry.Role.FLOOR?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();
      check(level.getBlockState(pos(tile.cell())).isAir(),"FRESH_ABOVE_GROUND_FIXTURE");fixture(tile.cell(),wanted);
     }
     if(fixtureCursor==plan.cells().size()){repository=DomainJournalRepository.open(world);kernel();preflight=new DomainWorldAdapter.Preflight(level,plan);stage=1;}return;
    }
    if(stage==1){preflight.step(128);if(preflight.complete()){var changes=preflight.changes();changedCells=changes.size();check(changedCells>2000&&changedCells<=4096,"FULL_RADIUS20_OVERLAY_BUDGET");engine=start(0,changes);adapter=DomainWorldAdapter.forJournal(level,engine.journal());stage=2;}return;}
    if(stage==2){engine.place(adapter,64);if(engine.journal().phase()==DomainOverlay.Phase.ACTIVE){check(engine.journal().entries().stream().allMatch(e->e.status()==DomainOverlay.Status.APPLIED),"ALL_PLACEMENT_VERIFIED");stage=3;}return;}
    if(stage==3){if(++activeTicks>=400){engine.requestRestore("DIRECT_PROBE_EXPIRY");stage=4;}return;}
    if(stage==4){engine.restore(adapter,64);if(engine.terminal()){claims.add("FULL_RADIUS20_PLACEMENT_400TICK_DIRECT_EXPIRY_RESTORATION");stage=5;}return;}
    if(stage==5){
     for(int end=Math.min(plan.cells().size(),verifyCursor+128);verifyCursor<end;verifyCursor++){
      var tile=plan.cells().get(verifyCursor);BlockState expected=tile.role()==DomainGeometry.Role.FLOOR?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();check(level.getBlockState(pos(tile.cell())).equals(expected),"EXACT_NATIVE_FULL_PLAN_ORIGINAL");
     }
     if(verifyCursor==plan.cells().size()){
      for(var saved:repository.loadAll()){var nativeWorld=DomainWorldAdapter.forJournal(level,saved);for(var e:saved.entries())check(nativeWorld.state(e.cell()).equals(e.original()),"EXACT_NATIVE_LEDGER_ORIGINAL");}
      check(repository.loadAll().stream().allMatch(j->j.phase()==DomainOverlay.Phase.VERIFIED_TERMINAL),"ALL_SLOTS_TERMINAL");
      level.getServer().saveEverything(true,true,true);repository.close();repository=null;
      claims.add(scenario.equals("baseline")?"NATIVE_NORMAL_TERMINAL":"GENUINE_REOPEN_TERMINAL_JOURNAL_AND_BLOCKS");receipt("PASS",null);finished=true;helper.succeed();
     }
    }
   }catch(Exception|AssertionError error){finished=true;try{if(repository!=null)repository.close();}catch(IOException ignored){}try{receipt("FAIL",error);}catch(IOException ignored){}helper.fail("DOMAIN_PRIVATE_PROBE_FAILED: "+error.getClass().getSimpleName()+":"+error.getMessage());}
  }
  void kernel()throws Exception{
   var cell=new DomainGeometry.Cell(112,16,64);fixture(cell,Blocks.AIR.defaultBlockState());
   var first=start(0,List.of(change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState())));var nativeWorld=DomainWorldAdapter.forJournal(level,first.journal());first.place(nativeWorld,1);check(first.journal().phase()==DomainOverlay.Phase.ACTIVE,"NATIVE_SINGLE_ACTIVE");first.requestRestore("DIRECT_EXPIRY");first.restore(nativeWorld,1);check(first.terminal()&&level.getBlockState(pos(cell)).isAir(),"NATIVE_SINGLE_EXACT_RESTORE");
   var conflict=start(0,List.of(change(cell,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState())));nativeWorld=DomainWorldAdapter.forJournal(level,conflict.journal());conflict.place(nativeWorld,1);fixture(cell,Blocks.COBBLESTONE.defaultBlockState());conflict.requestRestore("THIRD_PARTY");conflict.restore(nativeWorld,1);check(!conflict.terminal()&&conflict.journal().entries().get(0).status()==DomainOverlay.Status.CONFLICT&&level.getBlockState(pos(cell)).is(Blocks.COBBLESTONE),"THIRD_PARTY_EDIT_PRESERVED");
   try{start(0,List.of());throw new AssertionError("unresolved reuse");}catch(IllegalArgumentException expected){checks++;}
   // Explicit owned-fixture reconciliation after first proving the third-party edit was preserved.
   fixture(cell,Blocks.AIR.defaultBlockState());conflict.restore(nativeWorld,1);check(conflict.terminal(),"EXPLICIT_FIXTURE_RECONCILIATION");claims.add("THIRD_PARTY_EDIT_PRESERVED_UNTIL_EXPLICIT_FIXTURE_RESET");
   actorCase(new DomainGeometry.Cell(114,16,64),false);actorCase(new DomainGeometry.Cell(116,16,64),true);restoreActor(new DomainGeometry.Cell(118,16,64));
   DomainOverlay[] occupied=new DomainOverlay[4];DomainWorldAdapter[] worlds=new DomainWorldAdapter[4];
   for(int i=0;i<4;i++){var c=new DomainGeometry.Cell(120+i,16,64);fixture(c,Blocks.AIR.defaultBlockState());occupied[i]=start(i,List.of(change(c,Blocks.AIR.defaultBlockState(),Blocks.BLACKSTONE.defaultBlockState())));worlds[i]=DomainWorldAdapter.forJournal(level,occupied[i].journal());occupied[i].place(worlds[i],1);}
   check(repository.loadAll().size()==4,"FOUR_NATIVE_ACTIVE_SLOTS");for(int i=0;i<4;i++){try{start(i,List.of());throw new AssertionError("exhausted reuse");}catch(IllegalArgumentException expected){checks++;}occupied[i].requestRestore("KERNEL_END");occupied[i].restore(worlds[i],1);check(occupied[i].terminal(),"NATIVE_SLOT_TERMINAL");}claims.add("FOUR_SLOT_EXHAUSTION_TERMINAL_REUSE");
   var far=new DomainGeometry.Cell(1_000_000,16,1_000_000);check(!level.hasChunkAt(pos(far)),"GENUINELY_UNLOADED_BEFORE");
   var ledger=new DomainOverlay.Journal(identity(3,999),DomainOverlay.Phase.PLACING,List.of(new DomainOverlay.Entry(far,DomainStateCodec.encode(Blocks.AIR.defaultBlockState()),DomainStateCodec.encode(Blocks.BLACKSTONE.defaultBlockState()),DomainOverlay.Status.RESERVED)),0,"PROBE_UNLOADED");
   var unloaded=DomainWorldAdapter.forJournal(level,ledger);check(!unloaded.loaded(far),"ADAPTER_UNLOADED");try{unloaded.state(far);throw new AssertionError("unloaded state lookup");}catch(IOException expected){checks++;}try{unloaded.set(far,ledger.entries().get(0).overlay());throw new AssertionError("unloaded mutation");}catch(IOException expected){checks++;}
   var farPreflight=new DomainWorldAdapter.Preflight(level,DomainGeometry.plan(far.x(),far.y(),far.z(),level.getMinBuildHeight(),level.getMaxBuildHeight()));try{farPreflight.step(1);throw new AssertionError("unloaded preflight");}catch(IOException expected){checks++;}check(!level.hasChunkAt(pos(far))&&!level.hasChunkAt(new BlockPos(far.x()-20,far.y()-1,far.z())),"NO_IMPLICIT_CHUNK_LOAD");claims.add("ACTUAL_UNLOADED_ADAPTER_AND_PREFLIGHT_NO_LOAD");
  }
  void actorCase(DomainGeometry.Cell cell,boolean removal)throws Exception{
   BlockState original=removal?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),overlay=removal?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState();fixture(new DomainGeometry.Cell(cell.x(),15,cell.z()),Blocks.STONE.defaultBlockState());fixture(cell,original);
   var actor=new ArmorStand(level,cell.x()+.5,cell.y()+(removal?1:0),cell.z()+.5);actor.setNoGravity(true);check(level.addFreshEntity(actor),"REAL_ARMOR_STAND_ADDED");
   try{var guarded=start(1,List.of(change(cell,original,overlay)));var nativeWorld=DomainWorldAdapter.forJournal(level,guarded.journal());guarded.place(nativeWorld,1);check(guarded.journal().phase()==DomainOverlay.Phase.RESTORING&&level.getBlockState(pos(cell)).equals(original),removal?"ACTOR_FOOT_REMOVAL_REJECTED":"ACTOR_SOLID_PLACEMENT_REJECTED");guarded.restore(nativeWorld,1);check(guarded.terminal(),"UNMUTATED_ACTOR_CASE_TERMINAL");}finally{actor.discard();}
   claims.add(removal?"ACTUAL_ACTOR_FOOT_REMOVAL_PROTECTED":"ACTUAL_ACTOR_SOLID_PLACEMENT_PROTECTED");
  }
  void restoreActor(DomainGeometry.Cell cell)throws Exception{
   fixture(cell,Blocks.AIR.defaultBlockState());var guarded=start(2,List.of(change(cell,Blocks.AIR.defaultBlockState(),Blocks.STONE.defaultBlockState())));var nativeWorld=DomainWorldAdapter.forJournal(level,guarded.journal());guarded.place(nativeWorld,1);
   var actor=new ArmorStand(level,cell.x()+.5,cell.y()+1,cell.z()+.5);actor.setNoGravity(true);check(level.addFreshEntity(actor),"REAL_RESTORE_ACTOR_ADDED");
   try{guarded.requestRestore("ACTOR_RESTORE");guarded.restore(nativeWorld,1);check(!guarded.terminal()&&level.getBlockState(pos(cell)).is(Blocks.STONE),"OCCUPIED_RESTORATION_DEFERRED");byte[] before=Files.readAllBytes(repository.slot(dimension,2).path());guarded.restore(nativeWorld,1);check(Arrays.equals(before,Files.readAllBytes(repository.slot(dimension,2).path())),"UNCHANGED_NATIVE_DEFERRAL_NO_REWRITE");}finally{actor.discard();}
   guarded.restore(nativeWorld,1);check(guarded.terminal()&&level.getBlockState(pos(cell)).isAir(),"RESTORATION_AFTER_ACTOR_EXIT");claims.add("ACTUAL_ACTOR_RESTORATION_DEFER_AND_EXIT");
  }
  DomainOverlay start(int index,List<DomainOverlay.Change> changes)throws Exception{var slot=repository.slot(dimension,index);var previous=slot.read();return DomainOverlay.start(identity(index,previous==null?1:previous.identity().generation()+1),changes,slot,previous);}
  DomainOverlay.Identity identity(int index,long generation){return new DomainOverlay.Identity(UUID.randomUUID(),UUID.fromString(nonce),dimension,index,generation);}
  DomainOverlay.Change change(DomainGeometry.Cell cell,BlockState original,BlockState overlay)throws IOException{return new DomainOverlay.Change(cell,DomainStateCodec.encode(original),DomainStateCodec.encode(overlay));}
  void fixture(DomainGeometry.Cell cell,BlockState state)throws IOException{if(cell.x()<44||cell.x()>127||cell.z()<44||cell.z()>84||cell.y()<15||cell.y()>28||!level.hasChunkAt(pos(cell)))throw new IOException("DOMAIN_PROBE_FIXTURE_BOUNDS");if(!level.getBlockState(pos(cell)).equals(state)&&!level.setBlock(pos(cell),state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))throw new IOException("DOMAIN_PROBE_FIXTURE_SET");}
  void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  void receipt(String verdict,Throwable error)throws IOException{
   JsonObject result=new JsonObject();result.addProperty("scope","DIRECT_NATIVE_DOMAIN_RELIABILITY_BASELINE");result.addProperty("verdict",verdict);result.addProperty("scenario",scenario);result.addProperty("nonce",nonce);result.addProperty("sourceRevision",source);result.addProperty("pid",ProcessHandle.current().pid());result.addProperty("world",world.toString());result.addProperty("checks",checks);result.addProperty("plannedCells",plan.cells().size());result.addProperty("changedCells",changedCells);result.addProperty("activeTicks",activeTicks);result.addProperty("gameTicks",level.getGameTime()-started);result.addProperty("errorClass",error==null?"":error.getClass().getSimpleName());var items=new com.google.gson.JsonArray();claims.forEach(items::add);result.add("claims",items);
   byte[] bytes=(DomainJournalCodec.canonical(result)+"\n").getBytes(StandardCharsets.UTF_8);try(var channel=FileChannel.open(root.resolve(scenario+"-native.json"),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
  }
 }
 private static String required(String field)throws IOException{String value=System.getProperty("naturalghast.domainProbe."+field);if(value==null)throw new IOException("DOMAIN_PROBE_REQUIRED_PROPERTY");return value;}
 private static void safeDirectories(Path path)throws IOException{for(Path current=path;current!=null;current=current.getParent())if(Files.isSymbolicLink(current)||!Files.isDirectory(current,LinkOption.NOFOLLOW_LINKS))throw new IOException("DOMAIN_PROBE_UNSAFE_DIRECTORY");}
 private static BlockPos pos(DomainGeometry.Cell cell){return new BlockPos(cell.x(),cell.y(),cell.z());}
}

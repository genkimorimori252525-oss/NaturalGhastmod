package com.genki.soutoughast.entity.ai.domain;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

/** Explicitly constructed prototype boundary; no event registration or automatic boss caller. */
public final class DomainNativeCoordinator implements AutoCloseable {
 private final DomainJournalRepository repository;private final DomainCoordinator coordinator;private boolean closed;
 private DomainNativeCoordinator(MinecraftServer server)throws IOException{
  if(!server.isSameThread())throw new IOException("DOMAIN_COORDINATOR_SERVER_THREAD");
  repository=DomainJournalRepository.open(server.getWorldPath(LevelResource.ROOT).toRealPath());
  try{
   coordinator=new DomainCoordinator(new DomainCoordinator.Storage(){
    public List<DomainOverlay.Journal> load()throws IOException{return repository.loadAll();}
    public DomainOverlay.Journal read(String dimension,int slot)throws IOException{return repository.slot(dimension,slot).read();}
    public DomainOverlay.Store store(String dimension,int slot)throws IOException{return repository.slot(dimension,slot);}
   },new DomainCoordinator.Worlds(){
    private ServerLevel level(String dimension){return server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(dimension)));}
    public DomainOverlay.World world(DomainOverlay.Journal journal)throws IOException{
     ServerLevel level=level(journal.identity().dimension());if(level==null)return null;
     return DomainWorldAdapter.forJournal(level,journal);
    }
    public DomainOverlay.Durability durability(DomainOverlay.Journal journal)throws IOException{
     ServerLevel level=level(journal.identity().dimension());return level==null?null:DomainNativePersistence.barrier(level,journal);
    }
    public boolean ownerPresent(DomainOverlay.Identity identity)throws IOException{
     if(!server.isSameThread())throw new IOException("DOMAIN_COORDINATOR_SERVER_THREAD");ServerLevel level=level(identity.dimension());if(level==null)return false;
     var entity=level.getEntity(identity.owner());return entity instanceof LivingEntity actor&&actor.isAlive()&&!actor.isRemoved();
    }
   });
  }catch(IOException|RuntimeException error){repository.close();throw error;}
 }
 public static DomainNativeCoordinator open(MinecraftServer server)throws IOException{return new DomainNativeCoordinator(Objects.requireNonNull(server));}
 public DomainCoordinator coordinator(){if(closed)throw new IllegalStateException("DOMAIN_COORDINATOR_CLOSED");return coordinator;}
 @Override public void close()throws IOException{if(closed)return;closed=true;try{coordinator.shutdown();}finally{repository.close();}}
}

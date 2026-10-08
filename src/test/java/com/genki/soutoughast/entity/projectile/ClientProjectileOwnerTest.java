package com.genki.soutoughast.entity.projectile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/** Authoritative client owner state; native world delivery is verified separately in Tank. */
public final class ClientProjectileOwnerTest {
    private record Owner(int id,boolean removed) {}
    private static int checks;
    public static void main(String[] args){
        var state=new ClientProjectileOwner<Owner>();Map<Integer,Owner> entities=new HashMap<>();
        var calls=new AtomicInteger();IntFunction<Owner> lookup=id->{calls.incrementAndGet();return entities.get(id);};
        check(state.resolve(1,lookup,e->!e.removed())==null&&calls.get()==0,"zero never looks up");
        state.update(10);check(state.resolve(1,lookup,e->!e.removed())==null&&calls.get()==1,"absent owner stays pending");
        check(state.resolve(1,lookup,e->!e.removed())==null&&calls.get()==1,"repeated reads have one lookup per tick");
        var boss=new Owner(10,false);entities.put(10,boss);
        check(state.resolve(1,lookup,e->!e.removed())==null,"arrival in same tick waits bounded retry");
        check(state.resolve(2,lookup,e->!e.removed())==boss&&calls.get()==2,"next tick resolves retained authoritative ID");
        state.update(10);
        check(state.resolve(2,lookup,e->!e.removed())==boss&&calls.get()==2,"duplicate spawn/metadata owner retains cache within same tick");
        check(state.resolve(3,lookup,e->!e.removed())==boss&&calls.get()==2,"resolved cache does not poll");
        var player=new Owner(20,false);entities.put(20,player);state.update(20);
        check(state.resolve(3,lookup,e->!e.removed())==player,"replacement resolves actual Player");
        state.update(30);check(state.resolve(3,lookup,e->!e.removed())==null&&calls.get()==3,"replacement immediately drops Player without second lookup");
        check(state.resolve(4,lookup,e->!e.removed())==null&&calls.get()==4,"absent replacement never returns Player");
        var replacement=new Owner(30,false);entities.put(30,replacement);
        check(state.resolve(5,lookup,e->!e.removed())==replacement,"replacement resolves when admitted");
        state.update(0);check(state.resolve(6,lookup,e->!e.removed())==null&&calls.get()==5,"explicit zero clears cached owner");
        state.update(40);state.clear();entities.put(40,new Owner(40,false));
        check(state.resolve(7,lookup,e->!e.removed())==null&&calls.get()==5,"removal cancels pending owner");
        state.update(50);entities.put(50,new Owner(50,true));
        check(state.resolve(8,lookup,e->!e.removed())==null&&calls.get()==6,"removed lookup result rejected");
        entities.put(50,new Owner(50,false));check(state.resolve(9,lookup,e->!e.removed())!=null,"retry after rejected result");
        state.clear();check(state.id()==0&&state.resolve(10,lookup,e->!e.removed())==null,"clear cancels resolved state");
        var cached=new ClientProjectileOwner<Owner>();cached.update(10);
        check(cached.resolve(11,lookup,e->!e.removed())==boss,"admit cache for removal check");
        check(cached.resolve(12,lookup,e->false)==null,"removed cache immediately becomes unavailable");
        entities.put(10,new Owner(10,false));
        check(cached.resolve(13,lookup,e->!e.removed())==entities.get(10),"same ID replacement resolves distinct live instance");
        System.out.println("PASS: "+checks+" client owner state checks (native delivery separate)");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}

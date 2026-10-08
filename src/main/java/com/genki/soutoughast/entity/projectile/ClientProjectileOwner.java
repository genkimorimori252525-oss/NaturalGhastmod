package com.genki.soutoughast.entity.projectile;

import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Client-only authoritative ID; unresolved owners retry once per world tick. */
final class ClientProjectileOwner<T> {
    private int id;
    private long lastLookupTick=Long.MIN_VALUE;
    private T owner;

    int id(){return id;}
    void update(int id){if(this.id!=id)owner=null;this.id=id;}
    void clear(){id=0;owner=null;}
    T resolve(long tick,IntFunction<T> lookup,Predicate<T> live){
        if(owner!=null&&!live.test(owner))owner=null;
        if(id==0)return null;
        if(owner==null&&lastLookupTick!=tick){
            lastLookupTick=tick;owner=lookup.apply(id);
            if(owner!=null&&!live.test(owner))owner=null;
        }
        return owner;
    }
}

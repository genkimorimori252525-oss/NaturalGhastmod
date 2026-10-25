package com.genki.soutoughast.danmaku.core;

import java.util.HashMap;
import java.util.UUID;

/** One gate per server level, shared across every owner swarm. No health manipulation. */
public final class DanmakuHurtGate {
    private final HashMap<UUID,Long> previous=new HashMap<>();
    private long lastTick=-1;
    public boolean allow(UUID target,long tick){
        if(target==null||tick<0)throw new IllegalArgumentException("DANMAKU_HURT_CONTEXT");
        if(tick<lastTick)previous.clear();lastTick=tick;
        previous.entrySet().removeIf(e->tick-e.getValue()>600);
        Long before=previous.get(target);if(before!=null&&tick-before<10)return false;
        if(before==null&&previous.size()>=256)return false;
        previous.put(target,tick);return true;
    }
    public int retainedTargets(){return previous.size();}
}

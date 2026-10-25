package com.genki.soutoughast.danmaku.core;

import java.util.function.Predicate;

/** Abort the query itself on the first over-budget visit, before collecting/filtering the population. */
public final class DanmakuCandidateBudget<T> implements Predicate<T> {
    public static final class Exceeded extends RuntimeException {public Exceeded(){super("DANMAKU_ENTITY_QUERY_BUDGET");}}
    private final int limit;private final Predicate<T> accepted;private int visited;
    public DanmakuCandidateBudget(int limit,Predicate<T> accepted){if(limit<1||limit>128||accepted==null)throw new IllegalArgumentException("DANMAKU_CANDIDATE_LIMIT");this.limit=limit;this.accepted=accepted;}
    @Override public boolean test(T value){if(++visited>limit)throw new Exceeded();return accepted.test(value);}
    public int visited(){return visited;}
}

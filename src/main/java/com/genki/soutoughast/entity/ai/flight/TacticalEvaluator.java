package com.genki.soutoughast.entity.ai.flight;

/** Context first, soft observed range/velocity preference second, bounded variation last. */
public final class TacticalEvaluator {
    public enum Action { DRIFT, FALSE_APPROACH, FALSE_RETREAT, LATERAL_FAKE, VERTICAL_FAKE, PASS_BY_FAKE, DOUBLE_FAKE, ABORT_FAKE }
    public Action choose(MobilityContext.Kind context,CombatAnchor.Range range,FlightVector observedVelocity,
                         double variation,TacticalMemory memory){
        if(!Double.isFinite(variation)||variation<0||variation>=1)throw new IllegalArgumentException("Invalid tactical variation");
        if(context==MobilityContext.Kind.GROUND_FORCED||variation<.75)return Action.DRIFT;
        Action best=Action.DRIFT;double score=0;
        for(Action action:Action.values()){
            if(action==Action.DRIFT||context==MobilityContext.Kind.CONFINED&&action!=Action.LATERAL_FAKE)continue;
            if(action==Action.PASS_BY_FAKE&&(context!=MobilityContext.Kind.OPEN_AIR||range!=CombatAnchor.Range.TOO_CLOSE||variation<.88||variation>=.91))continue;
            if(action==Action.DOUBLE_FAKE&&(variation<.94||variation>=.955))continue;
            if(action==Action.ABORT_FAKE&&(variation<.91||variation>=.93))continue;
            double base=switch(action){
                case FALSE_APPROACH -> range==CombatAnchor.Range.TOO_FAR?1.7:.8;
                case FALSE_RETREAT -> range==CombatAnchor.Range.TOO_CLOSE?1.7:.8;
                case LATERAL_FAKE -> 1.05+Math.min(.2,Math.hypot(observedVelocity.x(),observedVelocity.z()));
                case VERTICAL_FAKE -> context==MobilityContext.Kind.OPEN_AIR?.9:.6;
                case PASS_BY_FAKE -> 1.9;
                case DOUBLE_FAKE, ABORT_FAKE -> 1.35;
                default -> 0;
            };
            double candidate=base*memory.weight(action)+.12*Math.sin(variation*19+action.ordinal()*2.7);
            if(candidate>score){score=candidate;best=action;}
        }
        return best;
    }
}

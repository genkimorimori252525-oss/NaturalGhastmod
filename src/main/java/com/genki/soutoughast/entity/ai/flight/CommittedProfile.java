package com.genki.soutoughast.entity.ai.flight;

/** Attack recipe/observed endpoint commit before launch; the world path commits at physical spawn. */
public record CommittedProfile(CommittedTrajectory.Kind kind,CommittedTrajectory.Strength strength,int side,double lobHeight,FlightVector endpoint) {
    public CommittedProfile {
        if(kind==null||endpoint==null)throw new IllegalArgumentException("COMMITTED_RECIPE_REQUIRED");
        boolean valid=switch(kind){
            case BURST, BOMB -> strength==null&&side==0&&lobHeight==0;
            case CURVE -> strength!=null&&(side==1||side==-1)&&lobHeight==0;
            case LOB -> strength==null&&side==0&&lobHeight>=4&&lobHeight<=12;
        };
        if(!valid)throw new IllegalArgumentException("INVALID_COMMITTED_RECIPE");
    }
    public CommittedTrajectory pathFrom(FlightVector muzzle){return switch(kind){
        case BURST -> CommittedTrajectory.burst(muzzle,endpoint);
        case CURVE -> CommittedTrajectory.curve(muzzle,endpoint,strength,side);
        case LOB -> LobTerminalSubdivision.refine(CommittedTrajectory.lob(muzzle,endpoint,lobHeight));
        case BOMB -> CommittedTrajectory.bomb(muzzle,endpoint);
    };}
}

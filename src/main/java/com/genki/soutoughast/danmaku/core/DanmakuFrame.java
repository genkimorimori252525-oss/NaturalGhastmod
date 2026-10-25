package com.genki.soutoughast.danmaku.core;

/** Parallel-transported player-readable frame. Birth frames never follow later target movement. */
public record DanmakuFrame(Vec origin,Vec right,Vec up,Vec forward) {
    public record Vec(double x,double y,double z) {
        public static final Vec ZERO=new Vec(0,0,0),UP=new Vec(0,1,0);
        public Vec {if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("DANMAKU_NONFINITE_VECTOR");}
        public Vec add(Vec v){return new Vec(x+v.x,y+v.y,z+v.z);}
        public Vec subtract(Vec v){return new Vec(x-v.x,y-v.y,z-v.z);}
        public Vec scale(double n){return new Vec(x*n,y*n,z*n);}
        public double dot(Vec v){return x*v.x+y*v.y+z*v.z;}
        public Vec cross(Vec v){return new Vec(y*v.z-z*v.y,z*v.x-x*v.z,x*v.y-y*v.x);}
        public double length(){return Math.sqrt(dot(this));}
        public Vec unit(){double n=length();if(n<1e-9)throw new IllegalArgumentException("DANMAKU_ZERO_AXIS");return scale(1/n);}
    }
    public DanmakuFrame {
        if(origin==null||right==null||up==null||forward==null||Math.abs(right.length()-1)>1e-6||Math.abs(up.length()-1)>1e-6
                ||Math.abs(forward.length()-1)>1e-6||Math.abs(right.dot(up))>1e-6||Math.abs(right.dot(forward))>1e-6
                ||Math.abs(up.dot(forward))>1e-6||forward.cross(right).subtract(up).length()>1e-6)throw new IllegalArgumentException("DANMAKU_INVALID_FRAME");
    }
    public static DanmakuFrame toward(Vec mouth,Vec observedEye,DanmakuFrame previous){
        Vec forward=observedEye.subtract(mouth).unit(),right=null;
        if(previous!=null){var projected=previous.right.subtract(forward.scale(previous.right.dot(forward)));if(projected.length()>1e-4)right=projected.unit();}
        if(right==null){var horizontal=Vec.UP.cross(forward);right=horizontal.length()>1e-4?horizontal.unit():new Vec(1,0,0);}
        Vec up=forward.cross(right).unit();return new DanmakuFrame(mouth,right,up,forward);
    }
    public Vec velocity(GrandDanmakuScore.SpawnSpec spec){return right.scale(spec.vx()).add(up.scale(spec.vy())).add(forward.scale(spec.vz()));}
    public Vec birth(GrandDanmakuScore.SpawnSpec spec){return origin.add(right.scale(spec.x())).add(up.scale(spec.y()-GrandDanmakuScore.EMITTER_Y)).add(forward.scale(spec.z()));}
}

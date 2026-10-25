package com.genki.soutoughast.entity.ai.flight;

/** Shared director remains exclusive and finite after adding the new major action. */
public final class GrandDanmakuAdmissionTest {
    private static int checks;
    private static void check(boolean pass,String reason){checks++;if(!pass)throw new AssertionError(reason);}
    public static void main(String[] args){
        var director=new MajorActionDirector();check(!director.shouldBeginGrandDanmaku(true,false,28,.999),"initial major quiet preserved");
        for(int i=0;i<240;i++)director.tick(true);
        check(!director.shouldBeginGrandDanmaku(false,false,28,.999),"no observed clearance no Grand");check(!director.shouldBeginGrandDanmaku(true,true,28,.999),"ordinary offense not interrupted");
        check(!director.shouldBeginGrandDanmaku(true,false,8,.999),"close range not admitted");check(!director.shouldBeginGrandDanmaku(true,false,80,.999),"far range not admitted");
        int selected=0;for(int i=0;i<1000;i++)if(director.shouldBeginGrandDanmaku(true,false,28,(i+.5)/1000))selected++;check(selected==15,"rare deliberate admission, no per-tick forced selection");
        check(director.shouldBeginGrandDanmaku(true,false,28,.999),"eligible major may begin");director.began();
        check(!director.shouldBeginDomain(true,false,1),"Grand excludes Domain");check(!director.shouldBegin(MobilityContext.Kind.OPEN_AIR,true,false,28,.999),"Grand excludes Overhead");
        check(!director.shouldBeginRelocation(MobilityContext.Kind.OPEN_AIR,true,false,12,.999),"Grand excludes relocation");
        director.finished();for(int i=0;i<600;i++)director.tick(true);check(!director.shouldBeginGrandDanmaku(true,false,28,.999),"shared recent-major spacing retained after recovery");
        for(int i=0;i<600;i++)director.tick(true);check(director.shouldBeginGrandDanmaku(true,false,28,.999),"normal eligible work resumes after finite spacing");
        System.out.println("Grand Danmaku shared admission checks: "+checks);
    }
}

package com.genki.soutoughast.danmaku;

import java.nio.charset.StandardCharsets;

/** Effective client resources must match the pinned implementation, including the complete shader closure. */
public final class DanmakuResourceIntegrity {
    private DanmakuResourceIntegrity(){}
    public static boolean matches(byte[] expected,byte[] actual,boolean shader){
        if(expected==null||actual==null||expected.length>2097152||actual.length>2097152)return false;
        if(shader){String source=new String(expected,StandardCharsets.UTF_8);if(source.contains("#moj_import")||source.contains("#include"))return false;}
        return GrandDanmakuAssets.hash(expected).equals(GrandDanmakuAssets.hash(actual));
    }
}

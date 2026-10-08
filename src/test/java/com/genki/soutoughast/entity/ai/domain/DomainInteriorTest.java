package com.genki.soutoughast.entity.ai.domain;
public final class DomainInteriorTest {
 public static void main(String[] args){
  var p=DomainGeometry.plan(0,16,0,-64,320);int checks=0;
  if(!DomainInterior.contains(p,-2,16,-2,2,20,2))throw new AssertionError("full4x4 fits");checks++;
  if(DomainInterior.contains(p,16,16,7,20,20,11))throw new AssertionError("center inside is insufficient for4x4 shell corner");checks++;
  if(DomainInterior.contains(p,-2,16,-2,2,27.001,2))throw new AssertionError("roof intersects actual body");checks++;
  if(DomainInterior.contains(p,-2,15.9,-2,2,20,2))throw new AssertionError("body below support plane");checks++;
  if(DomainInterior.contains(p,Double.NaN,16,-2,2,20,2))throw new AssertionError("nonfinite");checks++;
  if(!DomainInterior.contains(p,-2,16,-2,14,20,2))throw new AssertionError("whole swept grounded footprint");checks++;
  if(DomainInterior.contains(p,-2,16,-2,22,20,2))throw new AssertionError("swept endpoint outside");checks++;
  System.out.println("PASS: "+checks+" complete Domain interior/roof/sweep checks");
 }
}

package com.genki.soutoughast.tank;
import java.io.IOException;
public final class ObservationFailureBoundaryTest {
 public static void main(String[] args){
  var guard=new ObservationFailureBoundary();int[] gameplay={0},telemetry={0};
  if(guard.run(()->{throw new IOException("disk denied");}))throw new AssertionError("I/O failure accepted");
  gameplay[0]++; // The native collision caller continues after observer failure.
  if(!guard.failed()||!guard.failure().contains("IOException")||gameplay[0]!=1)throw new AssertionError("Failure escaped or was lost");
  if(guard.run(()->telemetry[0]++)||telemetry[0]!=0)throw new AssertionError("Failed observer continued");
  var success=new ObservationFailureBoundary();if(!success.run(()->telemetry[0]++)||success.failed()||telemetry[0]!=1)throw new AssertionError("Valid observation suppressed");
  var bounded=new ObservationFailureBoundary();bounded.run(()->{throw new IllegalStateException("x".repeat(5000));});if(bounded.failure().length()>1024)throw new AssertionError("Failure record unbounded");
  System.out.println("PASS: 5 observer failure containment checks (not native damage proof)");
 }
}

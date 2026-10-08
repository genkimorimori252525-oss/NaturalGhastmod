package com.genki.soutoughast.tank;
import com.genki.soutoughast.entity.ai.CommittedPathClearance;
import com.genki.soutoughast.entity.ai.flight.*;
/** Actual mapped terminal-footprint contract, without opening any saved world. */
public final class ProfileReloadGeometryTest {
 public static void main(String[] args){
  var original=CommittedTrajectory.lob(new FlightVector(3,236,45),new FlightVector(49,223.75,45),6);
  var corrected=CommittedTrajectory.lob(new FlightVector(3.5,236,45.5),new FlightVector(49.5,223.75,45.5),6);
  int end=original.points().size()-1,next=corrected.points().size()-1;
  var rejected=CommittedPathClearance.terminalFootprint(CommittedPathClearance.sweptBox(original.points().get(end-1),original.points().get(end)),223);
  var accepted=CommittedPathClearance.terminalFootprint(CommittedPathClearance.sweptBox(corrected.points().get(next-1),corrected.points().get(next)),223);
  if(!rejected.isEmpty()||accepted.isEmpty()||accepted.size()>4)throw new AssertionError("BOUNDED_TERMINAL_FIXTURE_REQUIRED");
  System.out.println("PASS: reconstructed original footprint rejected; centered fixture terminal cells="+accepted.size());
 }
}

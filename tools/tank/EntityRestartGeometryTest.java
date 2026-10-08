package com.genki.soutoughast.tank;
import com.genki.soutoughast.entity.ai.flight.*;
import com.genki.soutoughast.entity.projectile.CommittedTrajectoryCodec;
public final class EntityRestartGeometryTest {
 public static void main(String[] args){
  var bomb=CommittedTrajectory.bomb(new FlightVector(45.5,246,35.5),new FlightVector(45.5,223.75,35.5));
  var lob=CommittedTrajectory.lob(new FlightVector(3.5,236,45.5),new FlightVector(49.5,223.75,45.5),6);int checks=0;
  for(var path:new CommittedTrajectory[]{bomb,lob}){
   if(path.points().size()-1-9<4)throw new AssertionError("INSUFFICIENT_POST_STOP_PATH");
   for(var p:path.points())if(p.x()<.5||p.x()>51.5||p.z()<.5||p.z()>51.5||p.y()>247||p.y()<223.75)throw new AssertionError("PRIVATE_CORRIDOR_BOUND");
   var flight=new CommittedTrajectory.Flight(path);for(int i=0;i<9;i++)if(flight.next()==null)throw new AssertionError("NATIVE_TICK_BUDGET");
   if(flight.index()!=9||!CommittedTrajectoryCodec.read(CommittedTrajectoryCodec.write(flight)).path().equals(path))throw new AssertionError("MAPPED_CODEC");checks++;
  }
  System.out.println("Restart profile geometry PASS "+checks+" BOMBremaining="+(bomb.points().size()-1-9));
 }
}

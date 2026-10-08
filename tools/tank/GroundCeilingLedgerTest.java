package com.genki.soutoughast.tank;
import java.util.Arrays;
public final class GroundCeilingLedgerTest {
 static class World implements GroundCeilingLedger.Cells {
  final GroundCeilingLedger.Cell[] cells=new GroundCeilingLedger.Cell[2704];int writes,fail=-1,blocked=-1;boolean race;
  World(){Arrays.fill(cells,GroundCeilingLedger.Cell.STONE);}
  public GroundCeilingLedger.Cell read(int i){if(race&&writes==0&&i==0){race=false;cells[0]=GroundCeilingLedger.Cell.OTHER;}return cells[i];}
  public boolean actorFree(int i){return i!=blocked;}
  public void write(int i,boolean stone){cells[i]=stone?GroundCeilingLedger.Cell.STONE:GroundCeilingLedger.Cell.AIR;if(writes++==fail)throw new IllegalStateException("INJECTED_AFTER_WRITE");}
 }
 public static void main(String[] args){int checks=0;
  for(boolean closing:new boolean[]{false,true})for(int fail:new int[]{0,1352}){
   var ledger=new GroundCeilingLedger();var world=new World();if(closing)ledger.transition(world,false);world.writes=0;world.fail=fail;
   try{ledger.transition(world,closing);throw new AssertionError("NO_INJECTED_FAILURE");}catch(IllegalStateException expected){}
   world.fail=-1;var cleanup=ledger.cleanup(world);
   if(cleanup.conflicts()!=0||cleanup.actorBlocked()!=0||Arrays.stream(world.cells).anyMatch(x->x!=GroundCeilingLedger.Cell.STONE))throw new AssertionError("PARTIAL_CLEANUP");checks++;
  }
  var ledger=new GroundCeilingLedger();var world=new World();ledger.transition(world,false);world.cells[20]=GroundCeilingLedger.Cell.OTHER;world.blocked=21;
  var cleanup=ledger.cleanup(world);if(cleanup.conflicts()!=1||cleanup.actorBlocked()!=1||world.cells[20]!=GroundCeilingLedger.Cell.OTHER||world.cells[21]!=GroundCeilingLedger.Cell.AIR||world.cells[22]!=GroundCeilingLedger.Cell.STONE)throw new AssertionError("CONFLICT_ACTOR_PRESERVATION");checks++;
  ledger=new GroundCeilingLedger();world=new World();world.cells[10]=GroundCeilingLedger.Cell.AIR;cleanup=ledger.cleanup(world);
  if(cleanup.conflicts()!=1||world.cells[10]!=GroundCeilingLedger.Cell.AIR)throw new AssertionError("UNOWNED_AIR");checks++;
  ledger=new GroundCeilingLedger();world=new World(){int reads;@Override public GroundCeilingLedger.Cell read(int i){if(++reads==2705)cells[i]=GroundCeilingLedger.Cell.OTHER;return cells[i];}};
  try{ledger.transition(world,false);throw new AssertionError("NO_FRESH_GUARD");}catch(IllegalStateException expected){}if(world.writes!=0)throw new AssertionError("STALE_PREDECESSOR_WRITE");checks++;
  System.out.println("Ground ceiling ownership PASS "+checks);
 }
}

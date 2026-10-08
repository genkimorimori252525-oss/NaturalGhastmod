package com.genki.soutoughast.tank;
import java.util.BitSet;

/** Private finite fixture ownership, not persistent Domain restoration authority. */
final class GroundCeilingLedger {
 static final int SIZE=2704;
 enum Cell { STONE, AIR, OTHER, UNLOADED }
 interface Cells { Cell read(int index);boolean actorFree(int index);void write(int index,boolean stone); }
 record Cleanup(int restored,int actorBlocked,int conflicts){}
 private final BitSet owned=new BitSet(SIZE);
 void transition(Cells cells,boolean restore){
  Cell expected=restore?Cell.AIR:Cell.STONE;
  for(int i=0;i<SIZE;i++)require(cells.read(i)==expected&&(!restore||owned.get(i)&&cells.actorFree(i)),"BATCH_PREVALIDATION");
  for(int i=0;i<SIZE;i++){
   require(cells.read(i)==expected&&(!restore||owned.get(i)&&cells.actorFree(i)),"FRESH_PREDECESSOR");
   if(!restore)owned.set(i); // Intent survives an exception after native mutation.
   cells.write(i,restore);require(cells.read(i)==(restore?Cell.STONE:Cell.AIR),"WRITE_READBACK");
   if(restore)owned.clear(i);
  }
 }
 Cleanup cleanup(Cells cells){
  int restored=0,blocked=0,conflicts=0;
  for(int i=0;i<SIZE;i++){
   try{
    Cell actual=cells.read(i);if(actual==Cell.STONE){owned.clear(i);continue;}
    if(actual!=Cell.AIR||!owned.get(i)){conflicts++;continue;}
    if(!cells.actorFree(i)){blocked++;continue;}
    if(cells.read(i)!=Cell.AIR){conflicts++;continue;}
    cells.write(i,true);if(cells.read(i)==Cell.STONE){owned.clear(i);restored++;}else conflicts++;
   }catch(RuntimeException failure){conflicts++;}
  }
  return new Cleanup(restored,blocked,conflicts);
 }
 private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
}

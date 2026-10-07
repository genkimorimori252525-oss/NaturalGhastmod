package com.genki.soutoughast.tank;

/** Private observer failures stop observation; they never escape into a native gameplay hook. */
public final class ObservationFailureBoundary {
 @FunctionalInterface public interface Operation {void run()throws Exception;}
 private volatile String failure;
 public boolean failed(){return failure!=null;}
 public String failure(){return failure;}
 public boolean run(Operation operation){
  if(failed())return false;
  try{operation.run();return true;}catch(Exception error){
   synchronized(this){if(failure==null){String text=error.getClass().getSimpleName()+":"+error.getMessage();failure=text.substring(0,Math.min(1024,text.length()));}}
   return false;
  }
 }
}

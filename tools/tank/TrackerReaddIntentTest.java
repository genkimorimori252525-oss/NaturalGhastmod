package com.genki.soutoughast.tank;

import java.io.IOException;

/** Faults at the publication/invocation boundary; uses the same private native helper. */
public final class TrackerReaddIntentTest {
 public static void main(String[] args)throws Exception{
  int[] calls={0};var normal=new TrackerReaddIntent();normal.execute(()->{},()->calls[0]++);require(calls[0]==1&&!normal.canCleanup(),"SUCCESS_ONCE");
  var publication=new TrackerReaddIntent();expectFailure(()->publication.execute(()->{throw new IOException("PUBLICATION_FAULT");},()->calls[0]++));
  require(publication.canCleanup()&&calls[0]==1,"PRE_CALL_FAILURE_PERMITS_CLEANUP");publication.execute(()->{},()->calls[0]++);require(calls[0]==2&&!publication.canCleanup(),"ONE_CLEANUP_ADD");
  var invocation=new TrackerReaddIntent();expectFailure(()->invocation.execute(()->{},()->{throw new IOException("INVOCATION_FAULT");}));require(!invocation.canCleanup(),"INVOKED_FAILURE_UNCERTAIN");expectFailure(()->invocation.execute(()->{},()->calls[0]++));require(calls[0]==2,"NO_UNCERTAIN_RETRY");
  var after=new TrackerReaddIntent();expectFailure(()->after.execute(()->{},()->{calls[0]++;throw new IOException("AFTER_ADD_FAULT");}));require(!after.canCleanup()&&calls[0]==3,"AFTER_ADD_NO_RETRY");expectFailure(()->after.execute(()->{},()->calls[0]++));require(calls[0]==3,"NO_DUPLICATE_ADD");
  System.out.println("PASS tracker intent: normal/pre-publication/during-invocation/after-add; cleanup exactly once when authorized");
 }
 private static void expectFailure(TrackerReaddIntent.Action action)throws Exception{try{action.run();throw new AssertionError("MISSING_FAILURE");}catch(IOException expected){}}
 private static void require(boolean valid,String label){if(!valid)throw new AssertionError(label);}
}

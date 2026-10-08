package com.genki.soutoughast.tank;

import java.io.IOException;

/** Private one-call intent: preparation can fail before the native invocation begins. */
final class TrackerReaddIntent {
 @FunctionalInterface interface Action {void run()throws IOException;}
 private boolean begun;
 boolean canCleanup(){return !begun;}
 void execute(Action prepare,Action invoke)throws IOException{
  if(begun)throw new IOException("TRACKER_ADD_ALREADY_BEGUN");
  prepare.run();
  begun=true;
  invoke.run();
 }
}

package edu.kit.joana.ui.ifc.wala.cli;

import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput;
import edu.kit.joana.util.NullPrintStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LimitedIFCConsoleOutput implements IFCConsoleOutput {

  private boolean disableStandardOutput;

  public LimitedIFCConsoleOutput() {
    this(false);
  }

  public LimitedIFCConsoleOutput(boolean disableStandardOutput) {
    this.disableStandardOutput = disableStandardOutput;
  }

  @Override public void log(String logMessage) {
    //System.out.print(logMessage);
  }

  @Override public void logln(String logMessage) {
    //System.out.println(logMessage);
  }

  @Override public void info(String infoMessage) {
    //System.out.println(infoMessage);
  }

  @Override public void error(String errorMessage) {
    System.err.println(errorMessage);
  }

  @Override public Answer question(String questionMessage) {
    System.out.println(questionMessage);
    try {
      return new BufferedReader(new InputStreamReader(System.in)).readLine().contains("y") ? Answer.YES : Answer.NO;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @Override public PrintStream getPrintStream() {
    return disableStandardOutput ? new NullPrintStream() : System.out;
  }

  @Override public PrintStream getDebugPrintStream() {
    if (Logger.getGlobal().isLoggable(Level.INFO)) {
      return getPrintStream();
    }
    return new NullPrintStream();
  }
}

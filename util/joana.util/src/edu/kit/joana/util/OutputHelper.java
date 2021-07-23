package edu.kit.joana.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputHelper {

  public static String captureOutput(Runnable runnable) {
    PrintStream oldOut = System.out;
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    System.setOut(new PrintStream(os));
    try {
      runnable.run();
    } finally {
      System.setOut(oldOut);
    }
    return os.toString();
  }

}

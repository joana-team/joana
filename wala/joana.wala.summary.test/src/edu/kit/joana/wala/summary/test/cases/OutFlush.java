package edu.kit.joana.wala.summary.test.cases;

import java.io.*;

public class OutFlush {

  static OutputStream out;

  static class aSystem {
    public static java.io.PrintStream out;
  }

  public static void main(String[] args) {

    System.out.flush();
  }
}

package edu.kit.joana.wala.summary.test.cases;

public class StaticField {

  public static void main(String args[])
  {
    new StaticField();
  }

  public StaticField(){
    init();
  }

  /* For generating thread ID */
  private static long threadSeqNumber;

  private void init() {

    /* Set thread ID */
    threadSeqNumber = 0;
  }
}
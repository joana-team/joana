package edu.kit.joana.wala.summary.test.cases;

import JLex.Main;

public class UnconnectedGraph {

  public static void main(String[] arg) {
    try {
      new Main();
    } catch (Error e) {
      b();
    }
  }

  static int a(int a){
    return a;
  }

  static void b(){
    a(2);
  }
}

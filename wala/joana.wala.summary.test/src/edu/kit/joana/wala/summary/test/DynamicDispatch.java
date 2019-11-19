package edu.kit.joana.wala.summary.test;

public class DynamicDispatch {

  static A a;

  public static void main(String[] args) {
    a.flush();
  }
}


interface A {

  void flush();

}

class B implements A {
  @Override public void flush() {

  }
}

class C implements A {
  @Override public void flush() {

  }
}
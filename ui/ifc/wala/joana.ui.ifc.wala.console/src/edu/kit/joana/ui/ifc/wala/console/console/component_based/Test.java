package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import org.junit.Assert;

import java.util.Arrays;

public class Test {

  public static void main(String[] args) {
   test2();
   testReturnConnectedToParam();
  }

  //@org.junit.Test
  static void test1(){
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a"), new Method("Source", "b")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn("Source", "a"), new MethodReturn("Sink", "a")));
    System.out.println(flows);
  }

  //@org.junit.Test
  static void test2(){
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn("Sink", "a")));
    System.out.println(flows);
  }

  //@org.junit.Test
  static void testReturnConnectedToParam(){
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")),
        Arrays.asList(new MethodReturn("Sink", "a")));
    System.out.println(flows);
    Assert.assertFalse(flows.isEmpty());
  }
}

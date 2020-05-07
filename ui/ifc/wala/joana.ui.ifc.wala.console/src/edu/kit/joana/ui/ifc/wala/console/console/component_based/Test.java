package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.Arrays;

public class Test {

  public static void main(String[] args) {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer();
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a"), new Method("Source", "b")),
        Arrays.asList(new Method("Sink", "a"), new Method("Sink", "b")));
    System.out.println(flows);
  }
}

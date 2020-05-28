package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import edu.kit.joana.component.connector.*;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

public class Test {

  public static void main(String[] args) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(new File("/home/bechberger-local/Code/joana/dist"));

    System.out.println(processBuilder.directory().getAbsolutePath());


    processBuilder.redirectErrorStream();
    processBuilder.redirectOutput();
      processBuilder.command(
         "java",
          "-cp",
          "joana.ui.ifc.wala.console.jar" ,
          "edu.kit.joana.ui.ifc.wala.console.console.component_based.CLI",  "analyze",
          "42.json",
          "helloWorld.json"
      );


    Process process = processBuilder.start();


    int exitVal = process.waitFor();

    System.out.println(exitVal);

    if (exitVal == 0) {
      System.out.println("Success!");
    } else {
      System.out.println("Failure!");
    }

    test2WithZipRoundTrip();
    test2();
    testReturnConnectedToParam();
    testReturnNotConnectedToParamViaFlowsAndMethodParamSource();
    testReturnNotConnectedToParamViaFlows();
    testReturnConnectedToParamViaFlows();
  }

  //@org.junit.Test
  static void test1() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a"), new Method("Source", "b")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn("Source", "a"), new MethodReturn("Sink", "a")));
    System.out.println(flows);
  }

  //@org.junit.Test
  static void test2() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer
        .analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new Method("Sink", "a"), new MethodReturn("Sink", "a")));
    System.out.println("test2: " + flows);
  }

  static void test2WithZipRoundTrip() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer();
    JoanaCall joanaCall = new JoanaCall("component_sample", new Flows(), Arrays.asList(new Method("Source", "a")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn("Sink", "a")), Level.INFO);
    joanaCall.roundTrip(c -> System.out.println("test2Z: " + flowAnalyzer.processJoanaCall(c)));
  }

  //@org.junit.Test
  static void testReturnConnectedToParam() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn("Sink", "a")));
    System.out.println(flows);
    Assert.assertFalse(flows.isEmpty());
  }

  static void testReturnNotConnectedToParamViaFlows() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn("Sink", "a")));
    System.out.println(flows);
    Assert.assertEquals(2, flows.size());
  }

  static void testReturnNotConnectedToParamViaFlowsAndMethodParamSource() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new MethodParameter("Source", "a", 1)), Arrays.asList(new MethodReturn("Sink", "a")));
    System.out.println(flows);
    Assert.assertEquals(0, flows.size());
  }

  static void testReturnConnectedToParamViaFlows() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    flowAnalyzer.setKnownFlows(new Flows().add(new MethodParameter("Sink", "a", 1), new MethodReturn("Sink", "a")));
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn("Sink", "a")));
    System.out.println(flows);
    Assert.assertEquals(3, flows.size());
  }
}

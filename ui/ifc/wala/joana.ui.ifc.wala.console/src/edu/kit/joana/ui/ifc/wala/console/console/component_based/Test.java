package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import edu.kit.joana.component.connector.*;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;

public class Test {

  public static void main(String[] args) throws IOException, InterruptedException {
    testServer();
    test2WithZipRoundTrip();
    test2();
    testReturnConnectedToParam();
    testReturnNotConnectedToParamViaFlowsAndMethodParamSource();
    testReturnNotConnectedToParamViaFlows();
    testReturnConnectedToParamViaFlows();
    System.exit(0);
  }

  static void testServer() throws UnknownHostException {
    Thread thread = new Thread(() -> CLIServer.run());
    thread.start();
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer();
    JoanaCall joanaCall = new JoanaCall("component_sample", new Flows(), Arrays.asList(new Method("Source", "a")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn(new Method("Sink", "a"))), Level.INFO, Lattice.BASIC);
    JoanaCallReturn result = joanaCall.processOnServer(InetAddress.getLocalHost().getHostName());
    Assert.assertEquals(7, result.asFlows().flows.size());
    System.out.println(result.asFlows().flows);
    Assert.assertTrue(result.asFlows().flows.contains(new Method("Source", "a"), new MethodReturn(new Method("Sink", "a"))));
    Assert.assertEquals(7, result.asFlows().flows.size());
    thread.interrupt();
    //System.exit(0);
  }

  //@org.junit.Test
  static void test1() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a"), new Method("Source", "b")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn(new Method("Source", "a")), new MethodReturn(new Method("Sink", "a"))));
    System.out.println(flows);
  }

  //@org.junit.Test
  static void test2() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer
        .analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new Method("Sink", "a"), new MethodReturn(new Method("Sink", "a"))));
    System.out.println("test2: " + flows);
  }

  static void test2WithZipRoundTrip() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer();
    JoanaCall joanaCall = new JoanaCall("component_sample", new Flows(), Arrays.asList(new Method("Source", "a")),
        Arrays.asList(new Method("Sink", "a"), new MethodReturn(new Method("Sink", "a"))), Level.INFO, Lattice.BASIC);
    joanaCall.roundTrip(c -> System.out.println("test2Z: " + flowAnalyzer.processJoanaCall(c)));
  }

  //@org.junit.Test
  static void testReturnConnectedToParam() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(true);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn(new Method("Sink", "a"))));
    System.out.println(flows);
    Assert.assertFalse(flows.isEmpty());
  }

  static void testReturnNotConnectedToParamViaFlows() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn(new Method("Sink", "a"))));
    System.out.println(flows);
    Assert.assertEquals(2, flows.size());
  }

  static void testReturnNotConnectedToParamViaFlowsAndMethodParamSource() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new MethodParameter(new Method("Source", "a"), 1)), Arrays.asList(new MethodReturn(new Method("Sink", "a"))));
    System.out.println(flows);
    Assert.assertEquals(0, flows.size());
  }

  static void testReturnConnectedToParamViaFlows() {
    FlowAnalyzer flowAnalyzer = new BasicFlowAnalyzer(false);
    flowAnalyzer.setClassPath("component_sample");
    flowAnalyzer.setKnownFlows(new Flows().add(new MethodParameter(new Method("Sink", "a"), 1), new MethodReturn(new Method("Sink", "a"))));
    Flows flows = flowAnalyzer.analyze(Arrays.asList(new Method("Source", "a")), Arrays.asList(new MethodReturn(new Method("Sink", "a"))));
    System.out.println(flows);
    Assert.assertEquals(3, flows.size());
  }
}

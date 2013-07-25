/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.test;


import java.io.BufferedReader;
import java.util.Collection;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;
import edu.kit.joana.util.io.IOFactory;

public class AnnotationTest {


	private static IFCConsole console;

	@BeforeClass
	public static void setUp() {
	    	BufferedReader in = new BufferedReader(IOFactory.createUTF8ISReader(System.in));
		console = new IFCConsole(in, new PrintStreamConsoleWrapper(System.out, System.out, in, System.out, System.out));
	}


	@Test
	public void test1() {
		console.processCommand("searchEntries");
		console.processCommand("selectEntry toy.dd.ShouldLeakWithStaticField.main([Ljava/lang/String;)V");
		console.processCommand("buildSDG");
		console.processCommand("source toy.dd.A.b secret");
		console.processCommand("sink java.lang.System.out public");
		console.processCommand("run");
		Collection<? extends IViolation<SecurityNode>> iFlows = console.getLastAnalysisResult();
		Assert.assertFalse(iFlows.isEmpty());
	}

	@Test
	public void test2() {
		console.processCommand("searchEntries");
		console.processCommand("selectEntry toy.dd.ShouldLeakWithoutStaticField.main([Ljava/lang/String;)V");
		console.processCommand("buildSDG");
		console.processCommand("source toy.dd.A.b secret");
		console.processCommand("sink java.lang.System.out public");
		console.processCommand("run");
		Collection<? extends IViolation<SecurityNode>> iFlows = console.getLastAnalysisResult();
		Assert.assertFalse(iFlows.isEmpty());
	}

	@Test
	public void test3() {
		console.processCommand("searchEntries");
		console.processCommand("selectEntry toy.test.LeakByPrintingInt.main([Ljava/lang/String;)V");
		console.processCommand("buildSDG");
		console.processCommand("source toy.test.IntSecret.secretValue secret");
		console.processCommand("sink java.lang.System.out public");
		console.processCommand("run");
		Collection<? extends IViolation<SecurityNode>> iFlows = console.getLastAnalysisResult();
		Assert.assertFalse(iFlows.isEmpty());
	}
}

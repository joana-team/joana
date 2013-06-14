/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import static org.junit.Assert.*;

import java.io.IOException;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;

import org.junit.Test;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import static edu.kit.joana.ifc.wala.sdpn.CSDGwithSDPNBuilder.runAnalysis;

import java.util.LinkedList;
import java.util.List;

public class IntegrationJUnitTest {

	static final String CLASS_PATH = "../jSDG-sdpn/bin";
	static final String JRE_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";


	/** use SDGCreatorMoJoStyle to build sdgs with MoJo */
	static final SDGCreator sdgCreator = new SDGCreatorJSDGStyle();
	/*
	 * Template for new tests.
	 */
	// @Test
	// public void testXYZ() throws Exception {
	// String mainClass = "Lexamples/testdata/" + "";
	// boolean interpretKill = true;
	// int expectedSuspects = 0;
	// int expectedRemoved = 0;
	//
	//
	// RefinementResult expected = new
	// RefinementResult(expectedRemoved,expectedSuspects);
	// RefinementResult actual = runOn(mainClass,interpretKill);
	// assertEquals(expected,actual);
	// }

	@Test
	public void testKilling01_I() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing01";
		boolean interpretKill = true;
		int expectedSuspects = 1;
		int expectedRemoved = 1;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	/**
	 * Here the value is killed by an method invoked by the reader.
	 * If the overwrite positions aren't included during pruning this fails.
	 */
	@Test
	public void testKilling02_I() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing02";
		boolean interpretKill = true;
		int expectedSuspects = 1;
		int expectedRemoved = 1;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling03_I() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing03";
		boolean interpretKill = true;
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling04_I() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing04";
		boolean interpretKill = true;
		int expectedSuspects = 1;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}


	@Test
	public void testKilling01_UI() throws Exception,
			InvalidClassFileException {
		String mainClass = "Lexamples/testdata/" + "Killing01";
		boolean interpretKill = false;
		int expectedSuspects = 1;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	/**
	 * Here the value is killed by an method invoked by the reader but the call
	 * to this method is pruned therefore the analysis is imprecise here.
	 */
	@Test
	public void testKilling02_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing02";
		boolean interpretKill = false;
		int expectedSuspects = 1;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling03_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing03";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling04_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing04";
		boolean interpretKill = false;
		int expectedSuspects = 1;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testA_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "A";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	/**
	 * This fails due to some bug in Joana - does not fail if sdg is built with MoJo
	 */
	@Test
	public void testB_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "B";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}


	/**
	 * This test fails if sdg is built with MoJo.
	 * Strangely, the number of removed edges is correct, but the number of suspected
	 * edges is 14 instead of 2, which is the reason why this test fails...
	 * @throws Exception
	 */
	@Test
	public void testC_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "C";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	/**
	 * Same as B but this time there is a real flow - does not fail if sdg is built with MoJo
	 */
	@Test
	public void testD_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "D";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	@Test
	public void testWait01_UI() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Wait01";
		boolean interpretKill = false;
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass, interpretKill);
		assertEquals(expected, actual);
	}

	public RefinementResult runOn(String mainClass, boolean interpretKill)
			throws IllegalArgumentException, CancelException,
			PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
			List<String> runtimeLibs = new LinkedList<String>();
			runtimeLibs.add(JRE_LIB);		
			return runAnalysis(sdgCreator, CLASS_PATH, mainClass, runtimeLibs, "/tmp", false, true 
					,1000*60*15);
	}

	public RefinementResult runOn(String mainClass)
			throws IllegalArgumentException, CancelException,
			PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
		return runAnalysis(sdgCreator, CLASS_PATH, mainClass, JRE_LIB);
	}

}

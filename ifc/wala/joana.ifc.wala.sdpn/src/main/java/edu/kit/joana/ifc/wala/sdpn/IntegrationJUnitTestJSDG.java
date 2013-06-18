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

public class IntegrationJUnitTestJSDG {

	static final String CLASS_PATH = "bin";
	static final String JRE_LIB = "Primordial,Java,jarFile,../../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar";


	/** use SDGCreatorMoJoStyle to build sdgs with MoJo */
	static final SDGCreator sdgCreator = new SDGCreatorJSDGStyle();
	/*
	 * Template for new tests.
	 */
	// @Test
	// public void testXYZ() throws Exception {
	// String mainClass = "Lexamples/testdata/" + "";
	// int expectedSuspects = 0;
	// int expectedRemoved = 0;
	//
	//
	// RefinementResult expected = new
	// RefinementResult(expectedRemoved,expectedSuspects);
	// RefinementResult actual = runOn(mainClass);
	// assertEquals(expected,actual);
	// }

	@Test
	public void testKilling01() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing01";
		int expectedSuspects = 1;
		int expectedRemoved = 1;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	/**
	 * Here the value is killed by an method invoked by the reader.
	 * If the overwrite positions aren't included during pruning this fails.
	 */
	@Test
	public void testKilling02() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing02";
		int expectedSuspects = 1;
		int expectedRemoved = 1;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling03() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing03";
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	@Test
	public void testKilling04() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Killing04";
		int expectedSuspects = 1;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}


	

	@Test
	public void testA() throws Exception {
		String mainClass = "Lexamples/testdata/" + "A";
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	/**
	 * This fails due to some bug in Joana - does not fail if sdg is built with MoJo
	 */
	@Test
	public void testB() throws Exception {
		String mainClass = "Lexamples/testdata/" + "B";
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}


	/**
	 * This test fails if sdg is built with MoJo.
	 * Strangely, the number of removed edges is correct, but the number of suspected
	 * edges is 14 instead of 2, which is the reason why this test fails...
	 * @throws Exception
	 */
	@Test
	public void testC() throws Exception {
		String mainClass = "Lexamples/testdata/" + "C";
		int expectedSuspects = 2;
		int expectedRemoved = 2;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	/**
	 * Same as B but this time there is a real flow - does not fail if sdg is built with MoJo
	 * jsdg misses a dependency here
	 */
	@Test
	public void testD() throws Exception {
		String mainClass = "Lexamples/testdata/" + "D";
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	@Test
	public void testWait01() throws Exception {
		String mainClass = "Lexamples/testdata/" + "Wait01";
		int expectedSuspects = 2;
		int expectedRemoved = 0;

		RefinementResult expected = new RefinementResult(expectedRemoved,
				expectedSuspects);
		RefinementResult actual = runOn(mainClass);
		assertEquals(expected, actual);
	}

	public RefinementResult runOn(String mainClass)
			throws IllegalArgumentException, CancelException,
			PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
			List<String> runtimeLibs = new LinkedList<String>();
			runtimeLibs.add(JRE_LIB);		
			return runAnalysis(sdgCreator, CLASS_PATH, mainClass, runtimeLibs, "/tmp", false, true 
					,1000*60*15);
	}

}

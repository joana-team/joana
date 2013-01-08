/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.IllicitFlow;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.MHPType;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class JoinAnalysisIFCMantelTest {


	public static final String MANTEL_CLASS_NAME = "tests.Mantel00Page10";
	
	private static final String[] MANTEL_SECRET_SOURCES = {
		MANTEL_CLASS_NAME + "$Portfolio.run()V:1",
		MANTEL_CLASS_NAME + "$Portfolio.run()V:8"
	};
	
	private static final String[] MANTEL_PUBLIC_OUTPUT = {
		MANTEL_CLASS_NAME + "$EuroStoxx50.run()V:12"
	};
	
	private static SDGProgramPart searchInstruction(SDGMethod m, String pat) {
		for (SDGInstruction i : m.getInstructions()) {
			if (i.getNode().getLabel().contains(pat)) {
				return i;
			}
		}
		fail("no instruction containing '" + pat + "' found in method " + m);
		throw new IllegalStateException("this statement should not be reachable");
	}

	public static IFCAnalysis buildAndAnnotateMantel(final MHPType mhp) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(MANTEL_CLASS_NAME);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(true);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setMhpType(mhp);
		
		SDGProgram prog = null;
		
		try {
			prog = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException e) {
			throw new ApiTestException(e);
		} catch (IOException e) {
			throw new ApiTestException(e);
		} catch (UnsoundGraphException e) {
			throw new ApiTestException(e);
		} catch (CancelException e) {
			throw new ApiTestException(e);
		}
		
		IFCAnalysis ana = new IFCAnalysis(prog);
		
		for (String secretSrcTxt : MANTEL_SECRET_SOURCES) {
			SDGProgramPart secretSrc = ana.getProgramPart(secretSrcTxt);
			assertNotNull(secretSrc);
			ana.addSourceAnnotation(secretSrc, BuiltinLattices.STD_SECLEVEL_HIGH);
		}
		
		for (String publicOutTxt : MANTEL_PUBLIC_OUTPUT) {
			SDGProgramPart publicOut = ana.getProgramPart(publicOutTxt);
			assertNotNull(publicOut);
			ana.addSinkAnnotation(publicOut, BuiltinLattices.STD_SECLEVEL_LOW);
		}
		
		/**
		 * HACK!
		 * The eclipse java compiler and the standard java compiler handle string concatenation differently, which
		 * causes the bytecode instruction of the flush statement in the main method of the Mantel00Page10 example
		 * to be dependent on the used compiler. To get the tests passed irrespective of the compiler used to
		 * compile the Mantel00Page10 example code, a workaround is used here. Rather than hardcoding the bytecode
		 * instruction index of the flush statement to be annotated as public sink, the flush statement is searched 
		 * in the main method. This exploits the fact that there is only one flush instruction in the main method. 
		 * This assumption seems to break harder than an assumption about the exact bytecode instruction index of the flush instruction.
		 */
		SDGProgramPart ppMain = ana.getProgramPart(MANTEL_CLASS_NAME + ".main([Ljava/lang/String;)V");
		assertTrue(ppMain instanceof SDGMethod);
		SDGMethod methMain = (SDGMethod) ppMain;
		SDGProgramPart flushInstruction = searchInstruction(methMain, "flush");
		assertNotNull(flushInstruction);
		ana.addSinkAnnotation(flushInstruction, BuiltinLattices.STD_SECLEVEL_LOW);
		return ana;
	}
	
	@Test
	public void testMantelWithImpreciseAnalysis() {
		try {
			IFCAnalysis ana = buildAndAnnotateMantel(MHPType.SIMPLE);
			Collection<IllicitFlow> illegal = ana.doIFC(IFCType.PROBABILISTIC_WITH_SIMPLE_MHP);
			assertFalse(illegal.isEmpty());
			assertEquals(378, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testMantelWithPreciseAnalysis() {
		try {
			IFCAnalysis ana = buildAndAnnotateMantel(MHPType.PRECISE);
			Collection<IllicitFlow> illegal = ana.doIFC(IFCType.PROBABILISTIC_WITH_PRECISE_MHP);
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

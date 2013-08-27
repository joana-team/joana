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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class JoinAnalysisIFCMantelTest {


	public static final String MANTEL_CLASS_NAME = "tests.Mantel00Page10";
	
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
		
		SDGMethod portfolioRun = ana.getProgram().getMethod(MANTEL_CLASS_NAME + "$Portfolio.run()V");
		assertNotNull(portfolioRun);
		List<SDGInstruction> calls2GetPFNames = portfolioRun.getAllCalls(JavaMethodSignature.fromString(MANTEL_CLASS_NAME + "$Portfolio.getPFNames()[Ljava/lang/String;"));
		assertEquals(1, calls2GetPFNames.size());
		ana.addSourceAnnotation(calls2GetPFNames.get(0), BuiltinLattices.STD_SECLEVEL_HIGH);
		List<SDGInstruction> calls2GetPFNums = portfolioRun.getAllCalls(JavaMethodSignature.fromString(MANTEL_CLASS_NAME + "$Portfolio.getPFNums()[I"));
		assertEquals(1, calls2GetPFNums.size());
		ana.addSourceAnnotation(calls2GetPFNums.get(0), BuiltinLattices.STD_SECLEVEL_HIGH);
		
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
		SDGMethod methMain = ana.getProgram().getMethod(MANTEL_CLASS_NAME + ".main([Ljava/lang/String;)V");
		List<SDGInstruction> flushInstructions = methMain.getAllCalls(JavaMethodSignature.fromString("java.io.BufferedWriter.flush()V"));
		
		//List<SDGInstruction> flushInstruction = searchInstructionByName(methMain, "flush");
		assertEquals(1, flushInstructions.size());
		ana.addSinkAnnotation(flushInstructions.get(0), BuiltinLattices.STD_SECLEVEL_LOW);
		
		SDGMethod methEuroStoxxRun = ana.getProgram().getMethod(MANTEL_CLASS_NAME + "$EuroStoxx50.run()V");
		List<SDGInstruction> fi2 = methEuroStoxxRun.getAllCalls(JavaMethodSignature.fromString("java.io.BufferedWriter.flush()V"));
		
		//List<SDGInstruction> flushInstruction = searchInstructionByName(methMain, "flush");
		assertEquals(1, fi2.size());
		ana.addSinkAnnotation(fi2.get(0), BuiltinLattices.STD_SECLEVEL_LOW);
		

		return ana;
	}
	
	@Test
	public void testMantelWithImpreciseAnalysis() {
		try {
			IFCAnalysis ana = buildAndAnnotateMantel(MHPType.SIMPLE);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.PROBABILISTIC_WITH_SIMPLE_MHP);
			assertFalse(illegal.isEmpty());
			assertEquals(385, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testMantelWithPreciseAnalysis() {
		try {
			IFCAnalysis ana = buildAndAnnotateMantel(MHPType.PRECISE);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.PROBABILISTIC_WITH_PRECISE_MHP);
			assertEquals(0, illegal.size());
			assertTrue(illegal.isEmpty());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

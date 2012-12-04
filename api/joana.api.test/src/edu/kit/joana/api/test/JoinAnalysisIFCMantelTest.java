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
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class JoinAnalysisIFCMantelTest {

	public static final String CLASSPATH = "../../example/joana.example.many-small-progs/bin";
	public static final String STUBS = "../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar";

	public static final String MANTEL_CLASS_NAME = "tests.Mantel00Page10";
	
	private static final String[] MANTEL_SECRET_SOURCES = {
		MANTEL_CLASS_NAME + "$Portfolio.run()V:1",
		MANTEL_CLASS_NAME + "$Portfolio.run()V:8"
	};
	
	private static final String[] MANTEL_PUBLIC_OUTPUT = {
		MANTEL_CLASS_NAME + "$EuroStoxx50.run()V:12",
		MANTEL_CLASS_NAME + ".main([Ljava/lang/String;)V:159"
	};

	public static IFCAnalysis buildAndAnnotateMantel(final MHPType mhp) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(MANTEL_CLASS_NAME);
		SDGConfig config = new SDGConfig(CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
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

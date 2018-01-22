/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 * 
 * These test verify that ifc remains sound when using callgraph pruning.
 */
public class CallbackPruningTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz, Package applicationPackage, boolean ignore) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		final SDGConfig cfg = BuildSDG.top_sequential;
		final Atom applicationPackageAtom = Atom.findOrCreateUnicodeAtom(applicationPackage.getName().replace(".", "/"));
		cfg.setPruningPolicy(new PruningPolicy() {
			@Override
			public boolean check(CGNode n) {
				final boolean result = n.getMethod().getDeclaringClass().getReference().getName().getPackage().startsWith(applicationPackageAtom); 
				return result;
			}
		});
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz, cfg, ignore);
		
		final String filename = clazz.getCanonicalName()
								+ (ignore ? ".ignore" : ".passon")
								+ ".pdg";
		
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), filename);
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), filename);
		}
		
		return ana;
	}
		
	private static void testPreciseEnough(Class<?> clazz, Package applicationPackage) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAnnotateDump(clazz, applicationPackage, false);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			//System.out.println(illegal);
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise, we're precise enough to find out that there aren't
			IFCAnalysis ana = buildAnnotateDump(clazz, applicationPackage, true);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
		}
	}

	@SuppressWarnings("unused")
	private static void testTooImprecise(Class<?> clazz, Package applicationPackage) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAnnotateDump(clazz, applicationPackage, false);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise there aren't, but the analysis not precise enough to
			IFCAnalysis ana = buildAnnotateDump(clazz, applicationPackage, true);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}
	}

	@Test
	public void testTest1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(
				joana.api.testdata.pruning.test1.application.Application.class,
				joana.api.testdata.pruning.test1.application.Application.class.getPackage()
		);
	}
}

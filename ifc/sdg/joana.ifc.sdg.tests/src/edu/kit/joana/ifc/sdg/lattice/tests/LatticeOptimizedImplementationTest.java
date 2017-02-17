package edu.kit.joana.ifc.sdg.lattice.tests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.ILatticeOperations;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.lattice.impl.PowersetLattice;

import edu.kit.joana.api.lattice.LowHighLattice;

public class LatticeOptimizedImplementationTest {

	@SafeVarargs
	private static <T> Set<T> asSet(T... ts) {
		return new HashSet<T>(Arrays.<T>asList(ts));
	}
	

	@SuppressWarnings("deprecation")
	private <ElementType> void testOperationsvsStatic(ILatticeOperations<ElementType> someLattice) {
		for (ElementType x : someLattice.getElements()) {
			assertEquals(
				new HashSet<>(LatticeUtil.collectAllGreaterElements(x, someLattice)),
				new HashSet<>(LatticeUtil.collectAllGreaterElements(x, (IStaticLattice<ElementType>) someLattice))
			);
			assertEquals(
					new HashSet<>(LatticeUtil.collectAllLowerElements(x, someLattice)),
					new HashSet<>(LatticeUtil.collectAllLowerElements(x, (IStaticLattice<ElementType>) someLattice))
				);
		}		
	}

	
	@SuppressWarnings("deprecation")
	private <ElementType> void testOperations(ILatticeOperations<ElementType> someLattice) {
		for (ElementType x : someLattice.getElements()) {
			assertEquals(
				new HashSet<>(LatticeUtil.collectAllGreaterElements(x, someLattice)),
				new HashSet<>(someLattice.collectAllGreaterElements(x))
			);

			for (ElementType y : someLattice.getElements()) {
				assertEquals(
						LatticeUtil.leastUpperBounds(x, y, someLattice),
						someLattice.leastUpperBounds(x, y)
				);
				assertEquals(
						LatticeUtil.greatestLowerBounds(x, y, someLattice),
						someLattice.greatestLowerBounds(x, y)
				);
			}
		}
		
		for (Collection<ElementType> xs: Sets.powerSet(new HashSet<>(someLattice.getElements()))) {
			assertEquals(
					LatticeUtil.findTopElements(xs, someLattice),
					someLattice.findTopElements(xs)
			);
			assertEquals(
					LatticeUtil.findBottomElements(xs, someLattice),
					someLattice.findBottomElements(xs)
			);
		}
		
		assertEquals(
				LatticeUtil.findUnreachableFromBottom(someLattice.getElements(), someLattice),
				someLattice.findUnreachableFromBottom(someLattice.getElements())
		);
		assertEquals(
				LatticeUtil.findUnreachableFromTop(someLattice.getElements(), someLattice),
				someLattice.findUnreachableFromTop(someLattice.getElements())
		);

		
	}

	@SuppressWarnings("deprecation")
	private <ElementType> void testStaticLattice(IStaticLattice<ElementType> someLattice) {
		for (ElementType x : someLattice.getElements()) {
			assertEquals(
				new HashSet<>(LatticeUtil.collectAllGreaterElements(x, someLattice)), // ignore order
				new HashSet<>(someLattice.collectAllGreaterElements(x))
			);
			assertEquals(
				new HashSet<>(LatticeUtil.collectAllLowerElements(x, someLattice)),
				new HashSet<>(someLattice.collectAllLowerElements(x))
			);
			assertEquals(
				new HashSet<>(LatticeUtil.collectNoninterferingElements(x, someLattice)),
				new HashSet<>(someLattice.collectNoninterferingElements(x))
			);

			for (ElementType y : someLattice.getElements()) {
				assertEquals(
						LatticeUtil.isLeq(someLattice, x, y),
						someLattice.isLeq(x, y)
				);
			}
		}
	}
	
	@Test
	public void testPowerset() {
		PowersetLattice<String> smartHomeLattice = new PowersetLattice<>(
			asSet("customer", "provider", "application")
		);
		assertNull(LatticeValidator.validateIncremental(smartHomeLattice));
		
		testStaticLattice(smartHomeLattice);
		testOperations(smartHomeLattice);
		testOperationsvsStatic(smartHomeLattice);
	}
	
	@Test
	public void testLowHigh() {
		LowHighLattice lowHighLattice = LowHighLattice.INSTANCE; 
		
		testStaticLattice(lowHighLattice);
	}
	
	@Test
	public void testCompleted() {
		IEditableLattice<String> smartHomeLattice = new EditableLatticeSimple<>();
		String[] levels  = { "customer", "provider", "application",
		                     "customer provider", "application customer", "application provider"
		};
		for (String level : levels) {
			smartHomeLattice.addElement(level);
		}
		smartHomeLattice.setImmediatelyGreater("customer provider", "provider");
		smartHomeLattice.setImmediatelyGreater("customer provider",  "customer");
		smartHomeLattice.setImmediatelyGreater("application customer", "customer");
		smartHomeLattice.setImmediatelyGreater("application customer", "application");
		smartHomeLattice.setImmediatelyGreater("application provider", "application");
		smartHomeLattice.setImmediatelyGreater("application provider", "provider");
		
		IEditableLattice<String> completion = LatticeUtil.dedekindMcNeilleCompletion(smartHomeLattice);
		
		testStaticLattice(completion);
		testOperations(completion);
		testOperationsvsStatic(completion);
	}
}

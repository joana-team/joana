package edu.kit.joana.ifc.sdg.lattice.tests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.impl.PowersetLattice;

public class PowerSetLatticeTest {

	@SafeVarargs
	private static <T> Set<T> asSet(T... ts) {
		return new HashSet<T>(Arrays.asList(ts));
	}
	
	@Test
	public void testSmartHome() {
		PowersetLattice<String> smartHomeLattice = new PowersetLattice<>(
			asSet("customer", "provider", "application")
		);
		assertNull(LatticeValidator.validateIncremental(smartHomeLattice));
		
		Set<String> top = smartHomeLattice.getTop();
		Set<String> bot = smartHomeLattice.getBottom();
		assertEquals(
			asSet("customer", "provider", "application"),
			top
		);
		assertTrue(bot.isEmpty());
		
		assertEquals(
			top,
			smartHomeLattice.leastUpperBound(
				asSet("customer", "provider"),
				asSet("customer", "application")
			)
		);
		assertEquals(
			asSet("customer", "provider"),
			smartHomeLattice.leastUpperBound(
				asSet("customer", "provider"),
				asSet("customer", "provider")
			)
		);
		
		assertEquals(
			asSet(
				asSet("customer"),
				asSet("customer", "provider"),
				asSet("customer", "application"),
				top
			),
			new HashSet<>(smartHomeLattice.collectAllGreaterElements(asSet("customer"))) // ignore order
		);
	}
}

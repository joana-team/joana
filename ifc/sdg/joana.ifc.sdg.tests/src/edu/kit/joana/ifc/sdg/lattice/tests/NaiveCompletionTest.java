package edu.kit.joana.ifc.sdg.lattice.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;

public class NaiveCompletionTest {

	@Test
	public void testSmartHome() {
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
		
		LatticeUtil.naiveTopBottomCompletion(smartHomeLattice);
		assertNull(LatticeValidator.validateIncremental(smartHomeLattice));
	}

	
	public static void main(String[] args) {
		new NaiveCompletionTest().testSmartHome();
	}
}

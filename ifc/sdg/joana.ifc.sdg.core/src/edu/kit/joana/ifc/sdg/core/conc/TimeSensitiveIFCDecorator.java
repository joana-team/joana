/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * This class extends an ifc checker with the ability of being time-sensitive. It only makes sense to extend an
 * ifc checker which checks for direct or indirect possibilistic leaks, but for the sake of abstractness (and
 * because there is no distinction between possibilistic and probabilistic checkers) it can wrap arbitrary 
 * ifc checkers.
 * @author Martin Mohr
 */
public class TimeSensitiveIFCDecorator extends IFC {
	
	private final IFC baseIFC;
	
	public TimeSensitiveIFCDecorator(IFC baseIFC) {
		super(baseIFC.getSDG(), baseIFC.getLattice());
		this.baseIFC = baseIFC;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		Collection<ClassifiedViolation> baseVios = baseIFC.checkIFlow();
		Collection<ClassifiedViolation> refinedVios = new LinkedList<ClassifiedViolation>();
		Nanda tsbwSlicer = new Nanda(baseIFC.getSDG(), new NandaBackward());
		for (ClassifiedViolation vio : baseVios) {
			Collection<SDGNode> tsSlice = tsbwSlicer.slice(vio.getSink());
			if (tsSlice.contains(vio.getSource())) {
				// there is a path without time-travel between source and sink
				refinedVios.add(vio);
			} 
		}
		
		return refinedVios;
	}

}

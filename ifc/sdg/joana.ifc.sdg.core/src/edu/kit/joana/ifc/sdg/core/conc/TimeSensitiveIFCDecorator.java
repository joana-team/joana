/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.ViolationFilter;
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
public class TimeSensitiveIFCDecorator extends IFC<String> {
	
	private final IFC<String> baseIFC;
	
	public TimeSensitiveIFCDecorator(IFC<String> baseIFC) {
		super(baseIFC.getSDG(), baseIFC.getLattice());
		this.baseIFC = baseIFC;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		Collection<? extends IViolation<SecurityNode>> baseVios = baseIFC.checkIFlow();
		Nanda tsbwSlicer = new Nanda(baseIFC.getSDG(), new NandaBackward());
		TSFilter filter = new TSFilter(tsbwSlicer);
		return filter.filter(baseVios);
	}
	
	private static class TSFilter extends ViolationFilter<SecurityNode> {
		
		private Nanda tsbwSlicer;
		
		TSFilter(Nanda tsbwSlicer) {
			this.tsbwSlicer = tsbwSlicer;
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.TimeSensitiveIFCDecorator.ViolationFilter#acceptIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
		 */
		@Override
		protected boolean acceptIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
			Collection<SDGNode> tsSlice = tsbwSlicer.slice(iFlow.getSink());
			return tsSlice.contains(iFlow.getSource());
		}
	}

}

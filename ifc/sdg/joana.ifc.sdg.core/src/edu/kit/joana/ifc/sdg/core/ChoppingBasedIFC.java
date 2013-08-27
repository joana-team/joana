/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * This class provides an IFC algorithm based on a given chopping algorithm, which is interchangeable. The algorithms proceeds as follows: <p>
 * For each pair (s,t) where s is an information source and t is an information sink, it is checked whether
 * <ol>
 * <li>The security level of s is strictly higher than or incomparable to the security level of t</li>
 * <li>The chop of s and t is not empty.</li>
 * </ol>
 * If these two conditions are satisfied, a violation for (s,t) is reported.<p>
 * Note, that this class can only be used to check for possibilistic leaks. To check also for probabilistic leaks, use {@link ProbabilisticNIChecker}.<p>
 * Also, there is no support for declassification, unless the provided chopper is configured appropriately.
 * @author Martin Mohr
 */
public class ChoppingBasedIFC extends IFC {
	
	private final Chopper chopper;
	
	/**
	 * Instantiates a new ChoppingBasedIFC object.
	 * @param sdg SDG to perform IFC algorithm on
	 * @param lattice security lattice to use for IFC algorithm
	 * @param chopper chopper to be used to check for the existence of information flows between sources and sinks.
	 */
	public ChoppingBasedIFC(SDG sdg, IStaticLattice<String> lattice, Chopper chopper) {
		super(sdg, lattice);
		this.chopper = chopper;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		Collection<SecurityNode> srcs = collectSources();
		Collection<SecurityNode> snks = collectSinks();
		Collection<ClassifiedViolation> vios = new LinkedList<ClassifiedViolation>();
		for (SecurityNode src : srcs) {
			for (SecurityNode snk : snks) {
				addPossibleViolation(vios, src, snk);
			}
		}

		return vios;
	}
	
	private void addPossibleViolation(Collection<ClassifiedViolation> vios, SecurityNode src, SecurityNode snk) {
		String srcLevel = src.getProvided();
		String snkLevel = snk.getRequired();
		if (!l.leastUpperBound(srcLevel, snkLevel).equals(snkLevel)) {
			Collection<SDGNode> chop = chopper.chop(src, snk);
			if (!chop.isEmpty()) {
				vios.add(ClassifiedViolation.createViolation(snk, src, snkLevel));
			}
		}
	}
	
	private Collection<SecurityNode> collectSources() {
		return filter(new SourceNodePredicate());
	}
	
	private Collection<SecurityNode> collectSinks() {
		return filter(new SinkNodePredicate());
	}
	
	private Collection<SecurityNode> filter(SecurityNodePredicate p) {
		Collection<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (SDGNode n : g.vertexSet()) {
			SecurityNode sN = (SecurityNode) n;
			if (p.satisfies(sN)) {
				ret.add(sN);
			}
		}
		
		return ret;
	}
	
	private static interface SecurityNodePredicate {
		boolean satisfies(SecurityNode n);
	}
	
	private static class SourceNodePredicate implements SecurityNodePredicate {

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.ChoppingBasedIFC.SecurityNodePredicate#satisfies(edu.kit.joana.ifc.sdg.core.SecurityNode)
		 */
		@Override
		public boolean satisfies(SecurityNode n) {
			return n.isInformationSource();
		}
		
	}
	
	private static class SinkNodePredicate implements SecurityNodePredicate {

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.ChoppingBasedIFC.SecurityNodePredicate#satisfies(edu.kit.joana.ifc.sdg.core.SecurityNode)
		 */
		@Override
		public boolean satisfies(SecurityNode n) {
			return n.isInformationSink();
		}
		
	}
}

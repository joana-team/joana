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
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * Anbindung an den Algorithmus fuer probabilistische Noninterferenz.
 * Hauptaufgabe ist das Managen von ProgressListenern.
 * 
 * @author giffhorn
 */
public class ProbabilisticNIChecker extends IFC {

	private final MHPAnalysis mhp;

	private final boolean timeSens;
	
	private ConflictScanner prob;
	
	/**
	 * Erzeugt eine neue Instanz.
	 * 
	 * @param sdg
	 *            Ein SDG
	 * @param lattice
	 *            Ein Sicherheitsverband
	 */
	public ProbabilisticNIChecker(SDG sdg, IStaticLattice<String> lattice) {
		this(sdg, lattice, PreciseMHPAnalysis.analyze(sdg));
	}

	public ProbabilisticNIChecker(SDG sdg, IStaticLattice<String> lattice, MHPAnalysis mhp) {
		this(sdg, lattice, mhp, false);
	}
	
	public ProbabilisticNIChecker(SDG sdg, IStaticLattice<String> lattice, MHPAnalysis mhp, boolean timeSens) {
		super(sdg, lattice);
		this.mhp = mhp;
		this.timeSens = timeSens;
	}

	/**
	 * Berechnet, ob der SDG probabilistisch noninterferent ist.
	 * 
	 * Verwendet den BarrierIFCSlicer, um expliziten und impliziten Fluss zu
	 * untersuchen, und den ProbabilisticNISlicer, um pobabilistiche Lecks zu
	 * finden.
	 * 
	 * @return Eine Liste mit den gefundenen Sicherheitsverletzungen.
	 * @throws InterSlicePluginException
	 * @throws NotInLatticeException
	 */
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		Collection<ClassifiedViolation> ret = null; // list to be returned
		IFC is = new BarrierIFCSlicer(g, l);
		
		if (timeSens) {
			is = new TimeSensitiveIFCDecorator(is);
		}

		probInit = System.currentTimeMillis();
		prob = ProbabilisticNISlicer.simpleCheck(g, l, mhp, this.timeSens);
		probInit = System.currentTimeMillis() - probInit;

		probCheck = System.currentTimeMillis();
		ret = translate(prob.check());
		probCheck = System.currentTimeMillis() - probCheck;

		flowCheck = System.currentTimeMillis();
		ret.addAll(is.checkIFlow());
		flowCheck = System.currentTimeMillis() - flowCheck;

		//dataChannels = prob.dataChannels;
		//orderChannels = prob.orderChannels;

		return ret;
	}
	
	public Collection<ClassifiedViolation> translate(Collection<? extends IViolation> vios) {
		ViolationTranslator trans = new ViolationTranslator();
		return trans.translate(vios);
	}
	
	public void setProbSlicer(ConflictScanner prob) {
		this.prob = prob;
	}
	
	public ConflictScanner getProbSlicer() {
		return prob;
	}

	public long probInit;
	public long probCheck;
	public long flowCheck;

	public long dataChannels;
	public long orderChannels;
}

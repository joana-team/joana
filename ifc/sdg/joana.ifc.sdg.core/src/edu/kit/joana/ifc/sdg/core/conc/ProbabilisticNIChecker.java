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
import edu.kit.joana.ifc.sdg.core.violations.Violation;
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
	public static final boolean DEBUG = false;

	private final MHPAnalysis mhp;

	private final boolean timeSens;
	
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
	public Collection<Violation> checkIFlow() throws NotInLatticeException {
		// long slicestart = System.currentTimeMillis();
		Collection<Violation> ret = null; // list to be returned
		IFC is = new BarrierIFCSlicer(g, l);
		if (timeSens) {
			is = new TimeSensitiveIFCDecorator(is);
		}
		probInit = System.currentTimeMillis();
		ProbabilisticNISlicer prob = ProbabilisticNISlicer.simpleCheck(g, l, mhp, this.timeSens);
		probInit = System.currentTimeMillis() - probInit;
		// is.addProgressListener(this);
		// prob.addProgressListener(this);

		// if (DEBUG) System.out.println("Started slicing at " + slicestart);

		try {
			probCheck = System.currentTimeMillis();
			ret = prob.check();
			probCheck = System.currentTimeMillis() - probCheck;

			flowCheck = System.currentTimeMillis();
			ret.addAll(is.checkIFlow());
			flowCheck = System.currentTimeMillis() - flowCheck;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// long sliceend = System.currentTimeMillis();
		// if (DEBUG) System.out.println("Finished slicing at " + sliceend +
		// " | slice duration: " + (sliceend-slicestart));

		// if (DEBUG) System.out.println("ret.size: " + ret.size() + " ret: " +
		// ret);
		// this.progressChanged("Done", 100, 100);
		dataChannels = prob.dataChannels;
		orderChannels = prob.orderChannels;

		return ret;
	}

	public long probInit;
	public long probCheck;
	public long flowCheck;

	public long dataChannels;
	public long orderChannels;
}

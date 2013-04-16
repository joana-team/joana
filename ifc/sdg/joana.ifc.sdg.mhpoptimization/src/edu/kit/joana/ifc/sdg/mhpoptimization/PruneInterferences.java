/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * Utility class which provides the possiblity to prune interference edges from
 * a given SDGs using MHP information, for example the results of a preliminary
 * MHP analysis.
 * 
 * @author Martin Mohr
 */
public final class PruneInterferences {

	private static final Logger debug = Log.getLogger(Log.L_SDG_INTERFERENCE_DEBUG);
	private static final boolean IS_DEBUG = debug.isEnabled();
	
	/**
	 * Convenience method for {@link #pruneInterferences(SDG, MHPAnalysis)}, which runs the required MHPAnalysis
	 * and uses this MHPAnalysis for the actual pruning. The given SDG is expected to be {@link CSDGPreprocessor#preprocessSDG(SDG)
	 *  preprocessed} since the used MHP algorithms require {@link ThreadsInformation information about the threads} to be available.<br>
	 * The parameter 'mhpType' determines the algorithm, with which the MHP information is obtained. If 'mhpType' is
	 * SIMPLE, then {@link SimpleMHPAnalysis} is used, if it is PRECISE, then {@link PreciseMHPAnalysis} is used. If it
	 * is NONE, then this method returns immediately without analyzing or pruning anything.<br>
	 * @param g SDG to prune interference edges from - must have been {@link CSDGPreprocessor#preprocessSDG(SDG) preprocessed}
	 * @param mhpType determines which MHP algorithm is used (see method comment)
	 */
	public static final void prunePreprocessedCSDG(SDG g, MHPType mhpType) {
		MHPAnalysis mhpAnalysis;
		switch (mhpType) {
		case NONE:
			return;
		case SIMPLE:
			mhpAnalysis = SimpleMHPAnalysis.analyze(g);
			break;
		case PRECISE:
			mhpAnalysis = PreciseMHPAnalysis.analyze(g);
			break;
		default:
			throw new IllegalStateException("unhandled case: " + mhpType);
		}
		
		pruneInterferences(g, mhpAnalysis);
	}
	
	/**
	 * Convenience method for {@link #prunePreprocessedCSDG(SDG, MHPType)}. It ensures that the SDG has been preprocessed, so that
	 * the required thread information is available. 
	 * @param g SDG to prune interference edges from
	 * @param mhpType determines which MHP algorithm is used (see {@link #prunePreprocessedCSDG(SDG, MHPType)}
	 */
	public static final void preprocessAndPruneCSDG(SDG g, MHPType mhpType) {
		CSDGPreprocessor.preprocessSDG(g);
		prunePreprocessedCSDG(g, mhpType);
	}
	
	
	/**
	 * Prunes spurious interference edges from the given SDG. An interference edge is considered spurious, if
	 * the incident nodes cannot happen in parallel, i.e. their execution order is fixed - according to
	 * the given MHP analysis result.<br>
	 * Note, that the SDG is altered by this method!
	 * @param graph SDG to prune interference edges from
	 * @param mhp mhp analysis result of given SDG which is to be taken as a basis for the pruning
	 */
	public static final void pruneInterferences(SDG graph, MHPAnalysis mhp) {
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		int all = 0;
		int x = 0;
		for (SDGEdge e : graph.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				all++;
				// nicht parallel
				if (!mhp.isParallel(e.getSource(), e.getTarget())) {
					remove.add(e);
					x++;
				}
			}
		}
	
		for (SDGEdge e : remove) {
			if (IS_DEBUG) debug.outln("Edge between " + e.getSource() + " and " + e.getTarget() + " of kind " + e.getKind() + " is spurious.");
			graph.removeEdge(e);
		}
	
		if (IS_DEBUG) debug.outln("	" + x + " of " + all + " edges removed");
	}

}

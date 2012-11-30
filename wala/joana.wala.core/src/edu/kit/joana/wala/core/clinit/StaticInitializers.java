/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.clinit;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;

/**
 * This is testcode. It is guaranteed to not work properly.
 * 
 * TODO: @author Add your name here.
 */
public class StaticInitializers {

	private final SDGBuilder sdg;

	private StaticInitializers(final SDGBuilder sdg) {
		assert sdg.cfg.staticInitializers == StaticInitializationTreatment.ACCURATE;
		this.sdg = sdg;
	}

	public static void compute(final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		final StaticInitializers si = new StaticInitializers(sdg);
		si.run(progress);
	}

	private void run(final IProgressMonitor progress) throws CancelException {
		/* 1. do probably not initialized analysis
		 * 2. add calls to static initializers where
		 * 	- a static field access
		 *  - a call to a static method
		 *  - a new instance operation
		 *  is issued on a class that has not been initialized
		 */
		MonitorUtil.throwExceptionIfCanceled(progress);


	    ProbablyNotInitilizedAnalysis notInit = new ProbablyNotInitilizedAnalysis(sdg.getNonPrunedWalaCallGraph(), sdg.cfg.cache);
	    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = notInit.analyze();
	    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = notInit.getSupergraph();

	    for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {

	    	if (bb.getNode().toString().contains("testInterproc")) {
	    		IExplodedBasicBlock delegate = bb.getDelegate();

		        if (delegate.getNumber() == 4) {
					IntSet solution = result.getResult(bb);
					IntIterator intIterator = solution.intIterator();
					List<Pair<CGNode, Integer>> applicationDefs = new ArrayList<Pair<CGNode,Integer>>();

					while (intIterator.hasNext()) {
						int next = intIterator.next();
						final Pair<CGNode, Integer> def = notInit.getDomain().getMappedObject(next);

						if (def.fst.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
							System.out.println(def);
							applicationDefs.add(def);
						}
					}
		        }
	    	}
	    }
	}

}

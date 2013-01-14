/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.map.hash.TIntObjectHashMap;

import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;

/**
 * Contains some utility methods concerning output of info messages etc.
 * 
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
public final class ThreadInfoDebugOutput {

	private ThreadInfoDebugOutput() {
		throw new IllegalStateException();
	}

	public static void printThreads(final TIntObjectHashMap<IntSet> threads, final TIntObjectHashMap<IntSet> threadIds,
			final SDGBuilder builder) {

		final Logger log = Log.getLogger(Log.L_WALA_INTERFERENCE_DEBUG);

		if (!log.isEnabled()) {
			return;
		}

		for (final int pdgId : threads.keys()) {
			final PDG pdg = builder.getPDGforId(pdgId);
			final IntSet ids = threadIds.get(pdg.getId());
			String id = "";
			for (final IntIterator it = ids.intIterator(); it.hasNext();) {
				id += it.next();
				if (it.hasNext()) {
					id += ',';
				}
			}

			log.outln("\nThreadID(" + id + ") - " + pdg + " calls:\n{");
			final IntSet transitiveCalled = threads.get(pdgId);
			transitiveCalled.foreach(new IntSetAction() {

				public void act(final int x) {
					PDG pdg = builder.getPDGforId(x);
					log.outln(PrettyWalaNames.methodName(pdg.getMethod()) + ", ");
				}

			});
			log.outln("}");
		}
	}

}

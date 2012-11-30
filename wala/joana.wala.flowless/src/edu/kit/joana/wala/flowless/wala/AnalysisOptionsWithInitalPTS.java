/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.wala;

import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class AnalysisOptionsWithInitalPTS extends AnalysisOptions {

	public AnalysisOptionsWithInitalPTS(AnalysisScope scope, Iterable<EntrypointWithPTS> entries) {
		super(scope, entries);
	}

	@SuppressWarnings("unchecked")
	public Iterable<EntrypointWithPTS> getEntrypoints() {
		return (Iterable<EntrypointWithPTS>) super.getEntrypoints();
	}

	public void setEntrypoints(Iterable<? extends Entrypoint> entrypoints) {
		assert checkForEntrypointWithPTS(entrypoints) : "Only entrypoints with points-to information are allowed.";

		super.setEntrypoints(entrypoints);
	}

	private final static boolean checkForEntrypointWithPTS(Iterable<? extends Entrypoint> entrypoints) {
		Iterator<? extends Entrypoint> it = entrypoints.iterator();
		while (it.hasNext()) {
			Entrypoint e = it.next();
			if (!(e instanceof EntrypointWithPTS)) {
				return false;
			}
		}

		return true;
	}

}

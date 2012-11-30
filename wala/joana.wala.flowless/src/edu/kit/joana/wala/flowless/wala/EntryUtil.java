/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.wala;

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class EntryUtil {

	private EntryUtil() {
	}

	public static AnalysisOptions createAnalysisOptionsWithPTS(final IClassHierarchy cha, final IMethod method, final PointsTo pts, boolean noSubclasses) {
		if (method == null || !method.equals(cha.resolveMethod(method.getReference()))) {
			throw new IllegalArgumentException("Method must be part of the class hierarchy.");
		}

		EntrypointWithPTS entry = new EntrypointWithPTS(method, cha, pts, noSubclasses);
		List<EntrypointWithPTS> entries = new LinkedList<EntrypointWithPTS>();
		entries.add(entry);

		AnalysisOptionsWithInitalPTS options = new AnalysisOptionsWithInitalPTS(cha.getScope(), entries);

		return options;
	}
}

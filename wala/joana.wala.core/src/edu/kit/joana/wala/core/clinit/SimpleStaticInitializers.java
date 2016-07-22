/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.clinit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGCallReturn;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.PDGNode.Kind;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.util.NotImplementedException;

/**
 * Add static initializers to the pdg. This is done in a simple not really conservative approximation, by
 * adding all initializers to the start of the program. This ignores potential indirect information flow
 * through the order in which the initializers are called.
 * We also ignore static initializers from api/library and only consider stuff from the application.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 * @deprecated Not used => We start earlier by using the fakeWorldClinit method of wala. We keep this here to serve as
 * starting point for the not-so-simple precise static initializer treatment that is yet to come.
 */
@Deprecated
public class SimpleStaticInitializers {

	private final SDGBuilder sdg;
	private final BasicCallGraph<?> cg;
	private PDG fakeRoot;
	private PDGNode lastCall;

	private SimpleStaticInitializers(final SDGBuilder sdg) {
		assert sdg.cfg.staticInitializers == StaticInitializationTreatment.SIMPLE;
		final CallGraph tmp = sdg.getNonPrunedWalaCallGraph();
		if (!(tmp instanceof BasicCallGraph)) {
			throw new IllegalArgumentException(tmp.getClass().getSimpleName() + " is not a BasicCallGraph.");
		}

		this.sdg = sdg;
		this.cg = (BasicCallGraph<?>) tmp;
		throw new NotImplementedException();
	}

	public static void compute(final SDGBuilder sdg, final IProgressMonitor progress)
			throws CancelException, UnsoundGraphException {
		final SimpleStaticInitializers si = new SimpleStaticInitializers(sdg);
		si.run(progress);
	}

	private void run(final IProgressMonitor progress) throws CancelException, UnsoundGraphException {
		final Set<CGNode> clinits = findRelevantClinits();

		if (clinits.isEmpty()) { return; }

		// create a pdg that is used as entry point. it calls all clinits and the main method.
		// and it connects all modified static fields by the clinits to main and other clinits.
		fakeRoot = createFakeRootPDG(progress);

		for (final CGNode clinit : clinits) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final PDG pdg = sdg.createAndAddPDG(clinit, progress);
			addFakeCall(pdg);
		}

		// add controlflow loop for all clinits but not the main method.
		// this captures the approximation that we do not know in which order they are excetuted.
		final PDGNode lastClinit = lastCall;
		addFakeCall(sdg.getMainPDG());
		fakeRoot.addEdge(lastClinit, fakeRoot.entry, PDGEdge.Kind.CONTROL_FLOW);
	}

	private void addFakeCall(final PDG callee) {
		if (lastCall == null) {
			lastCall = fakeRoot.entry;
			for (final PDGEdge in : fakeRoot.incomingEdgesOf(fakeRoot.exception)) {
				if (in.kind == PDGEdge.Kind.CONTROL_FLOW) {
					lastCall = in.from;
					break;
				}
			}
		}

		final MethodReference tgt = callee.getMethod().getReference();

		String label = tgt.getSelector().getName().toString() + "()";

		final TypeReference type = tgt.getReturnType();
		lastCall = fakeRoot.createNode(label, Kind.CALL, type);

		final PDGNode[] in = new PDGNode[tgt.getNumberOfParameters()];

		final StringBuffer extLabel = new StringBuffer(label.substring(0, label.length() - 1));

		for (int i = 0; i < in.length; i++) {
			// do not append this pointer to normal param list
			extLabel.append("p" + i);
			if (i + 1 < in.length) {
				extLabel.append(", ");
			}

			final TypeReference pType = tgt.getParameterType(i);
			PDGNode actIn = fakeRoot.createNode("param " + (i + 1) + " [?]", PDGNode.Kind.ACTUAL_IN, pType);
			in[i] = actIn;
		}

		extLabel.append(')');
		lastCall.setLabel(extLabel.toString());

		PDGNode retVal = null;
		if (tgt.getReturnType() != TypeReference.Void) {
			retVal = fakeRoot.createNode("ret 0", PDGNode.Kind.ACTUAL_OUT, type);
		}
		final PDGNode excVal = fakeRoot.createNode("ret _exception_", PDGNode.Kind.ACTUAL_OUT,
				TypeReference.JavaLangException);
		final PDGCallReturn out = new PDGCallReturn(retVal, excVal);

		fakeRoot.addCall(lastCall, in, out);

		// fix control flow
		// set last act-out as start node for control flow to next one.
		if (excVal != null) {
			lastCall = excVal;
		} else if (retVal != null) {
			lastCall = retVal;
		}
	}

	private PDG createFakeRootPDG(final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		if (sdg.getPDGforId(SDGBuilder.PDG_FAKEROOT_ID) != null) {
			throw new IllegalStateException("There is already a pdg with id " + SDGBuilder.PDG_FAKEROOT_ID
					+ ": " + sdg.getPDGforId(SDGBuilder.PDG_FAKEROOT_ID));
		}

		final PDG fakeRoot = PDG.buildDummy(sdg, "*Main*", cg.getFakeWorldClinitNode(), SDGBuilder.PDG_FAKEROOT_ID,
				sdg.cfg.ext, sdg.cfg.out, progress);

		return fakeRoot;
	}

	private Set<CGNode> findRelevantClinits() {
		final Set<CGNode> clinits = new HashSet<CGNode>();
		final Iterator<CGNode> succs = cg.getSuccNodes(cg.getFakeWorldClinitNode());

		while (succs.hasNext()) {
			final CGNode clinit = succs.next();

			if (!isPrimordial(clinit)) {
				// consider only clinits not from api classes
				clinits.add(clinit);
			}
		}

		return clinits;
	}

	private static final boolean isPrimordial(final CGNode n) {
		final IClass cls = n.getMethod().getDeclaringClass();
		final ClassLoaderReference clr = cls.getClassLoader().getReference();
		return clr.getName() == AnalysisScope.PRIMORDIAL;
	}

}

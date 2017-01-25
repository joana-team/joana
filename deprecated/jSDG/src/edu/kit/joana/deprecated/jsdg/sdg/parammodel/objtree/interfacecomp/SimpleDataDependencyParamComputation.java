/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp.InterproceduralInterfaceComputation.InterProcStatus;

/**
 * Adapter class to use old interface computation algorithms with the new
 * GlobalInterfaceComputation interface
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class SimpleDataDependencyParamComputation {

	private SimpleDataDependencyParamComputation() {
	}

	public static void compute(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException {
		if (!sdg.isSimpleDataDependency()) {
			throw new IllegalArgumentException("Use GlobalObjectTreePropagation class for non simple data dependencies.");
		}

		final InterproceduralInterfaceComputation iic = new IICSimple(sdg);

		Collection<PDG> changed = sdg.getAllContainedPDGs();
		do {
			Set<PDG> tmp = new HashSet<PDG>();

			for (PDG calleePdg : changed) {
				Iterator<? extends AbstractPDGNode> it =
					sdg.getPredNodes(calleePdg.getRoot(), EdgeType.CL);

				while (it.hasNext()) {
					AbstractPDGNode callerEntry = it.next();

					// skip root node and non-entry node -> this are the
					// function const expressions at the root level
					if (!(callerEntry instanceof EntryNode)
							|| callerEntry == sdg.getRoot()) {
						continue;
					}

					PDG callerPdg = sdg.getPdgForId(callerEntry.getPdgId());

					assert (callerPdg != null) : "No pdg found for " + callerEntry;
					assert (callerPdg.getParamModel() instanceof ObjTreeParamModel);
					assert (calleePdg.getParamModel() instanceof ObjTreeParamModel);

					ObjTreeParamModel callerModel = (ObjTreeParamModel) callerPdg.getParamModel();
					ObjTreeParamModel calleeModel = (ObjTreeParamModel) calleePdg.getParamModel();

					InterProcStatus stat = iic.calcInterProc(callerPdg, callerModel, calleePdg, calleeModel);

					if (stat.isNormalParamsChanged()) {
						tmp.add(callerPdg);
					}
					if (stat.isRetvalOrExceptionChanged()) {
						tmp.add(calleePdg);
					}

				}
			}

			progress.worked(1);
			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}

			changed = tmp;
		} while (!changed.isEmpty());

		progress.done();
	}

}

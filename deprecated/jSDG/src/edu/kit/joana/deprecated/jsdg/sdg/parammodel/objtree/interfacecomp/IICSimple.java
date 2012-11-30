/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.Set;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ActualInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.FormInOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ObjTreeParamModel;


/**
 * Simple interface computation with no child nodes. Beware: No safe approximation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class IICSimple extends
		InterproceduralInterfaceComputation {

	public IICSimple(SDG sdg) {
		super(sdg);
	}

	/**
	 * Connects the act-in/out nodes of the CallNode call (contained in the
	 * caller PDG) to the matching form-in/out nodes of the PDG calleePdg.
	 * Checks the objecttree of caller and callee for a single call. Iff they
	 * do not match new nodes are created and the status is set to "changed".
	 */
	protected InterProcStatus calcInterProc(PDG callerPdg, ObjTreeParamModel callerModel,
		CallNode call, PDG calleePdg, ObjTreeParamModel calleeModel) {
		InterProcStatus status = new InterProcStatus();

		connectParamsSimple(callerPdg, callerModel, call, calleePdg, calleeModel);

		return status;
	}

	/**
	 * Just connects corresponding parameter nodes (act-in/out to form-in/out).
	 * Only for simpleDataDependency - no fancy objecttrees...
	 * @param callerPdg pdg of the caller
	 * @param call callnode of this pdg with act-in/out nodes
	 * @param calleePdg callee pdg of the called method with form-in/out nodes
	 * @return
	 */
	private void connectParamsSimple(PDG callerPdg, ObjTreeParamModel callerModel,
			CallNode call, PDG calleePdg, ObjTreeParamModel calleeModel) {
		Set<ActualInOutNode> actInNodes = callerModel.getRootActIns(call);
		Set<FormInOutNode> formInNodes = calleeModel.getRootFormIns();

		//connect form-in with act-in
		for (FormInOutNode fIn : formInNodes) {
			if (fIn.getField() == null) {
				ActualInOutNode actIn = getCorrespondantSameInOut(actInNodes, fIn);

				assert (actIn != null);

				callerPdg.addNode(fIn);
				SDG.addParameterInDependency(callerPdg, actIn, fIn);
			}
		}

		// stop propagating return parameters iff a new thread is started
		if (isStartOfANewThread(callerPdg, calleePdg)) {
			return;
		}

		Set<ActualInOutNode> actOutNodes = callerModel.getRootActOuts(call);
		Set<FormInOutNode> formOutNodes = calleeModel.getRootFormOuts();

		//connect form-out with act-out
		for (FormInOutNode fOut : formOutNodes) {
			if (fOut.getField() == null && !(fOut.isExit() && fOut.isVoid())) {
				ActualInOutNode actOut = getCorrespondantSameInOut(actOutNodes, fOut);

				// create missing act-out node
				if (actOut == null && !fOut.isExit()) {
					actOut = callerModel.makeActualInOut(fOut,
						call.getInstruction(), fOut.getPointsTo());
					callerModel.addRootActOut(call, actOut);
					callerPdg.addExpressionControlDependency(call, actOut);
				}

				assert (actOut != null || fOut.isExit());

				/**
				 * actOut may be null because return values of methods can be ignored
				 */
				if (actOut != null) {
					SDG.addParameterOutDependency(calleePdg, fOut, actOut);
				}
			}
		}
	}

}

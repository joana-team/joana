/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;

import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.Call;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * An example:
 * When modeling "void String.<init>(int i)" as a stub there is the problem that
 * as the method is void we do not get any dependency of the String object initialized to the
 * parameter that is used to initialize it (int i).
 *
 * The workaround for now is to search for all callsites of stub initializers and
 * add a dependency from all actual-in nodes of the initializer call to the nodes
 * who access the this pointer of the newly created object. This way the dependency
 * of the immutable object to its initial value is established.
 *
 * I'm not sure for now if data dependencies starting from act-in nodes may break
 * summary computation. Perhaps we have to create a void return act-out node for this.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FixStubInitializerDependencies {

	private final SDG sdg;

	public static void apply(SDG sdg, IProgressMonitor progress) throws PDGFormatException, CancelException, WalaException {
		FixStubInitializerDependencies fix = new FixStubInitializerDependencies(sdg);
		fix.compute(progress);
	}

	private FixStubInitializerDependencies(SDG sdg) {
		this.sdg = sdg;
	}

	private void compute(IProgressMonitor progress) throws PDGFormatException, CancelException, WalaException {

		// for all calls to initializers stubs we create the missing dependency
		// from the parameters that are passed to the constructor to the this
		// pointer of the created object
		for (Call call : sdg.getAllCalls()) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

			IMethod target = call.callee.getMethod();
			if (target.isInit() && sdg.isImmutableClass(target)) {
				for (CallNode cn : call.caller.getCallsTo(call.callee)) {
//					IParamSet<? extends AbstractParameterNode> refs =
//						call.caller.getParamModel().getRefParams(cn);
					int thisPtr = cn.getInstruction().getUse(0);

					assert (thisPtr >= 0);

					// search all nodes using ssa var no. thisPtr
					Iterator<SSAInstruction> it =  call.caller.getIR().iterateAllInstructions();
					while (it.hasNext()) {
						SSAInstruction ssa = it.next();
						if (ssa == cn.getInstruction()) {
							continue;
						}

						for (int i = 0; i < ssa.getNumberOfUses(); i++) {
							if (ssa.getUse(i) == thisPtr) {
								List<AbstractPDGNode> nodes = call.caller.getNodesForInstruction(ssa);
								if (nodes != null && !nodes.isEmpty()) {
									IParamModel callerModel = call.caller.getParamModel();

									AbstractParameterNode aOut =
										callerModel.getMatchingActualNode(cn, call.callee, call.callee.getExit());
									if (aOut == null) {
										aOut = callerModel.makeVoidActualOut(cn, call.callee);
									}

									for (AbstractPDGNode node : nodes) {
										if (aOut.isRoot()) {
											call.caller.addDataDependency(aOut, node);
										} else {
											call.caller.addHeapDataDependency(aOut, node);
										}
//										for (AbstractParameterNode pn : refs) {
//											call.caller.addDataDependency(pn, node);
//										}
									}
								} else {
									Log.warn("No nodes for " + Util.prettyShortInstruction(ssa) + " found!");
								}
							}
						}
					}
				}
			}

			if (progress.isCanceled()) {
				throw CancelException.make("Cancel during fix of stub initializer dependencies.");
			}
		}
	}

}

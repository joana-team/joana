/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ExceptionExitNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ExitNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphHeapParameterFactory;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.util.SourceLocation;

/**
 * Intraprocedural part of the object graph parameter model.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
abstract class ObjGraphIntraProcParamComputation implements IParamComputation {

	private final boolean isFieldSensitive;
	private final ObjGraphHeapParameterFactory heap;

	public ObjGraphIntraProcParamComputation(boolean fieldSensitive) {
		this.isFieldSensitive = fieldSensitive;
		this.heap = new ObjGraphHeapParameterFactory();
	}

	public final void computeModRef(PDG pdg, IProgressMonitor monitor)
			throws PDGFormatException {
		IParamModel model = pdg.getParamModel();
		if (!(model instanceof ObjGraphParamModel)) {
			throw new IllegalArgumentException(
				"Mod/Ref can only be computed for an WalaParamModel: " +
				model.getClass().getName());
		}

		ObjGraphParamModel wModel = (ObjGraphParamModel) model;

		if (pdg.getIR() != null && !pdg.isStub()) {
			createFormInFromSignature(pdg, wModel);

			visitGetSetInvoke(pdg, wModel);

			registerReturnAndExceptionalReturn(pdg, wModel);
		} else {
			createFormalNodesForEmptyIR(pdg, wModel);
		}
	}

	private void registerReturnAndExceptionalReturn(PDG pdg, ObjGraphParamModel model) {
		// add output dependencies of return statements
		if (!model.getExit().isVoid()) {
			createReturnDataDependencies(pdg, model);
		}

		// add throw statements
		if (!pdg.isIgnoreExceptions()) {
			if (!pdg.getThrows().isEmpty()) {
				createExceptionDataDepencendies(pdg, model);
			} else if (pdg.containsCatch()) {
				ExplodedControlFlowGraph explCfg = ExplodedControlFlowGraph.make(pdg.getIR());

				registerDefinitelyCatched(pdg, explCfg);
			}
		}

	}

	private void registerDefinitelyCatched(PDG pdg, ExplodedControlFlowGraph explCfg) {
		for (SSAInstruction instr : pdg.getIR().getInstructions()) {
			if (instr != null) {
				List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(instr);
				if (nodes != null && !nodes.isEmpty()) {
					AbstractPDGNode catchn = findCatchFor(pdg, explCfg, instr);
					if (catchn != null) {
						for (AbstractPDGNode n : nodes) {
							n.setCatchedBy(catchn);
						}
					}
				}
			}
		}

		for (CallNode cn : pdg.getAllCalls()) {
			final AbstractPDGNode catcher = cn.getCatcher();
			if (catcher != null) {
				for (AbstractParameterNode param : pdg.getParamModel().getModParams(cn)) {
					if (param.isException()) {
						param.setCatchedBy(catcher);
					}
				}
			}
		}
	}

	private void createReturnDataDependencies(PDG pdg, ObjGraphParamModel model) {
		// hashcode e.g. is a synthetic method with a primitive return type
		// TODO check what to do with synthetic methods that have a
		// non-primitive return type -> we issue a warning for now and skip them
		//
		// There may be Methods that do have an empty return set and are neither
		// primitive nor synthetic. This happens if they throw an exception:
	    // T childValue(T parentValue) {
	    //     throw new UnsupportedOperationException();
	    // }
		ExitNode exit = model.getExit();

		/*
		 * Constructors always have the returntype void, but in their case
		 * the exit node models the returning of the created object. So the
		 * exit node is neiter void nor primitive but the returns are. This
		 * would screw up the code below.
		 */
		if (!pdg.getMethod().isInit()) {
			for (NormalNode ret : pdg.getReturns()) {

				SSAInstruction instr = pdg.getInstructionForNode(ret);
				int def = instr.getUse(0);

				assert (def >= 0);

				pdg.addDataDependency(ret, exit, def);

				if (!exit.isPrimitive()) {
					PointerKey pk = pdg.getPointerKey(def);
					Set<PointerKey> pKeys = HashSetFactory.make();
					pKeys.add(pk);

					exit.mergePointerKeys(pKeys);
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(def);
					exit.mergePointsTo(pts);
				}
			}
		}
	}

	private void createExceptionDataDepencendies(PDG pdg, ObjGraphParamModel model) {
		// compute merged points-to set of all exception returns
		OrdinalSet<InstanceKey> pts = null;
		for (NormalNode node : pdg.getThrows()) {
			SSAInstruction instr = pdg.getInstructionForNode(node);

			assert (instr instanceof SSAAbstractThrowInstruction);

			if (instr.getDef() < 0) {
				continue;
			}

			if (pts == null) {
				pts= pdg.getPointsToSet(instr.getDef());
			} else {
				pts = OrdinalSet.unify(pts,	pdg.getPointsToSet(instr.getDef()));
			}
		}

		for (CallNode call : pdg.getAllCalls()) {
			for (AbstractParameterNode param : pdg.getParamModel().getModParams(call)) {
				if (param.isException()) {
					SSAInvokeInstruction instr = call.getInstruction();
					if (instr.getException() < 0) {
						continue;
					}

					if (pts == null) {
						pts= pdg.getPointsToSet(instr.getException());
					} else {
						pts = OrdinalSet.unify(pts,	pdg.getPointsToSet(instr.getException()));
					}
				}
			}
		}

		if (pts == null) {
			Log.info("No points-to set for exceptions in " + pdg.getMethod() +
				"found -> no excpetion node created.");
			return;
		}

		ExceptionExitNode exitThrow = model.getExceptionExit();
		exitThrow.mergePointsTo(pts);

		pdg.addParameterChildDependency(pdg.getRoot(), exitThrow);

		ExplodedControlFlowGraph explCfg = ExplodedControlFlowGraph.make(pdg.getIR());

		if (pdg.containsCatch()) {
			registerDefinitelyCatched(pdg, explCfg);
		}

		for (NormalNode node : pdg.getThrows()) {
			SSAInstruction instr = pdg.getInstructionForNode(node);

			assert (instr instanceof SSAAbstractThrowInstruction);
			int exc = ((SSAAbstractThrowInstruction) instr).getException();

			AbstractPDGNode catcher = node.getCatcher();
			if (catcher != null) {
				pdg.addDataDependency(node, catcher, exc);
			} else {
				addDataDepToCatchAlongControlFlow(pdg, explCfg, instr, node);
				pdg.addDataDependency(node, exitThrow, exc);
			}
		}

		for (CallNode call : pdg.getAllCalls()) {
			for (AbstractParameterNode param : pdg.getParamModel().getModParams(call)) {
				if (param.isException()) {
					SSAInvokeInstruction instr = call.getInstruction();

					int exc = instr.getException();


					AbstractPDGNode catcher = param.getCatcher();
					if (catcher != null) {
						pdg.addDataDependency(param, catcher, exc);
					} else {
						addDataDepToCatchAlongControlFlow(pdg, explCfg, instr, param);
						pdg.addDataDependency(param, exitThrow, exc);
					}

//					boolean connectionToExit = addDataDepToCatchAlongControlFlow(pdg, explCfg, instr, param);
//
//					if (connectionToExit) {
//						pdg.addDataDependency(param, exitThrow, exc);
//					}
				}
			}
		}
	}

	private AbstractPDGNode findCatchFor(PDG pdg, ExplodedControlFlowGraph cfg, SSAInstruction instr) {
		IExplodedBasicBlock bbNode = cfg.getBlockForInstruction(instr.iindex);

		IExplodedBasicBlock catchAll = null;
		for (IExplodedBasicBlock bbSucc : cfg.getExceptionalSuccessors(bbNode)) {
			if (bbSucc.isCatchBlock()) {
				if (catchAll == null) {
					catchAll = bbSucc;
				} else {
					catchAll = null;
					break;
				}
			} else {
				catchAll = null;
				break;
			}
		}

		if (catchAll != null) {
			SSAGetCaughtExceptionInstruction catchi = catchAll.getCatchInstruction();
//				final int excVal = catchi.getException();
//				if (excVal >= 0) {
//					final String excStr = pdg.getIR().getSymbolTable().getValueString(excVal);
//					if (TypeReference.JavaLangThrowable.getName().toString().equals(excStr) ||
//							TypeReference.JavaLangException.getName().toString().equals(excStr)) {
//						catchesAll = true;
//					}
//				}

			List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(catchi);
			if (nodes != null) {
				AbstractPDGNode catchn = nodes.iterator().next();

				assert (catchn instanceof CatchNode) : "Node of catch instruction is not a catch node: " + catchn;

				return catchn;
			}
		}

		return null;
	}

	private void addDataDepToCatchAlongControlFlow(PDG pdg, ExplodedControlFlowGraph cfg, SSAInstruction instr, AbstractPDGNode exc) {
//		SSAInstruction[] iis = cfg.getInstructions();
		IExplodedBasicBlock bbNode = cfg.getBlockForInstruction(instr.iindex);
//		for (int index = 0; index < iis.length; index++) {
//			if (iis[index] == instr) {
//				bbNode = cfg.getBlockForInstruction(index);
//				break;
//			}
//		}

		if (bbNode == null) {
			Log.warn("No cfg node found for " + instr);
//			return true;
		}

//		boolean connectionToExit = true;

		for (IExplodedBasicBlock bbSucc : cfg.getExceptionalSuccessors(bbNode)) {
			if (bbSucc.isCatchBlock()) {
				SSAGetCaughtExceptionInstruction catchi = bbSucc.getCatchInstruction();
//				final int excVal = catchi.getException();
//				if (excVal >= 0) {
//					final String excStr = pdg.getIR().getSymbolTable().getValueString(excVal);
//					if (TypeReference.JavaLangThrowable.getName().toString().equals(excStr) ||
//							TypeReference.JavaLangException.getName().toString().equals(excStr)) {
//						connectionToExit = false;
//						exc.setCatchedBy(exc);
//					}
//				}

				List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(catchi);
				if (nodes != null) {
					AbstractPDGNode catchn = nodes.iterator().next();

					assert (catchn instanceof CatchNode) : "Node of catch instruction is not a catch node: " + catchn;

					pdg.addDataDependency(exc, catchn);
				}
			}
		}

//		return connectionToExit;
	}

	private void visitGetSetInvoke(PDG pdg, ObjGraphParamModel model) {
		ObjGraphBasicNodeCreationVisitor visitor =
			new ObjGraphBasicNodeCreationVisitor(pdg, model);

		for (Iterator<SSAInstruction> it = pdg.getIR().iterateNormalInstructions(); it.hasNext();) {
			SSAInstruction instr = it.next();
			if (instr != null) {
				instr.visit(visitor);
			}
		}
	}

	/**
	 * Create dummy nodes with dependencies to return node for pdgs with an empty ir.
	 * @param pdg
	 * @param model
	 */
	private void createFormalNodesForEmptyIR(PDG pdg, ObjGraphParamModel model) {
		int stopAt = pdg.getMethod().getNumberOfParameters();
		FormInLocalNode params[] = new FormInLocalNode[stopAt];

		for (int paramNum = 0; paramNum < stopAt; paramNum++) {
			TypeReference tref = pdg.getMethod().getParameterType(paramNum);
			FormInLocalNode fInParam = model.makeFormInDummy(tref.isPrimitiveType(), tref);
			if (!pdg.getMethod().isStatic() && paramNum == 0) {
				fInParam.setLabel("this");
			} else {
				fInParam.setLabel("param " + paramNum);
			}

			params[paramNum] = fInParam;

			model.addRef(fInParam);

			pdg.addParameterChildDependency(pdg.getRoot(), fInParam);
			pdg.addDataDependency(fInParam, pdg.getExit());
		}

		model.setParameterIn(params);
	}

	private void createFormInFromSignature(final PDG pdg, final ObjGraphParamModel model) {
		final int stopAt = pdg.getMethod().getNumberOfParameters();
		final FormInLocalNode params[] = new FormInLocalNode[stopAt];
		final boolean isStatic = pdg.getMethod().isStatic();

		for (int paramNum = 0; paramNum < stopAt; paramNum++) {
			final int ssaVar = pdg.getIR().getParameter(paramNum);
			final TypeReference tref = pdg.getMethod().getParameterType(paramNum);
			final PointerKey pk = pdg.getPointerKey(ssaVar);
			final Set<PointerKey> pKeys = HashSetFactory.make();
			pKeys.add(pk);

			final int displayParamNum = (isStatic ? paramNum + 1 : paramNum);
			final OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(ssaVar);
			final FormInLocalNode fInParam =
					model.makeFormInLocal(tref.isPrimitiveType(), tref, pKeys, pts, paramNum, displayParamNum);

			if (!isStatic && paramNum == 0) {
				fInParam.setLabel("this");
			} else {
				fInParam.setLabel("param " + displayParamNum);
			}

			params[paramNum] = fInParam;

			model.addRef(fInParam);

			pdg.addDefinesVar(fInParam, ssaVar);
			pdg.addParameterChildDependency(pdg.getRoot(), fInParam);

			try {
				SourcePosition pos = pdg.getMethod().getParameterSourcePosition(paramNum);
				if (pos != null) {
					SourceLocation loc = SourceLocation.getLocation(pdg.getSourceFileName(), pos.getFirstLine(),
							(pos.getFirstCol() >= 0 ? pos.getFirstCol() : 0), pos.getLastLine(),
							(pos.getLastCol() >= 0 ? pos.getLastCol() : 0));
					pdg.addLocation(fInParam, loc);
				}
			} catch (InvalidClassFileException e) {
				// no location -> no problem...
			}
		}

		model.setParameterIn(params);
	}

	public final void connectCallParamNodes(PDG caller, CallNode call, PDG callee)
			throws PDGFormatException {
		ObjGraphParamModel callerModel = (ObjGraphParamModel) caller.getParamModel();
		ObjGraphParamModel calleeModel = (ObjGraphParamModel) callee.getParamModel();

		// connect in params
		FormInLocalNode paramIn[] = calleeModel.getParameters();
		ActInLocalNode aIns[] = callerModel.getParameterIn(call);

		assert assertParams(paramIn, aIns, caller, call, callee);

		for (int paramNum = 0; paramNum < paramIn.length; paramNum++) {
			FormInLocalNode fIn = paramIn[paramNum];
			ActInLocalNode aIn = aIns[paramNum];
			callerModel.addParameterInOutRelation(call, callee, fIn, aIn);
			SDG.addParameterInDependency(caller, aIn, fIn);
		}

		// connect exit && exception exit
		ActOutLocalNode aOutReturn = callerModel.getParameterReturn(call);
		if (aOutReturn != null) {
			ExitNode exit = calleeModel.getExit();
			callerModel.addParameterInOutRelation(call, callee, exit, aOutReturn);
			SDG.addParameterOutDependency(callee, exit, aOutReturn);
		}

		ActOutLocalNode aOutExc = callerModel.getParameterException(call);
		if (aOutExc != null) {
			ExceptionExitNode excExit = calleeModel.getExceptionExit();
			if (excExit != null) {
				// when the method in question does not throw any exception
				// we do not need to model them and it can be savely ignored
				Log.debug("Ignoring exception at callsite becaue no exception in callee: " + callee);
				callerModel.addParameterInOutRelation(call, callee, excExit, aOutExc);
				SDG.addParameterOutDependency(callee, excExit, aOutExc);
			}
		}

		connectHeapParamNodes(caller, call, callee);
	}

	private static boolean assertParams(FormInLocalNode paramIn[], ActInLocalNode aIns[], PDG caller, CallNode call, PDG callee) {
		assert (paramIn != null) : "Caller: " + caller + "\nCallee: " + callee;
		if (aIns == null) {
			String msg = " form-ins: ";
			for (FormInLocalNode fIn : paramIn) {
				msg += fIn.toString() + " ";
			}

			assert (false) : "No actIns for " + call + msg;
		}

		assert (paramIn.length == aIns.length);

		return true;
	}

	public static final void connectHeapParamNodes(PDG caller, CallNode call, PDG callee) {
		ObjGraphParamModel callerModel = (ObjGraphParamModel) caller.getParamModel();
		ObjGraphParamModel calleeModel = (ObjGraphParamModel) callee.getParamModel();

		// connect the remaining parameters
		ObjGraphParamSet formIns = calleeModel.getRefParams();
		for (ObjGraphParameter fIn : formIns) {
			ActNode aIn = callerModel.getMatchingActualNode(call, callee, fIn);
			if (aIn == null) {
				// create node && add dependency
				aIn = callerModel.makeMatchingActNode(call, fIn);
				callerModel.addRef(call, aIn);
				callerModel.addParameterInOutRelation(call, callee, (FormNode) fIn, aIn);
				caller.addParameterChildDependency(call, aIn);

				SDG.addParameterInDependency(caller, aIn, fIn);
			}
		}

		ObjGraphParamSet formOuts = calleeModel.getModParams();
		for (ObjGraphParameter fOut : formOuts) {
			if (fOut.isException() || fOut.isExit()) {
				// skip nodes already handled
				continue;
			}

			ActNode aOut = callerModel.getMatchingActualNode(call, callee, fOut);
			if (aOut == null) {
				// create node && add dependency
				aOut = callerModel.makeMatchingActNode(call, fOut);
				callerModel.addMod(call, aOut);
				callerModel.addParameterInOutRelation(call, callee, (FormNode) fOut, aOut);
				caller.addParameterChildDependency(call, aOut);

				SDG.addParameterOutDependency(callee, fOut, aOut);
			}
		}
	}

	public final IParamModel getBasicModel(PDG pdg) {
		return new ObjGraphParamModel(pdg, heap, isFieldSensitive);
	}

}

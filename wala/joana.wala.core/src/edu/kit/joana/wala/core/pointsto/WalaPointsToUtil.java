/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.pointsto;

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.EmptyIterator;

import edu.kit.joana.wala.flowless.wala.ObjSensContextSelector;
import edu.kit.joana.wala.flowless.wala.ObjSensZeroXCFABuilder;

class FallbackContextInterpreter implements SSAContextInterpreter {

	private SSAContextInterpreter shrikeCI;

	public FallbackContextInterpreter(SSAContextInterpreter shrikeCI) {
		this.shrikeCI = shrikeCI;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateNewSites(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
		if (node.getMethod() instanceof SyntheticMethod || node.getMethod() instanceof ShrikeCTMethod) {
			return shrikeCI.iterateNewSites(node);
		} else {
			IR ir = getIR(node);
		    if (ir == null) {
		      return EmptyIterator.instance();
		    } else {
		      return ir.iterateNewSites();
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateFieldsRead(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
		return shrikeCI.iterateFieldsRead(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateFieldsWritten(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
		return shrikeCI.iterateFieldsWritten(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter#iterateCallSites(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
		if (node.getMethod() instanceof SyntheticMethod || node.getMethod() instanceof ShrikeCTMethod) {
			return shrikeCI.iterateCallSites(node);
		} else {
			IR ir = getIR(node);
		    if (ir == null) {
		      return EmptyIterator.instance();
		    } else {
		      return ir.iterateCallSites();
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.IClass)
	 */
	@Override
	public boolean recordFactoryType(CGNode node, IClass klass) {
		return shrikeCI.recordFactoryType(node, klass);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter#understands(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public boolean understands(CGNode node) {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getIR(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public IR getIR(CGNode node) {
		return shrikeCI.getIR(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getDU(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public DefUse getDU(CGNode node) {
		return shrikeCI.getDU(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public int getNumberOfStatements(CGNode node) {
		return shrikeCI.getNumberOfStatements(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
		return shrikeCI.getCFG(n);
	}
}

public final class WalaPointsToUtil {

	private WalaPointsToUtil() {}

	public static CallGraphBuilder makeContextFreeType(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope) {
	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }

	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, null, ZeroXInstanceKeys.NONE);
	}

	public static CallGraphBuilder makeContextSensSite(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

	    return ZeroXCFABuilder.make(cha, options, cache, null, new FallbackContextInterpreter(new DefaultSSAInterpreter(options, cache)),
	    		ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    		| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	public static CallGraphBuilder makeObjectSens(AnalysisOptions options, AnalysisCache cache,
		      IClassHierarchy cha, AnalysisScope scope, ObjSensContextSelector.MethodFilter filter) {

	    if (options == null) {
	      throw new IllegalArgumentException("options is null");
	    }
	    Util.addDefaultSelectors(options, cha);
	    Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
	    final ContextSelector defaultSelector = new DefaultContextSelector(options, cha);

		return ObjSensZeroXCFABuilder.make(cha, options, cache, new ObjSensContextSelector(defaultSelector, filter),
				new FallbackContextInterpreter(new DefaultSSAInterpreter(options, cache)),
				ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC | ZeroXInstanceKeys.SMUSH_MANY
	    		| ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

}

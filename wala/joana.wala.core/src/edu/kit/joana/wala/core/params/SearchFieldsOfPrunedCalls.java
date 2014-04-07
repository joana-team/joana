/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.util.PrettyWalaNames;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class SearchFieldsOfPrunedCalls {

	private final SDGBuilder sdg;

	private SearchFieldsOfPrunedCalls(final SDGBuilder sdg) {
		this.sdg = sdg;
	}

	public static LinkedList<Set<ParameterField>> compute(final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		final SearchFieldsOfPrunedCalls si = new SearchFieldsOfPrunedCalls(sdg);
		return si.run(progress);
	}

	private LinkedList<Set<ParameterField>> run(final IProgressMonitor progress) throws CancelException {
		final Set<CGNode> containsPrunedCalls = new HashSet<CGNode>();
		final CallGraph prunedCG = sdg.getWalaCallGraph();
		final CallGraph nonPrunedCG = sdg.getNonPrunedWalaCallGraph();
		for (final CGNode n : prunedCG) {
			if (prunedCG.getSuccNodeCount(n) == 0 && nonPrunedCG.getSuccNodeCount(n) > 0) {
				containsPrunedCalls.add(n);
			}
		}
		
		final ParameterFieldFactory pfact = sdg.getParameterFieldFactory();
		final IClassHierarchy cha = sdg.getClassHierarchy();
		final FieldAccessVisitor fav = new FieldAccessVisitor(pfact, cha, null);
		
		// detect fields in non-pruned cg
		for (final CGNode n : prunedCG) {
			final IR ir = n.getIR();
			if (ir != null) {
				ir.visitNormalInstructions(fav);
			}
		}
		
		final Set<ParameterField> appFields = fav.getFields();

//		for (final ParameterField f : appFields) {
//			System.out.println("app field: " + f);
//		}
		
		final FieldAccessVisitor favPruned = new FieldAccessVisitor(pfact, cha, appFields);

		for (final CGNode n : nonPrunedCG) {
			if (prunedCG.containsNode(n)) {
				continue;
			}
			
			final IR ir = n.getIR();
			if (ir != null) {
				ir.visitNormalInstructions(favPruned);
			}
		}
		
		final Set<ParameterField> prunedFields = favPruned.getFields();
		
//		for (final ParameterField f : prunedFields) {
//			System.out.println("pruned field: " + f);
//		}
		
		final LinkedList<Set<ParameterField>> subsets = new LinkedList<Set<ParameterField>>();
		
		for (final CGNode n : containsPrunedCalls) {
			final Set<ParameterField> pfOfN = findPrunedFieldsOf(n, appFields, prunedCG, nonPrunedCG, pfact, cha);
			subsets.add(pfOfN);
//			System.out.print("pruned of " + PrettyWalaNames.methodName(n.getMethod()) + ": ");
//			for (final ParameterField p : pfOfN) {
//				System.out.print(p + "; ");
//			}
//			System.out.println();
		}
		
		final LinkedList<Set<ParameterField>> partitions = computePartitions(prunedFields, subsets);
//		for (final Set<ParameterField> part : partitions) {
//			System.out.print("part: ");
//			for (final ParameterField p : part) {
//				System.out.print(p + "; ");
//			}
//			System.out.println();
//		}
		
		System.out.println(appFields.size() + " application fields + (" + prunedFields.size() + " library fields -> "
				+ partitions.size() + " partitions)");
		return partitions;
	}
	
	private static LinkedList<Set<ParameterField>> computePartitions(final Set<ParameterField> prunedFields,
			LinkedList<Set<ParameterField>> subsets) {
		final LinkedList<Set<ParameterField>> partitions = new LinkedList<Set<ParameterField>>();
		partitions.add(prunedFields);
		
		for (final Set<ParameterField> set : subsets) {
			final List<Set<ParameterField>> toRemove = new LinkedList<Set<ParameterField>>();
			final List<Set<ParameterField>> toAdd = new LinkedList<Set<ParameterField>>();
			
			for (final Set<ParameterField> p : partitions) {
				boolean relevant = false;
				for (final ParameterField pf : set) {
					if (p.contains(pf)) {
						relevant = true;
						break;
					}
				}
				
				if (relevant) {
					final Set<ParameterField> splitNotContained = new HashSet<ParameterField>();
					final Set<ParameterField> splitContained = new HashSet<ParameterField>();
					for (final ParameterField pf : p) {
						if (set.contains(pf)) {
							splitContained.add(pf);
						} else {
							splitNotContained.add(pf);
						}
					}
					
					if (!splitNotContained.isEmpty()) {
						toRemove.add(p);
						toAdd.add(splitContained);
						toAdd.add(splitNotContained);
					}
				}
			}
			
			if (!toRemove.isEmpty()) {
				partitions.removeAll(toRemove);
				partitions.addAll(toAdd);
			}
		}
		
		return partitions;
	}
	
	private static Set<ParameterField> findPrunedFieldsOf(final CGNode n, final Set<ParameterField> appFields,
			final CallGraph prunedCG, final CallGraph nonPrunedCG, final ParameterFieldFactory pfact,
			final IClassHierarchy cha) {
		final Set<CGNode> visited = new HashSet<CGNode>();
		final LinkedList<CGNode> worklist = new LinkedList<CGNode>();
		
		final FieldAccessVisitor fav = new FieldAccessVisitor(pfact, cha, appFields);
		
		visited.add(n);
		for (final Iterator<CGNode> it = nonPrunedCG.getSuccNodes(n); it.hasNext();) {
			final CGNode succ = it.next();
			if (!visited.contains(succ) && !prunedCG.containsNode(succ)) {
				worklist.add(succ);
			}
		}

		while (!worklist.isEmpty()) {
			final CGNode cur = worklist.pop();
			visited.add(cur);
			final IR ir = cur.getIR();
			if (ir != null) {
				ir.visitNormalInstructions(fav);
			}

			for (final Iterator<CGNode> it = nonPrunedCG.getSuccNodes(cur); it.hasNext();) {
				final CGNode succ = it.next();
				if (!visited.contains(succ) && !prunedCG.containsNode(succ)) {
					worklist.add(succ);
				}
			}
		}
		
		return fav.getFields();
	}
	
	private static class FieldAccessVisitor extends SSAInstruction.Visitor {
	    
		private final ParameterFieldFactory pfact;
		private final IClassHierarchy cha;
		private final Set<ParameterField> toIgnore;
		private final Set<ParameterField> fields = new HashSet<ParameterField>();
		
		private FieldAccessVisitor(final ParameterFieldFactory pfact, final IClassHierarchy cha,
				final Set<ParameterField> toIgnore) {
			this.pfact = pfact;
			this.cha = cha;
			this.toIgnore = toIgnore;
		}
		
		@Override
	    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			final ParameterField pf = pfact.getArrayField(instruction.getElementType());
			if (toIgnore == null || !toIgnore.contains(pf)) {
				fields.add(pf);
			}
	    }

	    @Override
	    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
	    	final ParameterField pf = pfact.getArrayField(instruction.getElementType());
			if (toIgnore == null || !toIgnore.contains(pf)) {
				fields.add(pf);
			}
	    }

	    @Override
	    public void visitGet(SSAGetInstruction instruction) {
			final FieldReference f = instruction.getDeclaredField();
			final IField ifield = cha.resolveField(f);

			if (ifield != null) {
				final ParameterField pf = pfact.getObjectField(ifield);
				if (toIgnore == null || !toIgnore.contains(pf)) {
					fields.add(pf);
				}
			}
	    }

	    @Override
	    public void visitPut(SSAPutInstruction instruction) {
			final FieldReference f = instruction.getDeclaredField();
			final IField ifield = cha.resolveField(f);

			if (ifield != null) {
				final ParameterField pf = pfact.getObjectField(ifield);
				if (toIgnore == null || !toIgnore.contains(pf)) {
					fields.add(pf);
				}
			}
	    }

	    public Set<ParameterField> getFields() {
	    	return Collections.unmodifiableSet(fields);
	    }
	    
	}
	
}

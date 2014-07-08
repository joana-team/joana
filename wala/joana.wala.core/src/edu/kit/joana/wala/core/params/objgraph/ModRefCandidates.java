/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.params.objgraph.ModRefSSAVisitor.CandidateConsumer;
import edu.kit.joana.wala.core.params.objgraph.ModRefSSAVisitor.PointsTo;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory;
import edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate;
import edu.kit.joana.wala.core.params.objgraph.candidates.UniqueParameterCandidate;

/**
 *
 * Intraprocedural and interprocedural modref candidates.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefCandidates implements Iterable<CGNode> {

	private final Map<CGNode, Collection<ModRefFieldCandidate>> all = new HashMap<CGNode, Collection<ModRefFieldCandidate>>();
	private final CandidateFactory candFact;
	private final ParameterFieldFactory paramFact;
	private final PointerAnalysis<InstanceKey> pa;
	private final SingleCandidateConsumer single;
	private final ModRefSSAVisitor singleVisitor;
	private final SingleCandidatePointsTo singlePts;
	private final boolean doStaticFields;

	private ModRefCandidates(final CandidateFactory candFact, final ParameterFieldFactory paramFact,
			final PointerAnalysis<InstanceKey> pa, final boolean doStaticFields) {
		this.candFact = candFact;
		this.paramFact = paramFact;
		this.pa = pa;
		this.single = new SingleCandidateConsumer(candFact);
		this.singlePts = new SingleCandidatePointsTo(pa.getHeapModel(), pa);
		this.doStaticFields = doStaticFields;
		this.singleVisitor = new ModRefSSAVisitor(single, paramFact, singlePts, pa.getClassHierarchy(), doStaticFields);
	}

	public static ModRefCandidates computeIntracProc(final ParameterFieldFactory paramFact,
			final CandidateFactory candFact, final CallGraph cg, final PointerAnalysis<InstanceKey> pa,
			final boolean doStaticFields, final IProgressMonitor progress) throws CancelException {
		final ModRefCandidates modref = new ModRefCandidates(candFact, paramFact, pa, doStaticFields);

		modref.run(cg, progress);

		return modref;
	}

	/**
	 * Interface for interprocedural propagation of mod-ref side-effects.
	 *
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 *
	 */
	public interface InterProcCandidateModel extends Iterable<ModRefFieldCandidate> {
		/**
		 * Add and merge a candidate from a called method to the modref of this method. The candidate toAdd is not
		 * altered.
		 * @param toAdd Candidate from a called method propagated to the modref of this method.
		 */
		public void addCandidate(ModRefFieldCandidate toAdd);

		/**
		 * Merge all given candidates to a single candidate.
		 * @param toMerge A list of candidates that should be merged.
		 */
		public ModRefFieldCandidate mergeCandidates(Iterable<ModRefFieldCandidate> toMerge, int markWithFlags);

		/**
		 * Remove a candidate from this model
		 * @param toRemove Candidate that should be removed. Must be contained in this model.
		 */
		public void removeCandidate(ModRefFieldCandidate toRemove);

		/**
		 * Returns the number of modref candidates in this model.
		 * @return The number of modref candidates in this model.
		 */
		public int size();

		/**
		 * Checks is a modref candidate is part of this model.
		 * @param n A modref candidate.
		 * @return <tt>true</tt> if the candidate is contained in this model.
		 */
		public boolean contains(ModRefCandidate n);
	}

	private static class CGNodeCandidates extends AbstractCollection<ModRefFieldCandidate> implements CandidateConsumer,
		InterProcCandidateModel {

		private final CandidateFactory fact;
		private Map<ParameterCandidate, ModRefFieldCandidate> cands = new HashMap<ParameterCandidate, ModRefFieldCandidate>();
		private Set<ModRefFieldCandidate> all = new HashSet<ModRefFieldCandidate>();

		private CGNodeCandidates(final CandidateFactory fact) {
			this.fact = fact;
		}

		@Override
		public void addModCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> pts) {
			final ParameterCandidate pc = fact.findOrCreateUnique(basePts, field, pts);

			if (cands.containsKey(pc)) {
				final ModRefFieldCandidate mrc = cands.get(pc);
				mrc.setMod();
			} else {
				final ModRefFieldCandidate mrc = new ModRefFieldCandidate(true, false, pc);
				if (pc.isMerged()) {
					mrc.flags |= ObjGraphParams.FLAG_MERGE_INITIAL_LIBRARY;
				}
				cands.put(pc, mrc);
				all.add(mrc);
			}
		}

		@Override
		public void addRefCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> pts) {
			final ParameterCandidate pc = fact.findOrCreateUnique(basePts, field, pts);

			if (cands.containsKey(pc)) {
				final ModRefFieldCandidate mrc = cands.get(pc);
				mrc.setRef();
			} else {
				final ModRefFieldCandidate mrc = new ModRefFieldCandidate(false, true, pc);
				if (pc.isMerged()) {
					mrc.flags |= ObjGraphParams.FLAG_MERGE_INITIAL_LIBRARY;
				}
				cands.put(pc, mrc);
				all.add(mrc);
			}
		}

		@Override
		public Iterator<ModRefFieldCandidate> iterator() {
			return Collections.unmodifiableSet(all).iterator();
		}

		@Override
		public boolean removeAll(final Collection<?> toRemove) {
			boolean changed = false;

			for (final Object o : toRemove) {
				changed |= remove(o);
			}

			return changed;
		}

		@Override
		public boolean remove(final Object o) {
			if (o instanceof ModRefFieldCandidate) {
				final ModRefFieldCandidate c = (ModRefFieldCandidate) o;

				if (contains(c)) {
					removeCandidate((ModRefFieldCandidate) o);
					return true;
				}
			}

			return false;
		}

		@Override
		public int size() {
			return all.size();
		}

		@Override
		public void addCandidate(final ModRefFieldCandidate toAdd) {
			final ModRefFieldCandidate mr = cands.get(toAdd.pc);
			if (mr == null) {
				final ModRefFieldCandidate newC = toAdd.clone();
				cands.put(toAdd.pc, newC);
				all.add(newC);
			} else {
				if (toAdd.isMod()) {
					mr.setMod();
				}
				if (toAdd.isRef()) {
					mr.setRef();
				}
			}
		}

		@Override
		public void removeCandidate(final ModRefFieldCandidate toRemove) {
			if (!cands.containsKey(toRemove.pc)) {
				throw new IllegalArgumentException();
			}

			cands.remove(toRemove.pc);

			if (!toRemove.pc.isUnique()) {
				for (final UniqueParameterCandidate up : toRemove.pc.getUniques()) {
					final ModRefFieldCandidate f = cands.get(up);

					if (toRemove.equals(f)) {
						cands.remove(up);
					}
				}
			}

			all.remove(toRemove);
		}

		@Override
		public ModRefFieldCandidate mergeCandidates(final Iterable<ModRefFieldCandidate> toMerge, final int markWithFlags) {
			final List<ParameterCandidate> pcands = new LinkedList<ParameterCandidate>();
			boolean isMod = false;
			boolean isRef = false;
			int flags = ModRefCandidate.FLAG_NOT_SET;

			for (final ModRefFieldCandidate mrc : toMerge) {
				// they could have been merged before
				assert cands.containsKey(mrc.pc);
				//assert mrc.equals(cands.get(mrc.pc));

				isMod |= mrc.isMod();
				isRef |= mrc.isRef();
				flags |= mrc.flags;
				pcands.add(mrc.pc);
				all.remove(mrc);
			}

			final OrdinalSet<UniqueParameterCandidate> pcs = fact.findUniqueSet(pcands);
			final ParameterCandidate mmpc = fact.createMerge(pcs);
			final ModRefFieldCandidate newCand = new ModRefFieldCandidate(isMod, isRef, mmpc);
			flags |= markWithFlags;
			newCand.flags = flags;

			cands.put(mmpc, newCand);

			for (final ParameterCandidate pc : pcands) {
				cands.put(pc, newCand);
			}
			all.add(newCand);
			
			return newCand;
		}

		@Override
		public boolean contains(final ModRefCandidate n) {
			return all.contains(n);
		}

	}

	private void run(final CallGraph cg, final IProgressMonitor progress) throws CancelException {
		final HeapModel hm = pa.getHeapModel();
        int progressCtr = 0;

        if (progress != null) {
            progress.beginTask("IntraProc ModRef candidates", cg.getNumberOfNodes());
        }

		for (final CGNode n : cg) {
			MonitorUtil.throwExceptionIfCanceled(progress);
            if (progress != null) {
                progressCtr++;
                if (progressCtr % 103 == 0) progress.worked(progressCtr);
            }
			final IR ir = n.getIR();
			if (ir == null) { continue; }

			final PointsTo pts = new PointsTo() {
				@Override
				public OrdinalSet<InstanceKey> getPointsTo(final int ssaVar) {
					if (ssaVar >= 0) {
						final PointerKey pk = hm.getPointerKeyForLocal(n, ssaVar);
						return (pk != null ? pa.getPointsToSet(pk) : null);
					}

					return null;
				}
			};

			final CGNodeCandidates consumer = new CGNodeCandidates(candFact);
			all.put(n, consumer);
			final ModRefSSAVisitor visitor = new ModRefSSAVisitor(consumer, paramFact, pts, cg.getClassHierarchy(), doStaticFields);
			ir.visitNormalInstructions(visitor);
		}

        if (progress != null) progress.done();
	}

	public InterProcCandidateModel getCandidates(final CGNode n) {
		return (InterProcCandidateModel) all.get(n);
	}

	public Map<CGNode, Collection<ModRefFieldCandidate>> getCandidateMap() {
		return all;
	}

	private static final class SingleCandidatePointsTo implements PointsTo {

		private final HeapModel hm;
		private final PointerAnalysis<InstanceKey> pa;
		private CGNode n;

		private SingleCandidatePointsTo(final HeapModel hm, final PointerAnalysis<InstanceKey> pa) {
			this.hm = hm;
			this.pa = pa;
		}

		@Override
		public OrdinalSet<InstanceKey> getPointsTo(final int ssaVar) {
			if (ssaVar >= 0) {
				final PointerKey pk = hm.getPointerKeyForLocal(n, ssaVar);
				return (pk != null ? pa.getPointsToSet(pk) : null);
			}

			return null;
		}


	}

	private static final class SingleCandidateConsumer implements CandidateConsumer {

		private ModRefFieldCandidate last = null;
		private final CandidateFactory candFact;

		private SingleCandidateConsumer(final CandidateFactory candFact) {
			this.candFact = candFact;
		}

		@Override
		public void addModCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			final ParameterCandidate pc = candFact.findOrCreateUnique(basePts, field, fieldPts);
			last = new ModRefFieldCandidate(true, false, pc);
		}

		@Override
		public void addRefCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			final ParameterCandidate pc = candFact.findOrCreateUnique(basePts, field, fieldPts);
			last = new ModRefFieldCandidate(false, true, pc);
		}

	}

	public ModRefFieldCandidate createRefCandidate(final CGNode cgNode, final SSAInstruction instr) {
		single.last = null;
		singlePts.n = cgNode;
		instr.visit(singleVisitor);

		assert single.last != null;

		return single.last;
	}

	public ModRefFieldCandidate createModCandidate(final CGNode cgNode, final SSAInstruction instr) {
		single.last = null;
		singlePts.n = cgNode;
		instr.visit(singleVisitor);

		assert single.last != null;

		return single.last;
	}

	public long countCandidates() {
		long counter = 0;

		for (Entry<CGNode, Collection<ModRefFieldCandidate>> e : all.entrySet()) {
			counter += e.getValue().size();
		}

		return counter;
	}
	
	@Override
	public Iterator<CGNode> iterator() {
		return all.keySet().iterator();
	}

}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.params.objgraph.ModRefSSAVisitor.CandidateConsumer;
import edu.kit.joana.wala.core.params.objgraph.ModRefSSAVisitor.PointsTo;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory;
import edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate;
import edu.kit.joana.wala.core.params.objgraph.candidates.UniqueParameterCandidate;
import edu.kit.joana.wala.util.pointsto.WalaPointsToUtil;

/**
 *
 * Intraprocedural and interprocedural modref candidates.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefCandidates implements Iterable<CGNode> {

	private final Map<CGNode, Collection<ModRefFieldCandidate>> all = new ConcurrentHashMap<CGNode, Collection<ModRefFieldCandidate>>();
	private final CandidateFactory candFact;
	private final ParameterFieldFactory paramFact;
	private final PointerAnalysis<InstanceKey> pa;
	private final SingleCandidateConsumer single;
	private final ModRefSSAVisitor singleVisitor;
	private final SingleCandidatePointsTo singlePts;
	private final boolean doStaticFields;
	public final boolean ignoreExceptions;
	private final boolean isParallel;

	private ModRefCandidates(final CandidateFactory candFact, final ParameterFieldFactory paramFact,
			final PointerAnalysis<InstanceKey> pa, final boolean doStaticFields, final boolean ignoreExceptions, boolean isParallel) {
		this.candFact = candFact;
		this.paramFact = paramFact;
		this.pa = pa;
		this.single = new SingleCandidateConsumer(candFact);
		this.singlePts = new SingleCandidatePointsTo(pa.getHeapModel(), pa);
		this.doStaticFields = doStaticFields;
		this.ignoreExceptions = ignoreExceptions;
		this.singleVisitor = new ModRefSSAVisitor(single, paramFact, singlePts, pa.getClassHierarchy(), doStaticFields);
		this.isParallel = isParallel;
	}

	public static ModRefCandidates computeIntracProc(final ParameterFieldFactory paramFact,
			final CandidateFactory candFact, final CallGraph cg, final PointerAnalysis<InstanceKey> pa,
			final boolean doStaticFields, final boolean ignoreExceptions, final IProgressMonitor progress, boolean isParallel) throws CancelException {
		final ModRefCandidates modref = new ModRefCandidates(candFact, paramFact, pa, doStaticFields, ignoreExceptions, isParallel);

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
		 * If the candidate is already present in this method (either directly, or as part of a merged candidate),
		 * only the mod/ref status of the already present candidate is altered, but not new candidate is added.
		 * @param toAdd Candidate from a called method propagated to the modref of this method.
		 * @return the (possibly new) ModRefFieldCandidate that represents toAdd in this method.
		 */
		public ModRefFieldCandidate addCandidate(ModRefFieldCandidate toAdd);

		/**
		 * Merge all given candidates to a single candidate and add the merged node to this method.
		 * A new merged node is only created if there is not already merged node which all of the to-be-merged nodes are part of. 
		 * @param toMerge A list of candidates that should be merged.
		 * @return the ModRefFieldCandidate that represents the merging of toMerge in this method.
		 */
		public ModRefFieldCandidate mergeCandidates(Iterable<ModRefFieldCandidate> toMerge, int markWithFlags);

		
		/**
		 * Register all given candidates to be merged.
		 * Any such candidate will, if later added, be added only in form of the merged candidate.
		 * @param toMerge A list of candidates that should be registered as to-be-merged.
		 */
		public ModRefFieldCandidate registerMergeCandidates(Iterable<ModRefFieldCandidate> toMerge, int markWithFlags);
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

		/**
		 * Checks is any modref candidate with the given ParameterCandidate is part of this model
		 * @param n A ParameterCandidate.
		 * @return <tt>true</tt> if any candidate with the given ParameterCandidate is part of this model.
		 */
		public boolean containsParameterCandidate(ParameterCandidate pc);

	}

	private static class CGNodeCandidates extends AbstractCollection<ModRefFieldCandidate> implements CandidateConsumer,
		InterProcCandidateModel {

		private static boolean expensiveAssertions = false;
		private final CandidateFactory fact;
		private Map<ParameterCandidate, ModRefFieldCandidate> cands = new HashMap<ParameterCandidate, ModRefFieldCandidate>();
		private Set<ModRefFieldCandidate> all = new HashSet<ModRefFieldCandidate>();

		private CGNodeCandidates(final CandidateFactory fact) {
			this.fact = fact;
			assert invariant();
		}

		// a number of invariants holding for CGNodeCandidates.
		private boolean invariant() {
			if (!expensiveAssertions && cands.size() >= 300) return true;
			for (ModRefFieldCandidate cand : all) {
				if (!cands.containsKey(cand.pc)) {
					return false;
				}
			}
			
			// Any UniqueParameterCandidate is part of at most one (merged) Candidate.
			{
				Set<UniqueParameterCandidate> uniques = new HashSet<>();
				
				Set<ModRefFieldCandidate> values = new HashSet<>();
				values.addAll(cands.values());
				for (ModRefFieldCandidate modref : values) {
					ParameterCandidate pc = modref.pc;
					if (pc.isUnique()) {
						if (uniques.contains(pc)) {
							return false;
						}
						uniques.add((UniqueParameterCandidate)pc);
					} else {
						for (UniqueParameterCandidate up : pc.getUniques()) {
							if (uniques.contains(up)) {
								return false;
							}
						}
						for (UniqueParameterCandidate up : pc.getUniques()) {
							uniques.add(up);
						}
					}
				}
			}
			
			
			
			// The map cand is consistent with the ParameterCandidate field of all ModRefFieldCandidates.
			for (ModRefFieldCandidate cand : all) {
				final ModRefFieldCandidate cand2 = cands.get(cand.pc);
				if (cand2 != cand) {
					return false;
				}
			}
			
			// Any ParameterCandidate is *directly* mapped to the ModRefFieldCandidate which in which it is currently contained.
			for (ParameterCandidate pc : cands.keySet()) {
				ModRefFieldCandidate cand  = cands.get(pc);
				ModRefFieldCandidate cand2 = cands.get(cand.pc);
				if (cand != cand2) {
					return false;
				}
			}
			
			// Except for merged Candidates that are registered but now yet part of this method, cands.values() and all are the same set.
			if (expensiveAssertions && !cands.values().containsAll(all)) {
				return false;
			}

			return true;
		}
		@Override
		public void addModCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> pts) {
			final ParameterCandidate pc = fact.findOrCreateUnique(basePts, field, pts);

			cands.compute(pc, (k, mrc) -> {
				if (mrc != null) {
					all.add(mrc);
					mrc.setMod(); // TODO: this is not good: ModRefFieldCandidate#equals() uses this flag
				} else {
					mrc = new ModRefFieldCandidate(true, false, pc);
					if (pc.isMerged()) {
						mrc.flags |= ObjGraphParams.FLAG_MERGE_INITIAL_LIBRARY;
					}
					all.add(mrc);					
				}
				return mrc;
			});
			assert invariant();
		}

		@Override
		public void addRefCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> pts) {
			final ParameterCandidate pc = fact.findOrCreateUnique(basePts, field, pts);

			cands.compute(pc, (k, mrc) -> {
				if (mrc != null) {
					all.add(mrc);
					mrc.setRef(); // TODO: this is not good: ModRefFieldCandidate#equals() uses this flag
				} else {
					mrc = new ModRefFieldCandidate(false, true, pc);
					if (pc.isMerged()) {
						mrc.flags |= ObjGraphParams.FLAG_MERGE_INITIAL_LIBRARY;
					}
					all.add(mrc);					
				}
				return mrc;
			});
			
			assert invariant();
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

			assert invariant();
			return changed;
		}

		@Override
		public boolean remove(final Object o) {
			if (o instanceof ModRefFieldCandidate) {
				final ModRefFieldCandidate c = (ModRefFieldCandidate) o;

				if (contains(c)) {
					removeCandidate((ModRefFieldCandidate) o);
					assert invariant();
					return true;
				}
			}

			assert invariant();
			return false;
		}

		@Override
		public int size() {
			return all.size();
		}

		@Override
		public ModRefFieldCandidate addCandidate(final ModRefFieldCandidate toAdd) {
			final ModRefFieldCandidate mr = cands.get(toAdd.pc);
			final ModRefFieldCandidate result;
			if (mr == null) {
				final ModRefFieldCandidate newC = toAdd.clone();
				result = newC;
				cands.put(toAdd.pc, newC);
				all.add(newC);
			} else {
				assert cands.get(mr.pc) == mr;
				result = mr;
				all.add(result);
				if (toAdd.isMod()) {
					mr.setMod();
				}
				if (toAdd.isRef()) {
					mr.setRef();
				}
			}
			assert invariant();
			return result;
			
		}

		@Override
		public void removeCandidate(final ModRefFieldCandidate toRemove) {
			final ModRefFieldCandidate toRemoveInCands = cands.get(toRemove.pc);
			assert (toRemoveInCands == toRemove) ==  all.contains(toRemove);
			
			if (toRemoveInCands != toRemove) {
				throw new IllegalArgumentException("Trying to remove ModRefFieldCandidate " + toRemove + "that is not present.");
			}
			
			if (!cands.containsKey(toRemove.pc)) {
				return;
			}

			
			// TODO: if performance demands it, instead of checking every cand, start to remove things iteratively
			// from toRemove.pc.getUniques().
			
			final Iterator<Entry<ParameterCandidate, ModRefFieldCandidate>> candsIt = cands.entrySet().iterator();
			while (candsIt.hasNext()) {
				final Entry<ParameterCandidate, ModRefFieldCandidate> entry = candsIt.next();
				final ModRefFieldCandidate cand = entry.getValue();
				if (cand == toRemove) {
					candsIt.remove();
				}
			}
			
			assert !cands.containsKey(toRemove.pc);

			all.remove(toRemove);
			assert invariant();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel#registerMergeCandidates(java.lang.Iterable, int)
		 * 
		 * The purpose of this method is to support
		 *     ObjGraphParams.simpleReachabilityPropagateWithMerge(),
		 * in which candidates that belong to a CGNodes to due fixpoint-propagation are to be merged *before* they are
		 * actually added to this.all.
		 * Adding to this.all happens later, in
		 *     ObjGraphParams.adjustInterprocModRef()
		 */
		@Override
		public ModRefFieldCandidate registerMergeCandidates(Iterable<ModRefFieldCandidate> toMerge, int markWithFlags) {
			final Set<ParameterCandidate> pcands = new HashSet<>();
			boolean isMod = false;
			boolean isRef = false;
			int flags = ModRefCandidate.FLAG_NOT_SET;

			for (final ModRefFieldCandidate mrc : toMerge) {
				assert cands.containsKey(mrc.pc);

				// It must hold that 
				//    cands.containsKey(mrc.pc)
				// , but not necessarily: all.contains(mrc) || cands.values().contains(mrc)).
				// We take the request to merge mrc to mean that the ModRefFieldCandidate currently representing mrc.pc
				// (i.e.: cands.get(mrc.pc) is to be merged.
				
				ModRefFieldCandidate mrcInCands = cands.get(mrc.pc);
				
				/* The follwing assertions do, unfortunately, not hold:
				assert (mrc.isMod() | mrcInCands.isMod()) == mrcInCands.isMod();
				assert (mrc.isRef() | mrcInCands.isRef()) == mrcInCands.isRef();
				assert (mrc.flags   | mrcInCands.flags  ) == mrcInCands.flags;
				*  Consider the following scenario:
				*  Candidate mrc was created as a clone of some candidate mrc0 when mrc0 was added to 
				*  some *other* instance of CGNodeCandidates. At this point, e.g., mrc0.isMod == mrc.IsMod == false.
				*  
				*  Then, mrc escaped that other instance, (e.g.: via CGNodeCandidates.iterator()), and was added to this
				*  instance of CGNodeCandidates.
				*  Later, another instance mrcX with mrcX.pc.equals(mrc.pc) and mrcX.isMod == true was added to 
				*  the other instance, forcing mrc.isMod to be updated to true.
				*  
				*  Then, someone decides that in this instance, mrc ought to be merged with some other candidates.
				*  Then, for the current representative of mrc.pc in this instance (i.e.: mrcInCands), it possibly
				*  still holds that mrcInCands.isMod == false, because when mrc was added to this instance, this
				*  was then the value for mrc.isMod.
				*  
				*  TODO: this means that the update of mrc.isMod is only coincidentally reported to this instance of 
				*  CGNodeCandidates. Is this a general problem?
				*/
				isMod |= mrcInCands.isMod();
				isRef |= mrcInCands.isRef();
				flags |= mrcInCands.flags;
				
				pcands.add(mrcInCands.pc);
				all.remove(mrcInCands);
			}

			final OrdinalSet<UniqueParameterCandidate> pcs = fact.findUniqueSet(pcands);
			final ParameterCandidate mmpc = fact.createMerge(pcs);
			final ModRefFieldCandidate newCand = new ModRefFieldCandidate(isMod, isRef, mmpc);
			flags |= markWithFlags;
			newCand.flags = flags;

			final ModRefFieldCandidate oldCand = cands.get(mmpc);
			ModRefFieldCandidate result;
			if (oldCand != null) {
				// In this case, pcands have been merged before, into a ModRefFieldCandidate with ParameterCandidate mmpc
				// either
				// a) that ModRefFieldCandidate is still valid, in which case it holds that
				//      oldCand.equals(newCand)
				//    
				// b) that ModRefFieldCandidate has been merged itself after being created (ultimately: into oldCand),
				//    in which caset holds that  
				//      oldCand.pc.getUniques is a superset of newCand.pc.getUniques
				assert oldCand.equals(newCand) || WalaPointsToUtil.isSubsetOf(newCand.pc.getUniques(), oldCand.pc.getUniques());
				// In either case, we discard newCand.
				result = oldCand;
			} else {
				assert !cands.containsKey(mmpc);
				cands.put(mmpc, newCand);
				result = newCand;
			}
			
			// Establish the invariant that and key in cands *directly* points to the ModRefFieldCandidate that it is currently part of.
			final ArrayList<ParameterCandidate> candsKeys = new ArrayList<>(cands.size());
			candsKeys.addAll(cands.keySet());
			for (final ParameterCandidate pc : candsKeys) {
				if (pcands.contains(cands.get(pc).pc)) cands.put(pc, result);
			}
			
			assert invariant();
			return result;
		}

		@Override
		public ModRefFieldCandidate mergeCandidates(final Iterable<ModRefFieldCandidate> toMerge, final int markWithFlags) {
			ModRefFieldCandidate merged = registerMergeCandidates(toMerge, markWithFlags);
			all.add(merged);
			
			assert invariant();
			return merged;
		}

		
		@Override
		public boolean contains(final ModRefCandidate n) {
			return all.contains(n);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel#containsParameterCandidate(edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate)
		 */
		@Override
		public boolean containsParameterCandidate(ParameterCandidate pc) {
			boolean result = false;
			final ModRefFieldCandidate cand = cands.get(pc);
			if (cand != null) {
				result = all.contains(cand);
			}
			assert (result == allContainsAny(pc));
			return result;
		}
		
		private boolean allContainsAny(ParameterCandidate pc) {
			for (ModRefFieldCandidate cand : all) {
				if (cand.pc.equals(pc)) return true;
			}
			return false;
		}
	}

	private void run(final CallGraph cg, final IProgressMonitor progress) throws CancelException {
		final HeapModel hm = pa.getHeapModel();
        int progressCtr = 0;

        if (progress != null) {
            progress.beginTask("IntraProc ModRef candidates", cg.getNumberOfNodes());
        }
        StreamSupport.stream(cg.spliterator(), this.isParallel).forEach(n -> {
			//MonitorUtil.throwExceptionIfCanceled(progress);
            if (progress != null) {
                //progressCtr++;
                if (progressCtr % 103 == 0) progress.worked(progressCtr);
            }
			final IR ir = n.getIR();
			if (ir == null) { return; }

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
			synchronized (all) {
				all.put(n, consumer);
			}
			final ModRefSSAVisitor visitor = new ModRefSSAVisitor(consumer, paramFact, pts, cg.getClassHierarchy(), doStaticFields);
			ir.visitNormalInstructions(visitor);
		});

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

	public synchronized ModRefFieldCandidate createRefCandidate(final CGNode cgNode, final SSAInstruction instr) {
		single.last = null;
		singlePts.n = cgNode;
		instr.visit(singleVisitor);

		assert single.last != null;

		return single.last;
	}

	public synchronized ModRefFieldCandidate createModCandidate(final CGNode cgNode, final SSAInstruction instr) {
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

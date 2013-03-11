/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.dataflow.GenReach;
import edu.kit.joana.wala.core.killdef.Access.RW;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public final class AccessManager<T> {

	private static final boolean DEBUG_PRINT = false;

	private int currentId = 0;

	private final CGNode currentMethod;
	private final List<FieldAccess<T>> read = new LinkedList<FieldAccess<T>>();
	private final List<FieldAccess<T>> write = new LinkedList<FieldAccess<T>>();
	private final TIntObjectHashMap<Access<T>> iindex2acc = new TIntObjectHashMap<Access<T>>();
	private final TIntObjectHashMap<Value<Integer>> num2val = new TIntObjectHashMap<Value<Integer>>();
	private final TIntObjectHashMap<Value<FieldAccess<T>>> accid2equiv = new TIntObjectHashMap<Value<FieldAccess<T>>>();
	private final Map<T, Set<Access<T>>> node2access = new HashMap<T, Set<Access<T>>>();

	public AccessManager(final CGNode currentMethod) {
		this.currentMethod = currentMethod;
	}

	public CGNode getMethod() {
		return currentMethod;
	}

	public void mapNodeToAccess(final T node, final Access<T> acc) {
		if (acc.getNode() != null) {
			throw new IllegalStateException();
		}

		acc.setNode(node);
		Set<Access<T>> accs = node2access.get(node);
		if (accs == null) {
			accs = new HashSet<Access<T>>();
			node2access.put(node, accs);
		}

		accs.add(acc);
	}

	@SuppressWarnings("unchecked")
	public Set<Access<T>> getAccess(final T node) {
		final Set<Access<T>> accs = node2access.get(node);

		return (accs == null ? (Set<Access<T>>) Collections.EMPTY_SET : Collections.unmodifiableSet(accs));
	}

	public Map<FieldAccess<T>, Set<FieldAccess<T>>> buildMustReadMap() {
		final Map<FieldAccess<T>, Set<FieldAccess<T>>> mustRead = new HashMap<FieldAccess<T>, Set<FieldAccess<T>>>();

		for (final FieldAccess<T> r : read) {
			final Set<FieldAccess<T>> set = new HashSet<FieldAccess<T>>();

			for (final FieldAccess<T> w : write) {
				if (w.isSameAccess(r)) {
					set.add(w);
				}
			}

			mustRead.put(r, set);
		}

		return mustRead;
	}

	public Map<FieldAccess<T>, Set<FieldAccess<T>>> buildMustKillMap() {
		final Map<FieldAccess<T>, Set<FieldAccess<T>>> mustKill = new HashMap<FieldAccess<T>, Set<FieldAccess<T>>>();

		for (final FieldAccess<T> w1 : write) {
			final Set<FieldAccess<T>> set = new HashSet<FieldAccess<T>>();

			for (final FieldAccess<T> w2 : write) {
				if (w1 != w2 && w1.isSameAccess(w2)) {
					set.add(w2);
				}
			}

			mustKill.put(w1, set);
		}

		return mustKill;
	}

	public void computeEquivalenceClasses(final Reachability<T> reachable, final IProgressMonitor progress)
			throws CancelException {
		computeValueEquivClasses(reachable, progress);
		computeAccessEquivClasses(read, progress);
		computeAccessEquivClasses(write	, progress);
	}

	public List<FieldAccess<T>> createAdditionalInitialWrites() {
		final LinkedList<FieldAccess<T>> initial = new LinkedList<FieldAccess<T>>();

		for (final Value<FieldAccess<T>> w : findAccessEquivClasses(write)) {
			final FieldAccess<T> acc = w.v;
			final FieldAccess<T> copy = acc.createCopy(nextId());
			initial.add(copy);
		}

		write.addAll(initial);

		return initial;
	}

	private List<Value<FieldAccess<T>>> findAccessEquivClasses(final List<FieldAccess<T>> accs) {
		final List<Value<FieldAccess<T>>> equiv = new LinkedList<Value<FieldAccess<T>>>();

		for (final FieldAccess<T> na : accs) {
			final Value<FieldAccess<T>> v = findOrCreateAccessEquiv(na);

			if (!equiv.contains(v)) {
				equiv.add(v);
			}
		}

		return equiv;
	}

	@SuppressWarnings("unused")
	private void computeValueEquivClasses(final Reachability<T> reach, final IProgressMonitor progress)
			throws CancelException {
		boolean changed = true;

		while (changed) {
			changed = false;

			MonitorUtil.throwExceptionIfCanceled(progress);

			/*
			 * v2 = v1.f;	// from
			 * <no write to f>
			 * v3 = v1.f;	// to
			 *
			 * merge (v2, v3)
			 */
			for (final FieldAccess<T> to : read) {
				final Value<Integer> val = to.getValue();

				for (final FieldAccess<T> from : read) {
					if (to == from) continue;

					if (to.isSameAccess(from) && reach.isReachFromTo(from, to)
							&& !reach.isWriteInBetween(from, to, to.getField())) {
						final Value<Integer> val2 = from.getValue();
						final boolean change = val.merge(val2);
						changed |= change;

						if (DEBUG_PRINT && change) {
							System.out.println("\tm1(v" + val + ", v" + val2 + ")");
						}
					}
				}
			}

			/*
			 * additional case would be:
			 *
			 * v3 = v4.a;	// readA
			 * v1.f = v3;	// writeF
			 * v2 = v1.f;	// readF
			 *
			 * merge (v3, v2) iff n1 dominates n3 and no other write to f is in between
			 */
			for (final FieldAccess<T> readA : read) {
				final Value<Integer> val = readA.getValue();

				for (final FieldAccess<T> readF : read) {
					if (readA.getField().equals(readF.getField())) continue;

					final FieldAccess<T> writeF = reach.findLastWriteDominating(readF);

					if (writeF != null && writeF.getValue().equals(val) && reach.isDominating(readA, writeF)
							&& writeF.isSameAccess(readF)) {
						final Value<Integer> valReadF = readF.getValue();
						boolean change = val.merge(valReadF);
						changed |= change;
						
						if (DEBUG_PRINT && change) {
							System.out.println("\tm2(v" + val + ", v" + valReadF + ")");
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void computeAccessEquivClasses(final List<FieldAccess<T>> accs, final IProgressMonitor progress) {
		// find equivalent reads
		for (final FieldAccess<T> n1 : accs) {
			final Value<FieldAccess<T>> v1 = findOrCreateAccessEquiv(n1);

			for (final FieldAccess<T> n2 : accs) {
				if (n1 != n2 && n1.isSameAccess(n2)) {
					// put in equiv class
					final Value<FieldAccess<T>> v2 = findOrCreateAccessEquiv(n2);
					boolean change = v1.merge(v2);

					if (DEBUG_PRINT && change) {
						System.out.println("\tacc-merge(" + n1 + ", " + n2 + ")");
					}
				}
			}
		}
	}

	private Value<FieldAccess<T>> findOrCreateAccessEquiv(final FieldAccess<T> facc) {
		Value<FieldAccess<T>> v = accid2equiv.get(facc.id);
		if (v == null) {
			v = new Value<FieldAccess<T>>(facc, facc.id);
			accid2equiv.put(facc.id, v);
		}

		return v;
	}

	public Value<Integer> getValue(final int val) {
		Value<Integer> v = num2val.get(val);
		if (v == null) {
			v = new Value<Integer>(val, val);
			num2val.put(val, v);
		}

		return v;
	}

	public Access<T> getAccForInstr(final SSAInstruction instr) {
		return iindex2acc.get(instr.iindex);
	}

	public Iterable<FieldAccess<T>> getReads() {
		return Collections.unmodifiableList(read);
	}

	public Iterable<FieldAccess<T>> getWrites() {
		return Collections.unmodifiableList(write);
	}

	public Access<T> addDummy(final int iindex) {
		final DummyAccess<T> da = new DummyAccess<T>(iindex);

		iindex2acc.put(iindex, da);

		return da;
	}

	public Access<T> addCall(final int iindex, final CallSiteReference csr) {
		final CallAccess<T> da = new CallAccess<T>(iindex, csr);

		iindex2acc.put(iindex, da);

		return da;
	}

	public FieldAccess<T> addArrayRead(final int iindex, final ParameterField field, final int base,
			final int index, final int value) {
		final ArrayFieldAccess<T> aa = new ArrayFieldAccess<T>(nextId(), iindex, field, RW.READ, getValue(value),
				getValue(base), getValue(index));

		read.add(aa);
		iindex2acc.put(iindex, aa);

		return aa;
	}

	public FieldAccess<T> addArrayWrite(final int iindex, final ParameterField field, final int base,
			final int index, final int value) {
		final ArrayFieldAccess<T> aa = new ArrayFieldAccess<T>(nextId(), iindex, field, RW.WRITE, getValue(value),
				getValue(base), getValue(index));

		write.add(aa);
		iindex2acc.put(iindex, aa);

		return aa;
	}

	public FieldAccess<T> addFieldRead(final int iindex, final ParameterField field, final int base,
			final int value) {
		final ObjectFieldAccess<T> fa =
				new ObjectFieldAccess<T>(nextId(), iindex, field, RW.READ, getValue(value), getValue(base));

		read.add(fa);
		iindex2acc.put(iindex, fa);

		return fa;
	}

	public FieldAccess<T> addFieldWrite(final int iindex, final ParameterField field, final int base,
			final int value) {
		final ObjectFieldAccess<T> fa =
				new ObjectFieldAccess<T>(nextId(), iindex, field, RW.WRITE, getValue(value), getValue(base));

		write.add(fa);
		iindex2acc.put(iindex, fa);

		return fa;
	}

	public FieldAccess<T> addStaticRead(final int iindex, final ParameterField field, final int value) {
		final StaticFieldAccess<T> sa = new StaticFieldAccess<T>(nextId(), iindex, field, RW.READ, getValue(value));

		read.add(sa);
		iindex2acc.put(iindex, sa);

		return sa;
	}

	public FieldAccess<T> addStaticWrite(final int iindex, final ParameterField field, final int value) {
		final StaticFieldAccess<T> sa = new StaticFieldAccess<T>(nextId(), iindex, field, RW.WRITE, getValue(value));

		write.add(sa);
		iindex2acc.put(iindex, sa);

		return sa;
	}

	private int nextId() {
		return currentId++;
	}

	/**
	 * This class is used to build equivalence classes of arbitrary objects with ids. Do not
	 * put instances of this class in hash sets, they all share the same hash value.
	 *
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 *
	 * @param <I>
	 */
	public static final class Value<I> {

		private BitVector bv;
		public final I v;

		private Value(final I v, final int id) {
			this.v = v;
			this.bv = new BitVector();
			this.bv.set(id);
		}

		public boolean contains(final int id) {
			return bv.contains(id);
		}

		public boolean merge(final Value<I> other) {
			if (this.bv != other.bv) {
				// always use the bitvector instance with the smallest hashcode
				if (System.identityHashCode(this.bv) < System.identityHashCode(other.bv)) {
					this.bv.or(other.bv);
					other.bv = this.bv;
				} else {
					other.bv.or(this.bv);
					this.bv = other.bv;
				}

				// changed
				return true;
			}

			return false;
		}

		public int hashCode() {
			return 42; // internal data may change often, so no meaningful hash value possible
		}

		public boolean equals(Object obj) {
			if (obj instanceof Value<?>) {
				final Value<?> val =  ((Value<?>) obj);
				return this == val || val.bv == bv || val.bv.sameBits(bv);
			}

			return false;
		}

		public String toString() {
			return v + "_" + bv.toString();
		}

	}

	private interface GenReachProvider<T> {
		GenReach<T,Access<T>> create(Graph<T> flow, Map<T, Collection<Access<T>>> map);
	}

	private Map<T, OrdinalSet<Access<T>>> computeReachableAccesses(final GenReachProvider<T> genReachProvider,
			final Graph<T> flow, final IProgressMonitor progress) throws CancelException {
		// create gen maps
		final Map<T, Collection<Access<T>>> map = new HashMap<T, Collection<Access<T>>>();

		for (final T bb : flow) {
			final Set<Access<T>> accs = getAccess(bb);
			map.put(bb, accs);
		}
		
		if (DEBUG_PRINT){
			System.out.println("Gen maps per node:");
			for (final T bb : flow) {
				if (bb instanceof INodeWithNumber) {
					final Collection<Access<T>> accs = map.get(bb);
					System.out.print("bb" + ((INodeWithNumber) bb).getGraphNodeId() + ": ");
					
					for (final Access<T> a : accs) {
						final INodeWithNumber n = (INodeWithNumber) a.getNode();
						System.out.print("bb" + n.getGraphNodeId() + " ");
					}
					System.out.println();
				}
			}
		}

		final GenReach<T, Access<T>> genMayReach = genReachProvider.create(flow, map);
		final BitVectorSolver<T> solver = new BitVectorSolver<T>(genMayReach);
		solver.solve(progress);

		final Map<T, OrdinalSet<Access<T>>> reach = new HashMap<T, OrdinalSet<Access<T>>>();
		final OrdinalSetMapping<Access<T>> mapping = genMayReach.getLatticeValues();
		for (final T bb : flow) {
			final BitVectorVariable bvv = solver.getIn(bb);
			reach.put(bb, new OrdinalSet<Access<T>>(bvv.getValue(), mapping));
		}

		return reach;
	}

	public Map<T, OrdinalSet<Access<T>>> computeMayReachableAccesses(final Graph<T> flow,
			final IProgressMonitor progress) throws CancelException {
		return computeReachableAccesses(new GenReachProvider<T>() {
			public GenReach<T, Access<T>> create(Graph<T> flow, Map<T, Collection<Access<T>>> map) {
				return GenReach.createUnionFramework(flow, map);
			}
		}, flow, progress);
	}

	public Map<T, OrdinalSet<Access<T>>> computeMustReachableAccesses(final Graph<T> flow,
			final IProgressMonitor progress) throws CancelException {
		return computeReachableAccesses(new GenReachProvider<T>() {
			public GenReach<T, Access<T>> create(Graph<T> flow, Map<T, Collection<Access<T>>> map) {
				return GenReach.createIntersectionFramework(flow, map);
			}
		}, flow, progress);
	}

}

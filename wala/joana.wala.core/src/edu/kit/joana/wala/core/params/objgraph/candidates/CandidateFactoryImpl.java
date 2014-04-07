/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.params.objgraph.TVL;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;
import edu.kit.joana.wala.util.PrettyWalaNames;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class CandidateFactoryImpl implements CandidateFactory {

	private final MergeStrategy merge;
	private final Map<Atom, MultipleParamCandImpl> id2cand = new HashMap<Atom, MultipleParamCandImpl>();
	/* Maps parameter field -> set of candidates */
	private final Map<ParameterField, Set<UniqueParameterCandidate>> cache =
			new HashMap<ParameterField, Set<UniqueParameterCandidate>>();
	private MutableMapping<UniqueParameterCandidate> mapping = MutableMapping.make();

	public CandidateFactoryImpl(final MergeStrategy merge) {
		this.merge = merge;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#findOrCreateUnique(com.ibm.wala.util.intset.OrdinalSet, edu.kit.joana.wala.core.ParameterField, com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public UniqueParameterCandidate findOrCreateUnique(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
			final OrdinalSet<InstanceKey> fieldPts) {
		Set<UniqueParameterCandidate> candSet = cache.get(field);

		if (candSet == null) {
			candSet = new HashSet<UniqueParameterCandidate>();
			cache.put(field, candSet);
		}

		final UniqueParameterCandidate newCand;

		if (merge.doMerge(basePts, field, fieldPts)) {
			newCand = merge.getMergeCandidate(basePts, field, fieldPts);
		} else {
			newCand = new SingleParamCandImpl(basePts, field, fieldPts);
		}

		for (final UniqueParameterCandidate cand : candSet) {
			if (cand.equals(newCand)) {
				return cand;
			}
		}

		candSet.add(newCand);
		mapping.add(newCand);

		return newCand;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#findOrCreateUniqueMergable(com.ibm.wala.util.strings.Atom)
	 */
	@Override
	public UniqueMergableParameterCandidate findOrCreateUniqueMergable(final Atom id) {
		MultipleParamCandImpl c = id2cand.get(id);

		if (c == null) {
			c = new MultipleParamCandImpl(id);
			id2cand.put(id, c);
		}

		return c;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#removeUniqueCandidate(edu.kit.joana.wala.core.params.objgraph.candidates.UniqueParameterCandidate)
	 */
	@Override
	public void removeUniqueCandidate(final UniqueParameterCandidate toRemove) {
		final UniqueParameterCandidate cand = (UniqueParameterCandidate) toRemove;

		for (final ParameterField f : cand.getFields()) {
			final Set<UniqueParameterCandidate> candSet = cache.get(f);
			if (candSet != null) {
				candSet.remove(cand);

				if (candSet.isEmpty()) {
					cache.remove(f);
				}
			}
		}

		if (toRemove.isMerged()) {
			MultipleParamCandImpl merge = (MultipleParamCandImpl) toRemove;
			id2cand.remove(merge.id);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#createMerge(edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate, edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate)
	 */
	@Override
	public MetaMergableParameterCandidate createMerge(final ParameterCandidate a, final ParameterCandidate b) {
		return new MergeCandTwoImpl(a, b);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#createMerge(com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public MultiMergableParameterCandidate createMerge(final OrdinalSet<UniqueParameterCandidate> cands) {
		return new MergeCandImpl(cands);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#createSet(java.util.Collection)
	 */
	@Override
	public OrdinalSet<UniqueParameterCandidate> createSet(final Collection<UniqueParameterCandidate> cands) {
		return OrdinalSet.toOrdinalSet(cands, mapping);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#findUniqueSet(java.util.Collection)
	 */
	@Override
	public OrdinalSet<UniqueParameterCandidate> findUniqueSet(final Collection<ParameterCandidate> cands) {
		final List<UniqueParameterCandidate> uniques = new LinkedList<UniqueParameterCandidate>();

		for (final ParameterCandidate c : cands) {
			if (c.isUnique()) {
				uniques.add((UniqueParameterCandidate) c);
			} else {
				for (final UniqueParameterCandidate u : c.getUniques()) {
					uniques.add(u);
				}
			}
		}

		return OrdinalSet.toOrdinalSet(uniques, mapping);
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#getUniqueCandidates()
	 */
	@Override
	public Iterable<UniqueParameterCandidate> getUniqueCandidates() {
		return mapping;
	}

	private abstract static class MergableParameterCandidate implements ParameterCandidate {
		public final boolean isUnique() {
			return false;
		}

		public final boolean isMerged() {
			return true;
		}

		public final String toString() {
			final ParameterField f = getField();
			return "MERGE " + (f == null ? "*" : f.toString());
		}

		public final int getBytecodeIndex() {
			if (isStatic() == V.YES) {
				return BytecodeLocation.STATIC_FIELD;
			} else if (isRoot() == V.YES) {
				return BytecodeLocation.ROOT_PARAMETER;
			} else if (isArray() == V.YES) {
				return BytecodeLocation.ARRAY_FIELD;
			} else {
				return BytecodeLocation.OBJECT_FIELD;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate#isMustAliased(edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate)
		 */
		@Override
		public final boolean isMustAliased(final ParameterCandidate other) {
			return false;
		}
	}

	private final class MergeCandTwoImpl extends MergableParameterCandidate implements MetaMergableParameterCandidate {

		private final ParameterCandidate a;
		private ParameterCandidate b;

		private MergeCandTwoImpl(final ParameterCandidate a, final ParameterCandidate b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public V isArray() {
			return TVL.or(a.isArray(), b.isArray());
		}

		@Override
		public V isStatic() {
			return TVL.or(a.isStatic(), b.isStatic());
		}

		@Override
		public V isRoot() {
			return TVL.or(a.isRoot(), b.isRoot());
		}

		@Override
		public V isPrimitive() {
			return TVL.or(a.isPrimitive(), b.isPrimitive());
		}

		@Override
		public boolean isReachableFrom(ParameterCandidate other) {
			return a.isReachableFrom(other) || b.isReachableFrom(other);
		}

		@Override
		public boolean isBaseAliased(OrdinalSet<InstanceKey> pts) {
			return a.isBaseAliased(pts) || b.isBaseAliased(pts);
		}

		@Override
		public boolean isFieldAliased(OrdinalSet<InstanceKey> other) {
			return a.isFieldAliased(other) || b.isFieldAliased(other);
		}

		@Override
		public boolean isReferenceToField(ParameterField otherField) {
			return a.isReferenceToField(otherField) || b.isReferenceToField(otherField);
		}

		@Override
		public void merge(ParameterCandidate toMerge) {
			this.b = new MergeCandTwoImpl(this.b, toMerge);
		}

		@Override
		public OrdinalSet<UniqueParameterCandidate> getUniques() {
			final List<UniqueParameterCandidate> uniques = new LinkedList<UniqueParameterCandidate>();
			if (a.isUnique()) {
				uniques.add((UniqueParameterCandidate) a);
			} else {
				for (final UniqueParameterCandidate u : a.getUniques()) {
					uniques.add(u);
				}
			}

			if (b.isUnique()) {
				uniques.add((UniqueParameterCandidate) b);
			} else {
				for (final UniqueParameterCandidate u : b.getUniques()) {
					uniques.add(u);
				}
			}

			return OrdinalSet.toOrdinalSet(uniques, mapping);
		}

		@Override
		public final boolean isMayAliased(final ParameterCandidate pc) {
			return a.isMayAliased(pc) || b.isMayAliased(pc);
		}

		@Override
		public TypeReference getType() {
			final TypeReference ta = a.getType();
			final TypeReference tb = b.getType();

			return (ta != tb ? TypeReference.Unknown : ta);
		}

		@Override
		public String getBytecodeName() {
			final String bcNameA = a.getBytecodeName();
			final String bcNameB = b.getBytecodeName();

			return (bcNameA.equals(bcNameB) ? bcNameA : BytecodeLocation.UNKNOWN_PARAM);
		}

		@Override
		public ParameterField getField() {
			final ParameterField fA = a.getField();
			final ParameterField fB = b.getField();

			return (fA.equals(fB) ? fA : null);
		}
	}

	private static final class MergeCandImpl extends MergableParameterCandidate implements MultiMergableParameterCandidate {

		private OrdinalSet<UniqueParameterCandidate> cands;
		private V isArray;
		private V isStatic;
		private V isPrimitive;
		private V isRoot;

		private MergeCandImpl(final OrdinalSet<UniqueParameterCandidate> cands) {
			this.cands = cands;
			this.isArray = V.UNKNOWN;
			this.isStatic = V.UNKNOWN;
			this.isPrimitive = V.UNKNOWN;
			this.isRoot = V.UNKNOWN;
		}

		@Override
		public void merge(final OrdinalSet<UniqueParameterCandidate> additional) {
			this.cands = OrdinalSet.unify(this.cands, additional);
			// invalidate cached results
			if (this.isArray != V.MAYBE) { this.isArray = V.UNKNOWN; }
			if (this.isStatic != V.MAYBE) { this.isStatic = V.UNKNOWN; }
			if (this.isPrimitive != V.MAYBE) { this.isPrimitive = V.UNKNOWN; }
			if (this.isRoot != V.MAYBE) { this.isRoot = V.UNKNOWN; }
		}

		@Override
		public TVL.V isArray() {
			if (isArray == V.UNKNOWN) {
				// not cached => compute
				V tmp = V.UNKNOWN;
				for (final ParameterCandidate c : cands) {
					tmp = TVL.or(tmp, c.isArray());
					if (tmp == V.MAYBE) {
						break;
					}
				}

				isArray = tmp;
			}

			return isArray;
		}

		@Override
		public V isStatic() {
			if (isStatic == V.UNKNOWN) {
				// not cached => compute
				V tmp = V.UNKNOWN;
				for (final ParameterCandidate c : cands) {
					tmp = TVL.or(tmp, c.isStatic());
					if (tmp == V.MAYBE) {
						break;
					}
				}

				isStatic = tmp;
			}

			return isStatic;
		}

		@Override
		public TVL.V isRoot() {
			if (isRoot == V.UNKNOWN) {
				// not cached => compute
				V tmp = V.UNKNOWN;
				for (final ParameterCandidate c : cands) {
					tmp = TVL.or(tmp, c.isRoot());
					if (tmp == V.MAYBE) {
						break;
					}
				}

				isRoot = tmp;
			}

			return isRoot;
		}

		@Override
		public TVL.V isPrimitive() {
			if (isPrimitive == V.UNKNOWN) {
				// not cached => compute
				V tmp = V.UNKNOWN;
				for (final ParameterCandidate c : cands) {
					tmp = TVL.or(tmp, c.isPrimitive());
					if (tmp == V.MAYBE) {
						break;
					}
				}

				isPrimitive = tmp;
			}

			return isPrimitive;
		}

		@Override
		public boolean isReachableFrom(ParameterCandidate other) {
			for (ParameterCandidate c : cands) {
				if (c.isReachableFrom(other)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean isBaseAliased(OrdinalSet<InstanceKey> pts) {
			for (ParameterCandidate c : cands) {
				if (c.isBaseAliased(pts)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean isFieldAliased(OrdinalSet<InstanceKey> other) {
			for (ParameterCandidate c : cands) {
				if (c.isFieldAliased(other)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean isReferenceToField(final ParameterField otherField) {
			for (final ParameterCandidate c : cands) {
				if (c.isReferenceToField(otherField)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public OrdinalSet<UniqueParameterCandidate> getUniques() {
			return cands;
		}

		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			for (final UniqueParameterCandidate upc : cands) {
				if (upc.isMayAliased(pc)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public TypeReference getType() {
			TypeReference t = null;

			for (final ParameterCandidate c : cands) {
				final TypeReference otherType = c.getType();

				if (t == null) {
					t = otherType;
				} else if (t != otherType) {
					return TypeReference.Unknown;
				}
			}

			return (t == null ? TypeReference.Unknown : t);
		}

		@Override
		public String getBytecodeName() {
			String bcName = null;

			for (final ParameterCandidate c : cands) {
				final String otherName = c.getBytecodeName();

				if (bcName == null) {
					bcName = otherName;
				} else if (!bcName.equals(otherName)) {
					return BytecodeLocation.UNKNOWN_PARAM;
				}
			}

			return (bcName == null ? BytecodeLocation.UNKNOWN_PARAM : bcName);
		}

		@Override
		public ParameterField getField() {
			ParameterField f = null;

			for (final UniqueParameterCandidate uc : cands) {
				final ParameterField cur = uc.getField();

				if (cur == null || (f != null && !f.equals(cur))) {
					return null;
				}

				f = cur;
			}

			return f;
		}

	}

	private final static class MultipleParamCandImpl extends UniqueMergableParameterCandidate {

		// main representation (e.g. some artificial field)
		private final Atom id;
		private OrdinalSet<InstanceKey> basePts = null;
		private OrdinalSet<InstanceKey> fieldPts = null;
		private final Set<ParameterField> fieldEquiv = new HashSet<ParameterField>();
		private V isArray = V.UNKNOWN;
		private V isStatic = V.UNKNOWN;
		private V isPrimitive = V.UNKNOWN;
		private V isRoot = V.UNKNOWN;
		private TypeReference type = null;
		private String bcName = null;

		public MultipleParamCandImpl(final Atom id) {
			this.id = id;
		}

		@Override
		public void merge(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			this.basePts = unify(this.basePts, basePts);
			this.fieldPts = unify(this.fieldPts, fieldPts);
			this.fieldEquiv.add(field);
			final V curArray = (field.isArray() ? V.YES : V.NO);
			final V curStatic = (field.isStatic() ? V.YES : V.NO);
			final V curRoot = (basePts == null || basePts.isEmpty() ? V.YES : V.NO);
			final V curPrimitive = (field.isPrimitiveType() ? V.YES : V.NO);
			this.isArray = TVL.and(this.isArray, curArray);
			this.isStatic = TVL.and(this.isStatic, curStatic);
			this.isPrimitive = TVL.and(this.isPrimitive, curPrimitive);
			this.isRoot = TVL.and(this.isRoot, curRoot);
			if (this.type == null) {
				this.type = field.getType();
			} else if (this.type != field.getType()) {
				this.type = TypeReference.Unknown;
			}
			final String curBcName =
					(field.isField() ? PrettyWalaNames.bcFieldName(field.getField()) : BytecodeLocation.ARRAY_PARAM);
			if (this.bcName == null) {
				this.bcName = curBcName;
			} else if (!curBcName.equals(this.bcName)) {
				this.bcName = BytecodeLocation.UNKNOWN_PARAM;
			}
		}

		@Override
		public V isStatic() {
			return isStatic;
		}

		@Override
		public V isRoot() {
			return isRoot;
		}

		@Override
		public V isPrimitive() {
			return isPrimitive;
		}

		@Override
		public boolean isReachableFrom(ParameterCandidate other) {
			if (basePts != null && basePts.size() > 0 && !TVL.isTrue(other.isPrimitive())) {
				return other.isFieldAliased(basePts);
			}

			return false;
		}

		@Override
		public boolean isBaseAliased(OrdinalSet<InstanceKey> otherPts) {
			return basePts != null && otherPts != null && basePts.containsAny(otherPts);
		}

		@Override
		public boolean isFieldAliased(OrdinalSet<InstanceKey> otherPts) {
			return fieldPts != null && otherPts != null && fieldPts.containsAny(otherPts);
		}

		@Override
		public boolean isReferenceToField(ParameterField otherField) {
			return fieldEquiv.contains(otherField);
		}

		@Override
		public Set<ParameterField> getFields() {
			return Collections.unmodifiableSet(fieldEquiv);
		}

		@Override
		public int hashCode() {
			return id.hashCode() * 4711;
		}

		@Override
		public boolean equals(final Object obj) {
			return this == obj;
			//			if (obj instanceof MultipleParamCandImpl) {
//				final MultipleParamCandImpl other = (MultipleParamCandImpl) obj;
//
//				return this == other;
//			}
//
//			return false;
		}

		@Override
		public String toString() {
			return "UNIQ-MERGE(" + id + ")";
		}

		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			if (pc.isBaseAliased(basePts)) {
				for (final ParameterField f : fieldEquiv) {
					if (pc.isReferenceToField(f)) {
						return true;
					}
				}
			}

			return false;
		}

		@Override
		public TypeReference getType() {
			return type;
		}

		@Override
		public V isArray() {
			return isArray;
		}

		@Override
		public String getBytecodeName() {
			return bcName;
		}

		@Override
		public ParameterField getField() {
			if (fieldEquiv.size() == 1) {
				return fieldEquiv.iterator().next();
			}

			return null;
		}

	}

	private static final <T> OrdinalSet<T> unify(final OrdinalSet<T> a, final OrdinalSet<T> b) {
		if (a != null && b != null) {
			return OrdinalSet.unify(a, b);
		} else if (a == null) {
			return b;
		} else {
			return a;
		}
	}

	private final static class SingleParamCandImpl extends UniqueParameterCandidate {
		private final OrdinalSet<InstanceKey> basePts;
		private final ParameterField field;
		private final OrdinalSet<InstanceKey> fieldPts;

		private SingleParamCandImpl(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			this.basePts = basePts;
			this.field = field;
			this.fieldPts = fieldPts;
		}

		@Override
		public boolean isMerged() {
			return false;
		}

		@Override
		public V isStatic() {
			return (field.isStatic() ? V.YES : V.NO);
		}

		@Override
		public V isArray() {
			return (field.isArray() ? V.YES : V.NO);
		}

		@Override
		public V isRoot() {
			return (basePts == null || basePts.isEmpty() ? V.YES : V.NO);
		}

		@Override
		public V isPrimitive() {
			return (field.isPrimitiveType() ? V.YES : V.NO);
		}

		@Override
		public boolean isReachableFrom(ParameterCandidate other) {
			if (basePts != null && basePts.size() > 0 && !TVL.isTrue(other.isPrimitive())) {
				return other.isFieldAliased(basePts);
			}

			return false;
		}

		@Override
		public boolean isBaseAliased(OrdinalSet<InstanceKey> other) {
			return basePts == other || (basePts != null && other != null && basePts.containsAny(other));
		}

		@Override
		public boolean isFieldAliased(OrdinalSet<InstanceKey> other) {
			return fieldPts == other || (fieldPts != null && other != null && fieldPts.containsAny(other));
		}

		@Override
		public boolean isReferenceToField(ParameterField otherField) {
			return field.equals(otherField);
		}

		@Override
		public int hashCode() {
			return field.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (obj instanceof SingleParamCandImpl) {
				final SingleParamCandImpl other = (SingleParamCandImpl) obj;

				return field.equals(other.field) && pointsToEquals(basePts, other.basePts)
						&& pointsToEquals(fieldPts, other.fieldPts);
			}

			return false;
		}

		@Override
		public Set<ParameterField> getFields() {
			return Collections.singleton(field);
		}

		@Override
		public String toString() {
			return "UNIQ(" + field.getName() + ")";
		}

		@Override
		public String toDebugString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			if (basePts != null) {
				for (final InstanceKey ik : basePts) {
					sb.append(ik.toString() + " ");
				}
			}
			sb.append("} x " + field + " x { ");
			if (fieldPts != null) {
				for (final InstanceKey ik : fieldPts) {
					sb.append(ik.toString() + " ");
				}
			}
			sb.append("}");

			return sb.toString();
		}

		@Override
		public boolean isMustAliased(final ParameterCandidate pc) {
			if (field.isStatic()) {
				return !pc.isMerged() && pc.isReferenceToField(field);
			}

			return false;
		}

		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			return pc.isBaseAliased(basePts) && pc.isReferenceToField(field);
		}

		@Override
		public TypeReference getType() {
			return field.getType();
		}

		@Override
		public String getBytecodeName() {
			return (field.isField() ? PrettyWalaNames.bcFieldName(field.getField()) : BytecodeLocation.ARRAY_PARAM);
		}

		@Override
		public ParameterField getField() {
			return field;
		}

	}

	private static boolean pointsToEquals(final OrdinalSet<InstanceKey> a, final OrdinalSet<InstanceKey> b) {
		return OrdinalSet.equals(a, b);
	}

}

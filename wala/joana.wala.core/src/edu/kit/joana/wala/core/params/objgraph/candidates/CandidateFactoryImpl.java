/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import static edu.kit.joana.wala.util.pointsto.WalaPointsToUtil.unify;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.params.objgraph.TVL;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;
import edu.kit.joana.wala.util.NotImplementedException;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

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
	/* special map for object array fields, see documentation of makeObjArrayHash() below for details */
	private final TIntObjectMap<Set<UniqueParameterCandidate>> cacheObjArray =
			new TIntObjectHashMap<Set<UniqueParameterCandidate>>();
	private final MutableMapping<UniqueParameterCandidate> mapping = MutableMapping.make();
	private final OrdinalSetMapping<ParameterField> fieldMapping;

	public CandidateFactoryImpl(final MergeStrategy merge, final OrdinalSetMapping<ParameterField> fieldMapping) {
		this.merge = merge;
		this.fieldMapping = fieldMapping;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#findOrCreateUnique(com.ibm.wala.util.intset.OrdinalSet, edu.kit.joana.wala.core.ParameterField, com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public synchronized UniqueParameterCandidate findOrCreateUnique(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
			final OrdinalSet<InstanceKey> fieldPts) {
		if (field.isArray() && !field.getElementType().isPrimitiveType()) {
			// special case for object array fields - static types of array fields are always Object, so we need
			// some additional information for better hashing.
			return findOrCreateUniqueObjArray(basePts, field, fieldPts);
		}

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

	private synchronized UniqueParameterCandidate findOrCreateUniqueObjArray(final OrdinalSet<InstanceKey> basePts,
			final ParameterField field,	final OrdinalSet<InstanceKey> fieldPts) {
		final int id = makeObjArrayHash(fieldPts);

		Set<UniqueParameterCandidate> candSet = cacheObjArray.get(id);

		if (candSet == null) {
			candSet = new HashSet<UniqueParameterCandidate>();
			cacheObjArray.put(id, candSet);
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
	public synchronized UniqueMergableParameterCandidate findOrCreateUniqueMergable(final Atom id) {
		MultipleParamCandImpl c = id2cand.get(id);

		if (c == null) {
			c = new MultipleParamCandImpl(id);
			id2cand.put(id, c);
		}

		return c;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#createMerge(edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate, edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate)
	 */
	@Override
	public ParameterCandidate createMerge(final ParameterCandidate a, final ParameterCandidate b) {
		return new MergeCandTwoImpl(a, b);
	}
	
	private final Map<BitVector, ParameterCandidate> mergeCache = new HashMap<BitVector, ParameterCandidate>();

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#createMerge(com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public synchronized ParameterCandidate createMerge(final OrdinalSet<UniqueParameterCandidate> cands) {
		final IntSet set = cands.getBackingSet();
		if (set instanceof BitVectorIntSet) {
			final BitVectorIntSet bvint = (BitVectorIntSet) set;
			final BitVector bv = bvint.getBitVector();
			ParameterCandidate pc = mergeCache.get(bv);
			if (pc == null) {
				pc = new MergeCandImpl(cands);
				mergeCache.put(bv, pc);
			}
			
			return pc;
		}
		
		return new MergeCandImpl(cands);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory#findUniqueSet(java.util.Collection)
	 */
	@Override
	public synchronized OrdinalSet<UniqueParameterCandidate> findUniqueSet(final Collection<ParameterCandidate> cands) {
		final MutableIntSet uniq = new BitVectorIntSet();
		
		for (final ParameterCandidate c : cands) {
			if (c.isUnique()) {
				uniq.add(mapping.getMappedIndex(c));
			} else {
				final OrdinalSet<UniqueParameterCandidate> cUniq = c.getUniques();
				final IntSet backSet = cUniq.getBackingSet();
				uniq.addAll(backSet);
			}
		}
		
		return new OrdinalSet<UniqueParameterCandidate>(uniq, mapping);
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
			final OrdinalSet<ParameterField> fs = getFields();
			if (fs.size() == 1) {
				final ParameterField f = fs.iterator().next();
				return "MERGE "+ f.toString();
			} else {
				return "MERGE *";
			}
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
		
		public boolean equals(final Object obj) {
			throw new NotImplementedException();
		}
		
		public int hashCode() {
			throw new NotImplementedException();
		}
	}

	private final class MergeCandTwoImpl extends MergableParameterCandidate {

		private final ParameterCandidate a;
		private final ParameterCandidate b;

		private MergeCandTwoImpl(final ParameterCandidate a, final ParameterCandidate b) {
			this.a = a;
			this.b = b;
		}

		public int hashCode() {
			return a.hashCode() + b.hashCode();
		}
		
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			} else if (o instanceof MergeCandTwoImpl) {
				final MergeCandTwoImpl other = (MergeCandTwoImpl) o;
				
				return (a.equals(other.a) && b.equals(other.b))
					|| (a.equals(other.b) && b.equals(other.a));
			}
			
			return false;
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
		public boolean isReachableFrom(final ParameterCandidate other) {
			return a.isReachableFrom(other) || b.isReachableFrom(other);
		}

		@Override
		public boolean isBaseAliased(final OrdinalSet<InstanceKey> pts) {
			return a.isBaseAliased(pts) || b.isBaseAliased(pts);
		}

		@Override
		public boolean isFieldAliased(final OrdinalSet<InstanceKey> other) {
			return a.isFieldAliased(other) || b.isFieldAliased(other);
		}

		@Override
		public boolean isReferenceToField(final OrdinalSet<InstanceKey> other, final ParameterField otherField) {
			return a.isReferenceToField(other, otherField) || b.isReferenceToField(other, otherField);
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
		public boolean isReferenceToAnyField(final OrdinalSet<ParameterField> otherField) {
			return a.isReferenceToAnyField(otherField) || b.isReferenceToAnyField(otherField);
		}

		@Override
		public OrdinalSet<ParameterField> getFields() {
			return unify(a.getFields(), b.getFields());
		}

		@Override
		public OrdinalSet<InstanceKey> getBasePointsTo() {
			return unify(a.getBasePointsTo(), b.getBasePointsTo());
		}

		@Override
		public OrdinalSet<InstanceKey> getFieldPointsTo() {
			return unify(a.getFieldPointsTo(), b.getFieldPointsTo());
		}
	}

	private static final class MergeCandImpl extends MergableParameterCandidate {

		private final OrdinalSet<UniqueParameterCandidate> cands;
		private V isArray;
		private V isStatic;
		private V isPrimitive;
		private V isRoot;
		private OrdinalSet<InstanceKey> basePts;
		private OrdinalSet<InstanceKey> fieldPts;
		private int hashCode;

		private MergeCandImpl(final OrdinalSet<UniqueParameterCandidate> cands) {
			if (cands == null || cands.isEmpty()) {
				throw new IllegalArgumentException();
			}
			this.cands = cands;
			this.isArray = V.UNKNOWN;
			this.isStatic = V.UNKNOWN;
			this.isPrimitive = V.UNKNOWN;
			this.isRoot = V.UNKNOWN;
			adjustValues(cands);
		}
		
		private void adjustValues(final OrdinalSet<UniqueParameterCandidate> cands) {
			for (final UniqueParameterCandidate c : cands) {
				if (this.isArray != V.MAYBE) { 
					this.isArray = TVL.or(this.isArray, c.isArray()); 
				}
				if (this.isStatic != V.MAYBE) { 
					this.isStatic = TVL.or(this.isStatic, c.isStatic()); 
				}
				if (this.isPrimitive != V.MAYBE) { 
					this.isPrimitive = TVL.or(this.isPrimitive, c.isPrimitive()); 
				}
				if (this.isRoot != V.MAYBE) {
					this.isRoot = TVL.or(this.isRoot, c.isRoot());
				}
				basePts = unify(basePts, c.getBasePointsTo());
				fieldPts = unify(fieldPts, c.getFieldPointsTo());
				hashCode += c.hashCode();
			}
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			} else if (o instanceof MergeCandImpl) {
				final MergeCandImpl other = (MergeCandImpl) o;
				return OrdinalSet.equals(basePts, other.basePts) && OrdinalSet.equals(fieldPts, other.fieldPts)
						&& OrdinalSet.equals(cands, other.cands);
			}
			
			return false;
		}
		
		@Override
		public TVL.V isArray() {
			return isArray;
		}

		@Override
		public V isStatic() {
			return isStatic;
		}

		@Override
		public TVL.V isRoot() {
			return isRoot;
		}

		@Override
		public TVL.V isPrimitive() {
			return isPrimitive;
		}

		@Override
		public boolean isReachableFrom(final ParameterCandidate other) {
			return other.isFieldAliased(basePts);
		}

		@Override
		public boolean isBaseAliased(final OrdinalSet<InstanceKey> pts) {
			return basePts != null && pts != null && basePts.containsAny(pts);
		}

		@Override
		public boolean isFieldAliased(final OrdinalSet<InstanceKey> other) {
			return fieldPts != null && other != null && fieldPts.containsAny(other);
		}

		private boolean isReferenceToFieldSlow(final OrdinalSet<InstanceKey> other, final ParameterField otherField) {
			for (final ParameterCandidate c : cands) {
				if (c.isReferenceToField(other, otherField)) {
					assert otherField.isStatic() || basePts.containsAny(other);
					return true;
				}
			}

			return false;
		}
		
		@Override
		public boolean isReferenceToField(final OrdinalSet<InstanceKey> other, final ParameterField otherField) {
			boolean result = false;
			// basePts.containsAny(other) is a necessary condition for non-static fields
			if (otherField.isStatic() || basePts.containsAny(other)) {
				for (final ParameterCandidate c : cands) {
					if (c.isReferenceToField(other, otherField)) {
							result = true;
							break;
					}
				}
			} else {
				result = false;
			}
			assert result == isReferenceToFieldSlow(other, otherField);
			return result;
		}

		@Override
		public OrdinalSet<UniqueParameterCandidate> getUniques() {
			return cands;
		}

		
		private boolean isMayAliasedSlow(final ParameterCandidate pc) {
			for (final UniqueParameterCandidate upc : cands) {
				if (upc.isMayAliased(pc)) {
					assert pc.isStatic() != V.UNKNOWN;
					assert pc.isStatic() != V.NO || pc.isBaseAliased(basePts);
					return true;
				}
			}

			return false;
		}
		
		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			boolean result = false;
			assert pc.isStatic() != V.UNKNOWN;
			if (pc.isStatic() != V.NO || pc.isBaseAliased(basePts)) {
				for (final UniqueParameterCandidate upc : cands) {
					if (upc.isMayAliased(pc)) {
						result = true;
						break;
					}
				}
			}

			assert result == isMayAliasedSlow(pc);
			return result;
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
		public boolean isReferenceToAnyField(final OrdinalSet<ParameterField> otherField) {
			for (final UniqueParameterCandidate upc : cands) {
				if (upc.isReferenceToAnyField(otherField)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public OrdinalSet<ParameterField> getFields() {
			OrdinalSet<ParameterField> set = OrdinalSet.empty();
			for (final UniqueParameterCandidate upc : cands) {
				set = unify(set, upc.getFields());
			}
			
			return set;
		}

		@Override
		public OrdinalSet<InstanceKey> getBasePointsTo() {
			return basePts;
		}

		@Override
		public OrdinalSet<InstanceKey> getFieldPointsTo() {
			return fieldPts;
		}

	}

	private final class MultipleParamCandImpl extends UniqueMergableParameterCandidate {

		// main representation (e.g. some artificial field)
		private final Atom id;
		private OrdinalSet<InstanceKey> basePts = null;
		private OrdinalSet<InstanceKey> fieldPts = null;
		private OrdinalSet<ParameterField> fieldEquiv = null;
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
			if (fieldEquiv == null || !fieldEquiv.contains(field)) {
				final int index = fieldMapping.getMappedIndex(field);
				final OrdinalSet<ParameterField> single =
						new OrdinalSet<ParameterField>(IntSetUtil.make(new int[] {index}), fieldMapping);
				this.fieldEquiv = unify(this.fieldEquiv, single);
			}
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
		public boolean isReachableFrom(final ParameterCandidate other) {
			if (basePts != null && !TVL.isTrue(other.isPrimitive()) && !(basePts.isEmpty()) ) {
				return other.isFieldAliased(basePts);
			}

			return false;
		}

		@Override
		public boolean isBaseAliased(final OrdinalSet<InstanceKey> otherPts) {
			return basePts != null && otherPts != null && basePts.containsAny(otherPts);
		}

		@Override
		public boolean isFieldAliased(final OrdinalSet<InstanceKey> otherPts) {
			return fieldPts != null && otherPts != null && fieldPts.containsAny(otherPts);
		}

		@Override
		public boolean isReferenceToField(final OrdinalSet<InstanceKey> other, final ParameterField otherField) {
			if (otherField.isStatic()) {
				return fieldEquiv.contains(otherField);
			} else {
				return basePts != null && other != null && basePts.containsAny(fieldPts) && fieldEquiv.contains(otherField);
			}
		}

		@Override
		public int hashCode() {
			return id.hashCode() * 4711;
		}

		@Override
		public boolean equals(final Object obj) {
			return this == obj;
		}

		@Override
		public String toString() {
			return "UNIQ-MERGE(" + id + ")";
		}

		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			if (pc.isStatic() == V.NO) {
				final OrdinalSet<ParameterField> otherField = pc.getFields();
				if (otherField.size() > 1) {
					// merged candidate
					if (pc instanceof MultipleParamCandImpl) {
						return pc.isBaseAliased(basePts) && fieldEquiv.containsAny(((MultipleParamCandImpl) pc).fieldEquiv);
					} else {
						return pc.isBaseAliased(basePts) && pc.isReferenceToAnyField(fieldEquiv);
					}
				} else {
					return pc.isBaseAliased(basePts) && fieldEquiv.containsAny(otherField);
				}
			} else if (isStatic() != V.NO) {
				return pc.isReferenceToAnyField(fieldEquiv);
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
		public boolean isReferenceToAnyField(final OrdinalSet<ParameterField> otherField) {
			return fieldEquiv.containsAny(otherField);
		}

		@Override
		public OrdinalSet<ParameterField> getFields() {
			return fieldEquiv;
		}

		@Override
		public OrdinalSet<InstanceKey> getBasePointsTo() {
			return basePts;
		}

		@Override
		public OrdinalSet<InstanceKey> getFieldPointsTo() {
			return fieldPts;
		}

	}

	private final class SingleParamCandImpl extends UniqueParameterCandidate {
		private final OrdinalSet<InstanceKey> basePts;
		private final ParameterField field;
		private final OrdinalSet<InstanceKey> fieldPts;
		private final OrdinalSet<ParameterField> fields;
		private final int hash;

		private SingleParamCandImpl(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			this.basePts = basePts;
			this.field = field;
			this.fieldPts = fieldPts;
			final int id = fieldMapping.getMappedIndex(field);
			this.fields = new OrdinalSet<ParameterField>(IntSetUtil.make(new int[] {id}), fieldMapping);
			hash = field.hashCode() + 31 * makeObjArrayHash(basePts) + 7 * makeObjArrayHash(fieldPts);
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
		public boolean isReachableFrom(final ParameterCandidate other) {
			if (basePts != null && !TVL.isTrue(other.isPrimitive()) && !(basePts.isEmpty())) {
				return other.isFieldAliased(basePts);
			}

			return false;
		}

		@Override
		public boolean isBaseAliased(final OrdinalSet<InstanceKey> other) {
			return (basePts != null && other != null && basePts.containsAny(other));
		}

		@Override
		public boolean isFieldAliased(final OrdinalSet<InstanceKey> other) {
			return (fieldPts != null && other != null && fieldPts.containsAny(other));
		}

		@Override
		public boolean isReferenceToField(final OrdinalSet<InstanceKey> other, final ParameterField otherField) {
			if (otherField.isStatic()) {
				return field.equals(otherField);
			} else {
				return basePts != null && other != null && field.equals(otherField) && basePts.containsAny(other);
			}
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(final Object obj) {
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
				return !pc.isMerged() && pc.isReferenceToField(basePts, field);
			}

			return false;
		}

		@Override
		public boolean isMayAliased(final ParameterCandidate pc) {
			return pc.isReferenceToField(basePts, field);
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
		public boolean isReferenceToAnyField(final OrdinalSet<ParameterField> otherField) {
			return otherField.contains(field);
		}

		@Override
		public OrdinalSet<ParameterField> getFields() {
			return fields;
		}

		@Override
		public OrdinalSet<InstanceKey> getBasePointsTo() {
			return basePts;
		}

		@Override
		public OrdinalSet<InstanceKey> getFieldPointsTo() {
			return fieldPts;
		}

	}

	private final boolean pointsToEquals(final OrdinalSet<InstanceKey> a, final OrdinalSet<InstanceKey> b) {
		return OrdinalSet.equals(a, b);
	}

	/**
	 * In wala the static type of every non-primitive array is Object[]. Therefore our generated ParameterField name
	 * is "[Object]" for every non-primitive array access in the program. As we normally use the ParameterField to
	 * hash relevant information, we get huge collisions for array accesses. Thus we create a special hash value for
	 * object array accesses that includes additional information. We use the first element of the points-to set of
	 * access. As the points-to set is ordered, the first element is deterministic. 
	 * @param pts points-to set of the array access
	 * @return id of the first element of the points-to set (or some other fixed number if the set is empty or null)
	 */
	private static int makeObjArrayHash(final OrdinalSet<InstanceKey> pts) {
		if (pts == null) {
			return -2;
		} else if (pts.isEmpty()) {
			return -3;
		} else {
			return pts.size();
		}
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate;

/**
 * Factory for object fields. As Wala treat array fields different from normal
 * object fields, we use this class to combine them.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ParameterFieldFactory {

	private final Map<IField, ObjectField> ifield2field;
	private final Map<TypeReference, ArrayField> type2field;

	private final MutableMapping<ParameterField> fieldMapping;

	
	public ParameterFieldFactory() {
		ifield2field = HashMapFactory.make();
		type2field = HashMapFactory.make();
		fieldMapping = MutableMapping.make();
		fieldMapping.add(LOCK_FIELD);
		fieldMapping.add(CLASS_LOCK_FIELD);
	}

	/**
	 * Returns the ParameterField object representation for a given IField.
	 * Guarantees the following:
	 * ifield1 == ifield2 => getObjectField(ifield1) == getObjectField(ifield2)
	 * @param field IField
	 * @return ParamterField
	 */
	public synchronized ParameterField getObjectField(IField field) {
		if (field == null) {
			throw new IllegalArgumentException("Field should not be null.");
		}

		ObjectField ofield = ifield2field.get(field);
		if (ofield == null) {
			ofield = new ObjectField(field);
			ifield2field.put(field, ofield);
			fieldMapping.add(ofield);
		}

		return ofield;
	}

	/**
	 * Returns a ParameterField object representation for an array reference of
	 * a given type.
	 * Guarantees the following:
	 * type1 == type2 => getArrayField(type1) == getArrayField(type2)
	 * @param elemType type of the referenced objects in the array
	 * @return ParameterField
	 */
	public synchronized ParameterField getArrayField(final TypeReference elemType) {
		if (elemType == null) {
			throw new IllegalArgumentException("Element type should not be null.");
		}

		// Currently, we cannot properly deal with Arrays of elemType other than Object.
		// We'd either have to break the equals/hashCode contract, or adapt the 
		//    ParameterCandidate.isMustAliased(ParameterCandidate pc)
		//    ParameterCandidate.isMayAliased(ParameterCandidate pc)
		// to properly respect subtyping relation between Array Types. Specifically, we could not any longer 
		// user OrdinalSet<ParameterCandidate>.containsAny() checks in 
		//    ParameterCandidate.isReferenceToAnyField(OrdinalSet<ParameterField> otherField)
		
		// Instead, we make sure to only creete one canoical ArrayField for Arrays with reference type.
		// TODO: Currently, we do not even distinguish different dimentionalities, although this could be done in principle,
		// if only WALA ever provided useful types for, e.g., SSAArrayStoreInstructions.
		
		TypeReference canonicalType  = null;
		if (elemType.isPrimitiveType()) {
			canonicalType = elemType;
		} else if (elemType.isArrayType()) {
			// This is usually not taken, due to WALAs lack of interesting type information
			canonicalType = elemType.getArrayElementType();
		} else {
			assert elemType.isClassType();
			canonicalType = TypeReference.JavaLangObject;
		}
		
		ArrayField aField = type2field.get(canonicalType);
		if (aField == null) {
			aField = new ArrayField(canonicalType);
			type2field.put(elemType, aField);
			type2field.put(canonicalType, aField);
			fieldMapping.add(aField);
		}

		return aField;
	}

	public OrdinalSetMapping<ParameterField> getMapping() {
		return fieldMapping;
	}
	
	private static final Atom LOCK_NAME = Atom.findOrCreateAsciiAtom("<lock>");
	private static final FieldReference FIELD_REF = FieldReference.findOrCreate(TypeReference.JavaLangObject, LOCK_NAME, TypeReference.Boolean);
	private static final IField ILOCK = new IField() {

		@Override
		public IClassHierarchy getClassHierarchy() {
			return null;
		}

		@Override
		public Atom getName() {
			return FIELD_REF.getName();
		}

		@Override
		public IClass getDeclaringClass() {
			return null;
		}

		@Override
		public boolean isVolatile() {
			return false;
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public boolean isPublic() {
			return true;
		}

		@Override
		public boolean isProtected() {
			return false;
		}

		@Override
		public boolean isPrivate() {
			return false;
		}

		@Override
		public boolean isFinal() {
			return false;
		}

		@Override
		public FieldReference getReference() {
			return FIELD_REF;
		}

		@Override
		public TypeReference getFieldTypeReference() {
			return FIELD_REF.getFieldType();
		}

		@Override
		public Collection<Annotation> getAnnotations() {
			throw new UnsupportedOperationException();
		}
	};
	/** lock field for object locks */
	private static final ParameterField LOCK_FIELD = new ObjectField(ILOCK);

	private static final Atom CLASS_LOCK_NAME = Atom.findOrCreateAsciiAtom("<class_lock>");
	private static final FieldReference CLASS_FIELD_REF = FieldReference.findOrCreate(TypeReference.JavaLangObject, CLASS_LOCK_NAME, TypeReference.Boolean);
	private static final IField ICLASSLOCK = new IField() {

		@Override
		public IClassHierarchy getClassHierarchy() {
			return null;
		}

		@Override
		public Atom getName() {
			return CLASS_FIELD_REF.getName();
		}

		@Override
		public IClass getDeclaringClass() {
			return null;
		}

		@Override
		public boolean isVolatile() {
			return false;
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public boolean isPublic() {
			return true;
		}

		@Override
		public boolean isProtected() {
			return false;
		}

		@Override
		public boolean isPrivate() {
			return false;
		}

		@Override
		public boolean isFinal() {
			return false;
		}

		@Override
		public FieldReference getReference() {
			return CLASS_FIELD_REF;
		}

		@Override
		public TypeReference getFieldTypeReference() {
			return CLASS_FIELD_REF.getFieldType();
		}
		
		@Override
		public Collection<Annotation> getAnnotations() {
			throw new UnsupportedOperationException();
		}
	};

	/** lock field for static class locks */
	private static final ParameterField CLASS_LOCK_FIELD = new ObjectField(ICLASSLOCK);

	public ParameterField getLockField() {
		return LOCK_FIELD;
	}

	public ParameterField getClassLockField() {
		return CLASS_LOCK_FIELD;
	}

}

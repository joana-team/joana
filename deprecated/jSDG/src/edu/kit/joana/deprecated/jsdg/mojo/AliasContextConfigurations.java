/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.ExitParam;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ArrayField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.ObjGraphParamModel;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.wala.util.StatEv;

/**
 * Testclass to check the number of possible type-compatible alias configurations
 * for each method in the SDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class AliasContextConfigurations {

	private final CallGraph cg;
	private final String mainClass;
	private final Set<PDG> pdgs;

	public AliasContextConfigurations(CallGraph cg, String mainClass, Set<PDG> pdgs) {
		this.cg = cg;
		this.mainClass = mainClass;
		this.pdgs = Collections.unmodifiableSet(pdgs);
	}

	/**
	 * Computes the number of possible alias configurations for each method in
	 * the SDG.
	 *
	 * @throws CancelException
	 *
	 */
	public void testTypeInsteadPts(IProgressMonitor progress) throws CancelException {
		StatEv stat = new StatEv(mainClass + "_alias_contexts");
		try {
			stat.init();
		} catch (IOException e) {
			throw new CancelException(e);
		}

		for (PDG pdg : pdgs) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

	        Map<TypeReference, Integer> countTypes = HashMapFactory.make();

			System.out.println("Interface of " + pdg);
			IParamModel pm = pdg.getParamModel();
			for (IParameter p : pm.getRefParams()) {
				if (!p.isPrimitive()) {
				TypeReference tref = null;

				if (p.isOnHeap()) {
					tref = getFieldType(p, pdg);
				} else {
					tref = getParameterType(p, pdg);
				}

				if (tref == null) {
					System.err.println("FOO");
					continue;
				}

				Integer count = countTypes.get(tref);
				if (count == null) {
					count = 0;
				}
				count++;
				countTypes.put(tref, count);

				}
			}


			stat.enter("pdg", pdg.toString());

			BigInteger pos = new BigInteger("1");
			for (TypeReference tref : countTypes.keySet()) {
				Integer count = countTypes.get(tref);
				BigInteger possibilities = new BigInteger("2");
				possibilities = possibilities.pow(count);
				pos = pos.multiply(possibilities);
				System.out.println("\t" + count + " nodes of type " + tref + " => " + possibilities + " combinations");
				stat.enter("type", tref.toString());
				stat.event("aliases", possibilities.toString());
				stat.leave("type");
			}

			stat.event("total", pos.toString());
			stat.leave("pdg");

			System.out.println("Worst case possibilities: " + pos);
		}
	}

	private boolean mayHaveField(TypeReference type, ParameterField field) {
		if (type == TypeReference.JavaLangObject && !field.isArray()) {
			return true;
		}

		if (field.isField()) {
			ObjectField of = (ObjectField) field;
			Collection<IClass> impl = cg.getClassHierarchy().computeSubClasses(type);
			Atom fieldName = Atom.findOrCreateAsciiAtom(field.getName());
			for (IClass cls : impl) {
				IField f = cls.getField(fieldName);
				if (f != null && f.equals(of.getField())) {
					return true;
				}
			}
		} else if (field.isArray()) {
			ArrayField af = (ArrayField) field;
			if (type.isArrayType() && type.getArrayElementType().equals(af.getElementType())) {
				return true;
			}
		}

		return false;
	}

	private TypeReference getParameterType(IParameter p, PDG pdg) {
		TypeReference tref = null;

		if (p instanceof ParameterNode<?>) {
			ParameterNode<?> parent = (ParameterNode<?>) p;
			ParameterField field = parent.getField();
			if (field != null) {
				tref = getFieldType(field);
			} else {
				// a non-static root parameter -> stack passed method arg
				int ssaVarNumber = parent.getParamId();
				IR ir = pdg.getIR();
				int values[] = ir.getParameterValueNumbers();
				int paramNumber = -1;
				boolean found = false;
				for (paramNumber = 0; paramNumber < values.length && !found; paramNumber++) {
					found = values[paramNumber] != ssaVarNumber;
				}

				if (found) {
					tref = ir.getParameterType(paramNumber);
				} else {
					Log.warn("root parameter not found in method args: " + parent);
				}
			}
		} else if (p instanceof ObjGraphParameter) {
			ObjGraphParameter po = (ObjGraphParameter) p;
			tref = getObjParameterType(po, pdg);
		}

		return tref;
	}

	private TypeReference getObjParameterType(ObjGraphParameter parent, PDG pdg) {
		TypeReference tref = null;

		if (!parent.isOnHeap() && !parent.isStatic()) {
			// get type for method argument
			ObjGraphParamModel pm = (ObjGraphParamModel) pdg.getParamModel();
			FormInLocalNode[] fIns = pm.getParameters();
			int pos = -1;
			boolean found = false;

			for (pos = 0; pos < fIns.length; pos++) {
				if (fIns[pos].equals(parent)) {
					found = true;
					break;
				}
			}

			if (found) {
				if (!pdg.getMethod().isStatic() && pos == 0) {
					if (pos == 0) {
						// this pointer
						tref = pdg.getMethod().getDeclaringClass().getReference();
					} else {
						pos--;
						tref = pdg.getIR().getParameterType(pos);
					}
				} else {
					tref = pdg.getIR().getParameterType(pos);
				}
			}
		} else {
			// get type for field
			ParameterField field = parent.getBaseField();
			tref = getFieldType(field);
		}

		return tref;
	}

	private TypeReference getFieldType(IParameter p, PDG pdg) {
		assert p.isOnHeap();

		TypeReference tref = null;
		ParameterField field = null;
		if (p instanceof ParameterNode<?>) {
			ParameterNode<?> tmp = (ParameterNode<?>) p;
			field = tmp.getField();
		} else if (p instanceof ObjGraphParameter) {
			ObjGraphParameter tmp = (ObjGraphParameter) p;
			field = tmp.getBaseField();
		} else if (p instanceof ExitParam) {
			tref = pdg.getMethod().getReturnType();
		} else {
			Log.warn("Heap parameter of unknown type: " + p);
		}

		if (tref == null && field != null) {
			tref = getFieldType(field);
		}

		return tref;
	}


	private TypeReference getFieldType(ParameterField field) {
		TypeReference tref = null;

		if (field.isField()) {
			ObjectField of = (ObjectField) field;
			tref = of.getField().getFieldTypeReference();
		} else if (field.isArray()) {
			ArrayField af = (ArrayField) field;
			tref = af.getElementType();
		}

		return tref;
	}

	@Deprecated
	@SuppressWarnings("unused")
	private Set<TypeReference> getParentTypes(IParameter p, PDG pdg) {
		assert p.isOnHeap();

		Set<TypeReference> types = HashSetFactory.make();

		if (p instanceof ParameterNode<?>) {
			ParameterNode<?> pn = (ParameterNode<?>) p;
			ParameterNode<?> parent = (ParameterNode<?>) pn.getParent();
			ParameterField field = parent.getField();
			if (field != null) {
				TypeReference type = getFieldType(field);
				types.add(type);
			} else {
				// a non-static root parameter -> stack passed method arg
				int ssaVarNumber = parent.getParamId();
				IR ir = pdg.getIR();
				int values[] = ir.getParameterValueNumbers();
				int paramNumber = -1;
				boolean found = false;
				for (paramNumber = 0; paramNumber < values.length && !found; paramNumber++) {
					found = values[paramNumber] != ssaVarNumber;
				}

				if (found) {
					TypeReference pType = ir.getParameterType(paramNumber);
					types.add(pType);
				} else {
					Log.warn("root parameter not found in method args: " + parent);
				}
			}
		} else if (p instanceof ObjGraphParameter) {
			ObjGraphParameter po = (ObjGraphParameter) p;
			ParameterField field = po.getBaseField();
			for (IParameter pParent : pdg.getParamModel().getRefParams()) {
				ObjGraphParameter parent = (ObjGraphParameter) pParent;
				// get type && search if type allows field with name and type
				if (!parent.isPrimitive()) {
					TypeReference parentType = getObjParameterType(parent, pdg);
					if (mayHaveField(parentType, field)) {
						types.add(parentType);
					}
				}
			}
		}

		return types;
	}


}

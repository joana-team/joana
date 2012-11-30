/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.SDGBuilder;
import gnu.trove.map.hash.TIntObjectHashMap;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;

/**
 * Contains some utility methods concerning output of info messages etc.
 * 
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
public final class OutputUtilities {

	private OutputUtilities() {
	}

	public static void printThreads(final TIntObjectHashMap<IntSet> threads, final TIntObjectHashMap<IntSet> threadIds,
			final SDGBuilder builder) {

		final Logger log = Log.getLogger(Log.L_WALA_INTERFERENCE_DEBUG);

		if (!log.isEnabled()) {
			return;
		}

		for (int pdgId : threads.keys()) {
			PDG pdg = builder.getPDGforId(pdgId);
			IntSet ids = threadIds.get(pdg.getId());
			String id = "";
			for (IntIterator it = ids.intIterator(); it.hasNext();) {
				id += it.next();
				if (it.hasNext()) {
					id += ',';
				}
			}

			log.outln("\nThreadID(" + id + ") - " + pdg + " calls:\n{");
			IntSet transitiveCalled = threads.get(pdgId);
			transitiveCalled.foreach(new IntSetAction() {

				public void act(int x) {
					PDG pdg = builder.getPDGforId(x);
					log.outln(OutputUtilities.methodName(pdg.getMethod()) + ", ");
				}

			});
			log.outln("}");
		}
	}

	public static final String methodName(IMethod method) {
		return methodName(method.getReference());
	}

	/**
	 * Create a human readable typename from a TypeName object convert sth like
	 * [Ljava/lang/String to java.lang.String[]
	 * 
	 * @param tName
	 * @return type name
	 */
	public static final String typeName(TypeName tName) {
		StringBuilder test = new StringBuilder(tName.toString().replace('/', '.'));

		while (test.charAt(0) == '[') {
			test.deleteCharAt(0);
			test.append("[]");
		}

		// remove 'L' in front of object type
		test.deleteCharAt(0);

		return test.toString();
	}

	public static final String methodName(MethodReference mRef) {
		StringBuilder name = new StringBuilder(typeName(mRef.getDeclaringClass().getName()));

		name.append(".");
		name.append(mRef.getName().toString());
		name.append("(");
		for (int i = 0; i < mRef.getNumberOfParameters(); i++) {
			TypeReference tRef = mRef.getParameterType(i);
			if (i != 0) {
				name.append(",");
			}
			if (tRef.isPrimitiveType()) {
				if (tRef == TypeReference.Char) {
					name.append("char");
				} else if (tRef == TypeReference.Byte) {
					name.append("byte");
				} else if (tRef == TypeReference.Boolean) {
					name.append("boolean");
				} else if (tRef == TypeReference.Int) {
					name.append("int");
				} else if (tRef == TypeReference.Long) {
					name.append("long");
				} else if (tRef == TypeReference.Short) {
					name.append("short");
				} else if (tRef == TypeReference.Double) {
					name.append("double");
				} else if (tRef == TypeReference.Float) {
					name.append("float");
				} else {
					name.append("?" + tRef.getName());
				}
			} else {
				name.append(typeName(tRef.getName()));
			}
		}

		name.append(")");

		return name.toString();
	}

	public static String fieldName(IField field) {
		return fieldName(field.getReference());
	}

	public static String fieldName(FieldReference field) {
		TypeName type = field.getDeclaringClass().getName();

		return typeName(type) + "." + field.getName();
	}

}

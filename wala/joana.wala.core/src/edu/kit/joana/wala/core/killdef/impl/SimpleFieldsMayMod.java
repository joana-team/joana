/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.killdef.IFieldsMayMod;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class SimpleFieldsMayMod implements IFieldsMayMod {

	public static final IFieldsMayMod INSTANCE = new SimpleFieldsMayMod();
	private static final IFieldsMayModMethod METHOD = new IFieldsMayModMethod() {

		@Override
		public boolean mayMod(ParameterField f) {
			// conservative approximation - all fields may always be modified by a call
			return true;
		}

		@Override
		public boolean mayRef(ParameterField f) {
			// conservative approximation - all fields may always be modified by a call
			return true;
		}
	};

	@Override
	public IFieldsMayModMethod getFieldsModFor(final CGNode method) {
		return METHOD;
	}

	@Override
	public boolean mayCallModField(final CGNode method, final CallSiteReference csr, final ParameterField field) {
		return true;
	}

}

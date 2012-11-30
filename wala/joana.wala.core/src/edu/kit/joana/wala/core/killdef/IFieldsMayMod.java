/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

import edu.kit.joana.wala.core.ParameterField;

/**
 * Can be used to determine all fields possibly transitively modified during the execution of a given method.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface IFieldsMayMod {

	/**
	 * Shows all fields possibly transitively modified of the corresponding method.
	 *
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 *
	 */
	public interface IFieldsMayModMethod {

		public boolean mayMod(final ParameterField f);
		public boolean mayRef(final ParameterField f);

	}

	public IFieldsMayModMethod getFieldsModFor(final CGNode method);

	public boolean mayCallModField(final CGNode method, final CallSiteReference csr, final ParameterField field);

}

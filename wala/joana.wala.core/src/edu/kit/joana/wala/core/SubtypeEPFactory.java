/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Standard Entrypoint factory, which can be used for any java bytecode program.
 * @author Martin Mohr
 */
public final class SubtypeEPFactory implements EntryPointFactory {
	
	public static final EntryPointFactory INSTANCE = new SubtypeEPFactory();
	
	/**
	 * Prevent instantiation from outside
	 */
	private SubtypeEPFactory() {
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.EntryPointFactory#make(com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.cha.IClassHierarchy)
	 */
	@Override
	public Entrypoint make(IMethod m, IClassHierarchy cha) {
		return new SubtypesEntrypoint(m, cha);
	}

}

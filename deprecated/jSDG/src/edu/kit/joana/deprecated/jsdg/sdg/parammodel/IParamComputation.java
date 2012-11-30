/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel;


import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;

/**
 * Generic interface to trigger the parameter computation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IParamComputation {

	public IParamModel getBasicModel(PDG pdg);

	public void computeModRef(PDG pdg, IProgressMonitor monitor) throws PDGFormatException;

	public void computeTransitiveModRef(SDG sdg, IProgressMonitor monitor) throws PDGFormatException, CancelException, WalaException;

	public void connectCallParamNodes(PDG caller, CallNode call, PDG callee) throws PDGFormatException;

}

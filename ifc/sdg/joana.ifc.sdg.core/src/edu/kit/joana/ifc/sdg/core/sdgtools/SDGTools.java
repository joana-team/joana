/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 25.02.2005
 *
 */
package edu.kit.joana.ifc.sdg.core.sdgtools;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;


/**
 * This utility class offers some relatively simple Queries on SDGs
 *
 * @author naxan, Dennis Giffhorn
 */
public final class SDGTools {

	/**
	 * This utility class is not supposed to be instantiated.
	 */
	private SDGTools() {

	}

	 /**
     * Returns a list containing all security nodes in the given sdg which are {@link SecurityNode#isInformationSink() information sinks}.
     * Note that it is assumed that all nodes in the given sdg are of type {@link SecurityNode}. If this condition is violated,
     * a {@link ClassCastException} is thrown.
     * @param g the sdg from which the information sinks are to be extracted
     * @return a list containing all security nodes in the given which are {@link SecurityNode#isInformationSink() information sinks}
     * @throws ClassCastException if one of the nodes in the given sdg is not a {@link SecurityNode}
     */
	public static Collection<SecurityNode> getInformationSinks(SDG g) {
		List<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (Object o : g.vertexSet())  {
			SecurityNode temp = (SecurityNode) o;
			if (temp.isInformationSink()) ret.add(temp);
		}
		return ret;
	}

	 /**
     * Returns a list containing all security nodes in the given sdg which are {@link SecurityNode#isInformationSource() information sources}.
     * Note that it is assumed that all nodes in the given sdg are of type {@link SecurityNode}. If this condition is violated,
     * a {@link ClassCastException} is thrown.
     * @param g the sdg from which the information sources are to be extracted
     * @return a list containing all security nodes in the given which are {@link SecurityNode#isInformationSource() information sources}
     * @throws ClassCastException if one of the nodes in the given sdg is not a {@link SecurityNode}
     */
    public static Collection<SecurityNode> getInformationSources(SDG g) {
        List<SecurityNode> ret = new LinkedList<SecurityNode>();
        for (Object o : g.vertexSet())  {
            SecurityNode temp = (SecurityNode) o;
            if (temp.isInformationSource()) ret.add(temp);
        }
        return ret;
    }

    /**
     * Returns a list containing all security nodes in the given sdg which are {@link SecurityNode#isDeclassification() declassification nodes}.
     * Note that it is assumed that all nodes in the given sdg are of type {@link SecurityNode}. If this condition is violated,
     * a {@link ClassCastException} is thrown.
     * @param g the sdg from which the declassification nodes are to be extracted
     * @return a list containing all security nodes in the given which are {@link SecurityNode#isDeclassification() declassification nodes}
     * @throws ClassCastException if one of the nodes in the given sdg is not a {@link SecurityNode}
     */
	public static List<SecurityNode> getDeclassificationNodes(SDG g) {
		List<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (Object o : g.vertexSet()) {
			SecurityNode temp = (SecurityNode) o;
			if (temp.isDeclassification()) ret.add(temp);
		}
		return ret;
	}
	/**
	 * returns a list containing all SecurityNode in g that are of type 'annotation'
	 * @return List<SecurityNode>
	 */
	public static List<SecurityNode> getAnnotatedNodes(SDG g) {
		List<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (Object o : g.vertexSet()) {
			SecurityNode temp = (SecurityNode) o;
			if (temp.isInformationSource()) ret.add(temp);
		}
		return ret;
	}
	/**
	 * returns a list containing all SecurityNode in g that are of type 'unannotated'
	 * @return List<SecurityNode>
	 */
	public static List<SecurityNode> getUnAnnotatedNodes(SDG g) {
		List<SecurityNode> ret = new LinkedList<SecurityNode>();
		for (Object o : g.vertexSet()) {
			SecurityNode temp = (SecurityNode) o;
			if (temp.isUnannotated()) ret.add(temp);
		}
		return ret;
	}
}

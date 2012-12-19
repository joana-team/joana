/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

interface Parent<T extends TreeNode>{
	/** Returns the children.
	 */
	T [] getChildren();

	/** Removes the given child.
	 */
	void removeChild(T child);

	/** Adds a child.
	 * @param child The child to add.
	 */
	void addChild(T child);

	/** Returns true if this element has children.
	 */
	boolean hasChildren();
}

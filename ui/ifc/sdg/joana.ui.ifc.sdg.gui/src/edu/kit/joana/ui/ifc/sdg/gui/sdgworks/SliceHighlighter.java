/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 22.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.gui.sdgworks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Collection;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;

/**
 * Offers methods for highlighting a slice inside eclipse's java Editor
 *
 * @author naxan, giffhorn
 */
public class SliceHighlighter {
	private static final int MARKER_LEVEL = 0;
	private static final int CRITERION_LEVEL = -1;

	/**
	 * @param involvedNodes
	 * @throws CoreException
	 * @throws IOException
	 */
	public void addSliceNodes(IProject p, Collection<? extends SecurityNode> involvedNodes)
	throws CoreException, IOException {
		addSliceNodesExtern(p, involvedNodes);
	}

	private void addSliceNodesExtern(IProject p, Collection<? extends SecurityNode> involvedNodes)
	throws CoreException, IOException {
		HashMap<SecurityNode, Integer> nodesWithLevels = new HashMap<SecurityNode, Integer>();
		for (SecurityNode node : involvedNodes) {
			nodesWithLevels.put(node, MARKER_LEVEL);
		}
		if (NJSecPlugin.singleton().getSDGFactory().getCachedSDG(p).getJoanaCompiler()) {
			HighlightPlugin.getDefault().highlightJC(p, nodesWithLevels);
		} else {
			HighlightPlugin.getDefault().highlightAST(p, nodesWithLevels);
		}
	}

    public void removeAllSliceHighlighting(IProject project)
    throws CoreException {
        HighlightPlugin high = HighlightPlugin.getDefault();
        high.clearHighlight(project, MARKER_LEVEL);
        high.clearHighlight(project, CRITERION_LEVEL);
    }

	/**
	 * @throws IOException
	 * @throws CoreException
	 */
	public void revealSliceNode(IProject p, SecurityNode node)
	throws CoreException, IOException {
	    HighlightPlugin.getDefault().highlight(p, node, CRITERION_LEVEL);
	}
}

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
package edu.kit.joana.ui.wala.flowless.views;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;

import edu.kit.joana.ui.wala.flowless.views.FlowLessTreeContentProvider.TreeNode;

public class CheckFlowTreeViewer extends TreeViewer {

	public CheckFlowTreeViewer(final Composite parent) {
		super(parent);
	}

	public CheckFlowTreeViewer(final Composite parent,final int style) {
		super(parent, style);
	}

	public void setContentProvider(final IContentProvider p) {
		if (!(p instanceof FlowLessTreeContentProvider)) {
			throw new IllegalArgumentException();
		}

		super.setContentProvider(p);
	}

	public FlowLessTreeContentProvider getContentProvider() {
		return (FlowLessTreeContentProvider) super.getContentProvider();
	}

	public TreeNode getSelectedNode() {
		final ISelection sel = getSelection();
		if (sel instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) sel;
			if (!ssel.isEmpty()) {
				final Object o = ssel.getFirstElement();
				if (o instanceof TreeNode) {
					return (TreeNode) o;
				}
			}
		}

		return null;
	}
}

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
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.util.ArrayList;


import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.ActivityPart;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.parts.ActivityPartFactory;

public class DoubleLatticeDialog extends LatticeDialog {

	private GraphicalViewer viewer2;
	private String label2text;

	public DoubleLatticeDialog(Shell parent) {
		super(parent);
	}

	protected Control createDialogArea(Composite container) {
	    Composite parent = (Composite) super.createDialogArea(container);

	    Label label2 = new Label(parent, SWT.LEAD);
	    label2.setText(label2text);

	    viewer2 = new ScrollingGraphicalViewer();
	    viewer2.setRootEditPart(new ScalableRootEditPart());
	    domain.addViewer(viewer2);
	    viewer2.createControl(parent);
	    viewer2.setEditPartFactory(new ActivityPartFactory());
	    viewer2.getControl().setBackground(ColorConstants.listBackground);
	    viewer2.setSelectionManager(new SelMan());
	    viewer2.setContents(diagram);

	    return parent;
	}

	protected void okPressed() {
		String upper = ((Activity) ((ActivityPart) ((IStructuredSelection) viewer.getSelection()).getFirstElement()).getModel()).getName();
		ArrayList<String> res = new ArrayList<String>(2);
		res.add(upper);
		String lower = ((Activity) ((ActivityPart) ((IStructuredSelection) viewer2.getSelection()).getFirstElement()).getModel()).getName();
		res.add(lower);
		setResult(res);
		setReturnCode(OK);
		close();
	}

	public void setMessage2(String message2) {
	    label2text = message2;
	}

	public void setMessage1(String message1) {
	    setMessage(message1);
	}
}

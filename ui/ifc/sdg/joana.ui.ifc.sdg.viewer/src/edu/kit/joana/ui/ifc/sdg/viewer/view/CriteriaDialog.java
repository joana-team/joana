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
package edu.kit.joana.ui.ifc.sdg.viewer.view;

import java.util.ArrayList;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;

import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationCriteria;

public class CriteriaDialog extends SelectionDialog {

	private List list;
	private ArrayList<EvaluationCriteria> crits;

	public CriteriaDialog(Shell parentShell) {
		super(parentShell);
		crits = new ArrayList<EvaluationCriteria>();
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);

		list = new List(parent, SWT.BORDER);
		try {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint point = registry.getExtensionPoint("edu.kit.joana.ui.ifc.sdg.viewer.EvaluationCriteria");
			IExtension[] extensions = point.getExtensions();

			for (IExtension ext : extensions) {
				IConfigurationElement[] ces = ext.getConfigurationElements();

				for (IConfigurationElement ce : ces) {
					if (ce.getName().equals("criteria")) {
						Object impl;
						impl = ce.createExecutableExtension("class");

						if (impl instanceof EvaluationCriteria) {
							list.add(ce.getAttribute("class"));
							crits.add((EvaluationCriteria) impl);
						}
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		parent.layout();
		return parent;
	}

	protected void okPressed() {
		ArrayList<EvaluationCriteria> results = new ArrayList<EvaluationCriteria>();
		results.add(crits.get(list.getSelectionIndex()));
		setResult(results);
	    super.okPressed();
	}

}

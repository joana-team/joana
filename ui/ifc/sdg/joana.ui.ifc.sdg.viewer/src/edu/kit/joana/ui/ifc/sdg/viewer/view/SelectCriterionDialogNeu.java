/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view;

import java.util.ArrayList;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;

import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;

public class SelectCriterionDialogNeu extends SelectionDialog {

	// The currently selected algorithm in the AnalysisView.
	private Run alg;

	private List list;

	public SelectCriterionDialogNeu(Shell parentShell, Run alg) {
		super(parentShell);
		this.alg = alg;
	}

	protected Control createDialogArea(Composite container) {
		container.setSize(220, 270);
		Composite dialogShell = new Composite(container, SWT.NULL);
		dialogShell.layout();
		dialogShell.pack();

		{
			// the list with the kinds of criteria
			list = new List(dialogShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			list.setBounds(20, 15, 180, 180);
			for (Enum<?> e : alg.getKindsOfCriteria()) {
				list.add(e.toString());
			}
		}

		return container;
	}

	protected void okPressed() {
		int selected = list.getSelectionIndex();

		if (selected != -1) {
			ArrayList<Enum<?>> results = new ArrayList<Enum<?>>();
			results.add(alg.getKindsOfCriteria()[selected]);
			setResult(results);
		}
	    super.okPressed();
	}

}

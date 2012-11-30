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


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Criteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;

/** This class opens a dialog where the user can choose for a given criterion <br>
 * what kinds of criterion it shall be.
 * These kinds of criteria are retrieved from the currently selected
 * algorithm in the AnalysisView.
 *
 * @author giffhorn
 *
 */
public class SelectCriterionDialog extends org.eclipse.swt.widgets.Dialog {
	private Shell dialogShell;
	private List list;
	private Button chooser;
	private Button cancel;

	/** Opens a new SelectCriterionDialog.
	 *
	 * @param alg   The currently selected algorithm in the AnalysisView.
	 * @param crit  The given criterion.
	 */
	public static void create(Run alg, Criteria crit) {
		try {
			Display display = Activator.getDefault().getDisplay();
			Shell shell = display.getActiveShell();
			SelectCriterionDialog inst = new SelectCriterionDialog(alg, crit, shell, SWT.NULL);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// The currently selected algorithm in the AnalysisView.
	private Run alg;
	// The given criterion.
	private Criteria crit;

	/** Creates a new SelectCriterionDialog.
	 *
	 * @param alg2     The currently selected algorithm in the AnalysisView.
	 * @param crit    The given criterion.
	 * @param parent  The surrounding shell.
	 * @param style   Style settings.
	 */
	public SelectCriterionDialog(Run alg2, Criteria crit, Shell parent, int style) {
		super(parent, style);
		dialogShell = parent;
		this.alg = alg2;
		this.crit = crit;
	}

	/** Initializes and opens the Dialog.
	 */
    public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(220, 270);
			dialogShell.setText("Choose a Criterion");

			{
				// the list with the kinds of criteria
				list = new List(dialogShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				list.setBounds(20, 15, 180, 180);
				for (Enum<?> e : alg.getKindsOfCriteria()) {
					list.add(e.toString());
				}
			}
			{
				// a cancel button
				cancel = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				cancel.setText("Cancel");
				cancel.setBounds(20, 200, 90, 30);
				cancel.setSelection(true);
				cancel.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						dialogShell.close();
					}
				});
			}
			{
				// a choose button
				chooser = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				chooser.setText("OK");
				chooser.setBounds(110, 200, 90, 30);
				chooser.addSelectionListener(new SelectionAdapter() {

					// transmits the selected kind to the algorithm
					public void widgetSelected(SelectionEvent evt) {
						int selected = list.getSelectionIndex();

						if (selected != -1) {

							//alg.setCriteria(crit, alg.getKindsOfCriteria()[selected]);
							AnalysisView.getInstance().setCriteria(alg, crit, alg.getKindsOfCriteria()[selected]);
						}

						dialogShell.close();
					}
				});
			}
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

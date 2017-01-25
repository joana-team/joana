/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationCriteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;

public class EvaluationResultDialog extends Dialog {

	private Table table;
	private TableColumn[] columns;
	ArrayList<Run> runs;
	ArrayList<String> results;
	ArrayList<EvaluationCriteria> crits;

	public EvaluationResultDialog(Shell parentShell, ArrayList<Run> runs, ArrayList<String> results, ArrayList<EvaluationCriteria> crits) {
		super(parentShell);
		columns = new TableColumn[crits.size() + 1];
		this.runs = runs;
		this.crits = crits;
		this.results = results;
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);

		table = new Table(parent, SWT.SINGLE | SWT.BORDER);
		table.setHeaderVisible(true);
	  	table.setLinesVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);
		columns[0] = new TableColumn(table, SWT.NONE);
		columns[0].setText("Runs");
		for (int i = 1; i < columns.length; ++i) {
			columns[i] = new TableColumn(table, SWT.NONE);
			columns[i].setText(crits.get(i - 1).getName());
		}
		for (int j = 0; j < runs.size(); ++j) {
			TableItem t = new TableItem(table, SWT.NONE);
			t.setText(0, runs.get(j).getAlgorithm().getName());
			for (int i = 1; i < columns.length; ++i) {
				t.setText(i, results.get(j*crits.size() + i - 1));
			}
		}
		for (int i = 0; i < table.getColumnCount(); ++i) {
			table.getColumn(i).pack();
		}
		parent.layout();

		return parent;
	}
}

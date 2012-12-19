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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.RunView;

public class CompareRunDialog extends Dialog {

	private Table table;
	private TableColumn tID;
	private TableColumn tFirst;
	private TableColumn tSecond;
	private TableItem algorithm;
	private TableItem computation;
	private Button showFirst;
	private Button showSecond;
	private Button quit;
	private Shell dialogShell;
	private RunView first;
	private RunView second;

	private static CompareRunDialog inst;

	public static CompareRunDialog getInst() {
		return inst;
	}

	public static void create(RunView first) {
		Display display = Activator.getDefault().getDisplay();
		Shell shell = display.getActiveShell();
		inst = new CompareRunDialog(shell, SWT.RESIZE | SWT.CLOSE, first);
		inst.open();
	}

	private CompareRunDialog(Shell parent, int style, RunView first) {
		super(parent, style);
		this.first = first;
	}

	private void open() {
		dialogShell = new Shell(getParent(), getStyle());

		dialogShell.setText("Compare Runs");
		dialogShell.setLayout(new GridLayout());

		{ //Table
			table = new Table(dialogShell, SWT.SINGLE | SWT.BORDER);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			table.setLayoutData(data);
			tID = new TableColumn(table, SWT.NONE, 0);
			tID.setText("");
			tFirst = new TableColumn(table, SWT.NONE, 1);
			tFirst.setText("First Run");
			tSecond = new TableColumn(table, SWT.NONE, 2);
			tSecond.setText("Second Run");
			table.setHeaderVisible(true);
		  	table.setLinesVisible(true);

		  	algorithm = new TableItem(table, SWT.NONE);
		  	algorithm.setText(0, "Algorithm");
		  	algorithm.setText(1, first.getParent().getName());
		  	computation = new TableItem(table, SWT.NONE);
		  	computation.setText(0, "Computation");
		  	try {
				computation.setText(1, first.getRun().getResult() == null ? "not finished" : "finished");
			} catch (NullPointerException e) {
				computation.setText(1, "not finished");
			}
		}

		{
			Composite buttons = new Composite(dialogShell, SWT.NONE);
			buttons.setLayout(new RowLayout(SWT.HORIZONTAL));

			showFirst = new Button(buttons, SWT.PUSH);
			showFirst.setText("Show Result Run 1");
			showFirst.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					try {
						first.showResult();
					} catch (NullPointerException e) {
					}
				}
			});

			showSecond = new Button(buttons, SWT.PUSH);
			showSecond.setText("Show Result Run 2");
			showSecond.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					try {
						second.showResult();
					} catch (NullPointerException e) {
					}
				}
			});

			quit = new Button(buttons, SWT.PUSH);
			quit.setText("Exit");
			quit.setSelection(true);
			quit.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					dialogShell.close();
				}
			});
		}

		for (int i = 0; i < table.getColumnCount(); ++i) {
			table.getColumn(i).pack();
		}
		dialogShell.pack();
		dialogShell.open();
		Display display = dialogShell.getDisplay();
		while (!dialogShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public void setSecond(RunView second) {
		this.second = second;
		algorithm.setText(2, second.getParent().getName());
		try {
			computation.setText(2, second.getRun().getResult() == null ? "not finished" : "finished");
		} catch (NullPointerException e) {
			computation.setText(2, "not finished");
		}
	}

	public void update() {
		algorithm.setText(1, first.getParent().getName());
		algorithm.setText(2, second.getParent().getName());
		computation.setText(1, first.getRun().getResult() == null ? "not finished" : "finished");
		computation.setText(2, second.getRun().getResult() == null ? "not finished" : "finished");
	}

	public boolean isDisposed() {
		return dialogShell.isDisposed();
	}
}

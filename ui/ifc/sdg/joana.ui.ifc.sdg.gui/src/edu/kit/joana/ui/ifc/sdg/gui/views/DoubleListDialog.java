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
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.gui.views;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog that prompts for one element out of a list of elements. Uses
 * <code>IStructuredContentProvider</code> to provide the elements and
 * <code>ILabelProvider</code> to provide their labels.
 *
 * @since 2.1
 */
public class DoubleListDialog extends SelectionDialog {
	private IStructuredContentProvider fContentProvider1, fContentProvider2;
	private ILabelProvider fLabelProvider1, fLabelProvider2;
	private Object fInput1, fInput2;
	private TableViewer fTableViewer1, fTableViewer2;
	private String label2text;

	private boolean fAddCancelButton = true;
	private int widthInChars = 55;
	private int heightInChars = 15;

	/**
	 * Create a new instance of the receiver with parent shell of parent.
	 * @param parent
	 */
	public DoubleListDialog(Shell parent) {
		super(parent);
	}

	/**
	 * @param input The input for the list.
	 */
	public void setInput1(Object input1) {
		fInput1 = input1;
	}
	public void setInput2(Object input2) {
		fInput2 = input2;
	}
	/**
	 * @param sp The content provider for the list.
	 */
	public void setContentProvider1(IStructuredContentProvider sp) {
		fContentProvider1 = sp;
	}
	public void setContentProvider2(IStructuredContentProvider sp) {
		fContentProvider2 = sp;
	}
	/**
	 * @param lp The labelProvider for the list.
	 */
	public void setLabelProvider1(ILabelProvider lp) {
		fLabelProvider1 = lp;
	}
	public void setLabelProvider2(ILabelProvider lp) {
		fLabelProvider2 = lp;
	}
	/**
	 *@param addCancelButton if <code>true</code> there will be a cancel
	 * button.
	 */
	public void setAddCancelButton(boolean addCancelButton) {
		fAddCancelButton = addCancelButton;
	}

	/**
	 * @return the TableViewer for the receiver.
	 */
	public TableViewer getTableViewer1() {
		return fTableViewer1;
	}
	public TableViewer getTableViewer2() {
		return fTableViewer2;
	}
	protected void createButtonsForButtonBar(Composite parent) {
		if (!fAddCancelButton)
			createButton(parent, IDialogConstants.OK_ID,
					IDialogConstants.OK_LABEL, true);
		else
			super.createButtonsForButtonBar(parent);
	}
	protected Control createDialogArea(Composite container) {
	    Composite parent = (Composite) super.createDialogArea(container);
	    createMessageArea(parent);

	    fTableViewer1 = new TableViewer(parent, getTableStyle());
	    fTableViewer1.setContentProvider(fContentProvider1);
	    fTableViewer1.setLabelProvider(fLabelProvider1);
	    fTableViewer1.setInput(fInput1);

	    List<?> initialSelection = getInitialElementSelections();
	    if (initialSelection != null)
	        fTableViewer1
	        .setSelection(new StructuredSelection(initialSelection));
	    GridData gd1 = new GridData(GridData.FILL_BOTH);
	    gd1.heightHint = convertHeightInCharsToPixels(heightInChars);
	    gd1.widthHint = convertWidthInCharsToPixels(widthInChars);
	    Table table1 = fTableViewer1.getTable();
	    table1.setLayoutData(gd1);
	    table1.setFont(container.getFont());
	    if (fInput1 == null) {
	        table1.setEnabled(false);
	    }
	    Label label2 = new Label(parent, SWT.LEAD);
	    label2.setText(label2text);

	    fTableViewer2 = new TableViewer(parent, getTableStyle());
	    fTableViewer2.setContentProvider(fContentProvider2);
	    fTableViewer2.setLabelProvider(fLabelProvider2);
	    fTableViewer2.setInput(fInput2);
	    if (initialSelection != null)
	        fTableViewer2
	        .setSelection(new StructuredSelection(initialSelection));
	    GridData gd2 = new GridData(GridData.FILL_BOTH);
	    gd2.heightHint = convertHeightInCharsToPixels(heightInChars);
	    gd2.widthHint = convertWidthInCharsToPixels(widthInChars);
	    Table table2 = fTableViewer2.getTable();
	    table2.setLayoutData(gd2);
	    table2.setFont(container.getFont());
	    if (fInput2 == null) {
	        table2.setEnabled(false);
	    }
	    return parent;
	}

	/**
	 * Return the style flags for the table viewer.
	 * @return int
	 */
	protected int getTableStyle() {
	    return SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
	}

	/*
	 * Overrides method from Dialog
	 */
	@SuppressWarnings("unchecked")
	protected void okPressed() {
	    // Build a list of selected children.
	    IStructuredSelection selection1 = (IStructuredSelection) fTableViewer1.getSelection();
	    IStructuredSelection selection2 = (IStructuredSelection) fTableViewer2.getSelection();
	    ArrayList<List> results = new ArrayList<List>();
	    results.add(selection1.toList());
	    results.add(selection2.toList());
	    setResult(results);
	    super.okPressed();
	}
	/**
	 * Returns the initial height of the dialog in number of characters.
	 *
	 * @return the initial height of the dialog in number of characters
	 */
	public int getHeightInChars() {
	    return heightInChars;
	}
	/**
	 * Returns the initial width of the dialog in number of characters.
	 *
	 * @return the initial width of the dialog in number of characters
	 */
	public int getWidthInChars() {
	    return widthInChars;
	}
	/**
	 * Sets the initial height of the dialog in number of characters.
	 *
	 * @param heightInChars
	 *            the initialheight of the dialog in number of characters
	 */
	public void setHeightInChars(int heightInChars) {
	    this.heightInChars = heightInChars;
	}
	/**
	 * Sets the initial width of the dialog in number of characters.
	 *
	 * @param widthInChars
	 *            the initial width of the dialog in number of characters
	 */
	public void setWidthInChars(int widthInChars) {
	    this.widthInChars = widthInChars;
	}

	public void setMessage2(String message2) {
	    label2text = message2;
	}
	public void setMessage1(String message1) {
	    setMessage(message1);
	}
}

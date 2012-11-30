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

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class LatticeListDialog extends LatticeDialog {
	protected TableViewer table;
	protected IStructuredContentProvider cp;
	protected ILabelProvider lp;
	protected Object input;

	protected boolean fAddCancelButton = true;
	protected int widthInChars = 55;
	protected int heightInChars = 15;

	public LatticeListDialog(Shell parent) {
		super(parent);
		label1text = "";
	}

	@Override
	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);

		table = new TableViewer(parent, getTableStyle());
		if (cp == null) {
			cp = createCP();
		}
		table.setContentProvider(cp);
		if (lp == null) {
			lp = createLP();
		}
		table.setLabelProvider(lp);
		table.setInput(input);

	    List<?> initialSelection = getInitialElementSelections();
	    if (initialSelection != null)
	        table.setSelection(new StructuredSelection(initialSelection));
	    GridData gd1 = new GridData(GridData.FILL_BOTH);
	    gd1.heightHint = convertHeightInCharsToPixels(heightInChars);
	    gd1.widthHint = convertWidthInCharsToPixels(widthInChars);
	    Table table1 = table.getTable();
	    table1.setLayoutData(gd1);
	    table1.setFont(container.getFont());
	    if (input == null) {
	        table1.setEnabled(false);
	    }

		return parent;
	}

	protected IStructuredContentProvider createCP() {
		return new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

			}

			@Override
			public void dispose() {

			}

			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
		};
	}

	protected ILabelProvider createLP() {
		return new ILabelProvider() {

			@Override
			public void removeListener(ILabelProviderListener listener) {

			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener listener) {

			}

			@Override
			public String getText(Object element) {
				return element.toString();
			}

			@Override
			public Image getImage(Object element) {
				return null;
			}
		};
	}

	/**
	 * @param input The input for the list.
	 */
	public void setInput(Object input) {
		this.input = input;
	}

	/**
	 * @param sp The ContentProvider for the List.
	 */
	public void setContentProvider(IStructuredContentProvider sp) {
		cp = sp;
	}

	/**
	 * @param lp The labelProvider for the list.
	 */
	public void setLabelProvider(ILabelProvider lp) {
		this.lp = lp;
	}

	/**
	 * Return the style flags for the table viewer.
	 * @return int
	 */
	protected int getTableStyle() {
	    return SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
	}

	/**
	 * @return the TableViewer for the receiver.
	 */
	public TableViewer getTableViewer() {
		return table;
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

	@Override
	protected void okPressed() {
	    // Build a list of selected children.
	    IStructuredSelection selection = (IStructuredSelection) table.getSelection();
	    setResult(selection.toList());
	    parentOK();
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.util.Collection;


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ifc.sdg.core.violations.Violation.Classification;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.ViolationChangeListener;

public class ClassificationView extends ViewPart implements ViolationChangeListener {
	private TableViewer table;
	private TableNameSorter ns;
	private TableViewContentProvider tvcp;
	private TableViewLabelProvider vlp;

	class TableViewContentProvider implements IStructuredContentProvider {
		Object[] content;

		public TableViewContentProvider() {
			String[] as = {"No violation selected!"};
			content = as;
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}

		public void dispose() {}

		public Object[] getElements(Object parent) {
			return content;
		}

		public void setContent(Object[] lines) {
			content = lines;
		}
	}

	class TableViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof String) {
				return (String) obj;
			}

			Classification c = (Classification) obj;

			if (index==0) {
				return c.getName();
			} else if (index==1) {
				return c.getDescription();
			} else if (index==2) {
				return null;//"" + c.getSeverity();
			}
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			if (obj instanceof Classification) {
				Classification c = (Classification) obj;

				if (index == 2) {
					return NJSecPlugin.singleton().getImageRegistry().get(c.getRating().toString());

				} else if (index == 1) {
					return null;

				} else {
					// TODO: irgendwann mal durch dyn. bindung realisieren
					if ("Call Graph".equals(c.getName())) {
						return NJSecPlugin.singleton().getImageRegistry().get("cfg");

					} else if ("Implicit Flow".equals(c.getName())) {
						return NJSecPlugin.singleton().getImageRegistry().get("iflow");

					} else if ("Distance".equals(c.getName())) {
						return NJSecPlugin.singleton().getImageRegistry().get("dist");

					} else {
						// default image
						return getImage(obj);
					}
				}

			} else {
				return null;
			}
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	class TableNameSorter extends ViewerSorter {
		int col = 0;
		int multi = 1;

		TableNameSorter(int column) {
			col = column;
		}

		public void sortByColumn(int column) {
			if (col == column) multi *= -1;
			col = column;
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			String s1 = vlp.getColumnText(e1, col);
			String s2 = vlp.getColumnText(e2, col);
			return s1.compareTo(s2)*multi;
		}
	}

	/**
	 * Constructor
	 */
	public ClassificationView() {
		ViolationSelection.getInstance().addCView(this);
		NJSecPlugin.singleton().getSDGFactory().addViolationChangeListener(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		table = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
//		AutoResizeTableLayout layout = new AutoResizeTableLayout(table.getTable());
//		table.getTable().setLayout(layout);
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);
		tvcp = new TableViewContentProvider();
		table.setContentProvider(tvcp);
		table.setLabelProvider(vlp = new TableViewLabelProvider());
		table.setSorter(ns = new TableNameSorter(0));
		table.setInput(getViewSite());

		TableColumn c0 = new TableColumn(table.getTable(), SWT.LEAD, 0);
		TableColumn c1 = new TableColumn(table.getTable(), SWT.LEAD, 1);
		TableColumn c2 = new TableColumn(table.getTable(), SWT.LEAD, 2);

		c0.setText("Name");
		c0.setWidth(100);
//		layout.addColumnData(new ColumnWeightData(100));
		c0.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(0);
				table.refresh();
			}
		});

		c1.setText("Description");
		c1.setWidth(100);
//		layout.addColumnData(new ColumnWeightData(100));
		c1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(1);
				table.refresh();
			}
		});

		c2.setText("Severity");
		c2.setWidth(100);
//		layout.addColumnData(new ColumnWeightData(100));
		c2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(2);
				table.refresh();
			}
		});
	}

	@Override
	public void setFocus() {
		table.getControl().setFocus();
	}

	public void fillTable(Object[] objects) {
		tvcp.setContent(objects);
		table.refresh();
		Resizer.resize(table.getTable(), new int[] {30,5,5});
	}

	@Override
	public void violationsChanged(IProject p, Collection<edu.kit.joana.ifc.sdg.core.violations.Violation> violations) {
		String[] as = {"No violation selected!"};
		setContent(as);
	}

	public void setContent(Object[] newc) {
		if (tvcp != null) tvcp.setContent(newc);

		Runnable body = new Runnable() {
			public void run() {
				table.refresh();
			}
		};

		NJSecPlugin.singleton().getWorkbench().getDisplay().asyncExec(body);
	}
}

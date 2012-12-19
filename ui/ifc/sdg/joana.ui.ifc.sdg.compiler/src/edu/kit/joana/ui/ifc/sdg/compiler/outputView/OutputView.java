/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.outputView;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ui.ifc.sdg.compiler.builder.IJoanaBuildProblemMarker;

/**
 * A eclipse ui <code>IViewPart</code> displaying JOANA build problem marker
 * details of all files with an attached marker. To stay in sync with resource
 * changes, it implements <code>IResourceChangeListener</code> and registers
 * itself at the workspace upon creation.
 *
 */
public class OutputView extends ViewPart implements IResourceChangeListener {

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento != null)
			restorePreferences(memento);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		storePreferences(memento);
	}

	private static final String PREF_MARKERTABLE_COLPATH_W = "outputView.markerTable.colPath";

	private static final String PREF_MARKERTABLE_COLNAME_W = "outputView.markerTable.colName";
	private static final String PREF_ROOTSASH_W1 = "outputView.rootSash.w1";

	private static final String PREF_ROOTSASH_W0 = "outputView.rootSash.w0";

	private static final String PREF_TOPSASH_W1 = "outputView.topSash.w1";

	private static final String PREF_TOPSASH_W0 = "outputView.topSash.w0";

	private TableViewer markerTable;

	private Text output;

	private IMarker selectedMarker;

	private TableColumn colName;

	private TableColumn colPath;

	private SashForm topSash;

	private int[] pref_rootSash_weights = new int[] { 1, 1 };

	private int[] pref_markerTable_colWidths = new int[] { 100, 250 };

	private SashForm rootSash;

	private int[] pref_topSash_weights = new int[] { 1, 1 };

	private Text commandLine;

	/**
	 * Constructor
	 */
	public OutputView() {
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		rootSash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		topSash = new SashForm(rootSash, SWT.HORIZONTAL | SWT.SMOOTH);
		createMarkerTable(topSash);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		createOutput(topSash);
		createCommandLine(rootSash);

		rootSash.setWeights(pref_rootSash_weights);
		topSash.setWeights(pref_topSash_weights);
	}

	private void createCommandLine(Composite parent) {
		commandLine = new Text(parent, SWT.NONE);
	}

	private void storePreferences(IMemento memento) {
		memento.putInteger(PREF_ROOTSASH_W0, rootSash.getWeights()[0]);
		memento.putInteger(PREF_ROOTSASH_W1, rootSash.getWeights()[1]);
		memento.putInteger(PREF_TOPSASH_W0, topSash.getWeights()[0]);
		memento.putInteger(PREF_TOPSASH_W1, topSash.getWeights()[1]);
		memento.putInteger(PREF_MARKERTABLE_COLNAME_W, colName.getWidth());
		memento.putInteger(PREF_MARKERTABLE_COLPATH_W, colPath.getWidth());
	}

	private void restorePreferences(IMemento memento) {
		Integer rsw0 = memento.getInteger(PREF_ROOTSASH_W0);
		Integer rsw1 = memento.getInteger(PREF_ROOTSASH_W1);
		pref_rootSash_weights = new int[] { rsw0 == null ? 1 : rsw0, rsw1 == null ? 1 : rsw1 };

		Integer tsw0 = memento.getInteger(PREF_TOPSASH_W0);
		Integer tsw1 = memento.getInteger(PREF_TOPSASH_W1);
		pref_topSash_weights = new int[] { tsw0 == null ? 1 : tsw0, tsw1 == null ? 1 : tsw1 };

		Integer cName = memento.getInteger(PREF_MARKERTABLE_COLNAME_W);
		Integer cPath = memento.getInteger(PREF_MARKERTABLE_COLPATH_W);
		pref_markerTable_colWidths = new int[] { cName == null ? 100 : cName, cPath == null ? 250 : cPath };
	}

	private void createOutput(Composite parent) {
		output = new Text(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

		markerTable.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection().isEmpty())
					selectedMarker = null;
				else
					selectedMarker = (IMarker) ((IStructuredSelection) event.getSelection()).getFirstElement();
				updateOutputDisplay();
			}
		});
	}

	private void createMarkerTable(Composite parent) {
		markerTable = new TableViewer(parent, SWT.BORDER);
		colName = new TableColumn(markerTable.getTable(), SWT.NONE);
		colName.setText("Resource name");
		colName.setWidth(pref_markerTable_colWidths[0]);
		colPath = new TableColumn(markerTable.getTable(), SWT.NONE);
		colPath.setText("Path");
		colPath.setWidth(pref_markerTable_colWidths[1]);
		markerTable.getTable().setLinesVisible(true);
		markerTable.getTable().setHeaderVisible(true);

		// TODO: sorter
		markerTable.setLabelProvider(new JoanaBuildMarkerLabelProvider());
		markerTable.setContentProvider(new MarkerContentProvider(IJoanaBuildProblemMarker.ID, true));
		markerTable.setInput(ResourcesPlugin.getWorkspace());
	}

	private void updateOutputDisplay() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				String compilerOutput = "";
				String cmdLine = "";
				if (selectedMarker != null) {
					compilerOutput = selectedMarker.getAttribute(IJoanaBuildProblemMarker.COMPILER_OUTPUT, "");
					cmdLine = selectedMarker.getAttribute(IJoanaBuildProblemMarker.COMMAND_LINE, "");
				}
				output.setText(compilerOutput);
				commandLine.setText(cmdLine);
			}
		});
	}

	@Override
	public void setFocus() {

	}

	private void updateTable() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				markerTable.refresh(true);
			}
		});
	}

	public void resourceChanged(IResourceChangeEvent event) {
		updateTable();
	}

}

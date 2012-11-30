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


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm;
import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithms;
import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.DomAlgorithmUnmarshaller;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.RunView;


/** This class shows all available algorithms in a window.
 * A user can choose an algorithm for a selected SDG in the AnalysisView.
 *
 */
public class AlgorithmBrowser extends org.eclipse.swt.widgets.Composite {
	// the available algorithms
	private Algorithms algs;
	// the algorithms are shown in a tree
	private TreeViewer treeViewer1;
	// chooses an algorithm
	private Button chooser;
	private Button cancel;
	private Composite composite1;
	// a message field
	private Text text1;
	//Additional RunViewInfo
	private static RunView runinfo;


	/** The general class for elements in the algorithm tree.
	 * Every TreeObject contains an Algorithm it represents and a
	 * parent TreeParent.
	 */
	class TreeObject implements IAdaptable {
		// the represented algorithm
		private Algorithm alg;
		// the parent
		private TreeParent parent;

		/** Creates a new TreeObject.
		 *
		 * @param alg  The represented algorithm.
		 */
		public TreeObject(Algorithm alg) {
			this.alg = alg;
		}

		/** Set the parent to a new value.
		 *
		 * @param parent  The new parent.
		 */
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}

		/** Returns the parent.
		 *
		 */
		public TreeParent getParent() {
			return parent;
		}

		/** Returns the name of the represented algorithm.
		 *
		 */
		public String getName() {
			return alg.getName();
		}

		/** Returns the description of the algorithm.
		 *
		 */
		public String getDescription() {
			return alg.getDescription();
		}

		/** Returns the algorithm.
		 *
		 */
		public Algorithm getAlgorithm() {
			return alg;
		}

		/** Returns the name of the represented algorithm.
		 *
		 */
		public String toString() {
			return getName();
		}

		/** Inherited method; returns null.
		 *
		 */
		@SuppressWarnings("unchecked")
        public Object getAdapter(Class key) {
			return null;
		}

		/** Executed when a user double-clicks this TreeObject.
		 *
		 */
		public void doubleClicked() {
			showMessage("Execute " + alg.getClassName());
		}
	}

	/** A TreeObject that has children.
	 *
	 * @author giffhorn
	 *
	 */
	class TreeParent extends TreeObject {
		// the children
		private ArrayList<TreeObject> children;

		/** Creates a new TreeParent.
		 *
		 * @param alg  The algorithm to represent.
		 */
		public TreeParent(Algorithm alg) {
			super(alg);
			children = new ArrayList<TreeObject>();
		}

		/** Adds a child to this parent.
		 *
		 * @param child  The new child.
		 */
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}

		/** Removes a child.
		 *
		 * @param child  The child to remove.
		 */
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}

		/** Returns all children as a TreeObject array.
		 *  The array can have length 0.
		 */
		public TreeObject [] getChildren() {
			return children.toArray(new TreeObject[children.size()]);
		}

		/** Returns true if this parent has children.
		 */
		public boolean hasChildren() {
			return children.size() > 0;
		}
	}


	/**
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content
	 * (like Task List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider,
										   ITreeContentProvider {

		/** Empty inherited method.
		 */
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		/** Empty inherited method.
		 */
		public void dispose() {
		}

		/** Returns the children of a parent.
		 *
		 */
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}

		/** Returns a parent of a TreeObject.
		 * If child is not a TreeObject, it returns null.
		 */
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}

		/** Returns the children of a parent.
		 * If parent is not a TreeParent, it returns an empty Object array.
		 */
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}

		/** Returns true if this Object is a TreeParent and has children.
		 */
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}
	}

	/** Provides icons and text for TreeObjects and TreeParents.
	 *
	 * @author giffhorn
	 *
	 */
	class ViewLabelProvider extends LabelProvider {
		/** Returns the text of object.
		 * Calls toString() on obj.
		 */
		public String getText(Object obj) {
			return obj.toString();
		}

		/** Returns icons for TreeObjects and TreeParents.
		 */
		public Image getImage(Object obj) {
			if (obj instanceof TreeParent) {
				String imageKey = ISharedImages.IMG_OBJ_FOLDER;

				return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);

			} else {
				return Activator.getDefault().getImageRegistry().getDescriptor("slicer").createImage();
			}
		}
	}

	/** A dummy implementation for a ViewerSorter.
	 *
	 * @author giffhorn
	 *
	 */
	class NameSorter extends ViewerSorter {
	}

	/** A Listener that registres selections in the tree and shows a description in the text field.
	 *
	 * @author giffhorn
	 *
	 */
	class TreeListener implements ISelectionChangedListener {
        public void selectionChanged(SelectionChangedEvent event) {

            // if the selection is empty clear the label
            if(event.getSelection().isEmpty()) {
                text1.setText("");
                return;
            }
            IStructuredSelection selection = (IStructuredSelection)event.getSelection();

            if(selection.getFirstElement() instanceof TreeObject) {
                text1.setText(((TreeObject)selection.getFirstElement()).getDescription());
            }
        }
     }

	/** Opens a new AlgorithmBrowser.
	 */
	public static void showGUI() {
		// create window
		Display display = Activator.getDefault().getDisplay();
		Shell shell = new Shell(display);
		shell.setText("Available Slicing Algorithms");

		AlgorithmBrowser inst = new AlgorithmBrowser(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if(size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		} else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}

		Point oldPos = display.getActiveShell().getLocation();
		shell.setBounds(oldPos.x + 100, oldPos.y + 100, shell.getSize().x, shell.getSize().y);

		// open window
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public static void showGUI(RunView info) {
		runinfo = info;
		showGUI();
	}

	/** Creates a new AlgorithmBrowser.
	 *
	 * @param parent  The surrounding shell.
	 * @param style   Style settings.
	 */
	public AlgorithmBrowser(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		algs = DomAlgorithmUnmarshaller.getAvailableAlgorithms();
		initGUI();
	}

	/** Intializes the GUI.
	 */
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			this.setSize(535, 553);
			{
				FormData composite1LData = new FormData();
				composite1LData.width = 400;
				composite1LData.height = 348;
				composite1LData.left =  new FormAttachment(0, 1000, 14);
				composite1LData.top =  new FormAttachment(0, 1000, 21);
				composite1 = new Composite(this, SWT.BORDER);
				GridLayout composite1Layout = new GridLayout();
				composite1Layout.makeColumnsEqualWidth = true;
				composite1.setLayout(composite1Layout);
				composite1.setLayoutData(composite1LData);
				{
					// the tree
					treeViewer1 = new TreeViewer(composite1, SWT.NONE);
					treeViewer1.setContentProvider(new ViewContentProvider());
					treeViewer1.setLabelProvider(new ViewLabelProvider());
					GridData treeViewer1LData = new GridData();
					treeViewer1LData.widthHint = 373;
					treeViewer1LData.heightHint = 337;
					treeViewer1.getControl().setLayoutData(treeViewer1LData);
					treeViewer1.setSorter(new NameSorter());
					treeViewer1.setInput(initialize());
					treeViewer1.addSelectionChangedListener(new TreeListener());

				}
			}
			{
				// the choose-button
				chooser = new Button(this, SWT.PUSH | SWT.CENTER);
				FormData chooserLData = new FormData();
				chooserLData.width = 90;
				chooserLData.height = 30;
				chooserLData.left =  new FormAttachment(0, 1000, 429);
				chooserLData.top =  new FormAttachment(0, 1000, 308);
				chooser.setLayoutData(chooserLData);
				chooser.setText("OK");
				chooser.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						chooserWidgetSelected(evt);
					}
				});
			}
			{
				// the cancel-button
				cancel = new Button(this, SWT.PUSH | SWT.CENTER);
				FormData chooserLData = new FormData();
				chooserLData.width = 90;
				chooserLData.height = 30;
				chooserLData.left =  new FormAttachment(0, 1000, 429);
				chooserLData.top =  new FormAttachment(0, 1000, 343);
				cancel.setLayoutData(chooserLData);
				cancel.setText("Cancel");
				cancel.setFocus();
				cancel.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						close();
					}
				});
			}
			{
				// the message field
				text1 = new Text(this, SWT.MULTI | SWT.WRAP | SWT.BORDER);
				FormData text1LData = new FormData();
				text1LData.width = 500;
				text1LData.height = 126;
				text1LData.left =  new FormAttachment(0, 1000, 14);
				text1LData.top =  new FormAttachment(0, 1000, 392);
				text1.setLayoutData(text1LData);
				text1.setText("");
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Initializes the tree.
	 *
	 * @return  The invisible tree root
	 */
	private TreeParent initialize() {
		// the invisible tree root
		TreeParent invisibleRoot = new TreeParent(null);

		for (Algorithm a : algs.getAlgorithms()) {
			addSubAlgorithms(a, invisibleRoot);
		}

		return invisibleRoot;
	}

	/** Recursive helper method.
	 *
	 * @param category  The algorithm whose children shall be added.
	 * @param par       The TreeParent representing 'category'.
	 */
	private void addSubAlgorithms(Algorithm alg, TreeParent par) {
		if (alg.hasAlgorithms()) {
			// recursive descend
			TreeParent cat = new TreeParent(alg);
			par.addChild(cat);

			for (Algorithm a : alg.getAlgorithms()) {
				addSubAlgorithms(a, cat);
			}

		} else {
			TreeObject leaf = new TreeObject(alg);
			par.addChild(leaf);
		}
	}

	/** Opens a MessageDialog with the given message.
	 *
	 * @param message  The message.
	 */
	private void showMessage(String message) {
		MessageDialog.openInformation(
			treeViewer1.getControl().getShell(),
			"Sample View",
			message);
	}

	/** This method is called when a user chooses an algorithm.
	 * It propagates that algorithm to the AnalysisView.
	 *
	 * @param evt  Not used.
	 */
	private void chooserWidgetSelected(SelectionEvent evt) {
		ISelection select = treeViewer1.getSelection();
		Object obj = ((IStructuredSelection)select).getFirstElement();

		// only fetch the algorithm if the selected tree element is a leaf
		if (!(obj instanceof TreeParent)) {
			Algorithm alg = ((TreeObject)obj).getAlgorithm();
			if (runinfo == null) {
				AnalysisView.getInstance().setAlgorithm(alg);
			}
			else {
				AnalysisView.getInstance().setAlgorithm(alg, runinfo);
				runinfo = null;
			}
			this.getShell().close();
		}
	}

	/** Closes the SDGCreator.
	 *
	 */
	private void close() {
		this.getShell().close();
	}
}

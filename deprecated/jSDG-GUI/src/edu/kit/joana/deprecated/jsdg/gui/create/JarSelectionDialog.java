/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.create;

import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class JarSelectionDialog extends FilteredItemsSelectionDialog {

	private final IProject ip;

	public static class JarFile {
		public final IFile file;

		public JarFile(IFile file) {
			this.file = file;
		}

		public String toString() {
			String str = file.getFullPath().toOSString();

			return str;
		}
	}

	public JarSelectionDialog(IProject ip, Shell shell, boolean multi) {
		super(shell, multi);
		setTitle("Select libraries to include in analysis");
		setInitialPattern("*?");
		this.ip = ip;
		setSelectionHistory(new SelectionHistory() {

			@Override
			protected Object restoreItemFromMemento(IMemento memento) {
				return null;
			}

			@Override
			protected void storeItemToMemento(Object item, IMemento memento) {
			}

		});
	}


	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {

			@Override
			public boolean isConsistentItem(Object item) {
				return true;
			}

			@Override
			public boolean matchItem(Object item) {
				return matches(item.toString());
			}

		};
	}

	@Override
	protected void fillContentProvider(
			final AbstractContentProvider contentProvider,
			final ItemsFilter itemsFilter, final IProgressMonitor progressMonitor)
			throws CoreException {

		progressMonitor.beginTask("Searching", -1); //$NON-NLS-1$
		IResourceVisitor visit = new IResourceVisitor() {

			public boolean visit(IResource res) throws CoreException {
				if ((res instanceof IFile) && "jar".equals(((IFile) res).getFileExtension())) {
					JarFile jf = new JarFile((IFile) res);
					contentProvider.add(jf, itemsFilter);
				}

				progressMonitor.worked(1);

				return (res instanceof IFolder && !("jSDG".equals(res.getName()))) || res instanceof IProject;
			}

		};
		ip.accept(visit);

		progressMonitor.done();
	}

	private final static String DIALOG_SETTINGS = "Select.JAR.File.Dialog";
	private IDialogSettings settings;

	@Override
	protected IDialogSettings getDialogSettings() {
//		IDialogSettings settings = getDialogSettings().getSection(DIALOG_SETTINGS);
//		if (settings == null) {
//			settings = getDialogSettings().addNewSection(DIALOG_SETTINGS);
//		}
//
//		return settings;
		if (settings == null) {
			settings = new DialogSettings(DIALOG_SETTINGS);
		}

		return settings;
	}

	@Override
	public String getElementName(Object item) {
		return item.toString();
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		return new Comparator<Object>() {

			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}

		};
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

}

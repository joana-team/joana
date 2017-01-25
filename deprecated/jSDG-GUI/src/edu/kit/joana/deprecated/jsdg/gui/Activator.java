/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.kit.joana.deprecated.jsdg.gui.create.SDGCreationObserver;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "jSDG_GUI";

	// The shared instance
	private static Activator plugin;
	private Collection<SDGCreationObserver> creationObserver;

	/**
	 * The constructor
	 */
	public Activator() {
		this.creationObserver = new HashSet<SDGCreationObserver>();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public void showError(final Exception e, final String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			public void run() {
				Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
				//edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().getWorkbench().getDisplay().getActiveShell();
				if (e.getMessage() != null && e.getMessage().length() > 0) {
					MessageDialog.openError(activeShell, message, e.getMessage() + "\nSee error log for details.");
				} else {
					MessageDialog.openError(activeShell, message, message + "\nSee error log for details.");
				}
			}
		});

		ILog log = edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().getLog();
		if (e instanceof CoreException) {
			log.log(((CoreException) e).getStatus());
		} else {
			Status status = new Status(IStatus.ERROR, edu.kit.joana.deprecated.jsdg.gui.Activator.PLUGIN_ID, message, e);
			log.log(status);
		}
	}

	public Display getDisplay() {
		return getWorkbench().getDisplay();
	}

	public boolean addCreationObserver(SDGCreationObserver o) {
		return creationObserver.add(o);
	}

	public boolean removeCreationObserver(SDGCreationObserver o) {
		return creationObserver.remove(o);
	}

	public void sdgChanged(String outputSDGfile) {
		for (SDGCreationObserver o : creationObserver) {
			o.sdgChanged(outputSDGfile);
		}
	}

}

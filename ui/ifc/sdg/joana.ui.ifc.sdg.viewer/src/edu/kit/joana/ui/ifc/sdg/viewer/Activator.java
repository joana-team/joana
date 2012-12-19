/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * This class contains only pure eclipse-generated code.
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.kit.joana.ui.ifc.sdg.viewer";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
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

	protected void initializeImageRegistry(ImageRegistry ir) {
		String[][] images = {{"slicer", "icons/slice.png"},
							 {"choose", "icons/choose.png"},
							 {"remove", "icons/remove.png"},
							 {"configure", "icons/configure.png"},
							 {"results", "icons/results.png"},
							 {"clear", "icons/clear.png"},
							 {"criterion", "icons/flag.png"},
							 {"sanduhr", "icons/sanduhr.gif"},
							 {"haken", "icons/haken.jpg"},
							 {"todo", "icons/todo.png"}};

		for (int i = 0; i < images.length; i++) {
			String im = images[i][1];
			URL url = null;

			try {
				url = new URL(Activator.getDefault().getBundle().getEntry("/"), im);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			ImageDescriptor image = ImageDescriptor.createFromURL(url);
			ir.put(images[i][0], image);
		}
	}

	public File getLocalFile(String relativePath) {
		File file = null;

		try {
			URL url = FileLocator.resolve(Activator.getDefault().getBundle().getEntry("/"));

			url = new URL(url, relativePath);

		    file = new File(url.toURI());

		} catch (MalformedURLException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();

		} catch(URISyntaxException e) {
			e.printStackTrace();
		}

		return file;
	}

	public final void showError(final Exception e, final String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			public void run() {
				Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
				//jsdg_gui.Activator.getDefault().getWorkbench().getDisplay().getActiveShell();
				if (e.getMessage() != null && e.getMessage().length() > 0) {
					MessageDialog.openError(activeShell, message, e.getMessage() + "\nSee error log for details.");
				} else {
					MessageDialog.openError(activeShell, message, message + "\nSee error log for details.");
				}
			}
		});

		ILog log = getLog();
		if (e instanceof CoreException) {
			log.log(((CoreException) e).getStatus());
		} else {
			Status status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
			log.log(status);
		}
	}

	public Display getDisplay() {
		return getWorkbench().getDisplay();
	}
}

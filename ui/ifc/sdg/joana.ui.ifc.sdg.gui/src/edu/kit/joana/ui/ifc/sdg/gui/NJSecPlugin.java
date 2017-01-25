/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigurationAttributes;
import edu.kit.joana.ui.ifc.sdg.gui.launching.LaunchConfigurationTools;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerFactory;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.SDGFactory;

/**
 * The main plugin class to be used in the desktop.
 */
public class NJSecPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static NJSecPlugin plugin;

	/**
	 * Returns the shared instance.
	 */
	public static NJSecPlugin singleton() {
		return plugin;
	}


	/* The NJSec Object */

	//Resource bundle.
	private ResourceBundle resourceBundle;

	private NJSecMarkerFactory mf;
	private NJSecSelectionStore sl;
	private SDGFactory sdgf;

	/**
	 * The constructor.
	 */
	public NJSecPlugin() {
		super();
		plugin = this;
		mf = new NJSecMarkerFactory();
		sl = new NJSecSelectionStore();
		sdgf = new SDGFactory();

		try {
			resourceBundle = ResourceBundle.getBundle("edu.kit.joana.ifc.sdg.gui.NJSecPlugin");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		/**
		 * Add DynamicSDGLoader so always a SDG is loaded
		 * TODO How/Where to do that right ?
		 * It obviously works... but...
		 */
//		class DoItThen implements Runnable {
//
//			public void run() {
//				DynamicSDGLoader dpdgl = new DynamicSDGLoader();
//				sl.addActiveResourceChangeListener(dpdgl);
//				ResourcesPlugin.getWorkspace().addResourceChangeListener(dpdgl);
//			}
//		}
//		DoItThen myDoItThen = new DoItThen();
//		Thread myThread = new Thread(myDoItThen);
//		myThread.run();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = NJSecPlugin.singleton().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	public NJSecMarkerFactory getMarkerFactory() {
		return mf;
	}

	public NJSecSelectionStore getSelectionStore() {
		return sl;
	}

	public SDGFactory getSDGFactory() {
		return sdgf;
	}

	public ConfigReader getStandardLaunchConfigReader(IProject p) throws CoreException {
		return new ConfigReader(getStandardLaunchConfiguration(p));
	}

	public ILaunchConfiguration getStandardLaunchConfiguration(IProject p) throws CoreException {
		return LaunchConfigurationTools.getStandardLaunchConfiguration(p);
	}

	protected void initializeImageRegistry(ImageRegistry ir) {
		String[][] images = {	{"slice", "images/slice.gif"},
								{"slicemini", "images/slicemini.gif"},
								{"pdgandslicemini", "images/pdgandslicemini.gif"},
								{"hideann", "images/hideann.gif"},
								{"pdg", "images/pdg.gif"},
								{"pdgmini2", "images/pdgmini2.gif"},
								{"pdgmini3", "images/pdgmini3.gif"},
								{"pdgandslice", "images/pdgandslice.gif"},
								{"violation", "images/violation3.gif"},
								{"viopath", "images/violationpath.gif"},
								{"vionodeout", "images/vionodeout.gif"},
								{"vionodeann", "images/vionodeann.gif"},
								{"vionodeunann", "images/vionodeunann.gif"},
								{"vionodedeclass", "images/vionodedeclass.gif"},
								{"save", "images/save.gif"},
								{"open", "images/open.gif"},
								{"joana", "images/joana.png"},
								{"clear", "images/clear.png"},
								{"activated", "images/activated.png"},
								{"deactivated", "images/deactivated.png"},
								{"removeall", "images/removeall.png"},
								{"GREEN", "images/green_light.png"},
								{"YELLOW", "images/yellow_light.png"},
								{"RED", "images/red_light.png"},
								{"cfg", "images/cfg.gif"},
								{"iflow", "images/iflow.gif"},
								{"dist", "images/dist.gif"}
		};

		for (int i = 0; i < images.length; i++) {
			String im = images[i][1];
			URL url = null;

			try {
				url = new URL(NJSecPlugin.singleton().getBundle().getEntry("/"), im);

			} catch (MalformedURLException e) { }

			ImageDescriptor image = ImageDescriptor.createFromURL(url);
			ir.put(images[i][0], image);
		}
	}


	/* Convenience Methods */

	public Shell getShell() {
		return getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	public Shell getActiveShell() {
		return getWorkbench().getDisplay().getActiveShell();
	}

	public String getSymbolicName() {
		return getBundle().getSymbolicName();
	}

	public IProject getActiveProject() {
		return getSelectionStore().getActiveProject();
	}

	public IWorkbenchPage getActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	@SuppressWarnings("unchecked")
	public List<String> getMarker(IProject project)
	throws CoreException {
		return getStandardLaunchConfiguration(project).
				getAttribute(ConfigurationAttributes.MARKER, new ArrayList<String>());
	}

	public void showView(String descriptor)
	throws PartInitException {
		getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(descriptor);
	}

	/** Opens a standard error dialog.
	 * It logs the error status to the plug-in log, where it either uses
	 * - the passed status
	 * - the IStatus of e, if status == null and e is a CoreException
	 * - or a generic IStatus containing the ID of this plug-in.
	 *
	 * @param message
	 * @param status
	 * @param e
	 */
	public void showError(final String message, final IStatus status, final Exception e) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();

				if (e != null && e.getMessage() != null && e.getMessage().length() > 0) {
					MessageDialog.openError(activeShell, message, e.getMessage() + "\nSee error log for details.");
				} else {
					MessageDialog.openError(activeShell, message, message+"");
				}
			}
		});

		ILog log = getLog();
		if (status != null) {
			log.log(status);

		} else if (e instanceof CoreException) {
			log.log(((CoreException) e).getStatus());

		} else {
			Status s = new Status(IStatus.ERROR, "NJSecPlugin.PLUGIN_ID", message, e);
			log.log(s);
		}
	}
}

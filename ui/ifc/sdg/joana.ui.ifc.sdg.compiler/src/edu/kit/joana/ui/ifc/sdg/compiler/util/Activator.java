/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.kit.joana.ui.ifc.sdg.compiler.builder.BuildpathListener;
import edu.kit.joana.ui.ifc.sdg.compiler.builder.FileChangeListener;
import edu.kit.joana.ui.ifc.sdg.compiler.configUI.ConfigConstants;
import edu.kit.joana.ui.ifc.sdg.compiler.nature.IJoanaNatureChangeListener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * The ID of the JOANA compiler plugin
	 */
	public static final String PLUGIN_ID = "edu.kit.joana.ui.ifc.sdg.compiler";

	/**
	 * The string constant for the shared JOANA nature icon
	 */
	public static final String IMAGE_JOANA_NATURE = "joanaNatureImage";

	// The shared instance
	private static Activator plugin;

	private Collection<IJoanaNatureChangeListener> natureChangeListeners = new ArrayList<IJoanaNatureChangeListener>();

	/**
	 * Registers a JOANA nature change listener
	 *
	 * @param listener
	 *            the listener to be registered.
	 */
	public void addJoanaNatureChangeListener(IJoanaNatureChangeListener listener) {
		natureChangeListeners.add(listener);
	}

	/**
	 * Deregisters a JOANA nature change listener
	 *
	 * @param listener
	 *            the listener to be deregistered.
	 */
	public void removeJoanaNatureChangeListener(IJoanaNatureChangeListener listener) {
		natureChangeListeners.remove(listener);
	}

	/**
	 * Notifies all registered JOANA nature change listeners that the nature
	 * assignment state of a JDT project has changed.
	 *
	 * @param project
	 *            the JDT project on which the change happened.
	 */
	public void fireJoanaNatureChanged(IJavaProject project) {
		for (IJoanaNatureChangeListener l : natureChangeListeners)
			l.notifyJoanaNatureChanged(project);
	}

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		JavaCore.addElementChangedListener(new BuildpathListener());
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new FileChangeListener());
	}

	/*
	 * (non-Javadoc)
	 *
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

	protected void initializeDefaultPreferences(IPreferenceStore store) {
		ConfigConstants.setCompilerDefaults(store);
	}

	protected void initializeImageRegistry(ImageRegistry ir) {
		String[][] images = { { IMAGE_JOANA_NATURE, "images/joanaNature.gif" } };

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

	/**
	 * Returns the workspace.
	 *
	 * @return the workspace.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
}

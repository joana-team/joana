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
package edu.kit.joana.ui.ifc.sdg.latticeeditor;

import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.ui.LatticeEditor;

/**
 * Plugin class used by the lattice editor.
 */
public class LatticeEditorPlugin extends AbstractUIPlugin {

	// The shared instance.
	private static LatticeEditorPlugin plugin = null;
	// Resource bundle.
	private ResourceBundle resourceBundle;

	private ArrayList<LatticeEditor> les = new ArrayList<LatticeEditor>();
	private ArrayList<ChangedLatticeeditorsListener> leslisteners = new ArrayList<ChangedLatticeeditorsListener>();

	public static final String PLUGIN_ID = "edu.kit.joana.ifc.sdg.latticeeditor";

	/**
	 * Creates a new LatticeEditorPlugin with the given descriptor
	 *
	 * @param descriptor
	 *            the descriptor
	 */
	public LatticeEditorPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("de.naxan.NJSec.LatticeEditorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	// public LatticeEditorPlugin(IPluginDescriptor descriptor) {
	// super(descriptor);
	// plugin = this;
	// }

	public ArrayList<LatticeEditor> getLatticeEditors() {
		return les;
	}

	public void registerLatticeEditor(LatticeEditor le) {
		if (!les.contains(le))
			les.add(le);
		notifyChangedLatticeeditorListeners();
	}

	public void unregisterLatticeEditor(LatticeEditor le) {
		les.remove(le);
		notifyChangedLatticeeditorListeners();
	}

	/**
	 *
	 */
	private void notifyChangedLatticeeditorListeners() {
		for (int i = 0; i < leslisteners.size(); i++) {
			leslisteners.get(i).changedLatticeEditors();
		}

	}

	public void addChangedLatticeeditorListeners(ChangedLatticeeditorsListener l) {
		if (!leslisteners.contains(l))
			leslisteners.add(l);
	}

	public void removeChangedLatticeeditorListeners(ChangedLatticeeditorsListener l) {
		leslisteners.remove(l);
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static LatticeEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
//		ResourceBundle bundle = IFlowCheckingPlugin.getDefault().getResourceBundle();
		ResourceBundle bundle = ResourceBundle.getBundle("edu.kit.joana.ifc.sdg.core.IflowcheckingPluginResources");
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

}

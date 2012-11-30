/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;
//package edu.kit.joana.ifc.sdg.core;
//
//import java.util.MissingResourceException;
//import java.util.ResourceBundle;
//
//import org.eclipse.ui.plugin.AbstractUIPlugin;
//import org.osgi.framework.BundleContext;
//
///**
// * The main plugin class to be used in the desktop.
// */
//public class IFlowCheckingPlugin extends AbstractUIPlugin {
//	//The shared instance.
//	private static IFlowCheckingPlugin plugin;
//	//Resource bundle.
//	/**
//	 * @uml.property  name="resourceBundle"
//	 */
//	private ResourceBundle resourceBundle;
//
//	/**
//	 * The constructor.
//	 */
//	public IFlowCheckingPlugin() {
//		super();
//
//		plugin = this;
//		try {
//			resourceBundle = ResourceBundle.getBundle("edu.kit.joana.ifc.sdg.core.IflowcheckingPluginResources");
//		} catch (MissingResourceException x) {
//			resourceBundle = null;
//		}
//	}
//
//	/**
//	 * This method is called upon plug-in activation
//	 */
//	public void start(BundleContext context) throws Exception {
//		super.start(context);
//	}
//
//	/**
//	 * This method is called when the plug-in is stopped
//	 */
//	public void stop(BundleContext context) throws Exception {
//		super.stop(context);
//	}
//
//	/**
//	 * Returns the shared instance.
//	 */
//	public static IFlowCheckingPlugin getDefault() {
//		return plugin;
//	}
//
//	/**
//	 * Returns the string from the plugin's resource bundle,
//	 * or 'key' if not found.
//	 */
//	public static String getResourceString(String key) {
//		ResourceBundle bundle = IFlowCheckingPlugin.getDefault().getResourceBundle();
//		try {
//			return (bundle != null) ? bundle.getString(key) : key;
//		} catch (MissingResourceException e) {
//			return key;
//		}
//	}
//
//	/**
//	 * Returns the plugin's resource bundle,
//	 * @uml.property  name="resourceBundle"
//	 */
//	public ResourceBundle getResourceBundle() {
//		return resourceBundle;
//	}
//}

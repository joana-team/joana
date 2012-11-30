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
package edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight;

import java.util.Map;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Provides highlighting for Java project.
 *
 * Realized as a singleton.
 *
 * @author kai brueckner, giffhorn
 */
public class HighlightPlugin extends AbstractUIPlugin {
    public static final int FIRST_LEVEL = -3;
    public static final int LAST_LEVEL  = 50;
    public final static String SDG_ID = "sdgID";

    //super type for the used marker types
    public static final String MARKER_ID = "edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.";

    //key for the color base
    public static final String COLOR_KEY = "highlight.baseColor";

    //key for the maximum levels available as marker
    public static final String MAXLEVELS = "highlight.maxLevels";
    //key for the base colors
    public static final String STEP_RED = "highlight.stepRed";
    public static final String STEP_BLUE = "highlight.stepBlue";
    public static final String STEP_GREEN = "highlight.stepGreen";


	//The shared instance.
	private static HighlightPlugin plugin;

	//marker factory
	private MarkerFactory mf;

	/**
	 * The constructor.
	 */
	public HighlightPlugin() {
		super();
		plugin = this;
		mf = new MarkerFactory();
	}

	/**
	 * Returns the shared instance.
	 */
	public static HighlightPlugin getDefault() {
		if (plugin == null) {
			plugin = new HighlightPlugin();
		}

		return plugin;
	}


	/* Plug-in-related code */

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
//		initializeDefaultPreferences(getPreferenceStore());
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight", path);
	}

//	// TODO: deprecated
//	public void initializeDefaultPreferences(IPreferenceStore fStore) {
//		fStore.setDefault(MAXLEVELS,50);
//		fStore.setDefault(STEP_RED,1.0);
//		fStore.setDefault(STEP_GREEN,0.1);
//		fStore.setDefault(STEP_BLUE,0.1);
//		fStore.setDefault(COLOR_KEY,(new RGB(230,140,100)).toString());
//		savePluginPreferences();
//	}


	/* Code highlighting */

	/**
	 * Highlights the given SDGNodes via markers.
	 * The given map defines which kind of marker shall be created for a node
	 * (edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.level. + the value in the map).
	 *
	 * @param project  The project which should be annotated.
	 * @param nodes    HashMap containing the SDGNodes to annotate and the level of the annotation.
	 */
	public void highlightAST (IProject project, Map<? extends SDGNode, Integer> nodes) {
	    mf.createMarkersAST(project, nodes);
	}

	/**
	 * Highlights the given SDGNodes via markers.
	 * The given map defines which kind of marker shall be created for a node
	 * (edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.level. + the value in the map).
	 *
	 * @param project  The project which should be annotated.
	 * @param nodes    HashMap containing the SDGNodes to annotate and the level of the annotation.
	 */
	public void highlightJC (IProject project, Map<? extends SDGNode, Integer> nodes) {
	    mf.createMarkersJC(project, nodes);
	}

	/**
	 * Highlights the given SDGNode via a marker.
	 *
	 * @param project which should be annotated
	 * @param node  the SDGNodes to annotate
	 * @param lvl the level of the annotation
	 */
	public void highlight (IProject project, SDGNode n, int lvl) {
	    if (lvl < FIRST_LEVEL) {
	        lvl = 0;
	    }

		if(lvl > LAST_LEVEL) {
			lvl = LAST_LEVEL;
		}

		mf.createCustomMarkerAST(project,n,MARKER_ID+"level"+lvl);
	}


	/* Clearing the highlights */

	/**
	 * Clears all highlights in the given project.
	 *
	 * The kind is
	 *
	 * @param project  A project.
	 */
	public void clearAll(IProject project) throws CoreException {
	    for (int lvl = FIRST_LEVEL; lvl <= LAST_LEVEL; lvl++) {
	        project.deleteMarkers(MARKER_ID+"level"+lvl, true, IResource.DEPTH_INFINITE);
	    }
	}

	/**
	 * Clears all highlights of a given kind in the given project.
	 *
	 * The kind is created by appending the given lvl to the identifier edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.level.
	 *
	 * @param project  A project.
	 * @param lvl      Identifies the kind of highlights that shall be removed.
	 */
	public void clearHighlight(IProject project, int lvl) throws CoreException {
		project.deleteMarkers(MARKER_ID+"level"+lvl, true, IResource.DEPTH_INFINITE);
	}

//    public File getLocalFile(String relativePath) {
//        File file = null;
//
//        try {
//            URL url = FileLocator.resolve(HighlightPlugin.getDefault().getBundle().getEntry("/"));
//
//            url = new URL(url, relativePath);
//
//            file = new File(url.toURI());
//
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//
//        } catch(URISyntaxException e) {
//            e.printStackTrace();
//        }
//
//        return file;
//    }

	@Override
	protected void initializeDefaultPluginPreferences() {
		getPreferenceStore().setDefault("graphviewer.path", "");
		getPreferenceStore().setDefault("graphviewer.port", 4444);
	}

}

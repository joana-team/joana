/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 17.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.gui.sdgworks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.deprecated.jsdg.gui.create.SDGCreationObserver;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerConstants;

/**
 * Offers several methods for generating and messing around with SDGs
 * @author naxan
 *
 */
public class SDGFactory implements SDGCreationObserver {

	/**
	 * Remembers SDGs for IProjects
	 */
	private HashMap<IProject, SDG> sdgCache = new HashMap<IProject, SDG>();

	/**
	 * Remembers Paths of SDGs
	 */
	private HashMap<SDG, String> sdgPath = new HashMap<SDG, String>();

	/**
	 * Remembers Violations for IProjects
	 */
	private HashMap<IProject, Collection<Violation>> vioRem = new HashMap<IProject, Collection<Violation>>();

	/**
	 * Holds violationChangeListeners that need to be notified in case of
	 * changed violation (caused by e.g. checkIFlow)
	 */
	private ArrayList<ViolationChangeListener> violationChangeListeners = new ArrayList<ViolationChangeListener>();

	public SDGFactory() {
		edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().addCreationObserver(this);
	}

	/**
	 * may return null if no cached sdg present for given IProject p
	 * @param p
	 * @return SDG from Cache
	 */
	public SDG getCachedSDG(IProject p) {
		SDG sdg = sdgCache.get(p);
		return sdg;
	}

	public String getSDGPath(SDG g) {
		return sdgPath.get(g);
	}

    public SDG loadSDG(ConfigReader cr)
    throws IOException, CoreException {
        // load SDG, annotate it with security markers and return it
        String sdgLocation = cr.getSDGLocation();
        IProject p = cr.getIProject();
        SDG sdg = SDG.readFrom(sdgLocation, new SecurityNode.SecurityNodeFactory());
        sdgPath.put(sdg, sdgLocation);

        return annotateSDG(sdg, p);
    }

    public SDG loadSDG(String sdgFile, IProject p)
    throws IOException, CoreException {
        // load SDG, annotate it with security markers and return it
        SDG sdg = SDG.readFrom(sdgFile, new SecurityNode.SecurityNodeFactory());
        sdgPath.put(sdg, sdgFile);

        return annotateSDG(sdg, p);
    }

	private SDG annotateSDG(SDG sec, IProject p)
	throws CoreException {
        // Annotate as far as known
        IMarker[] amarker = getAllMarkers(p);
        if (amarker != null) {
            applyAvailableMarkerMatchings(amarker, sec);
        }

        // cache the SDG
        sdgCache.put(p, sec);

        return sec;
	}

//	/***
//	 * Regenerates cached SDG as specified in standard configuration for project p
//	 * Extracts information from standard configuration and caches SDG returned from {@link #getSDG}
//	 * @param p
//	 * @param monitor
//	 * @throws CoreException
//	 */
//	public void regenerateCachedSDG(IProject p, IProgressMonitor monitor) throws CoreException {
//		if (p == null || monitor == null) return;
//
//		ConfigReader cr = new ConfigReader(LaunchConfigurationTools.getStandardLaunchConfiguration(p));
//        if( cr.configuration == null) return;
//
//		regenerateCachedSDG(cr, monitor);
//	}
//
//	private void regenerateCachedSDG(ConfigReader cr, IProgressMonitor monitor) {
//	    monitor.beginTask("Regenerating/Reading SDG", IProgressMonitor.UNKNOWN);
//
//	    // Construct new MyRunnableWithProgress, set its IProject p and run in ProgressMonitorDialog
//	    MyRunnableWithProgress op = new MyRunnableWithProgress();
//	    op.config(cr);
//
//	    try {
//	        op.run(monitor);
//
//	    } catch (InterruptedException e) {
//	        IStatus status= new Status(IStatus.ERROR,
//	                NJSecPlugin.getDefault().getBundle().getSymbolicName(), 0,
//	                "Error occured while generating SDG", e);
//	        NJSecErrorDialog.openError("Error",
//	                "Error occured while generating SDG", status);
//	        NJSecPlugin.getDefault().getLog().log(status);
//	    }
//	}


	/* ** SDG ANNOTATION ** */

	/**
	 * Returns all NJSec Annotation Markers used in IProject p
	 * Anotating Markers are: defining, redefining, restricting
	 * Update: only returns markers that are active.
	 * @param p
	 * @return
	 * @throws CoreException
	 */
	private IMarker[] getAllMarkers(IProject p)
	throws CoreException {
	    List<IMarker> markers = new LinkedList<IMarker>();
	    IMarker[] temp = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(p, NJSecMarkerConstants.MARKER_TYPE_INPUT);

	    for (int i = 0; i < temp.length; ++i) {
	    	if(temp[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true)) {
	    		markers.add(temp[i]);
	    	}
	    }

	    temp = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(p, NJSecMarkerConstants.MARKER_TYPE_REDEFINE);

	    for (int i = 0; i < temp.length; ++i) {
	    	if(temp[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true)) {
	    		markers.add(temp[i]);
	    	}
	    }

	    temp = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(p, NJSecMarkerConstants.MARKER_TYPE_OUTPUT);

	    for (int i = 0; i < temp.length; ++i) {
	    	if(temp[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true)) {
	    		markers.add(temp[i]);
	    	}
	    }

	    return markers.toArray(new IMarker[markers.size()]);
	}

	private void applyAvailableMarkerMatchings(IMarker[] annmarker, SDG g)
	throws CoreException {
	    for (int i = 0; i < annmarker.length; i++) {
	    	String[] nodeids = annmarker[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, "").split(";");

        	for (int j = 0; j < nodeids.length; ++j) {
        		if (!nodeids[j].equals("")) {
        			SecurityNode toAnnotate = (SecurityNode) g.getNode(Integer.parseInt(nodeids[j]));

		            if (toAnnotate != null) {
		                try {
		                    if (annmarker[i].getType().equals(NJSecMarkerConstants.MARKER_TYPE_INPUT)) {
		                        toAnnotate.setProvided(annmarker[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, ""));

		                    } else if (annmarker[i].getType().equals(NJSecMarkerConstants.MARKER_TYPE_OUTPUT)) {
		                        toAnnotate.setRequired(annmarker[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, ""));

		                    } else if (annmarker[i].getType().equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
		                    	String[] allProvided = annmarker[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").split(";");
		                    	String[] allRequired = annmarker[i].getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "").split(";");

		                        toAnnotate.setProvided(allProvided[0]);
		                        toAnnotate.setRequired(allRequired[0]);

		                        if (allProvided.length > 1) {
		                        	LinkedList<String[]> dec = new LinkedList<String[]>();
		                        	for (int x = 0; x < allProvided.length; x++) {
		                        		String p = allProvided[x];
		                        		String r = allRequired[x];
		                        		dec.add(new String[] {r, p});
		                        	}
		                        	toAnnotate.setAdditionalDeclass(dec);
		                        }
		                    }

		                } catch (CoreException e) {
		                    String msg = "Problem while getting Marker-Attributes for SDG Annotation: " + e.toString();
		                    NJSecPlugin.singleton().showError(msg, null, e);
		                }
		            }
        		}
	        }
	    }
	}

	public ArrayList<SecurityNode> findSecurityNodes(int sline, int hysteresis, String ssource, SDG g) {
	    ArrayList<SecurityNode> ret = new ArrayList<SecurityNode>();
	    String oldSource = null;

	    for (Object o : g.vertexSet()) {
	        SecurityNode vertex = (SecurityNode) o;
	        String source = vertex.getSource();

	        /***
	         * FIXME its a hack
	         * Falls ein Knoten keine Source Annotation hat,
	         * wird angenommen, er befaende sich im selben File wie der
	         * zuletzt aufgetretene Knoten mit Source Annotation
	         */
	        if (source == null) {
	            source = oldSource;
	        } else {
	            oldSource = source;
	        }

	        int startLine = vertex.getSr();

	        String mrp = ssource.replaceAll("\\\\", "/");
	        String sourcecom = null;
	        if (source != null) sourcecom = source.replaceAll("\\\\", "/");
	        mrp = mrp.endsWith(".class") ? mrp.substring(0, mrp.length()-6) + ".java" : mrp;

	        boolean sourceMatches = mrp != null && sourcecom != null && mrp.endsWith(sourcecom);

	        if ((sourceMatches && Math.abs(startLine-sline)<=hysteresis) || sline == 0) {
	            ret.add(vertex);
	        }
	    }

	    return ret;
	}

	/**
	 * Gibt eine Liste von SecurityNodes zurueck die innerhalb der gegebenen Methoden liegen.
	 * @param procs Liste mit Methodennummern.
	 * @param ssource
	 * @param g
	 * @return
	 */
	public ArrayList<SecurityNode> findSecurityNodesProc(List<Integer> procs, String ssource, SDG g) {
	    ArrayList<SecurityNode> ret = new ArrayList<SecurityNode>();
	    String oldSource = null;

	    for (Object o : g.vertexSet()) {
	        SecurityNode vertex = (SecurityNode) o;
	        String source = vertex.getSource();

	        /***
	         * FIXME its a hack
	         * Falls ein Knoten keine Source Annotation hat,
	         * wird angenommen, er befaende sich im selben File wie der
	         * zuletzt aufgetretene Knoten mit Source Annotation
	         */
	        if (source == null) {
	            source = oldSource;
	        } else {
	            oldSource = source;
	        }

	        String mrp = ssource.replaceAll("\\\\", "/");
	        String sourcecom = null;
	        if (source != null) sourcecom = source.replaceAll("\\\\", "/");
	        mrp = mrp.endsWith(".class") ? mrp.substring(0, mrp.length()-6) + ".java" : mrp;

	        boolean sourceMatches = mrp != null && sourcecom != null && mrp.endsWith(sourcecom);

	        if (sourceMatches && procs.contains(vertex.getProc())) {
	            ret.add(vertex);
	        }
	    }

	    return ret;
	}

	/* ** VIOLATIONS ** */

	/***
	 * Handling of Violations / ViolationChangelisteners, etc
	 * New Violations for a project should get announced through a call to violationsChanged
	 * Then every Listener is notified
	 *
	 * @param p
	 * @param violations
	 */
	public void violationsChanged(IProject p, Collection<Violation> violations) {
	    vioRem.put(p, violations);
	    if (p.equals(NJSecPlugin.singleton().getActiveProject())) {
	        notifyViolationChangeListeners(p, violations);
	    }
	}

	public void addViolationChangeListener(ViolationChangeListener vcl) {
	    if (!violationChangeListeners.contains(vcl)) violationChangeListeners.add(vcl);
	}

	public void notifyViolationChangeListeners(IProject p, Collection<Violation> violations) {
	    for (ViolationChangeListener v : violationChangeListeners) {
	        v.violationsChanged(p, violations);
	    }
	}

	public Collection<Violation> getRemViolations(IProject activeProject) {
	    Collection<Violation> ret = vioRem.get(activeProject);
	    if (ret == null) return new ArrayList<Violation>();
	    return ret;
	}

	@Override
	public void sdgChanged(String file) {
//		IProject p = NJSecPlugin.singleton().getActiveProject();
//		try {
//			SDG sdg = loadSDG(file, p);
//			sdgCache.put(p, sdg);
//		} catch (IOException e) {
//		} catch (CoreException e) {
//		}
	}
}

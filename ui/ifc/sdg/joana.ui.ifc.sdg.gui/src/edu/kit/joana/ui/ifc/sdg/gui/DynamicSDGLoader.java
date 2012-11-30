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
package edu.kit.joana.ui.ifc.sdg.gui;
//package edu.kit.joana.ifc.sdg.gui;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import edu.kit.joana.ifc.sdg.gui.launching.ConfigReader;
//import edu.kit.joana.ifc.sdg.gui.launching.LaunchConfigurationTools;
//
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IResource;
//import org.eclipse.core.resources.IResourceChangeEvent;
//import org.eclipse.core.resources.IResourceChangeListener;
//import org.eclipse.core.resources.IResourceDelta;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
//import org.eclipse.core.runtime.jobs.Job;
//import org.eclipse.jdt.core.IJavaElement;
//import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jface.dialogs.ErrorDialog;
//
//
//public class DynamicSDGLoader implements ActiveResourceChangeListener, IResourceChangeListener {
//
//	IProject lastProject = null;
//	HashMap<IResource, Long> timeStamps = new HashMap<IResource, Long>();
//
//	public void activeResourceChanged(IResource activeResource, IProject activeProject) {
//		if (lastProject != activeProject) {
//			if (NJSecPlugin.getDefault().getSDGFactory().getCachedSDG(activeProject) == null) {
//
//				/**
//				 * get necessary Informations from StandardLaunchConfiguration and regenerate SDG
//				 */
////				try {
//
//					this.reBuildProjectIfNecessary(activeProject);
////
////				} catch (CoreException e) {
////					ErrorDialog.openError(NJSecPlugin.getDefault().getWorkbench().getDisplay().getActiveShell(), "Error",
////							"Problem while generating SDG from DynamicLoader", e.getStatus());
////					NJSecPlugin.getDefault().getLog().log(e.getStatus());
////				}
//
//
//			}
//		}
//		lastProject = activeProject;
//	}
//
//
//	public void resourceChanged(IResourceChangeEvent event) {
//		if (event!=null && event.getDelta() != null && event.getDelta().getKind() == IResourceDelta.CHANGED) {
//
//			IResource[] mps = getModifiedClassFiles(event.getDelta());
//			IProject[] ps = getDirtyProjects(mps);
//
//			for (IProject p : ps) {
//				reBuildProjectIfNecessary(p);
//			}
//
//		}
//	}
//
//	public IProject[] getDirtyProjects(IResource[] rs) {
//		ArrayList<IProject> ret = new ArrayList<IProject>();
//		for (IResource r : rs) {
//			IProject p = r.getProject();
//			if (p != null) {
//				long newTimeStamp = r.getModificationStamp();
//				long oldTimeStamp = -1;
//				if (timeStamps.containsKey(r)){
//					oldTimeStamp = timeStamps.get(r);
//				}
//
//				if (newTimeStamp == oldTimeStamp) {
//
//				} else {
//					timeStamps.put(r, newTimeStamp);
//					ret.remove(p);
//					ret.add(p);
//				}
//			}
//		}
//		return ret.toArray(new IProject[0]);
//	}
//	public void reBuildProjectIfNecessary(IProject project) {
//
//
//		System.out.println(project.getName() + " CHANGED!");
//
//		try {
//			ConfigReader cr = new ConfigReader(LaunchConfigurationTools.getStandardLaunchConfiguration(project));
//			if (cr.getSDGRebuildNecessary() ||
//					NJSecPlugin.getDefault().getSDGFactory().getCachedSDG(project) == null) {
//
//				   class TrivialJob extends Job {
//					   public TrivialJob() {
//						   super("Generating SDG");
//						   setUser(true);
//					   }
//					   public IStatus run(IProgressMonitor monitor)  {
//						   IProject activeProject = (IProject)get("project");
//						   try {
//							NJSecPlugin.getDefault().getSDGFactory().regenerateCachedSDG(activeProject, monitor);
//							monitor.done();
//						} catch (CoreException e) {
//							put("exception", e);
//						}
//						   return Status.OK_STATUS;
//					   }
//
//					   private HashMap<Object, Object> properties = new HashMap<Object, Object>();
//
//
//					   public void put(Object key, Object value) {
//						   properties.put(key, value);
//					   }
//					   public Object get(Object key) {
//						   return properties.get(key);
//					   }
//				   }
//
//				   TrivialJob tj = new TrivialJob();
//
//					tj.put("project", project);
//					tj.schedule();
//					if (tj.get("exception") != null) throw (CoreException)tj.get("exception");
//
////				NJSecPlugin.getDefault().getSDGFactory().regenerateCachedSDG(project, null);
//			}
//		} catch (CoreException e) {
//			ErrorDialog.openError(NJSecPlugin.getDefault().getWorkbench().getDisplay().getActiveShell(), "Error",
//					"Problem while regenerating SDG dynamically", e.getStatus());
//			NJSecPlugin.getDefault().getLog().log(e.getStatus());
//		}
//	}
//
//	public IResource[] getModifiedClassFiles(IResourceDelta ird) {
//		ArrayList<IResource> ret = new ArrayList<IResource>();
//		ArrayList<IResourceDelta> work = new ArrayList<IResourceDelta>();
//		work.add(ird);
//
//		while (!work.isEmpty()) {
//			IResourceDelta now = work.remove(0);
//			for (IResourceDelta ir : now.getAffectedChildren()) {
//				work.add(ir);
//			}
//
//			IResource r = now.getResource();
//			if (r != null) {
//				IJavaElement je = JavaCore.create(r);
//
//				if (je != null && je.getElementType() == IJavaElement.CLASS_FILE) {
//					ret.remove(r);
//					ret.add(r);
//				}
//			}
//		}
//		return ret.toArray(new IResource[0]);
//
//	}
//}

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
package edu.kit.joana.ui.wala.flowless.actions;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.navigator.ResourceNavigator;

import edu.kit.joana.ui.wala.flowless.Activator;
import edu.kit.joana.ui.wala.flowless.util.ProjectUtil;
import edu.kit.joana.ui.wala.flowless.views.FlowLessView;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class CheckFlowAction extends Action implements ISelectionListener {

	private final FlowLessView view;
	private final FlowCheckResultConsumer resultConsumer;
	private IJavaProject currentProject = null;

	public CheckFlowAction(final FlowLessView view, final FlowCheckResultConsumer resultConsumer) {
		super();
		this.view = view;
		this.resultConsumer = resultConsumer;
		this.setText("Check IFC");
		this.setDescription("Check the IFC annotations of the current project.");
		this.setId("joana.ui.wala.flowless.checkFlowAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/check_flow_action.png"));
	}

	public final static class ProjectConf {

		public static final String TMP_DIR = "jSDG";

		private final IJavaProject jp;

		private final String binDir;
		private final List<String> srcDirs = new LinkedList<String>();
		private final List<String> extraJars = new LinkedList<String>();

		private ProjectConf(final IJavaProject jp, final String binDir) {
			if (binDir == null || jp == null) {
				throw new IllegalArgumentException();
			}

			this.jp = jp;
			this.binDir = binDir;
		}

		public static ProjectConf create(final IJavaProject jp) throws JavaModelException {
			final IPath binDir = jp.getOutputLocation();
			final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			final IFile resolvedBinDir = root.getFile(binDir);

			final ProjectConf pconf = new ProjectConf(jp, resolvedBinDir.getRawLocationURI().getRawPath());

			for (final IPath srcPath : ProjectUtil.findProjectSourcePaths(jp)) {
				final IFile resolvedSrcDir = root.getFile(srcPath);
				final IPath rawSrcDir = resolvedSrcDir.getRawLocation();
				if (rawSrcDir != null) {
					pconf.addSrc(rawSrcDir.toOSString());
				}
			}

			for (final IPath jarPath : ProjectUtil.findProjectJars(jp)) {
				final IFile resolvedJarFile = root.getFile(jarPath);
				final IPath rawJarFile = resolvedJarFile.getRawLocation();
				if (rawJarFile != null) {
					pconf.addJar(rawJarFile.toOSString());
				}
			}

			return pconf;
		}

		public IJavaProject getProject() {
			return jp;
		}

		private void addSrc(final String src) {
			if (src == null) {
				throw new IllegalArgumentException();
			}

			srcDirs.add(src);
		}

		private void addJar(final String jar) {
			if (jar == null) {
				throw new IllegalArgumentException();
			}

			extraJars.add(jar);
		}

		public String getBinDir() {
			return binDir;
		}

		public List<String> getSrcDirs() {
			return Collections.unmodifiableList(srcDirs);
		}

		public List<String> getJarDirs() {
			return Collections.unmodifiableList(extraJars);
		}

		public String getTempDir() throws CoreException {
			final IFolder projectTmpDir = jp.getProject().getFolder(TMP_DIR);
			final String dir = projectTmpDir.getRawLocation().makeAbsolute().toOSString();

			final File f = new File(dir);
			if (!f.exists()) {
				projectTmpDir.create(IResource.TEAM_PRIVATE | IResource.DERIVED, true, null);
			}

			return dir;
		}

		public String getLibLocation() {
			// libs are contained in the checkflow jar - at the root level
			// the loading routine should try to load the resource from the classloader, if the filesystem
			// does not contain the requested file.
			return "/";
		}

		public PrintStream getLogOut() {
			return System.out;
		}

		public String toString() {
			final StringBuffer sb = new StringBuffer("Project configuration of " + jp.getElementName() + "\n");

			sb.append("SRC:\n");
			for (final String src : srcDirs) {
				sb.append("\t'" + src + "'\n");
			}

			sb.append("JAR:\n");
			for (final String jar : extraJars) {
				sb.append("\t'" + jar + "'\n");
			}

			sb.append("BIN:\n\t'" + binDir + "'");

			return sb.toString();
		}
	}

	@Override
	public void run() {
		if (getCurrentProject() == null) {
			tryToGuessProject();
		}

		if (getCurrentProject() == null) {
			view.showMessage("Could not find current project. Aborting.");
			return;
		}

		final IJavaProject jp = getCurrentProject();
		try {
			resultConsumer.consume(null);
			final ProjectConf pconf = ProjectConf.create(jp);
			PlatformUI.getWorkbench().getProgressService().run(true, true, new CheckFlowRunnable(pconf, resultConsumer));
			view.showMessage(pconf.toString());
		} catch (InvocationTargetException e) {
			view.showMessage(e.getClass().getCanonicalName());
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			view.showMessage(e.getClass().getCanonicalName());
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (JavaModelException e) {
			view.showMessage(e.getClass().getCanonicalName());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static String getStackTrace(final Throwable t) {
		final StringWriter out = new StringWriter();
		t.printStackTrace(new PrintWriter(out));

		return out.toString();
	}

	@SuppressWarnings("deprecation")
	private boolean tryToGuessProject() {
		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final IEditorPart editorPart = page.getActiveEditor();

		if (editorPart instanceof JavaEditor) {
			final JavaEditor jed = (JavaEditor) editorPart;
			final IJavaElement jelem = JavaUI.getEditorInputJavaElement(jed.getEditorInput());
			final IJavaProject jp = jelem.getJavaProject();

			if (jp != null) {
				changeCurrentProjectTo(jp);
//				System.out.println("Selected through editor part: " + jp.getElementName());
				return true;
			}
		}

		for (final IViewReference vref : page.getViewReferences()) {
			final IViewPart part = vref.getView(false);
			if (!page.isPartVisible(part)) {
				continue;
			}
//			if (part instanceof PackageExplorerPart) {
//				final PackageExplorerPart pex = (PackageExplorerPart) part;
//				final ISelection sel = page.getSelection(pex.getClass().getCanonicalName());
//				System.out.println("Selected in package explorer: " + sel);
//			} else if (part instanceof ProjectExplorer) {
//				final ProjectExplorer pex = (ProjectExplorer) part;
//				pex.getNavigatorContentService().get
//				final ISelection sel = page.getSelection(pex.getClass().getCanonicalName());
//				System.out.println("Selected in project explorer: " + sel);
//			} else
			if (part instanceof ResourceNavigator) {
				final ResourceNavigator rn = (ResourceNavigator) part;
				final ISelection sel = rn.getTreeViewer().getSelection();
//				System.out.println("Selected in resource navigator: " + sel);
				if (sel instanceof IStructuredSelection) {
					final Object ires = ((IStructuredSelection) sel).getFirstElement();
					if (ires instanceof IResource) {
						final IProject p = ((IResource) ires).getProject();
						try {
							if (p.isNatureEnabled(JavaCore.NATURE_ID)) {
								final IJavaProject jp = JavaCore.create(p);
								changeCurrentProjectTo(jp);
								return true;
//								System.out.println("Found java project: " + jp.getElementName());
							}
						} catch (CoreException e) {}
					}
				}
			}
		}

		return false;
	}

	@Override
	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		if (part != this) {
			IJavaProject project = null;

			if (selection instanceof IStructuredSelection) {
				final IStructuredSelection ssel = (IStructuredSelection) selection;
				final Object first = ssel.getFirstElement();

			    if (first instanceof IResource) {
			        final IProject p = ((IResource) first).getProject();
			    } else if (first instanceof PackageFragmentRootContainer) {
			        project = ((PackageFragmentRootContainer) first).getJavaProject();
			    } else if (first instanceof IJavaElement) {
			        project = ((IJavaElement) first).getJavaProject();
			    }
			} else if (selection instanceof ITextSelection && part instanceof JavaEditor) {
				final ITextSelection tset = (ITextSelection) selection;
				final JavaEditor jed = (JavaEditor) part;
				try {
					final IJavaElement element = SelectionConverter.resolveEnclosingElement(jed, tset);
			        project = element.getJavaProject();
				} catch (JavaModelException e) {}
		    }

			if (project != null) {
//				System.out.println("New project selected: " + project.getElementName());
				changeCurrentProjectTo(project);
			}
		}
	}

	private synchronized void changeCurrentProjectTo(final IJavaProject jp) {
		assert jp != null;
		if (jp != currentProject) {
			this.currentProject = jp;
		}
	}

	public synchronized IJavaProject getCurrentProject() {
		return this.currentProject;
	}

}

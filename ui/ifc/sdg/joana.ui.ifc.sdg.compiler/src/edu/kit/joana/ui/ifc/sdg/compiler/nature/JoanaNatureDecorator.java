/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.nature;

import java.util.ArrayList;
import java.util.Collection;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A JFace label decorator used for displaying the JOANA nature icon on a
 * project having the JOANA nature. This decorator is used by a extension to the
 * org.eclipse.ui.decorators extension point.
 *
 * To react on JOANA nature assignment state changes, it implements the
 * <code>IJoanaNatureChangeListener</code> interface and registers itself at
 * the plugins activator upon creation.
 *
 */
public class JoanaNatureDecorator implements ILightweightLabelDecorator, IJoanaNatureChangeListener {

	private Collection<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IJavaProject) {
			IJavaProject javaProject = (IJavaProject) element;
			try {
				if (PluginUtil.hasNature(javaProject.getProject(), JoanaNature.ID)) {
					ImageRegistry ir = Activator.getDefault().getImageRegistry();
					ImageDescriptor desc = ir.getDescriptor(Activator.IMAGE_JOANA_NATURE);
					decoration.addOverlay(desc, IDecoration.TOP_LEFT);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public void addListener(ILabelProviderListener listener) {
		listeners.add(listener);
	}

	public void dispose() {
		Activator.getDefault().removeJoanaNatureChangeListener(this);
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Constructor
	 */
	public JoanaNatureDecorator() {
		Activator.getDefault().addJoanaNatureChangeListener(this);
	}

	public void notifyJoanaNatureChanged(IJavaProject project) {
		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, project);
		for (ILabelProviderListener l : listeners)
			l.labelProviderChanged(event);
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.configUI;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Implements a preference store for a project. It uses a project's persistend
 * property mechanism to store values. This implementation does not properly
 * support the modification of deafult values.
 *
 */
public class ProjectPreferenceStore extends EventManager implements IPreferenceStore {

	private final IProject project;
	private final String namespace;

	/**
	 * Constructor
	 *
	 * @param project
	 *            the project to store the preferences for
	 * @param namespace
	 *            the namespace to be used while storing the properties
	 */
	public ProjectPreferenceStore(IProject project, String namespace) {
		assert project != null;
		this.project = project;
		this.namespace = namespace;
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		addListenerObject(listener);
	}

	public boolean contains(String name) {
		try {
			return getProperty(name) != null;
		} catch (CoreException e) {
			return false;
		}
	}

	private String getProperty(String name, String def) {
		try {
			String val = getProperty(name);
			if (val == null)
				return def;
			return val;
		} catch (CoreException e) {
			return def;
		}
	}

	private String getProperty(String name) throws CoreException {
		return project.getPersistentProperty(getQualifiedName(name));
	}

	private QualifiedName getQualifiedName(String name) {
		return new QualifiedName(namespace, name);
	}

	public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
		PropertyChangeEvent event = new PropertyChangeEvent(project, name, oldValue, newValue);
		for (Object o : getListeners())
			((IPropertyChangeListener) o).propertyChange(event);
	}

	public boolean getBoolean(String name) {
		String value = getProperty(name, IPreferenceStore.FALSE);
		return value.equals(IPreferenceStore.TRUE);
	}

	public boolean getDefaultBoolean(String name) {
		return false;
	}

	public double getDefaultDouble(String name) {
		return 0;
	}

	public float getDefaultFloat(String name) {
		return 0;
	}

	public int getDefaultInt(String name) {
		return 0;
	}

	public long getDefaultLong(String name) {
		return 0;
	}

	public String getDefaultString(String name) {
		return null;
	}

	public double getDouble(String name) {
		String val = getProperty(name, "0");
		try {
			return Double.parseDouble(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public float getFloat(String name) {
		String val = getProperty(name, "0");
		try {
			return Float.parseFloat(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getInt(String name) {
		String val = getProperty(name, "0");
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public long getLong(String name) {
		String val = getProperty(name, "0");
		try {
			return Long.parseLong(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getString(String name) {
		String val = getProperty(name, "");
		try {
			return val;
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public boolean isDefault(String name) {
		try {
			return getProperty(name) == null;
		} catch (CoreException e) {
			return false;
		}
	}

	public boolean needsSaving() {
		return false;
	}

	public void putValue(String name, String value) {
		try {
			project.setPersistentProperty(getQualifiedName(name), value);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		this.removeListenerObject(listener);
	}

	public void setDefault(String name, double value) {
	}

	public void setDefault(String name, float value) {
	}

	public void setDefault(String name, int value) {
	}

	public void setDefault(String name, long value) {
	}

	public void setDefault(String name, String defaultObject) {
	}

	public void setDefault(String name, boolean value) {
	}

	public void setToDefault(String name) {
		putValue(name, null);
	}

	public void setValue(String name, double value) {
		putValue(name, "" + value);
	}

	public void setValue(String name, float value) {
		putValue(name, "" + value);
	}

	public void setValue(String name, int value) {
		putValue(name, "" + value);
	}

	public void setValue(String name, long value) {
		putValue(name, "" + value);
	}

	public void setValue(String name, String value) {
		putValue(name, "" + value);
	}

	public void setValue(String name, boolean value) {
		putValue(name, "" + value);
	}
}

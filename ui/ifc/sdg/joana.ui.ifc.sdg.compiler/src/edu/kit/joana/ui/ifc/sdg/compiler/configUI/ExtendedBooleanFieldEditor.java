/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.configUI;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * Enhances JFace's <code>BooleanFieldEditor</code> to support multiple
 * property change listeners.
 *
 */
public class ExtendedBooleanFieldEditor extends BooleanFieldEditor implements IPropertyChangeListener {

	/**
	 * Constructor
	 */
	public ExtendedBooleanFieldEditor() {
		super();
		super.setPropertyChangeListener(this);
	}

	/**
	 * Constructor
	 *
	 * @param name
	 *            the name of the edited property
	 * @param label
	 *            the text to be displayed next to the control
	 * @param parent
	 *            the parent composite
	 */
	public ExtendedBooleanFieldEditor(String name, String label, Composite parent) {
		super(name, label, parent);
		super.setPropertyChangeListener(this);
	}

	/**
	 * Constructor
	 *
	 * @param name
	 *            the name of the edited property
	 * @param style
	 *            the style of the control
	 * @param labelText
	 *            the text to be displayed next to the control
	 * @param parent
	 *            the parent composite
	 */
	public ExtendedBooleanFieldEditor(String name, String labelText, int style, Composite parent) {
		super(name, labelText, style, parent);
		super.setPropertyChangeListener(this);
	}

	private IPropertyChangeListener auxListener;

	@Override
	public void setPropertyChangeListener(IPropertyChangeListener listener) {
		auxListener = listener;
	}

	private Collection<IPropertyChangeListener> listeners = new ArrayList<IPropertyChangeListener>();

	/**
	 * Adds a property change listener
	 *
	 * @param listener
	 *            the property change listener to be added
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a property change listener
	 *
	 * @param listener
	 *            the property change listener to be removed
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	public void propertyChange(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : listeners)
			listener.propertyChange(event);
		if (auxListener != null)
			auxListener.propertyChange(event);
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;


@SuppressWarnings("rawtypes")
public class EntryMethodsComboBoxModel implements ComboBoxModel {

	private List<JavaMethodSignature> entries;
	private JavaMethodSignature selected = null;
	private final Set<ListDataListener> listeners = new HashSet<ListDataListener>();
	private boolean searchInProgress = false;

	public EntryMethodsComboBoxModel() {
		this.entries = new LinkedList<JavaMethodSignature>();
	}

	@Override
	public void addListDataListener(ListDataListener arg0) {
		listeners.add(arg0);
	}

	@Override
	public synchronized Object getElementAt(int arg0) {
		if (!entries.isEmpty()) {
			return entries.get(arg0);
		} else if (searchInProgress) {
			return "searching entry methods... ";
		} else {
			return "hit update to search for entries in the current classpath";
		}
	}

	public synchronized void searchStarted() {
		if (!searchInProgress) {
			this.searchInProgress = true;
			this.selected = null;
			final int size = entries.size();
			this.entries.clear();

			for (ListDataListener l : listeners) {
				l.contentsChanged(new ListDataEvent(this,
						ListDataEvent.INTERVAL_REMOVED, 0, size));
			}
		}
	}

	public synchronized void searchFinished(List<JavaMethodSignature> found) {
		this.searchInProgress = false;

		if (this.entries.size() > 0) {
			final int size = entries.size();
			this.entries.clear();

			for (ListDataListener l : listeners) {
				l.contentsChanged(new ListDataEvent(this,
						ListDataEvent.INTERVAL_REMOVED, 0, size));
			}
		}

		this.entries.addAll(found);




		final int size = entries.size();

		for (ListDataListener l : listeners) {
			l.contentsChanged(new ListDataEvent(this,
					ListDataEvent.INTERVAL_ADDED, 0, size));
		}
	}

	@Override
	public int getSize() {
		if (entries.isEmpty()) {
			return 0;
		} else {
			return entries.size();
		}
	}

	@Override
	public void removeListDataListener(ListDataListener arg0) {
		listeners.remove(arg0);
	}

	@Override
	public synchronized Object getSelectedItem() {
		if (searchInProgress) {
			return "searching entry methods... ";
		} else if (entries.isEmpty()) {
			return "hit update to search for entries in the current classpath";
		} else if (selected == null) {
			return "please select entry method from the list";
		} else {
			return selected;
		}
	}

	@Override
	public void setSelectedItem(Object arg0) {
		if (arg0 instanceof JavaMethodSignature) {
			if (selected != arg0) {
				selected = (JavaMethodSignature) arg0;
			}
		} else if (selected != null) {
			selected = null;
		}
	}

}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.latticeeditor.ui;

import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeProblemDescription;
import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeChangedListener;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeEditorPlugin;

public class LatticeValidationJob<ElementType> extends Job {

	private IEditableLattice<ElementType> lattice;

	private Collection<LatticeChangedListener<ElementType>> listeners;

	private IResource res;

	private String latticeSource;

	public LatticeValidationJob(IEditableLattice<ElementType> lattice, String latticeName, IResource res, String latticeSource, Collection<LatticeChangedListener<ElementType>> listeners) {
		super("Validating lattice '" + latticeName + "'");
		assert lattice != null;
		assert latticeName != null;
		assert listeners != null;
		assert latticeSource != null;

		this.latticeSource = latticeSource;
		this.res = res;
		this.lattice = lattice;
		this.listeners = listeners;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Status status = new Status(IStatus.OK, LatticeEditorPlugin.PLUGIN_ID, "Lattice validation finished");
		LatticeProblemDescription<ElementType> problem = null;
		try {
			problem = LatticeValidator.validateIncremental(lattice);
		} catch (Exception e) {
			e.printStackTrace();
			status = new Status(IStatus.ERROR, LatticeEditorPlugin.PLUGIN_ID, "Lattice validation failed", e);
			problem = new LatticeProblemDescription<ElementType>("Lattice validation failed");
		}
		for (LatticeChangedListener<ElementType> listener : listeners)
			listener.latticeChanged(res, latticeSource, problem);
		return status;
	}

}

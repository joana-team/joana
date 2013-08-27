/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.ifcalgorithms;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import edu.kit.joana.ifc.sdg.core.ClassifyingIFC;
import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.metrics.IMetrics;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class IFCJob extends Job {
    protected IProject p;
    protected SDG sdg;
    protected IStaticLattice<String> l;
    protected ClassifyingIFC ifc;
    protected Collection<IMetrics> metrics;
    protected Collection<ClassifiedViolation> violations;

    private CoreException ex;

    public IFCJob(String str, IProject p, SDG sdg, IStaticLattice<String> l, IFC ifc) {
        super(str);
        setUser(true);
        this.p = p;
        this.sdg = sdg;
        this.l = l;
        metrics = new LinkedList<IMetrics>();
        this.ifc = new ClassifyingIFC(ifc);
    }

    public void addMetrics(IMetrics m) {
    	metrics.add(m);
    }

    public Collection<ClassifiedViolation> checkIFlow(IProgressMonitor monitor) throws CoreException {
        // set progress listener
        Progress myp = new Progress(monitor);
        ifc.addProgressListener(myp);

        int nodeCount = sdg.vertexSet().size() * sdg.vertexSet().size();
        monitor.beginTask("Checking Security", nodeCount);
        Collection<ClassifiedViolation> vios = null;

        try {
            vios = ifc.checkIFlow();
            for (IMetrics m : metrics) {
            	vios = m.computeMetrics(sdg, vios);
            }

        } catch (NotInLatticeException e) {
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0,
                    "Some element is not in given Lattice: " + e.toString(), e);
            throw new CoreException(status);
        }

        monitor.done();

        return vios;
    }

    public IStatus run(IProgressMonitor monitor) {
         try {
             violations = checkIFlow(monitor);
             NJSecPlugin.singleton().getSDGFactory().violationsChanged(p, violations);

         } catch (CoreException e) {
             ex = e;
         }

        return Status.OK_STATUS;
    }

    public CoreException getCoreException() {
        return ex;
    }
}

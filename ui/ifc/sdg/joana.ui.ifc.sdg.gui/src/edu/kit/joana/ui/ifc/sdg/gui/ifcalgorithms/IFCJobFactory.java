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
package edu.kit.joana.ui.ifc.sdg.gui.ifcalgorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import edu.kit.joana.ifc.sdg.core.InterFlowChecker7;
import edu.kit.joana.ifc.sdg.core.IntransitiveIFCChecker;
import edu.kit.joana.ifc.sdg.core.conc.PossibilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.metrics.CallGraphMetrics;
import edu.kit.joana.ifc.sdg.core.metrics.DistanceMetrics;
import edu.kit.joana.ifc.sdg.core.metrics.ImplicitExplicitFlowMetrics;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;

public final class IFCJobFactory {
    private IFCJobFactory() { }

    public static IFCJob createIFCJob(ConfigReader cr, IProgressMonitor monitor, SDG sdg)
    throws CoreException {
        IFCJob job = null;

        IProject p = cr.getIProject();
        File latticeFile = new File(cr.getLatticeLocation());
        IStaticLattice<String> l = null;

        try {
            l = LatticeUtil.compileBitsetLattice(new FileInputStream(latticeFile));

        } catch (IOException e) {
            monitor.isCanceled();
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Couldn't create or read lattice File", e);
            throw new CoreException(status);

        } catch (WrongLatticeDefinitionException e) {
            monitor.isCanceled();
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Invalid lattice File", e);
            throw new CoreException(status);
        }

        if (cr.getClassicNI()) {
        	InterFlowChecker7 ifc = new InterFlowChecker7(sdg, l);
            job = new IFCJob("Checking Security", p, sdg, l, ifc);
            job.addMetrics(new ImplicitExplicitFlowMetrics());
            job.addMetrics(new DistanceMetrics());
            job.addMetrics(new CallGraphMetrics());

        } else if (cr.getClassicNIWithTermination()) {


        } else if (cr.getKrinkeNI()) {
            IntransitiveIFCChecker ifc = new IntransitiveIFCChecker(sdg, l);
            job = new IFCJob("Checking Security", p, sdg, l, ifc);
            job.addMetrics(new ImplicitExplicitFlowMetrics());
            job.addMetrics(new DistanceMetrics());
            job.addMetrics(new CallGraphMetrics());

        } else if (cr.getPossibilisticNI()) {
        	PossibilisticNIChecker ifc = new PossibilisticNIChecker(sdg, l);
            job = new IFCJob("Checking Possibilistic Security", p, sdg, l, ifc);
            job.addMetrics(new ImplicitExplicitFlowMetrics());
            job.addMetrics(new DistanceMetrics());
            job.addMetrics(new CallGraphMetrics());

        } else if (cr.getPossibilisticNIWithTermination()) {

        } else if (cr.getProbabilisticNI()) {
        	ProbabilisticNIChecker ifc = new ProbabilisticNIChecker(sdg, l);
            job = new IFCJob("Checking Probabilistic Security", p, sdg, l, ifc);
            job.addMetrics(new ImplicitExplicitFlowMetrics());
            job.addMetrics(new DistanceMetrics());
            job.addMetrics(new CallGraphMetrics());

        } else if (cr.getProbabilisticNIWithTermination()) {

        }

        return job;
    }
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;


/**
 * Anbindung an den Algorithmus fuer possibilistische Noninterferenz.
 * Hauptaufgabe ist das Managen von ProgressListenern.
 *
 * @author giffhorn
 */
public class PossibilisticNIChecker extends IFC {
	private static final boolean DEBUG = false;

    /** Erzeugt eine neue Instanz.
     *
     * @param sdg       Ein SDG
     * @param lattice   Ein Sicherheitsverband
     */
    public PossibilisticNIChecker(SDG sdg, IStaticLattice<String> lattice) {
        super(sdg, lattice);
    }

    /** Erzeugt eine neue Instanz
     *
     * @param sdg       Eine Datei, die einen SDG enthaelt
     * @param lattice   Eine Datei, die einen Sicherheitsverband enthaelt
     *
     * @throws WrongLatticeDefinitionException
     * @throws IOException
     */
    public PossibilisticNIChecker(File sdg, File lattice) throws WrongLatticeDefinitionException, IOException {
        super(sdg, lattice);
    }

    /** Berechnet, ob der SDG possibilistisch noninterferent ist.
     *
     * Ruft dazu den BarrierIFCSlicer auf. Misst Ausfuehrungszeiten und aktualisiert die Progressbar.
     *
     * @return Eine Liste mit den gefundenen Sicherheitsverletzungen.
     *
     * @throws NotInLatticeException
     */
    public Collection<Violation> checkIFlow() throws NotInLatticeException {
        long slicestart = System.currentTimeMillis();
        if (DEBUG) System.out.println("Checking possibilistic noninterference");
        Collection<Violation> ret = null;    //list to be returned
        BarrierIFCSlicer is = new BarrierIFCSlicer(g, l);

        is.addProgressListener(this);

        if (DEBUG) System.out.println("Started slicing at " + slicestart);

        try{
            ret = is.checkIFlow();
        } catch(Exception e) {e.printStackTrace();}

        long sliceend = System.currentTimeMillis();
        if (DEBUG) System.out.println("time: " + (sliceend-slicestart));
        if (DEBUG) System.out.println("Finished slicing at " + sliceend + " | slice duration: " + (sliceend-slicestart));

        if (DEBUG) System.out.println("ret.size: " + ret.size() + " ret: " + ret);
        this.progressChanged("Done", 100, 100);
        return ret;
    }
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 28.06.2004
 *
 */
package edu.kit.joana.ifc.sdg.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;


//import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Abstrakte Klasse fuer IFC-Algorithmen.
 * Hauptaufgabe ist das Managen von ProgressListenern.
 * Die Methode checkIFlow() enthaelt die konkrete Implementierung des Algorithmus.
 *
 * @author giffhorn
 */
public abstract class IFC implements ProgressListener, ProgressAnnouncer {
	
	
	
	private ArrayList<ProgressListener> progressListeners = new ArrayList<ProgressListener>();

    // der SDG
	protected SDG g;
    // der lattice
	protected IStaticLattice<String> l;


    /** Berechnet, ob der SDG noninterferent ist und aktualisiert die Progressbar.
     *
     * @return Eine Liste mit den gefundenen Sicherheitsverletzungen.
     *
     * @throws NotInLatticeException
     */
	public abstract Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException;


    /** Erzeugt eine neue Instanz.
     *
     * @param sdg       Ein SDG
     * @param lattice   Ein Sicherheitsverband
     */
	public IFC(SDG sdg, IStaticLattice<String> lattice) {
		this.l = lattice;
	    setSDG(sdg);
	}

	public SDG getSDG() {
		return g;
	}

	public IStaticLattice<String> getLattice() {
		return l;
	}

    /** Erzeugt eine neue Instanz
     *
     * @param sdg       Eine Datei, die einen SDG enthaelt
     * @param lattice   Eine Datei, die einen Sicherheitsverband enthaelt
     *
     * @throws WrongLatticeDefinitionException
     * @throws IOException
     */
	public IFC(File sdg, File lattice) throws WrongLatticeDefinitionException, IOException {
		setLattice(lattice);
		setSDG(sdg);
	}


    /** Zur Wiederverwendung mit neuem Verband.
     *
     * @param lattice   Eine Datei, die einen Verband enthaelt.
     *
     * @throws WrongLatticeDefinitionException
     * @throws IOException
     */
	public void setLattice(File lattice) throws WrongLatticeDefinitionException, IOException {
		this.l = LatticeUtil.compileBitsetLattice(new FileInputStream(lattice));
	}

    /** Zur Wiederverwendung mit neuem Verband.
     *
     * @param lattice   Ein Verband.
     */
    public void setLattice(IStaticLattice<String> lattice) {
        this.l = lattice;
    }

    /** Zur Wiederverwendung mit einem neuen SDG.
     *
     * @param sdg   Ein SDG als String.
     *
     * @throws IOException
     */
    public void setSDG(String sdg) throws IOException {
        SDG graph = SDG.readFrom(new StringReader(sdg));
        setSDG(graph);
    }

    /** Zur Wiederverwendung mit einem neuen SDG.
     *
     * @param sdg   Ein SDG.
     */
    public void setSDG(SDG sdg) {
        this.g = sdg;
    }

    /** Zur Wiederverwendung mit einem neuen SDG.
     *
     * @param sdgfile   Eine Datei, die einen SDG enthaelt.
     *
     * @throws IOException
     */
    public void setSDG(File sdgfile) throws IOException {
        SDG graph = SDG.readFrom(new FileReader(sdgfile));
        setSDG(graph);
    }


    /* ProgressListener stuff */

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer#addProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void addProgressListener(ProgressListener pl) {
		if (!progressListeners.contains(pl)) progressListeners.add(pl);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer#removeProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void removeProgressListener(ProgressListener pl) {
		progressListeners.remove(pl);
	}

	public void notifyProgressListeners(String progressTitle, int progress, int maxProgress) {
		for (ProgressListener p : progressListeners) {
			p.progressChanged(progressTitle, progress, maxProgress);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener#progressChanged(java.lang.String, int, int)
	 */
	public void progressChanged(String progressTitle, int progress, int progressmax) {
		notifyProgressListeners(progressTitle, progress, progressmax);
	}

//	public IProgressMonitor getMonitor() {
//		for (ProgressListener p : progressListeners) {
//			return p.getMonitor();
//		}
//		return null;
//	}
}

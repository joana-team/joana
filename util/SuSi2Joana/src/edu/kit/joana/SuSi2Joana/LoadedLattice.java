package edu.kit.joana.SuSi2Joana;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.io.File;

public class LoadedLattice implements ILatticeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LoadedLattice.class);
    private final IEditableLattice<String> lattice;

    public LoadedLattice(File latFile) {
        try {
            this.lattice = LatticeUtil.loadLattice(latFile.getAbsolutePath());
        } catch (WrongLatticeDefinitionException e) {
            throw new IllegalStateException(e);
        }
    }

    public void extract(List<? extends SuSiFile.Entry> in) {
    }

    public String getSource(String cat) {
        try {
            this.lattice.getImmediatelyGreater(cat); // there's no contains method - we'll try-catch instead
            return cat;
        } catch (NotInLatticeException e) {
            logger.error("The lattice does not contain an entry for " + cat);
            return this.lattice.getTop();
        }
    }

    public String getSink(String cat) {
        try {
            this.lattice.getImmediatelyGreater(cat); // there's no contains method - we'll try-catch instead
            return cat;
        } catch (NotInLatticeException e) {
            logger.error("The lattice does not contain an entry for " + cat);
            return this.lattice.getBottom();
        }
    }

    public void write(final File out) throws IOException {
        logger.warn("Skipping to write out a loaded lattice.");
    }
}

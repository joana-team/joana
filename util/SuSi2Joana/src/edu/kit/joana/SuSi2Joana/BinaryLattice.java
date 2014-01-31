package edu.kit.joana.SuSi2Joana;

import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;

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

public class BinaryLattice implements ILatticeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(BinaryLattice.class);
    private final EditableLatticeSimple<String> lattice;

    public BinaryLattice() {
        this.lattice = new EditableLatticeSimple<String>();
        this.lattice.addElement("high");
        this.lattice.addElement("low");
        this.lattice.setImmediatelyGreater("low", "high");
    }

    public void extract(List<? extends SuSiFile.Entry> in) {
    }

    public String getSource(String cat) {
        return this.lattice.getTop();
    }

    public String getSink(String cat) {
        return this.lattice.getBottom();
    }

    public void write(final File out) throws IOException {
        logger.warn("Writing out the binary lattice - how exciting :)");
        final PrintStream ps = new PrintStream(out);

        this.writeLower(ps, this.lattice.getTop());
        logger.info("Written Lattice to " + out.getName());
        ps.close();
    }

    private void writeLower(final PrintStream out, String elem) {
        for (String lower : this.lattice.getImmediatelyLower(elem)) {
            out.println(lower + "<=" + elem);
            this.writeLower(out, lower);
        }
    }
}

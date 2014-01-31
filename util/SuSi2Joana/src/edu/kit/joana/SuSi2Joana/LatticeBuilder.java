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

public class LatticeBuilder implements ILatticeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LatticeBuilder.class);
    private final Set<String> entries = new HashSet<String>();
    private final EditableLatticeSimple<String> lattice;

    public LatticeBuilder() {
        this.lattice = new EditableLatticeSimple<String>();
        this.lattice.addElement("secure");
        this.lattice.addElement("insecure");
    }

    public void extract(List<? extends SuSiFile.Entry> in) {
        for (final SuSiFile.Entry e : in) {
            if (this.entries.add(e.cathegory)) {
                this.lattice.addElement(e.cathegory);
                this.lattice.setImmediatelyGreater(e.cathegory, "secure");
                this.lattice.setImmediatelyLower(e.cathegory, "insecure");
                logger.debug("Added " + e.cathegory);
            }
        }
    }

    public String getSource(String cat) {
        return cat;
    }

    public String getSink(String cat) {
        return cat;
    }

    public void write(final File out) throws IOException {
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

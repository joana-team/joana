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

public interface ILatticeBuilder {
    /**
     *  Collect Lattice-Elements from SuSi-file entries.
     */
    public void extract(List<? extends SuSiFile.Entry> in);

    /**
     *  Return a Lattice-Element for a Source of category cat.
     */
    public String getSource(String cat);

    /**
     *  Return a Lattice-Element for a Sink of category cat.
     */
    public String getSink(String cat);

    /**
     *  Dump the lattice to a file
     */
    public void write(final File out) throws IOException;
}

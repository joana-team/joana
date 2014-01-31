package edu.kit.joana.SuSi2Joana;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

import java.io.File;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import java.io.IOException;

public class SdgHandler {
    private final Logger logger = LoggerFactory.getLogger(SdgHandler.class);
    private SDG sdg;
    private IProgressMonitor mon;

    public SdgHandler(IProgressMonitor mon) {
        if (mon == null) {
            this.mon = new NullProgressMonitor();
        } else {
            this.mon = mon;
        }
    }

    public void readSDG(File in) {
        if (in == null) {
            throw new IllegalArgumentException("in may not be null");
        }

        final String path = in.getAbsolutePath();
        
        try {
            logger.info("Reading SDG from " + path);
            mon.beginTask("Reading SDG", IProgressMonitor.UNKNOWN);
            this.sdg = SDG.readFrom(path);
            mon.done();
        } catch (IOException e) {
            this.sdg = null;    
            logger.error("I/O error while reading sdg from file " + path);
        }
    }

    public void matchSources(List<? extends SuSiFile.Source> from, IfcScript into) {
        this.mon.beginTask("Extracting Call-Sites...", IProgressMonitor.UNKNOWN);
        final List< SDGNodeTuple > csites = this.sdg.getAllCallSites();
        this.mon.done();

        this.mon.beginTask("Matching Sources", csites.size());
nextCSite:
        for (int i = 0; i < csites.size(); ++i) {
            { // Using first
                final String meth = csites.get(i).getFirstNode().getBytecodeMethod();
                for (final SuSiFile.Entry e : from) {
                    if (meth.startsWith(e.clazz)) {
                        final String name = e.method.substring(0, e.method.indexOf("(")); // XXX ignores signature
                        if (meth.startsWith(e.clazz + "." + name)) {
                            logger.debug("Selecting " + meth + " as " + e.cathegory);
                            into.addSource(meth, e.cathegory);
                            continue nextCSite;
                        }
                    }
                }
            }
            { // Using second
                final String meth = csites.get(i).getSecondNode().getBytecodeMethod();
                for (final SuSiFile.Entry e : from) {
                    if (meth.startsWith(e.clazz)) {
                        final String name = e.method.substring(0, e.method.indexOf("(")); // XXX ignores signature
                        if (meth.startsWith(e.clazz + "." + name)) {
                            logger.debug("Selecting " + meth + " as " + e.cathegory);
                            into.addSource(meth, e.cathegory);
                            continue nextCSite;
                        }
                    }
                }
            }

            this.mon.worked(i);
        }
    }


    public void matchSinks(List<? extends SuSiFile.Sink> from, IfcScript into) {
        this.mon.beginTask("Extracting Call-Sites...", IProgressMonitor.UNKNOWN);
        final List< SDGNodeTuple > csites = this.sdg.getAllCallSites();
        this.mon.done();

        this.mon.beginTask("Matching Sinks", csites.size());
nextCSite:
        for (int i = 0; i < csites.size(); ++i) {
            { // Using first
                final String meth = csites.get(i).getFirstNode().getBytecodeMethod();
                for (final SuSiFile.Entry e : from) {
                    if (meth.startsWith(e.clazz)) {
                        final String name = e.method.substring(0, e.method.indexOf("(")); // XXX ignores signature
                        if (meth.startsWith(e.clazz + "." + name)) {
                            logger.debug("Selecting " + meth + " as " + e.cathegory);
                            into.addSink(meth, e.cathegory);
                            continue nextCSite;
                        }
                    }
                }
            }
            { // Using second
                final String meth = csites.get(i).getSecondNode().getBytecodeMethod();
                for (final SuSiFile.Entry e : from) {
                    if (meth.startsWith(e.clazz)) {
                        final String name = e.method.substring(0, e.method.indexOf("(")); // XXX ignores signature
                        if (meth.startsWith(e.clazz + "." + name)) {
                            logger.debug("Selecting " + meth + " as " + e.cathegory);
                            into.addSink(meth, e.cathegory);
                            continue nextCSite;
                        }
                    }
                }
            }

            this.mon.worked(i);
        }
    }


}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 01.02.2005
 *
 */
package edu.kit.joana.ifc.sdg.core.violations.paths;

import java.util.ArrayList;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;


/**
 * VioPathGenerator offers a method that generates<br>
 * all Pathes between to given nodes
 *
 * @author naxan
 *
 */
public class PathGenerator implements ProgressAnnouncer {

	private final ArrayList<ProgressListener> progressListeners = new ArrayList<ProgressListener>();
	/**
	 * @uml.property  name="firstProgressNotify"
	 */
//	private boolean firstProgressNotify = true;
	/**
	 * @uml.property  name="progress"
	 */
//	private int progress = 1;
	/**
	 * @uml.property  name="maxPathes"
	 */
//	private static final int maxPathes = 250;

    private final ContextSensitiveThreadChopper chopper;
	private final PathCollector pathCollector;

	/**
	 * Constructs a new VioPathGenerator for SDG g
	 * @param g - remembered for later use
	 */
	public PathGenerator (SDG g) {
		chopper = new ContextSensitiveThreadChopper(g);
		pathCollector = new PathCollector(g);
	}

	public ClassifiedViolation computeAllPaths(ClassifiedViolation v) {
		ViolationPathes p = computePaths(v);
		v.setViolationPathes(p);
		return v;
	}

	/**
	 * Generates all pathes from violation to outnode
	 *
	 * @return ViolationPathes containing all pathes from violation to outNode
	 */
	public ViolationPathes computePaths(ClassifiedViolation v) {
		ViolationPathes vioPaths = new ViolationPathes();
		Collection<SDGNode> chop = chopper.chop(v.getSource(), v.getSink());
		Collection<Path> rawPaths = pathCollector.collect(v.getSource(), v.getSink(), chop, v.getHighestSeverity());

		for (Path p : rawPaths) {
			vioPaths.add(p.convert());
		}

		return vioPaths;
	}

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

//	/**
//	 * Notify all ProgressListeners of newly done progress
//	 * @param progress
//	 */
//	private void notifyProgressListeners(int progr) {
//		String taskname = "";
//		if (firstProgressNotify) {
//			firstProgressNotify=false;
//			taskname = "Generating Violation Pathes (max " + maxPathes + ")";
//		}
//
//        for (ProgressListener p : progressListeners) {
//            p.progressChanged(taskname, progress, maxPathes);
//        }
//	}
}

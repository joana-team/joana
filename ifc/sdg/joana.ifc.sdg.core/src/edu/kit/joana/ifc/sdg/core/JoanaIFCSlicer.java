/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 29.03.2005
 *
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer;
import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.util.maps.BinaryMap;
import edu.kit.joana.util.maps.MultiMap;

/**
 * @author naxan
 *
 */
public class JoanaIFCSlicer implements ProgressAnnouncer {

	private final Logger debug = Log.getLogger(Log.L_SDG_CORE_DEBUG);
	
	/**
	 * @uml.property  name="l"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	IStaticLattice<String> l;
	/**
	 * @uml.property  name="g"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	SDG g;
	/**
	 * @uml.property  name="sumNodeID"
	 */
	private int sumNodeID = -1;
	/**
	 * @uml.property  name="coreAlgCount"
	 */
	private int coreAlgCount = 0;
	private ArrayList<ProgressListener> pls = new ArrayList<ProgressListener>();
	/**
	 * @uml.property  name="rememberSliceNodes"
	 */
	private boolean rememberSliceNodes = false;


	/**
	 * @uml.property  name="fSummaryEdge"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	BinaryMap<SecurityNode, SecurityNode, Boolean> fSummaryEdge = new BinaryMap<SecurityNode, SecurityNode, Boolean>();

	/**
	 * @uml.property  name="fsecVios"
	 * @uml.associationEnd  qualifier="vioSummaryNode:edu.kit.joana.ifc.sdg.graph.SecurityNode java.util.Set"
	 */
	private HashMap<SecurityNode, Set<SecurityNode>> fsecVios = new HashMap<SecurityNode, Set<SecurityNode>>();
	/**
	 * @uml.property  name="fdefNodes"
	 * @uml.associationEnd  qualifier="now:edu.kit.joana.ifc.sdg.graph.SecurityNode java.util.Set"
	 */
	private HashMap<SecurityNode, Set<SecurityNode>> fdefNodes = new HashMap<SecurityNode, Set<SecurityNode>>();
	/**
	 * @uml.property  name="fCanceled"
	 */
//	private boolean fCanceled = false;
	private HashSet<SecurityNode> sliceNodes = new HashSet<SecurityNode>();

	public JoanaIFCSlicer(SDG g, IStaticLattice<String> l) {
		this.l = l;
		this.g = g;
	}


	public Collection<SecurityNode> slice(SecurityNode startNode) throws NotInLatticeException {
		sliceNodes = new HashSet<SecurityNode>();
		rememberSliceNodes = true;
		violationsForOutgoingNode(startNode);
		rememberSliceNodes = false;
		return sliceNodes;

	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.PluggableSlicer6#slice(sdgParser.SecurityNode)
	 */
	public List<ClassifiedViolation> violationsForOutgoingNode(SecurityNode startNode) throws NotInLatticeException {

		/** worklist containing pathedges that are left to be looked at */
		HashList<Pathedge> worklist = new HashList<Pathedge>();

		/** visited contains pathedges that were already visited */
		HashMap<Pathedge, Pathedge> visited = new HashMap<Pathedge, Pathedge>();

		/** actoutPathedges contains pathedges starting at an actout-node */
		MultiMap<SecurityNode, Pathedge> actoutPathedges = new MultiMap<SecurityNode, Pathedge>();

		/** violations contains SimpleViolations representing all found security violations */
		LinkedList<ClassifiedViolation> violations = new LinkedList<ClassifiedViolation>();


		/* Initialize worklist */
		Pathedge startPE = new Pathedge(startNode, startNode, startNode.getRequired(), l.getBottom(), true);
		worklist.addFirst(startPE);
		visited.put(startPE, startPE);
		if (rememberSliceNodes) sliceNodes.add(startNode);


		/* Special case: startNode is ACT_OUT-node */
		if (startNode.getKind() == SecurityNode.Kind.ACTUAL_OUT) {
			addCorrespondingFormal(worklist, visited, actoutPathedges, startPE);
		}

		/* work */
		int showFactor = 5000;
		int nowFactor = showFactor;
		int mehrfach = 0;
		int vioCount = 0;
		int peCount = 0;
		announceProgress();
		int visitedold = 0;
		while (!worklist.isEmpty()) {
			if (++peCount > nowFactor) {
				if (debug.isEnabled()) {
					debug.outln("visited.size: " + visited.size() + " worklist.size: " + worklist.size()
							+ " corealgCount: " + coreAlgCount + " MultiCalc: " + mehrfach);
				}
				nowFactor += showFactor;

				announceProgress(visited.size()-visitedold);
				visitedold = visited.size();
//				announceInformation(vioCount, visited.size(), worklist.size(), coreAlgCount);
//				if (fCanceled) return violations;
			}

			Pathedge shorter = worklist.removeFirst();

			List<SecurityNode> predecessors = getPredecessors(shorter.source, shorter.target.equals(startNode));
			for (SecurityNode pred : predecessors) {

				/* Determine longer for use in corealg */
				Pathedge longer = new Pathedge(pred, shorter.target, l.getTop(), l.getBottom(), false);
				Pathedge longerInVisited = visited.get(longer);
				if (longerInVisited != null) {
					longer = longerInVisited;
				}

				/* call coreAlg */
				Pathedge newlonger = coreAlg(shorter, longer); ///we get a NEW longer !!
				/* get old longer - definition out of visited */

				if (longerInVisited == null) {
					worklist.addFirst(newlonger);
					visited.put(newlonger, newlonger);
				} else if (!longerInVisited.equalsExactly(newlonger)) {
					mehrfach++;
					worklist.addFirst(newlonger);
					visited.put(newlonger, newlonger);
				} //else we're done

				/* in case we found an ACTOUT-node */
				if (newlonger.source.getKind() == SecurityNode.Kind.ACTUAL_OUT) {
					addCorrespondingFormal(worklist, visited, actoutPathedges, newlonger);
				}

				/* Renew RedefSummaryNode iff we have a pathEdge that starts at a formalIn
				 * create or renew summaryNode and/or freepath-summaryEdge */
				if (!newlonger.target.equals(startNode) && // XXX warum?
						newlonger.source.getKind() == SecurityNode.Kind.FORMAL_IN &&
						newlonger.source.getProc() == newlonger.target.getProc()) {

					/* Call Summary-Renewing */
					boolean summaryChanged = renewDeclassSummary(newlonger);

					if (summaryChanged) {
						readdActOutNodesFor(newlonger, worklist, visited, actoutPathedges);
					}
				}

				/* Renew AnnSummaryNode iff we found an Annotation or Declassification Node */
				if (!newlonger.target.equals(startNode) &&
					(newlonger.source.isDeclassification() || newlonger.source.isInformationSource()) &&
					shorter.fp) {

					/* Call Summary-Renewing */
					boolean summaryChanged = renewAnnSummary(newlonger);

					if (summaryChanged) {
						readdActOutNodesFor(newlonger, worklist, visited, actoutPathedges);
					}
				}

				/* Ensuring Security */
				if (!getSupremum(newlonger.source.getProvided(), shorter.in).equals(shorter.in)) {

					vioCount++;
					if (newlonger.target.equals(startNode)) {
						if (newlonger.source.getKind() == SecurityNode.Kind.SUMMARY) {
							/* Handling VioSummary-Nodes */
							if (fsecVios.containsKey(longer.source)) {
								List<SecurityNode> vioSources = expandSummaryNode(newlonger.source);
								for (SecurityNode defNode : vioSources) {
									ClassifiedViolation newVio = ClassifiedViolation.createViolation(defNode, shorter.source, shorter.source.getRequired());
									if (!violations.contains(newVio)) violations.add(newVio);
								}
							}

							/* Handling Summary-Nodes */
							if (fdefNodes.containsKey(newlonger.source)) {
								List<SecurityNode> defSources = expandSummaryNode(newlonger.source);
								for (SecurityNode defNode : defSources) {
									if (!getSupremum(defNode.getProvided(), shorter.in).equals(shorter.in)) {
										ClassifiedViolation newVio = ClassifiedViolation.createViolation(defNode, shorter.source, shorter.source.getRequired());
										if (!violations.contains(newVio)) violations.add(newVio);
									}
								}
							}
						} else { /* Handling normal violation */
							ClassifiedViolation newVio = ClassifiedViolation.createViolation(newlonger.source, shorter.source, shorter.source.getRequired());
							if (!violations.contains(newVio)) violations.add(newVio);
						}
					} else { /* Handling violation in a procedure (local violation) */
						boolean vioChanged = addLocalViolation(newlonger);
						if (vioChanged) {
							readdActOutNodesFor(newlonger, worklist, visited, actoutPathedges);
						}
						//localVios.put(longer.target, longer.source);
					}
				}
			}
		}

		if (debug.isEnabled()) {
			debug.outln("Potential Violations Found: " + vioCount + " | Visited Pathedges: " + visited.size()
					+ " | Pathedges In Worklist: " + worklist.size() + " | Calculated Pathedges: " + coreAlgCount);
		}
		
		if (rememberSliceNodes) {
			for (Pathedge pe : visited.values()) {
				sliceNodes.add(pe.source);
			}
		}
		
		return violations;
	}


	private void addCorrespondingFormal(HashList<Pathedge> worklist,
			HashMap<Pathedge, Pathedge> visited,
			MultiMap<SecurityNode, Pathedge> actoutPathedges,
			Pathedge newlonger) {
		/* Remember ACTOUT-pathedge for future retrieval */
		actoutPathedges.add(newlonger.source, newlonger);

		/* do FORMALOUT-node */
		for (SDGNode n : g.getFormalOuts(newlonger.source)) {
			SecurityNode formalOut = (SecurityNode) n;
			Pathedge pefo = new Pathedge(	formalOut,
					formalOut,
					getInfimum(l.getTop(), formalOut.getRequired()),
					getSupremum(l.getBottom(), formalOut.getProvided()),
					!formalOut.isDeclassification());

			/* dieses containsKey sollte eigentlich equalsExactly verwenden
			 * allerdings sieht fuer ein FormalOut die pathedge *immer* genau
			 * so aus wie dieses pefo hier, so dass hier
			 * aus equals -> equalsExactly folgt */
			if (!visited.containsKey(pefo)) {
				worklist.addFirst(pefo);
				visited.put(pefo, pefo);
			}
		}
	}


	/**
	 * returns all annotation and declassification-nodes, that contribute to
	 * summaryNode.
	 * summaryNode may be of type ViolationSummaryNode oder AnnotationSummaryNode
	 * summaryNodes get recursively resolved
	 * (violationNode->ViolationNode->AnnotationNode only returns AnnotationNode)
	 * @param summaryNode
	 * @return
	 */
	private  List<SecurityNode> expandSummaryNode(SecurityNode summaryNode) {
		LinkedList<SecurityNode> ret = new LinkedList<SecurityNode>();
		LinkedList<SecurityNode> worklist = new LinkedList<SecurityNode>();
		HashSet<SecurityNode> done = new HashSet<SecurityNode>(131, (float)0.5);

		worklist.add(summaryNode);
		done.add(summaryNode);

		while (!worklist.isEmpty()) {
			SecurityNode now = worklist.removeFirst();

			if (now.getKind() == SecurityNode.Kind.SUMMARY) {
				Set<SecurityNode> pres = fsecVios.containsKey(now) ? fsecVios.get(now) : fdefNodes.get(now);
				if (pres != null) {
					for (SecurityNode pre : pres) {
						if (!done.contains(pre)) {
							worklist.add(pre);
							done.add(pre);
						}
					}
				}
			} else {
				ret.add(now);
			}
		}
		return ret;
	}


	/**
	 * @param longer
	 * @return
	 * @throws NotInLatticeException
	 */
	private boolean renewAnnSummary(Pathedge longer) throws NotInLatticeException {
		boolean changed = false;

		/** LinkedList containing this annotating or declassification node */
		HashSet<SecurityNode> defNodes = new HashSet<SecurityNode>();
		defNodes.add(longer.source);
		/** Annotation */
		String annOutClass = longer.source.getProvided();

		/** FormalOut node */
		SecurityNode formalOut = longer.target;
		/* For every corresponding act-out add or update corresponding AnnSummaryNode */
		SecurityNode entry = (SecurityNode) g.getEntry(formalOut);
		for (SDGNode n : g.getCallers(entry)) {
			SecurityNode caller = (SecurityNode) n;
			SecurityNode actualOut = (SecurityNode) g.getActualOut(caller, formalOut);
			/* get annSummaryNode connected to actOut... */
			if (actualOut == null) continue;
			SecurityNode annSummaryNode = getAnnSummaryNode(actualOut);

			/* ...If none exists, create a new one */
			if (annSummaryNode == null) {
				annSummaryNode = new SecurityNode(sumNodeID--,SecurityNode.Operation.SUMMARY,
						"summary", -1, "", actualOut.getSource(), -1, -1, -1, -1, actualOut.getBytecodeName(), -1);
				annSummaryNode.setProvided(annOutClass);
				fdefNodes.put(annSummaryNode, defNodes);
				g.addVertex(annSummaryNode);
				SDGEdge annEdge = new SDGEdge(annSummaryNode, actualOut, SDGEdge.Kind.SUMMARY);
				g.addEdge(annEdge);
				changed = true;
			/* else update the old */
			} else {
				Set<SecurityNode> annCon = fdefNodes.get(annSummaryNode);
				annCon.removeAll(defNodes);
				annCon.addAll(defNodes);
				String oldOutClass = annSummaryNode.getProvided();
				String newOutClass = getSupremum(oldOutClass, annOutClass);
				changed = changed || (!newOutClass.equals(oldOutClass));
				if (changed) annSummaryNode.setProvided(newOutClass);

			}
		}
		return changed;
	}


	/**
	 * Creates/Updates the secVio nodes connected to longer.target's corresponding act-outs
	 * @param target
	 * @param source
	 */
	private boolean addLocalViolation(Pathedge longer) {
		boolean changed = false;
		HashSet<SecurityNode> secVios = new HashSet<SecurityNode>();
		secVios.add(longer.source);
		SecurityNode formalOut = longer.target;
		/* For every corresponding act-out add or update corresponding VioSummaryNode */
		SecurityNode entry = (SecurityNode) g.getEntry(formalOut);
		for (SDGNode n : g.getCallers(entry)) {
			SecurityNode caller = (SecurityNode) n;
			SecurityNode actualOut = (SecurityNode) g.getActualOut(caller, formalOut);

			/* get vioSummaryNode connected to actOut... */
			if (actualOut == null) continue;

			SecurityNode vioSummaryNode = getVioSummaryNode(actualOut);

			/* ...If none exists, create a new one */
			if (vioSummaryNode == null) {
				vioSummaryNode = new SecurityNode(sumNodeID--,SecurityNode.Operation.SUMMARY, "summary", -1, "",
						actualOut.getSource(), -1, -1, -1, -1, actualOut.getBytecodeName(), -1);
				vioSummaryNode.setProvided(l.getTop());
				fsecVios.put(vioSummaryNode, secVios);
				g.addVertex(vioSummaryNode);
				SDGEdge secVioEdge = new SDGEdge(vioSummaryNode, actualOut, SDGEdge.Kind.SUMMARY);
				g.addEdge(secVioEdge);
				changed = true;
				if (debug.isEnabled()) {
					debug.outln("new secVio Summary Node Nr " + (sumNodeID + 1)
							+ " ( Violation at ??? ) connected to " +  actualOut);
				}
			/* else update the old */
			} else {
				Set<SecurityNode> secViosCon = fsecVios.get(vioSummaryNode);
				secViosCon.removeAll(secVios);
				secViosCon.addAll(secVios);
				if (debug.isEnabled()) {
					debug.outln("updated secVio Summary Node " + vioSummaryNode.getId()
							+ " ( Violation at ???) connected to " +  actualOut);
				}
			}
		}
		return changed;
	}


	/**
	 * @param longer
	 */
	private void readdActOutNodesFor(Pathedge longer, HashList<Pathedge> worklist,
			HashMap<Pathedge, Pathedge> visited, MultiMap<SecurityNode, Pathedge> actoutPathedges) {

		SecurityNode formalOut = longer.target;
		/* get all corresponding actualOut nodes and add corresponding pathedges to worklist */
		SecurityNode entry = (SecurityNode) g.getEntry(formalOut);
		for (SDGNode n : g.getCallers(entry)) {
			SecurityNode caller = (SecurityNode) n;
			SecurityNode actualOut = (SecurityNode) g.getActualOut(caller, formalOut);
			if (actualOut == null) continue;
			Set<Pathedge> actpathedges = actoutPathedges.get(actualOut);

			for (Pathedge pe : actpathedges) {
				Pathedge currentpe = visited.get(pe);
				if (currentpe != null) {
					worklist.addFirst(currentpe);
				} else {
					throw new NullPointerException("NJSecSlicer.readdActOutNodes");
				}
			}
		}
	}


	/**
	 * @param longer
	 * @return true, iff some summary-Information changed
	 * @throws NotInLatticeException
	 */
	private boolean renewDeclassSummary(Pathedge longer) throws NotInLatticeException {
		boolean changed = false;
		/* Determine which borders Pathedge longer's attributes allow */
		String maxIn = longer.in;
		String maxOut = longer.out;
		if (maxIn.equals(l.getTop())) maxIn = SecurityNode.UNDEFINED;
		if (maxOut.equals(l.getBottom())) maxOut = SecurityNode.UNDEFINED;

		if ((maxIn == SecurityNode.UNDEFINED || maxOut == SecurityNode.UNDEFINED) && !longer.fp) return false;


		/* Get formalIn and FormalOut node from Pathedge longer */
		SecurityNode formalIn = longer.source;
		SecurityNode formalOut = longer.target;

		/* get all corresponding actualIn and actualOut nodes */
		SecurityNode entry = (SecurityNode) g.getEntry(longer.source);
		for (SDGNode n : g.getCallers(entry)) {
			SecurityNode caller = (SecurityNode) n;
			SecurityNode actualIn = (SecurityNode) g.getActualIn(caller, formalIn);
			SecurityNode actualOut = (SecurityNode) g.getActualOut(caller, formalOut);

			/* try next caller, if no corresponding actualOut was found */
			if (actualOut == null) continue;

			if (maxIn != SecurityNode.UNDEFINED && maxOut != SecurityNode.UNDEFINED) {

				/* get summaryNode between local actualIn and actualOut... */
				SecurityNode summaryNode = getSummaryNodeBetween(actualIn, actualOut);

				/* ...If none exists, create a new one */
				if (summaryNode == null) {
					summaryNode = new SecurityNode(sumNodeID--,SecurityNode.Operation.SUMMARY, "summary", -1, "",
							actualOut.getSource(), -1, -1, -1, -1, actualOut.getBytecodeName(), -1);
					summaryNode.setRequired(maxIn);
					summaryNode.setProvided(maxOut);
					g.addVertex(summaryNode);

					SDGEdge sumEdge1 = new SDGEdge(actualIn, summaryNode, SDGEdge.Kind.SUMMARY);
					g.addEdge(sumEdge1);

					SDGEdge sumEdge2 = new SDGEdge(summaryNode, actualOut, SDGEdge.Kind.SUMMARY);
					g.addEdge(sumEdge2);
					changed = true;

				/* else update the old */
				} else {
					String oldIn = summaryNode.getRequired();
					String newIn = getInfimum(oldIn, maxIn);
					summaryNode.setRequired(newIn);
					String oldOut = summaryNode.getProvided();
					String newOut = getSupremum(oldOut, maxOut);
					summaryNode.setProvided(newOut);
					changed = changed || !oldIn.equals(newIn) || !oldOut.equals(newOut);

				}
			}
			/* Create Summary-Edge in case a freepath exists */
			if (longer.fp) {
				if (!summaryEdgeExistsBetween(actualIn, actualOut)) {
					SDGEdge sumEdge = new SDGEdge(actualIn, actualOut, SDGEdge.Kind.SUMMARY, "njsec");
					g.addEdge(sumEdge);
					changed = true;
				}
			}
		}

		return changed;
	}


	/**
	 * @param actualIn
	 * @param actualOut
	 * @return
	 */
	private boolean summaryEdgeExistsBetween(SecurityNode actualIn, SecurityNode actualOut) {
		if (fSummaryEdge.containsKey(actualOut, actualIn))
		    return fSummaryEdge.get(actualOut, actualIn);

		for (SDGEdge edge : g.getIncomingEdgesOfKind(actualOut, SDGEdge.Kind.SUMMARY)) {
			SecurityNode source = (SecurityNode) edge.getSource();
			if (source.equals(actualIn)) {
				fSummaryEdge.put(actualOut, actualIn, true);
				return true;
			}
		}
		return false;
	}


	/**
	 * returns the first SummaryNode it finds,
	 * that is connect to actOut by a summary edge
	 * @param actOut
	 * @return
	 */
	private SecurityNode getVioSummaryNode(SecurityNode actOut) {
		for (SDGEdge edge : g.getIncomingEdgesOfKind(actOut, SDGEdge.Kind.SUMMARY)) {
			SecurityNode source = (SecurityNode) edge.getSource();
			if (source.getKind() == SecurityNode.Kind.SUMMARY &&
					fsecVios.get(source) != null) {
				return source;
			}
		}
		return null;
	}


	/**
	 * returns the first summary-node it finds,
	 * that is connected from actualIn by a summary edge
	 * @param actualIn
	 * @param actualOut that parameter isn't even used in code
	 * @return
	 */
	private SecurityNode getSummaryNodeBetween(SecurityNode actualIn, SecurityNode actualOut) {
		for (SDGEdge edge : g.getOutgoingEdgesOfKind(actualIn, SDGEdge.Kind.SUMMARY)) {
			SecurityNode target = (SecurityNode) edge.getTarget();
			if (target.getKind() == SecurityNode.Kind.SUMMARY) {
				return target;
			}
		}
		return null;
	}

	private SecurityNode getAnnSummaryNode(SecurityNode actualOut) {
		for (SDGEdge edge : g.getIncomingEdgesOfKind(actualOut, SDGEdge.Kind.SUMMARY)) {
			SecurityNode source = (SecurityNode) edge.getSource();
			if (source.getKind() == SecurityNode.Kind.SUMMARY &&
				fdefNodes.get(source) != null) {
				return source;
			}
		}
		return null;
	}

	/**
	 * determines all predecessors for SecurityNode node
	 * follows all edges besides PARAMETER_OUT
	 * follows PARAMETER_IN only if Paramter followParamIn is true
	 * @param g
	 * @param node
	 * @param followParamIn
	 * @return
	 */
	public List<SecurityNode> getPredecessors(SecurityNode node, boolean followParamIn) {
		LinkedList<SecurityNode> ret = new LinkedList<SecurityNode>();

		for (SDGEdge edge : g.incomingEdgesOf(node)) {
			/**
			 * specify cases where edge should NOT be followed
			 * because the else is where it IS followed
			 */

			switch (edge.getKind()) {
			case PARAMETER_OUT:
				break; //don't follow
			case PARAMETER_IN:
			case CALL:
				if (followParamIn) {
					ret.add((SecurityNode) edge.getSource());
				} // else don't follow
			default:
				//follow
				if (edge.getKind().isSDGEdge()) {
					ret.add((SecurityNode) edge.getSource());
				}
			}
		}

		return ret;
	}


	private  Pathedge coreAlg(Pathedge shorter, Pathedge longer) throws NotInLatticeException {
		coreAlgCount++;
		/* Calculate Source for new longer-pathedge */
		SecurityNode so = longer.source;
		/* Calculate Target for new longer-pathedge */
		SecurityNode ta = shorter.target;
		assert ta == longer.target; //if shorter.target != longer.target -> error!

		/* Calculate max-in for new longer-pathedge */
		String in = so.isDeclassification() ? in = so.getRequired() :
			getInfimum(shorter.in, longer.in);

		/* Calculate max-out for new longer-pathedge */
		String out = shorter.fp ?
				getSupremum(longer.source.getProvided(), longer.out, shorter.out) :
				getSupremum(longer.out, shorter.out);

		/* Calculate freepath for new longer-pathedge */
		boolean fp = !longer.source.isDeclassification() && (shorter.fp || longer.fp);

		return new Pathedge(so, ta, in, out, fp);
	}

	private String getInfimum(String x, String y) throws NotInLatticeException {
		if (x == SecurityNode.UNDEFINED) {
			return y == SecurityNode.UNDEFINED ? l.getTop() : y;
		} else {
			return y == SecurityNode.UNDEFINED ? x : l.greatestLowerBound(x, y);
		}
	}

	private String getSupremum(String x, String y) throws NotInLatticeException {
		if (x == SecurityNode.UNDEFINED) {
			return y == SecurityNode.UNDEFINED ? l.getBottom() : y;
		} else {
			return y == SecurityNode.UNDEFINED ? x : l.leastUpperBound(x, y);
		}
	}

	private String getSupremum(String x, String y, String z) throws NotInLatticeException {
		return getSupremum(getSupremum(x, y), z);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer#addProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void addProgressListener(ProgressListener pl) {
		pls.remove(pl);
		pls.add(pl);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.interfaces.ProgressAnnouncer#removeProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void removeProgressListener(ProgressListener pl) {
		pls.remove(pl);
	}

	private void announceProgress() {
		for (ProgressListener pl : pls) {
			pl.progressChanged("Checking security...", 0, g.vertexSet().size()*10);
//			pl.getMonitor().subTask("Violations found: " + 0);
		}
	}
	private void announceProgress(int progress) {
		for (ProgressListener pl : pls) {
			pl.progressChanged("", progress, 0);
//			if (pl.getMonitor().isCanceled()) fCanceled = true;
		}
	}
//	private void announceInformation(int vioCount, int visited, int worklist, int coreAlg) {
//		for (ProgressListener pl : pls) {
//			pl.getMonitor().subTask("Potential Violations Found: " + vioCount
//					+ " | Visited Pathedges: " + visited
//					+ " | Pathedges In Worklist: " + worklist
//					+ " | Calculated Pathedges: " + coreAlg);
//		}
//	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.zest.core.viewers.EntityConnectionData;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferedNode;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelRegion;

/**
 * The SDGWrapper class serves as a wrapper for the CSDG data structure
 * and the MHP information.
 *
 * @author Le-Huan Stefan Tran
 */
public class SDGWrapper {
	private static final String FILE_POSTFIX_JAVA = ".java";

	private static final String APPLICATION_CLASSLOADER = "Application";

	// Set to "true" to print SDG statistics in main console
	private static final boolean PRINT_STATISTICS = false;

	// Use the singleton pattern
	private static SDGWrapper instance;

	private SDG sdg;
	private PreciseMHPAnalysis analysisMHP;
	private ThreadRegions regions;
	private ThreadsInformation threads;

	private IJavaProject jProject;
	private IProject project;
	private Collection<String> fileList;

	// Interferences between regions
	private Map<Object, Collection<ThreadRegion>> memoryInterferedRegions;
	private Map<Object, Collection<ThreadRegion>> memorySourceCodeInterferedRegions;

	// Direct control flow
	private Map<Object, Collection<ThreadRegion>> memoryNextRegions;
	private Map<Object, Collection<ThreadRegion>> memoryNextRegionsWithInterprocEdges;

	// Indirect control flow
	// Precalculation to increase performance
	private Map<Object, Collection<ThreadRegion>> memoryNextSourceCodeRegions;
	private Map<Object, Collection<ThreadRegion>> memoryNextInterferingRegions;
	private Map<Object, Collection<ThreadRegion>> memoryNextSourceCodeInterferingRegions;

	private Map<Object, Collection<ThreadRegion>> memoryNextSourceCodeRegionsWithInterprocEdges;
	private Map<Object, Collection<ThreadRegion>> memoryNextInterferingRegionsWithInterprocEdges;
	private Map<Object, Collection<ThreadRegion>> memoryNextSourceCodeInterferingRegionsWithInterprocEdges;

	private SDGWrapper() {

		memoryInterferedRegions = new HashMap<Object, Collection<ThreadRegion>>();
		memorySourceCodeInterferedRegions = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextRegions = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextRegionsWithInterprocEdges = new HashMap<Object, Collection<ThreadRegion>>();

		memoryNextSourceCodeRegions = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextInterferingRegions = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextSourceCodeInterferingRegions = new HashMap<Object, Collection<ThreadRegion>>();

		memoryNextSourceCodeRegionsWithInterprocEdges = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextInterferingRegionsWithInterprocEdges = new HashMap<Object, Collection<ThreadRegion>>();
		memoryNextSourceCodeInterferingRegionsWithInterprocEdges =
			new HashMap<Object, Collection<ThreadRegion>>();
	}

	public void changeModel(SDG newSDG, IJavaProject inProject) {
		sdg = newSDG;

		// Start calculations
		analysisMHP = PreciseMHPAnalysis.analyze(sdg);
		regions = analysisMHP.getTR();

		threads = sdg.getThreadsInfo();
		jProject = inProject;
		project = inProject.getProject();

		// Remove interferences between not parallel regions
		cleanCSDG(sdg, analysisMHP);

		// Updates the list of source code files
		updateFileList();

		// Calculate interferences and control flow
		calculateInterferences();
		calculateGeneralControlFlows();

		// Special control flows need precalculation of general control flows
		calculateSpecialControlFlows();


		// Print statistics of current CSDG when constant PRINT_STATISTICS is set
		if (PRINT_STATISTICS) {
			this.printStatisticsSDG();
		}
	}



	/**
	 * Reads the given .pdg-file and computes all dependencies and graphs.
	 *
	 * @param relativePath
	 */
	public void changeModel(IPath relativePath) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(relativePath);
		IPath pathWorkspace = file.getLocation();

		// Load specified .pdg-file
		if (pathWorkspace != null) {
			File sdgFile = pathWorkspace.toFile();

			try {
				Reader reader = new FileReader(sdgFile);
				sdg = SDG.readFrom(reader);
			} catch (IOException e) {
				Controller.getInstance().updateStatusBar("Reading .pdg-file failed.");
				e.printStackTrace();
			}

			// Start calculations
			regions = ThreadRegions.createPreciseThreadRegions(sdg);
			analysisMHP = PreciseMHPAnalysis.analyze(sdg);
			threads = sdg.getThreadsInfo();
			project = file.getProject();

			// Remove interferences between not parallel regions
			cleanCSDG(sdg, analysisMHP);

			// Updates the list of source code files
			updateFileList();

			// Calculate interferences and control flow
			calculateInterferences();
			calculateGeneralControlFlows();

			// Special control flows need precalculation of general control flows
			calculateSpecialControlFlows();


			// Print statistics of current CSDG when constant PRINT_STATISTICS is set
			if (PRINT_STATISTICS) {
				this.printStatisticsSDG();
			}
		}
	}

	/**
	 * Gets the instance of the SDGWrapper class.
	 * Implements the Singleton pattern.
	 *
	 * @return	The instance of the SDGWrapper class
	 */
	public static SDGWrapper getInstance() {
		if (instance == null) {
			instance = new SDGWrapper();
		}
		return instance;
	}
	/**
	 * Checks if a .pdg-file has been loaded and all calculations have been done.
	 * @return
	 */
	public boolean isCreated() {
		return this.sdg != null;
	}

	/**
	 * Return the project object.
	 *
	 * @return The project object
	 */
	public IProject getProject() {
		return this.project;
	}

	/**
	 * Gets all thread regions.
	 *
	 * @return	All thread regions.
	 */
	public Collection<ThreadRegion> getRegions() {
		// Get all regions of all threads
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		for (ThreadInstance thread : this.getThreads()) {
			regions.addAll(this.getRegions(thread.getId()));
		}

		//Collection<ThreadRegion> regions2 = analysisMHP.getThreadRegions();

		return regions;
	}

	/**
	 * Gets the region with the given node and thread ID
	 *
	 * @param node		A node belonging to the wanted region
	 * @param threadID	The thread ID of the wanted region
	 * @return			The wanted region
	 */
	public ThreadRegion getRegion(SDGNode node, int threadID) {
		return analysisMHP.getThreadRegion(node, threadID);
	}

	/**
	 * Gets the entry node of the given region
	 *
	 * @param region	The region whose entry node is to be returned
	 * @return			The entry node
	 */
	public SDGNode getEntryNode(ThreadRegion region) {
		return sdg.getEntry(region.getStart());
	}

	/**
	 * Gets all threads.
	 *
	 * @return	All threads
	 */
	public Collection<ThreadInstance> getThreads() {
		Iterator<ThreadInstance> iterThreads = threads.iterator();
		Collection<ThreadInstance> instances = new HashSet<ThreadInstance>();

		while(iterThreads.hasNext()) {
			ThreadInstance thread = iterThreads.next();
			instances.add(thread);
		}

		return instances;
	}

	/**
	 * Gets the thread of the given region.
	 *
	 * @param region	The region whose thread is to be returned
	 * @return			The thread
	 */
	public ThreadInstance getThread(ThreadRegion region) {
		return threads.getThread(region.getThread());
	}

	/**
	 * Gets the thread of the given ID.
	 *
	 * @param threadID	The ID of the thread to be returned
	 * @return			The thread
	 */
	public ThreadInstance getThread(int threadID) {
		return threads.getThread(threadID);
	}

	/**
	 * Gets the nodes of the given thread.
	 *
	 * @param thread	The thread whose nodes are wanted
	 * @return			The nodes
	 */
	public Collection<SDGNode> getNodes(ThreadInstance thread) {
		Collection<SDGNode> nodes = new HashSet<SDGNode>();

		for (ThreadRegion region : this.getRegions(thread)) {
			nodes.addAll(region.getNodes());
		}

		return nodes;
	}

	/**
	 * Gets the nodes of same region as the given node.
	 *
	 * @param node	The node whose region's nodes are to be returned
	 * @return		The nodes
	 */
	public Collection<SDGNode> getNodesOfSameRegion(SDGNode node) {
		Collection<SDGNode> allNodes = new HashSet<SDGNode>();

		for (ThreadRegion region : this.getRegions(node)) {
			allNodes.addAll(region.getNodes());
		}

		return allNodes;
	}

	/**
	 * Gets the regions of the given node.
	 *
	 * @param node	The node whose regions are to be returned
	 * @return		The regions
	 */
	public Collection<ThreadRegion> getRegions(SDGNode node) {
		return regions.getThreadRegions(node);
	}

	/**
	 * Gets the regions of the given thread.
	 *
	 * @param thread	The thread whose regions are to be returned
	 * @return			The regions
	 */
	public Collection<ThreadRegion> getRegions(ThreadInstance thread) {
		return regions.getThreadRegionSet(thread.getId());
	}

	/**
	 * Gets the parallel regions of the given region.
	 *
	 * @param region	The region whose parallel regions are to be returned
	 * @return			The parallel regions
	 */
	public Collection<ThreadRegion> getParallelRegions(ThreadRegion region) {
		Collection<ThreadRegion> parallelRegions = new HashSet<ThreadRegion>();
		SDGNode node = region.getStart();

		for (ThreadRegion testRegion : this.getRegions()) {
			if (analysisMHP.isParallel(testRegion.getStart(), node) &&
					!testRegion.getStart().equals(node)) {
				parallelRegions.add(testRegion);
			}
		}

		return parallelRegions;
	}

	/**
	 * Gets the interfering nodes of the given region.
	 *
	 * @param region	The region whose interfering nodes are to be returned
	 * @return			The interfering nodes
	 */
	public Collection<SDGNode> getInterferingNodes(ThreadRegion region) {
		Collection<SDGNode> interferingNodes = new HashSet<SDGNode>();

		// Loop through all nodes of the given region
		for (SDGNode node : region.getNodes()) {
			// Loop through all outgoing edges of each node
			for (SDGEdge edge : sdg.outgoingEdgesOf(node)) {
				if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
						|| edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
					interferingNodes.add(node);
				}
			}

			// Loop through all incoming edges of each node
			for (SDGEdge edge : sdg.incomingEdgesOf(node)) {
				if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
						|| edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
					interferingNodes.add(node);
				}
			}
		}

		return interferingNodes;
	}

	/**
	 * Gets the nodes interfered by the given node.
	 *
	 * @param node	The node whose interfered nodes are to be returned
	 * @return		The interfered nodes
	 */
	public Collection<SDGNode> getInterferedNodes(SDGNode node) {
		Collection<SDGNode> interferedNodes = new HashSet<SDGNode>();

		// Loop through all outgoing edges of each node
		for (SDGEdge edge : sdg.outgoingEdgesOf(node)) {
			if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
					|| edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				interferedNodes.add(edge.getTarget());
			}
		}

		// Loop through all incoming edges of each node
		for (SDGEdge edge : sdg.incomingEdgesOf(node)) {
			if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
					|| edge.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				interferedNodes.add(edge.getSource());
			}
		}

		return interferedNodes;
	}


	/**
	 * Gets the nodes interfered by the given region.
	 *
	 * @param region	The region whose interfered nodes are to be returned
	 * @return			The interfered nodes
	 */
	public Collection<SDGNode> getInterferedNodes(ThreadRegion region) {
		Collection<SDGNode> interferedNodes = new HashSet<SDGNode>();

		for (SDGNode source : region.getNodes()) {
			interferedNodes.addAll(this.getInterferedNodes(source));
		}

		return interferedNodes;
	}

	/**
	 * Gets the regions interfered by the given region.
	 *
	 * @param region	The region whose interfered regions are to be returned
	 * @return			The interfered regions
	 */
	public Collection<ThreadRegion> getInterferedRegions(ThreadRegion region) {
		Collection<ThreadRegion> interferedRegions = new HashSet<ThreadRegion>();

		for (SDGNode source : region.getNodes()) {
			for (SDGNode interferedNode : this.getInterferedNodes(source)) {
				for (ThreadRegion tmpRegion : this.getRegions(interferedNode)) {
					// Check for different regions (to avoid cycles!)
					if (!region.equals(tmpRegion)) {
						interferedRegions.add(tmpRegion);
					}
				}
			}
		}

		return interferedRegions;
	}

	/**
	 * Gets the regions with the given thread ID.
	 *
	 * @param threadID	The thread ID of the regions to be returned
	 * @return			The regions
	 */
	public Collection<ThreadRegion> getRegions(int threadID) {
		return regions.getThreadRegionSet(threadID);
	}

	/**
	 * Checks if the given region is in source code.
	 *
	 * @param region	The region to be checked
	 * @return			True if the given region is in source code
	 */
	public boolean isInSourceCode(ThreadRegion region) {
		Collection<SDGNode> nodes = region.getNodes();
		Iterator<SDGNode> iterNodes = nodes.iterator();
		boolean isInSourceCode = false;

		// A region is available in source code if one of its nodes is
		// located in one of the available source files
		while (iterNodes.hasNext() && !isInSourceCode) {
			SDGNode node = iterNodes.next();
			isInSourceCode = isInSourceCode(node);
		}

		return isInSourceCode;
	}

	/**
	 * Checks if the given node is in source code.
	 *
	 * @param node	The node to be checked
	 * @return		True if the given node is in source code
	 */
	public boolean isInSourceCode(SDGNode node) {
//		boolean isInSourceCode = false;
//		String source = node.getSource();
//
//		if (source != null) {
//			if (!source.isEmpty()) {
//				project.findMember(source);
//				source = project.getName() + "/src/"+ source;
//				if (this.fileList.contains(source)) {
//					isInSourceCode = true;
//				}
//			}
//		}
//
//		return isInSourceCode;

		if (node.getClassLoader() == null || !node.getClassLoader().equals(APPLICATION_CLASSLOADER)) {
			return false;
		}

		String source = node.getSource();
		if (source != null && !source.isEmpty()) {
			try {
				IJavaElement elem = jProject.findElement(new Path(source));
				return elem != null;

			} catch (JavaModelException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks if the given object has interferences.
	 *
	 * @param element	The object to be checked
	 * @return			True if there are interferences
	 */
	public boolean isInterfering(Object element) {
		boolean isInterfering = false;

		if (element instanceof ThreadInstance) {
			// Always show thread instance
			isInterfering = true;
		} else if (element instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) element;
			Collection<ThreadRegion> interfered = memoryInterferedRegions.get(region);

			if (interfered != null) {
				if (interfered.size() > 0) {
					isInterfering = true;
				}
			}
		} else if (element instanceof EntityConnectionData) {
			isInterfering = true;
		}

		return isInterfering;
	}

	/**
	 * Gets all nodes called by the given entry node.
	 *
	 * @param entry		The entry node
	 * @return			The called nodes
	 */
	public Collection<SDGNode> getCalledMethods(SDGNode entry) {
		if (entry == null || entry.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("Not an entry node: " + entry);
		}

		Set<SDGNode> calledEntries = new HashSet<SDGNode>();

		for (SDGNode n : sdg.getNodesOfProcedure(entry)) {
			if (n.getKind() == SDGNode.Kind.CALL) {
				for (SDGEdge call : sdg.outgoingEdgesOf(n)) {
					SDGNode called = call.getTarget();
					calledEntries.add(called);
				}
			}
		}

		return calledEntries;
	}

	public Collection<ThreadRegion> getInterferedRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryInterferedRegions.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getSourceCodeInterferedRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memorySourceCodeInterferedRegions.get(obj);
		}

		return regions;
	}

	/**
	 * Gets all next regions of the given object.
	 *
	 * @param obj	The object whose next regions are to be returned
	 * @return		The next regions
	 */
	public Collection<ThreadRegion> getNextRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextRegions.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextRegionsWithInterprocEdges(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextRegionsWithInterprocEdges.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextSourceCodeRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextSourceCodeRegions.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextSourceCodeRegionsWithInterprocEdges(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextSourceCodeRegionsWithInterprocEdges.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextInterferingRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextInterferingRegions.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextInterferingRegionsWithInterprocEdges(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextInterferingRegionsWithInterprocEdges.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextSourceCodeInterferingRegions(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextSourceCodeInterferingRegions.get(obj);
		}

		return regions;
	}

	public Collection<ThreadRegion> getNextSourceCodeInterferingRegionsWithInterprocEdges(Object obj) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (obj instanceof ThreadInstance || obj instanceof ThreadRegion) {
			regions = memoryNextSourceCodeInterferingRegionsWithInterprocEdges.get(obj);
		}

		return regions;
	}


	/* Label handles */

	/**
	 * Return the label of the given thread.
	 */
	public String getLabel(ThreadInstance thread) {
		String label = "Thread " + thread.getId() + " -> ";

		label = label + this.getThreadFunctionLabel(thread);
		label = label + this.getCallContext(thread);

		return label;
	}

	/**
	 * Return the label of the given thread region.
	 */
	public String getLabel(ThreadRegion region) {
		String label = this.getRegionLabel(region);

		// Add thread region information
		if (this.isInSourceCode(region)) {
			int parRegions = this.getParallelRegions(region).size();
			int interNodes = this.getInterferingNodes(region).size();

			if (parRegions > 0) {
				label = label.concat(" | Parallel regions: " + parRegions);

				if (interNodes > 0) {
					label = label.concat(" - Interfering nodes: " + interNodes);
				}
			}
		}

		return label;
	}

	/**
	 * Return the label of the given parallel region.
	 */
	public String getLabel(ViewParallelRegion parRegion) {
		ThreadRegion region = parRegion.getItem();
		String label = this.getRegionLabel(region);

		// Add thread information
		label = "Thread " + region.getThread() + " -> " + label;

		return label;
	}

	/**
	 * Return the label of the given node.
	 */
	public String getLabel(SDGNode node) {
		String label = node.getLabel();
		label = label + " : " + node.getSr();

		return label;
	}

	/**
	 * Return the label of the given intefered node.
	 */
	public String getLabel(ViewInterferedNode interNode) {
		SDGNode node = interNode.getItem();
		String label = node.getLabel();
		label = label + " : " + node.getSr();

		// Add thread information
		String threadsIDs = "";
		int[] threads = node.getThreadNumbers();
		for (int i = 0; i < threads.length; i++) {
			threadsIDs += threads[i] + ", ";
		}
		threadsIDs = threadsIDs.substring(0, threadsIDs.length()-2);

		label = "Thread " + threadsIDs + " -> " + label;

		return label;
	}

	/**
	 * Return the short label of the given thread.
	 */
	public String getShortLabel(ThreadInstance thread) {
		String label = this.getThreadFunctionLabel(thread);
		return label;
	}

	/**
	 * Return the short label of the given region.
	 */
	public String getShortLabel(ThreadRegion region) {
		String label = this.getRegionLabel(region);
		return label;
	}

	/**
	 * Print statistics for current SDG.
	 */
	public void printStatisticsSDG() {
		// General statistics
		System.out.println(sdg.getName());
		System.out.println("# threads: " + getThreads().size());
		System.out.println("# regions: " + getRegions().size());
		System.out.println("# nodes: " + sdg.vertexSet().size());
		System.out.println("# edges: " + sdg.edgeSet().size());
		Collection<ThreadRegion> allRegions = new HashSet<ThreadRegion>();

		// Count interferences (they are doubly saved, so divide by 2)
		int cnt = 0;
		for (ThreadRegion region : this.getRegions()) {
			cnt = cnt + memoryInterferedRegions.get(region).size();
		}

		System.out.println("# interferences: " + cnt / 2);


		// Count source code regions
		for (ThreadInstance thread : this.getThreads()) {
			for (ThreadRegion next : memoryNextSourceCodeRegions.get(thread)) {
				allRegions.add(next);
			}
		}
		for (ThreadRegion region : this.getRegions()) {
			if (this.isInSourceCode(region)) {
				allRegions.add(region);
			}
			for (ThreadRegion next : memoryNextSourceCodeRegions.get(region)) {
				allRegions.add(next);
			}
		}
		System.out.println("# source code regions: " + allRegions.size());


		// Count regions with interference
		allRegions.clear();
		for (ThreadInstance thread : this.getThreads()) {
			for (ThreadRegion next : memoryNextInterferingRegions.get(thread)) {
				allRegions.add(next);
			}
		}
		for (ThreadRegion region : this.getRegions()) {
			if (memoryInterferedRegions.get(region).size() > 0) {
				allRegions.add(region);
			}
			for (ThreadRegion next : memoryNextInterferingRegions.get(region)) {
				allRegions.add(next);
			}
		}
		System.out.println("# regions with interference: " + allRegions.size());


		// Count source code regions with interference
		allRegions.clear();
		for (ThreadInstance thread : this.getThreads()) {
			for (ThreadRegion next : memoryNextSourceCodeInterferingRegions.get(thread)) {
				allRegions.add(next);
			}
		}
		for (ThreadRegion region : this.getRegions()) {
			if (this.isInSourceCode(region) && memoryInterferedRegions.get(region).size() > 0) {
				allRegions.add(region);
			}
			for (ThreadRegion next : memoryNextSourceCodeInterferingRegions.get(region)) {
				allRegions.add(next);
			}
		}
		System.out.println("# source code regions with interference: " + allRegions.size());
		System.out.println();
	}


	/* Helper methods */

	/* Calculates interfered regions */
	private void calculateInterferences() {
		memoryInterferedRegions.clear();
		memorySourceCodeInterferedRegions.clear();

		for (ThreadRegion source : this.getRegions()) {
			// 1) "Show all regions"
			memoryInterferedRegions.put(source, this.getInterferedRegions(source));

			// 2) "Hide non-source code regions"
			Collection<ThreadRegion> targets = new HashSet<ThreadRegion>();
			for (ThreadRegion target : this.getInterferedRegions(source)) {
				if (this.isInSourceCode(target)) {
					targets.add(target);
				}
			}
			memorySourceCodeInterferedRegions.put(source, targets);
		}
	}



	/* Calculates general control flow */
	private void calculateGeneralControlFlows() {
		memoryNextRegions.clear();
		memoryNextRegionsWithInterprocEdges.clear();

		// Calculate next regions of threads
		for (ThreadInstance thread : this.getThreads()) {
			Collection<ThreadRegion> target = new HashSet<ThreadRegion>();

			// Handle main thread separately: Point root to *Start* region(s)
			// Else, add first found region
			if (sdg.inDegreeOf(thread.getEntry()) == 0) {			// thread.entry.getLabel().equals("*Start*")
				for (ThreadRegion region : this.getRegions(thread)) {
					if (sdg.inDegreeOf(thread.getEntry()) == 0) {										// this.getEntryNode(region).getLabel().contains("*Start*")
						target.add(region);
					}
				}
			} else {
				target.add(this.getRegions(thread).iterator().next());
			}

			memoryNextRegions.put(thread, target);
			memoryNextRegionsWithInterprocEdges.put(thread, target);
		}


		// Calculate next regions of regions
		for (ThreadRegion region : this.getRegions()) {
			Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();
			Collection<ThreadRegion> regionsWithInterproceduralEdges = new HashSet<ThreadRegion>();

			// Loop through each node
			for (SDGNode source : region.getNodes()) {
				// Loop through each relevant edge
				for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(source, SDGEdge.Kind.CONTROL_FLOW)) {
					SDGNode target = edge.getTarget();

					// Add all target regions except the start region
					// Check regions for being in same thread
					for (ThreadRegion targetRegion : this.getRegions(target)) {
						if (targetRegion.getThread() == region.getThread()) {
							regions.add(targetRegion);
							regionsWithInterproceduralEdges.add(targetRegion);
						}
					}
				}
				regions.remove(region);
				memoryNextRegions.put(region, regions);

				// Loop through interprocedural edges (call and return edges)
				for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(source, SDGEdge.Kind.CALL)) {
					SDGNode target = edge.getTarget();

					// Add all target regions except the start region
					// Check regions for being in same thread
					for (ThreadRegion targetRegion : this.getRegions(target)) {
						if (targetRegion.getThread() == region.getThread()) {
							regionsWithInterproceduralEdges.add(targetRegion);
						}
					}
				}

				// Loop through return edges
				for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(source, SDGEdge.Kind.RETURN)) {
					SDGNode target = edge.getTarget();

					// Add all target regions except the start region
					// Check regions for being in same thread
					for (ThreadRegion targetRegion : this.getRegions(target)) {
						if (targetRegion.getThread() == region.getThread()) {
							regionsWithInterproceduralEdges.add(targetRegion);
						}
					}
				}
				regionsWithInterproceduralEdges.remove(region);
				memoryNextRegionsWithInterprocEdges.put(region, regionsWithInterproceduralEdges);
			}
		}
	}

	/* Calculates special control flows */
	private void calculateSpecialControlFlows() {
		memoryNextSourceCodeRegions.clear();
		memoryNextInterferingRegions.clear();
		memoryNextSourceCodeInterferingRegions.clear();

		memoryNextSourceCodeRegionsWithInterprocEdges.clear();
		memoryNextInterferingRegionsWithInterprocEdges.clear();
		memoryNextSourceCodeInterferingRegionsWithInterprocEdges.clear();

		// Calculate next regions for threads
		for (ThreadInstance thread : this.getThreads()) {
			Collection<ThreadRegion> targets = new HashSet<ThreadRegion>();

			// 1) "Hide non-source code regions"
			for (ThreadRegion region : this.getNextRegions(thread)) {
				if (SDGWrapper.getInstance().isInSourceCode(region)) {
					targets.add(region);
					break;
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (SDGWrapper.getInstance().isInSourceCode(region)) {
						targets.add(region);
						break;
					}
				}
			}
			memoryNextSourceCodeRegions.put(thread, targets);

			targets = new HashSet<ThreadRegion>();
			for (ThreadRegion region : this.getNextRegionsWithInterprocEdges(thread)) {
				if (SDGWrapper.getInstance().isInSourceCode(region)) {
					targets.add(region);
					break;
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (SDGWrapper.getInstance().isInSourceCode(region)) {
						targets.add(region);
						break;
					}
				}
			}
			memoryNextSourceCodeRegionsWithInterprocEdges.put(thread, targets);


			// 2) "Hide not interfering regions"
			targets = new HashSet<ThreadRegion>();
			for (ThreadRegion region : this.getNextRegions(thread)) {
				if (this.memoryInterferedRegions.get(region).size() > 0) {
					targets.add(region);
					break;
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (this.memoryInterferedRegions.get(region).size() > 0) {
						targets.add(region);
						break;
					}
				}
			}
			memoryNextInterferingRegions.put(thread, targets);

			targets = new HashSet<ThreadRegion>();
			for (ThreadRegion region : this.getNextRegionsWithInterprocEdges(thread)) {
				if (this.memoryInterferedRegions.get(region).size() > 0) {
					targets.add(region);
					break;
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (this.memoryInterferedRegions.get(region).size() > 0) {
						targets.add(region);
						break;
					}
				}
			}
			memoryNextInterferingRegionsWithInterprocEdges.put(thread, targets);


			// 3) "Hide non-source code regions" & "Hide not interfering regions"
			targets = new HashSet<ThreadRegion>();

			for (ThreadRegion region : this.getNextRegions(thread)) {
				if (SDGWrapper.getInstance().isInSourceCode(region)) {
					if (this.memoryInterferedRegions.get(region).size() > 0) {
						targets.add(region);
						break;
					}
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (SDGWrapper.getInstance().isInSourceCode(region)) {
						if (this.memoryInterferedRegions.get(region).size() > 0) {
							targets.add(region);
							break;
						}
					}
				}
			}
			memoryNextSourceCodeInterferingRegions.put(thread, targets);

			targets = new HashSet<ThreadRegion>();
			for (ThreadRegion region : this.getNextRegionsWithInterprocEdges(thread)) {
				if (SDGWrapper.getInstance().isInSourceCode(region)) {
					if (this.memoryInterferedRegions.get(region).size() > 0) {
						targets.add(region);
						break;
					}
				}
			}
			// If no regions has been found, add first suitable region
			if (targets.isEmpty()) {
				for (ThreadRegion region : this.getRegions()) {
					if (SDGWrapper.getInstance().isInSourceCode(region)) {
						if (this.memoryInterferedRegions.get(region).size() > 0) {
							targets.add(region);
							break;
						}
					}
				}
			}
			memoryNextSourceCodeInterferingRegionsWithInterprocEdges.put(thread, targets);
		}


		// Calculate next regions of thread regions
		for (ThreadRegion source : this.getRegions()) {
			// 1) "Hide non-source code regions"
			// Init
			Collection<ThreadRegion> targets = new HashSet<ThreadRegion>();
			Collection<ThreadRegion> visited = new HashSet<ThreadRegion>();
			Queue<ThreadRegion> queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source) || !this.isInSourceCode(current)) {
					for (ThreadRegion target : this.getNextRegions(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source) && this.isInSourceCode(current)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextSourceCodeRegions.put(source, targets);

			// Init
			targets = new HashSet<ThreadRegion>();
			visited = new HashSet<ThreadRegion>();
			queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source) || !this.isInSourceCode(current)) {
					for (ThreadRegion target : this.getNextRegionsWithInterprocEdges(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source) && this.isInSourceCode(current)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextSourceCodeRegionsWithInterprocEdges.put(source, targets);


			// 2) "Hide not interfering regions"
			// Init
			targets = new HashSet<ThreadRegion>();
			visited = new HashSet<ThreadRegion>();
			queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source)
						|| !(this.memoryInterferedRegions.get(current).size() > 0)) {
					for (ThreadRegion target : this.getNextRegions(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source)
						&& (this.memoryInterferedRegions.get(current).size() > 0)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextInterferingRegions.put(source, targets);

			// Init
			targets = new HashSet<ThreadRegion>();
			visited = new HashSet<ThreadRegion>();
			queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source)
						|| !(this.memoryInterferedRegions.get(current).size() > 0)) {
					for (ThreadRegion target : this.getNextRegionsWithInterprocEdges(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source)
						&& (this.memoryInterferedRegions.get(current).size() > 0)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextInterferingRegionsWithInterprocEdges.put(source, targets);


			// 3) "Hide non-source code regions" & "Hide not interfering regions"
			// Init
			targets = new HashSet<ThreadRegion>();
			visited = new HashSet<ThreadRegion>();
			queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source) || !this.isInSourceCode(current)
						|| !(this.memoryInterferedRegions.get(current).size() > 0)) {
					for (ThreadRegion target : this.getNextRegions(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source) && this.isInSourceCode(current)
						&& (this.memoryInterferedRegions.get(current).size() > 0)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextSourceCodeInterferingRegions.put(source, targets);

			// Init
			targets = new HashSet<ThreadRegion>();
			visited = new HashSet<ThreadRegion>();
			queue = new LinkedList<ThreadRegion>();
			queue.add(source);
			visited.add(source);

			// Breadth First Search [BFS]
			while (!queue.isEmpty()) {
				ThreadRegion current = queue.poll();

				// Abort further search on region when region is in source code
				if (current.equals(source) || !this.isInSourceCode(current)
						|| !(this.memoryInterferedRegions.get(current).size() > 0)) {
					for (ThreadRegion target : this.getNextRegionsWithInterprocEdges(current)) {
						if (!visited.contains(target)) {
							queue.add(target);
							visited.add(target);
						}
					}
				}

				if (!current.equals(source) && this.isInSourceCode(current)
						&& (this.memoryInterferedRegions.get(current).size() > 0)) {
					targets.add(current);
				}
			}

			// Save control flow edges
			memoryNextSourceCodeInterferingRegionsWithInterprocEdges.put(source, targets);
		}
	}

	private void cleanCSDG(SDG graph, MHPAnalysis mhp) {
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		int all = 0;
		int x = 0;

		for (SDGEdge e : graph.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				all++;
				// not parallel
				if (!mhp.isParallel(e.getSource(), e.getTarget())) {
					remove.add(e);
					x++;
				}
			}
		}

		for (SDGEdge e : remove) {
			graph.removeEdge(e);
		}

		System.out.println("Removed " + x + " of " + all + " edges.");
	}

	@SuppressWarnings("restriction")
	private void updateFileList() {
		// Create visitor for walking through the tree
		IResourceVisitor visitor = new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) {
				if (resource.getName().endsWith(FILE_POSTFIX_JAVA)) {
					if (resource instanceof org.eclipse.core.internal.resources.File) {
						org.eclipse.core.internal.resources.File file = (org.eclipse.core.internal.resources.File) resource;
						String source = file.getFullPath().toString();

						// Remove first "/"
						source = source.substring(1);
						fileList.add(source);
					}
					return false;
				}
				return true;
			}
		};

		// Update file list
		if (project != null ) {
			if (project instanceof IAdaptable) {
				IAdaptable a = (IAdaptable) project;
				IJavaElement javaElement = (IJavaElement) a.getAdapter(IJavaElement.class);
				IJavaProject javaProject = javaElement.getJavaProject();
				fileList = new HashSet<String>();

				try {
					javaProject.getResource().accept(visitor);
				} catch (CoreException e) {
					Controller.getInstance().updateStatusBar("Walking through package explorer failed.");
					e.printStackTrace();
				}
			}
		}
	}

	private String getThreadFunctionLabel(ThreadInstance thread) {
		String label = thread.getEntry().getLabel();

		// Handle main thread
		if (label.equals("*Start*")) {
			for (SDGNode called : this.getCalledMethods(thread.getEntry())) {
				if (!called.getLabel().contains(".<clinit>") && sdg.inDegreeOf(called) != 0) {			//!called.getLabel().contains("*Start*")
					label = called.getLabel();
					break;
				}
			}
		}

		// Cut off the parameters and brackets
		if (label.indexOf("(") != -1) {
			label = label.substring(0, label.lastIndexOf("("));
			label = label + "()";
		}

		return label;
	}

	private String getRegionLabel(ThreadRegion region) {
		String label = this.getEntryNode(region).getLabel();

		// Cut off parameters
		if (label.indexOf("(") != -1) {
			label = label.substring(0, label.lastIndexOf("(") + 1);
			label = label.concat(")");

			// Cut off path name
			if (this.isInSourceCode(region)) {
				if (label.indexOf(".") != -1) {
					String tmp = label.substring(0, label.lastIndexOf("."));

					if (tmp.indexOf(".") != -1) {
						label = label.substring(tmp.lastIndexOf(".") + 1, label.length());
					}
				}
			}
		}

		// Add line and kind of start node
		label = label + " : " + region.getStart().getSr();
		label = label + " : " + region.getStart().getKind().toString();

		return label;
	}

	private String getCallContext(ThreadInstance thread) {
		String label = "";

		// Main thread is not forked...
		if (thread.getFork() != null) {
			// Add call location
			SDGNode fork = thread.getFork();
			label = label + " @ " + fork.getSource() + " : " + fork.getSr() + " [";

			for (int i = thread.getThreadContext().size() - 1; i >= 0; i--) {
				SDGNode node = thread.getThreadContext().get(i);
				String tmp = node.getLabel();
				tmp = tmp.substring(0, tmp.lastIndexOf("("));
				label += tmp + (i != 0 ? "->" : "");
			}

			label += "]";
		}

		return label;
	}
}

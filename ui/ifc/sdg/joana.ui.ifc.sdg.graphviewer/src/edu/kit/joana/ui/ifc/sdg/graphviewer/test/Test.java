/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.test;
///*
// * Test.java
// *
// * Created on 24. November 2005, 16:54
// */
//
//package edu.kit.joana.ui.ifc.sdg.graphviewer.test;
//
//import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.Node;
//import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDG;
//import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGLayoutAlgorithm;
//import edu.kit.joana.ui.ifc.sdg.graphviewer.layout.PDGConstants;
//import edu.kit.joana.ui.ifc.sdg.graphviewer.model.MethodGraph;
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.geom.Rectangle2D;
//import java.awt.image.BufferedImage;
//import java.awt.image.ColorModel;
//import java.awt.image.DataBuffer;
//import java.awt.image.IndexColorModel;
//import java.awt.image.PixelInterleavedSampleModel;
//import java.awt.image.SampleModel;
//import java.awt.image.SinglePixelPackedSampleModel;
//import java.awt.image.WritableRaster;
//import java.io.File;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//import java.util.TreeSet;
//import java.util.Vector;
//import javax.media.jai.JAI;
//import javax.media.jai.RenderedOp;
//import javax.media.jai.TiledImage;
//import javax.swing.JFrame;
//import javax.swing.JScrollPane;
//import javax.swing.WindowConstants;
//
//import edu.kit.joana.ifc.sdg.graph.SDG;
//import edu.kit.joana.ifc.sdg.graph.SDGEdge;
//import edu.kit.joana.ifc.sdg.graph.SDGNode;
//import edu.kit.joana.ifc.sdg.graph.SDG_Lexer;
//import edu.kit.joana.ifc.sdg.graph.SDG_Parser;
//import org.jgrapht.traverse.DepthFirstIterator;
//import org.jgraph.JGraph;
//import org.jgraph.graph.AttributeMap;
//import org.jgraph.graph.DefaultGraphModel;
//import org.jgraph.graph.Edge;
//import org.jgraph.graph.GraphCell;
//import org.jgraph.graph.GraphConstants;
//import org.jgraph.graph.GraphModel;
//import org.jgraph.util.JGraphUtilities;
//
///**
// * A test suite for various graph properties.
// * This class can process and layout many PDGs in one run.
// * The code to save a PDG as image is redundant because it was developed here
// * first and then integrated into the graph viewer.
// * Images with a color depth of 24 bits are untested.
// * @author Siegfried Weber
// */
//public class Test {
//
//    /**
//     * a predefined PDG
//     */
//    private static String GRAPH = "pdgs/javacard.framework.JCMain.pdg";
//    /**
//     * the output directory for the images
//     */
//    private static final File outDir = new File("build/test/images");
//    /**
//     * key for the dfsnum attribute
//     */
//    private static final String DFSNUM = "dfsnum";
//    /**
//     * key for the compnum attribute
//     */
//    private static final String COMPNUM = "compnum";
//
//    /**
//     * the proc number of the first PDG
//     */
//    private static int START_PROC = 0;
//    /**
//     * the number of PDGs to process
//     * 0 for all PDGs in the SDG
//     */
//    private static final int NR_PROC = 0;
//    /**
//     * maximum image size
//     * greater images are discarded
//     */
//    private static final double MAX_IMAGE_SIZE = 145000000d;
//    /**
//     * specifies whether the graph will be layouted
//     */
//    private static final boolean FLAG_LAYOUT = true;
//
//    /**
//     * the method names of all PDGs
//     */
//    private Vector<String> titles;
//
//    /**
//     * variable for depth first search
//     */
//    private int dfsnum = 0;
//    /**
//     * variable for depth first search
//     */
//    private int compnum = 0;
//
//    /**
//     * number of PDGs in the SDG
//     */
//    private int nProcs;
//    /**
//     * number of tree edges in the SDG
//     */
//    private int nTreeEdges;
//    /**
//     * number of forward edges in the SDG
//     */
//    private int nForwardEdges;
//    /**
//     * number of backward edges in the SDG
//     */
//    private int nBackwardEdges;
//    /**
//     * number of cross edges in the SDG
//     */
//    private int nCrossEdges;
//    /**
//     * number of loops in the SDG
//     */
//    private int nLoops;
//
//    /** Creates a new instance of Test */
//    public Test() {
//    }
//
//    /**
//     * Runs the tests.
//     * @param graph the SDG
//     */
//    public void test(SDG graph) {
//        final PDGLayoutAlgorithm layoutAlgorithm = new PDGLayoutAlgorithm();
//        Vector<SDG> methodGraphs = createMethodGraphs(graph);
//        nProcs = methodGraphs.size();
//
//        JFrame frame = null;
//        if(FLAG_LAYOUT) {
//            if(!outDir.mkdirs()) {
//                System.out.println("ERROR: cannot create output directory");
//            }
//            // initialize JFrame
//            frame = new JFrame();
//            frame.setSize(640, 480);
//            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
//            frame.setVisible(true);
//        }
//
//        // show method graphs
//        int endProc = nProcs - 1;
//        if(NR_PROC > 0)
//            endProc = Math.min(endProc, START_PROC + NR_PROC - 1);
//        for(int procID = START_PROC; procID <= endProc; procID++) {
//            JGraph component = null;
//            System.gc();
//            try {
//                // init graph
//                System.out.println("PROC " + procID);
//                System.out.println("+ init");
//                SDG methodGraph = methodGraphs.get(procID);
//                MethodGraph g = new MethodGraph(methodGraph, procID);
//                component = g.getJGraph();
//                methodGraph = null;
//                g = null;
//
//                count(component);
//                searchCycles(component);
//                searchMultiEdges(component);
//                searchLoops(component);
//
//                if(FLAG_LAYOUT) {
//                    // layout graph
//                    layout(component, layoutAlgorithm);
//
//                    // add graph to JFrame
//                    System.out.println("+ show");
//                    frame.setTitle(String.valueOf("PROC " + procID));
//                    frame.getContentPane().removeAll();
//                    frame.getContentPane().add(new JScrollPane(component));
//                    frame.getContentPane().validate();
//
//                    // save graph as image
//                    saveAsImage(component, procID);
//                }
//
//                System.out.println();
//            } catch(OutOfMemoryError e) {
//                System.out.println("ERROR: out of memory");
//                System.out.println();
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        System.out.println("Summary:");
//        System.out.println("  PROCs: " + nProcs);
//        System.out.println("  tree edges: " + nTreeEdges);
//        System.out.println("  forward edges: " + nForwardEdges);
//        System.out.println("  backward edges: " + nBackwardEdges);
//        System.out.println("  cross edges: " + nCrossEdges);
//        System.out.println("  loops: " + nLoops);
//        if(FLAG_LAYOUT) {
//            frame.setVisible(false);
//            frame.dispose();
//        }
//    }
//
//    /**
//     * Prints the number of vertices and edges.
//     * @param component a JGraph component
//     */
//    private void count(JGraph component) {
//        int nVertices = 0;
//        int nEdges = 0;
//        GraphModel model = component.getModel();
//        Object[] cells = DefaultGraphModel.getAll(model);
//        for(Object cell : cells) {
//            if(model.isEdge(cell))
//                nEdges++;
//            else if(!model.isPort(cell))
//                nVertices++;
//        }
//        System.out.println("  vertices: " + nVertices);
//        System.out.println("  edges: " + nEdges);
//    }
//
//    /**
//     * Returns the set of colors contained in the JGraph component.
//     * @param component a JGraph component
//     * @return a set of colors
//     */
//    private Set<Color> getColors(JGraph component) {
//        HashSet<Color> colors = new HashSet<Color>(100);
//        GraphModel model = component.getModel();
//        Object[] cells = DefaultGraphModel.getAll(model);
//        for(Object o : cells) {
//            GraphCell cell = (GraphCell) o;
//            AttributeMap map = cell.getAttributes();
//            if(model.isEdge(cell)) {
//                colors.add(GraphConstants.getLineColor(map));
//            } else if(!model.isPort(cell)) {
//                colors.add(GraphConstants.getForeground(map));
//                colors.add(GraphConstants.getBackground(map));
//            }
//        }
//        return colors;
//    }
//
//    /**
//     * Searches for cycles.
//     * @param component a JGraph component
//     */
//    private void searchCycles(JGraph component) {
//        System.out.println("+ searchCycles");
//        PDG pdg = new PDG(component);
//        List<Node[]> fbcEdges = new LinkedList<Node[]>();
//        for(Node root : pdg.getRoots()) {
//            searchCycles(pdg, root, fbcEdges);
//            nTreeEdges++;
//        }
//        classifyEdges(fbcEdges);
//    }
//
//    /**
//     * Searches for cylces in the PDG with depth first search.
//     * @param pdg a PDG
//     * @param node the current node
//     * @param fbcEdges a list of forward, backward and cross edges
//     */
//    private void searchCycles(PDG pdg, Node node, List<Node[]> fbcEdges) {
//        node.setAttribute(DFSNUM, ++dfsnum);
//        for(Node child : pdg.getChildren(node)) {
//            if(child.getAttribute(DFSNUM) == null) {
//                searchCycles(pdg, child, fbcEdges);
//            } else {
//                fbcEdges.add(new Node[] {node, child});
//            }
//        }
//        node.setAttribute(COMPNUM, ++compnum);
//    }
//
//    /**
//     * Classifies the edges in the given list.
//     * @param fbcEdges a list with forward, backward and cross edges
//     */
//    private void classifyEdges(List<Node[]> fbcEdges) {
//        if(!fbcEdges.isEmpty())
//            System.out.println("  edge classification:");
//        for(Node[] edge : fbcEdges) {
//            String classification = null;
//            int v_dfsnum = (Integer) edge[0].getAttribute(DFSNUM);
//            int w_dfsnum = (Integer) edge[1].getAttribute(DFSNUM);
//            if(v_dfsnum < w_dfsnum) {
//                classification = "forward";
//                nForwardEdges++;
//            } else {
//                int v_compnum = (Integer) edge[0].getAttribute(COMPNUM);
//                int w_compnum = (Integer) edge[1].getAttribute(COMPNUM);
//                if(v_compnum < w_compnum) {
//                    classification = "backward";
//                    nBackwardEdges++;
//                } else {
//                    classification = "cross";
//                    nCrossEdges++;
//                }
//            }
//            System.out.print("    " + classification + " edge: ");
//            print(edge[0], edge[1]);
//            System.out.println();
//        }
//    }
//
//    /**
//     * Searches for multi edges.
//     * @param graph a JGraph component
//     */
//    private void searchMultiEdges(JGraph graph) {
//        System.out.println("+ search multi edges");
//        GraphModel model = graph.getModel();
//        Object[] cells = DefaultGraphModel.getAll(model);
//        for(Object o : cells) {
//            GraphCell cell = (GraphCell) o;
//            if(!model.isEdge(cell) && !model.isPort(cell)) {
//                Object[] edges = DefaultGraphModel.getOutgoingEdges(model,
//                        cell);
//                TreeSet<String> cdNodes = new TreeSet<String>();
//                TreeSet<String> ddNodes = new TreeSet<String>();
//                for(Object edge : edges) {
//                    GraphCell target = (GraphCell) DefaultGraphModel.
//                            getTargetVertex(model, edge);
//                    String id = target.toString();
//                    if(id.length() == 0)
//                        id = PDGConstants.getToolTip(target.getAttributes());
//                    if(PDG.isDAGEdge((Edge) edge)) {
//                        boolean singleEdge = cdNodes.add(id);
//                        if(!singleEdge) {
//                            System.out.print("  multi control dependency " +
//                                    "edge: ");
//                            print(cell, target);
//                            System.out.println();
//                        }
//                    } else {
//                        boolean singleEdge = ddNodes.add(id);
//                        if(!singleEdge) {
//                            System.out.print("  multi data dependency edge: ");
//                            print(cell, target);
//                            System.out.println();
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * Searches for loops.
//     * @param graph a JGraph component
//     */
//    private void searchLoops(JGraph graph) {
//        System.out.println("+ searchLoops");
//        GraphModel model = graph.getModel();
//        Object[] cells = DefaultGraphModel.getAll(model);
//        boolean firstLoop = true;
//        for(Object cell : cells) {
//            if(!model.isEdge(cell) && !model.isPort(cell)) {
//                Object[] loops = DefaultGraphModel.getEdgesBetween(model, cell,
//                        cell, false);
//                if(loops.length > 0) {
//                    if(firstLoop) {
//                        System.out.println("  Loops:");
//                        firstLoop = false;
//                    }
//                    nLoops += loops.length;
//                    System.out.print("    " + loops.length + " loop");
//                    if(loops.length > 1)
//                        System.out.print("s");
//                    System.out.print(": ");
//                    print((GraphCell) cell);
//                    System.out.println();
//                }
//            }
//        }
//    }
//
//    /**
//     * Layouts a PDG.
//     * @param component the PDG as JGraph component
//     * @param layoutAlgorithm the layout algorithm
//     */
//    private void layout(JGraph component, PDGLayoutAlgorithm layoutAlgorithm) {
//        System.out.println("+ layout");
//        long timer = System.currentTimeMillis();
//        JGraphUtilities.applyLayout(component, layoutAlgorithm);
//        System.out.println("  time: " + (System.currentTimeMillis() - timer) +
//                " ms");
//    }
//
//    /**
//     * Creates a color model from the colors in the JGraph component.
//     * @param component the JGraph component
//     * @return a color model
//     */
//    private ColorModel createColorModel(JGraph component) {
//        ColorModel colorModel = null;
//        Set<Color> colors = getColors(component);
//        int nColors = colors.size();
//        if(nColors > 255) {
//            colorModel = ColorModel.getRGBdefault();
//        } else {
//            int[] cmap = new int[colors.size() + 1];
//            cmap[0] = Color.WHITE.getRGB();
//            int i = 1;
//            boolean hasAlpha = false;
//            for(Color color : colors) {
//                cmap[i] = color.getRGB();
//                if((cmap[i] & 0xFF000000) != 0xFF000000)
//                    hasAlpha = true;
//                i++;
//            }
//            colorModel = new IndexColorModel(8, cmap.length, cmap, 0, hasAlpha,
//                    -1, DataBuffer.TYPE_BYTE);
//        }
//        return colorModel;
//    }
//
//    /**
//     * Creates a sample model.
//     * @param colorModel a color model
//     * @param width the width of a sample
//     * @param height the height of a sample
//     * @return a sample model
//     */
//    private SampleModel createSampleModel(ColorModel colorModel, int width,
//            int height) {
//        SampleModel sampleModel = null;
//        if(colorModel instanceof IndexColorModel) {
//            sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
//                    width, height, 1, width, new int[] {0});
//        } else {
//            sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
//                    width, height,
//                    new int[] {0xFF0000, 0xFF00, 0xFF, 0xFF000000});
//        }
//        return sampleModel;
//    }
//
//    /**
//     * Creates a tiled image.
//     * @param component the JGraph component
//     * @param width the width of the image
//     * @param height the height of the image
//     * @return a tiled image
//     */
//    private TiledImage createImage(JGraph component, int width, int height) {
//        ColorModel colorModel = createColorModel(component);
//        SampleModel sampleModel = createSampleModel(colorModel, 1000, 1000);
//        return new TiledImage(0, 0, width, height, 0, 0, sampleModel,
//                colorModel);
//    }
//
//    /**
//     * Saves the image of a PDG in a file.
//     * @param component a JGraph component
//     * @param procID the proc number of the PDG
//     */
//    private void saveAsImage(JGraph component, int procID) {
//        System.out.println("+ save image");
//        Rectangle2D bounds = component.getCellBounds(component.getRoots());
//        double dWidth = bounds.getWidth();
//        double dHeight = bounds.getHeight();
//        int width = (int) dWidth;
//        int height = (int) dHeight;
//        System.out.println("  size: " + width + "x" + height);
//        if(dWidth * dHeight <= MAX_IMAGE_SIZE) {
//            TiledImage img = createImage(component, width, height);
//            System.out.println("  image created");
//            boolean printed = false;
//            try {
//                for(int tx = img.getMinTileX(); tx <= img.getMaxTileX(); tx++) {
//                    for(int ty = img.getMinTileY(); ty <=
//                            img.getMaxTileY(); ty++) {
//                        WritableRaster raster = img.getWritableTile(tx, ty);
//                        int x = raster.getMinX();
//                        int y = raster.getMinY();
//                        WritableRaster childRaster =
//                                raster.createWritableTranslatedChild(0, 0);
//                        ColorModel colorModel = img.getColorModel();
//                        BufferedImage tile = new BufferedImage(colorModel,
//                                childRaster, colorModel.isAlphaPremultiplied(),
//                                null);
//                        Graphics2D graphics = tile.createGraphics();
//                        graphics.translate(-x, -y);
//                        component.print(graphics);
//                    }
//                }
//                System.out.println("  graph painted");
//                printed = true;
//            } catch(ArrayIndexOutOfBoundsException e) {
//                System.out.println("ERROR: bug in Java");
//            }
//            if(printed) {
//                String filename = outDir.getAbsolutePath() + "/" + procID +
//                        ".png";
//                RenderedOp op = JAI.create("filestore", img, filename, "PNG");
//                System.out.println("  done");
//            }
//        } else {
//            System.out.println("  image is too big");
//        }
//    }
//
//    /**
//     * Creates all PDGs from a SDG.
//     * @param graph a SDG
//     * @return a vector of PDGs
//     */
//    private Vector<SDG> createMethodGraphs(SDG graph) {
//        Vector<SDG> methodGraphs = new Vector<SDG>();
//        titles = new Vector<String>();
//        for(Iterator vertexIterator = new DepthFirstIterator(graph);
//        vertexIterator.hasNext();) {
//            SDGNode currentVertex = (SDGNode) vertexIterator.next();
//            int currentProc = currentVertex.getProc();
//            if(methodGraphs.size() <= currentProc)
//                methodGraphs.setSize(currentProc + 1);
//            SDG methodGraph = methodGraphs.get(currentProc);
//            if(methodGraph == null) {
//                methodGraph = new SDG();
//                methodGraphs.set(currentProc, methodGraph);
//            }
//            methodGraph.addVertex(currentVertex);
//
//            if (currentVertex.getKind().equals(SDGNode.Kind.ENTRY)) {
//                if(titles.size() <= currentProc)
//                    titles.setSize(currentProc + 1);
//                titles.set(currentProc, currentVertex.getLabel().toString());
//            }
//        }
//        for(SDGEdge edge : graph.edgeSet()) {
//            SDGNode source = edge.getSource();
//            int sourceProc = source.getProc();
//            SDGNode target = edge.getTarget();
//            int targetProc = target.getProc();
//            if(sourceProc != targetProc) {
//                SDG sourceGraph = methodGraphs.get(sourceProc);
//                sourceGraph.addVertex(target);
//                sourceGraph.addEdge(edge);
//                SDG targetGraph = methodGraphs.get(targetProc);
//                targetGraph.addVertex(source);
//                targetGraph.addEdge(edge);
//            } else {
//                SDG methodGraph = methodGraphs.get(sourceProc);
//                methodGraph.addEdge(edge);
//            }
//        }
//        return methodGraphs;
//    }
//
//    /**
//     * Prints a textual representation of an edge.
//     * @param start the start node
//     * @param end the end node
//     */
//    private void print(Node start, Node end) {
//        print(start.getCell(), end.getCell());
//    }
//
//    /**
//     * Prints a textual representation of an edge.
//     * @param start the start node
//     * @param end the end node
//     */
//    private void print(GraphCell start, GraphCell end) {
//        print(start);
//        System.out.print(" -> ");
//        print(end);
//    }
//
//    /**
//     * Prints the textual representation of a JGraph cell.
//     * @param cell a JGraph cell
//     */
//    private void print(GraphCell cell) {
//        String label = cell.toString();
//        if(label.length() == 0) {
//            label = PDGConstants.getToolTip(cell.getAttributes());
//            int start = label.indexOf("<br>") + 4;
//            int end = label.indexOf("<br>", start);
//            String id = label.substring(start, end);
//            System.out.print(id + " (external node)");
//        } else {
//            int br = label.indexOf('<', 6);
//            String id = label.substring(6, br);
//            System.out.print(id);
//        }
//    }
//
//    /**
//     * Starts the test suite.
//     * @param args The first parameter denotes the proc number to start with.
//     *             The second parameter is a path to a PDG.
//     *             Both parameters are optional.
//     */
//    public static void main(String[] args) {
//        if(args.length > 0)
//            START_PROC = new Integer(args[0]);
//        if(args.length > 1)
//            GRAPH = args[1];
//        try {
//            SDG graph = SDG.readFrom(GRAPH);
//            new Test().test(graph);
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

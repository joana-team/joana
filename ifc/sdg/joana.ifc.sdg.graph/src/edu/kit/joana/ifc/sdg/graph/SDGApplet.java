/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

/* ----------------------
 * SDGApplet.java
 * ----------------------
 *
 */

//package edu.kit.joana.ifc.sdg.graph;
//
//import java.io.*;
//import java.util.*;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.Iterator;
//
//import org.jgraph.JGraph;
//import org.jgraph.graph.DefaultGraphCell;
//import org.jgraph.graph.GraphConstants;
//
//import org.jgrapht.*;
//import org.jgrapht.graph.*;
//import org.jgrapht.traverse.BreadthFirstIterator;
//import org.jgrapht.ext.JGraphModelAdapter;
//
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.Rectangle;
//
//import javax.swing.JApplet;
//import javax.swing.JFrame;
//
//
///**
// * A applet that uses JGraph to visualize an SDG.
// *
// * @author Tobias Eichinger
// *
// * @since May 2003
// */
//public class SDGApplet extends JApplet {
//
//    public SDG g;
//    private static final Color DEFAULT_BG_COLOR = Color.decode( "#FAFBFF" );
//    // size of display
//    private static final Dimension DEFAULT_SIZE = new Dimension( 1020, 720 );
//    private JGraphModelAdapter m_jgAdapter;
//
//
//    /**
//     * @see java.applet.Applet#init().
//     */
//    public void init( ) {
//        try {
//          // create a JGraphT graph
//          g = SDG.readFrom("/home/st/giffhorn/java/tests.ProducerConsumer.pdg");
//        } catch (Exception e) {
//          e.printStackTrace( );
//        }
//
//        //System.out.println(g);
//        // krinke algorithm
//        //g = SDG.slicealg(g);
//
//        // create a visualization using JGraph, via an adapter
//        m_jgAdapter = new JGraphModelAdapter( g );
//        JGraph jgraph = new JGraph( m_jgAdapter );
//
//        adjustDisplaySettings( jgraph );
//        getContentPane( ).add( jgraph );
//        resize( DEFAULT_SIZE );
//
//        drawGraphRandom ( g );
//    }
//
//
//    /**
//     * An alternative starting point for this applet, to also allow running
//     * this applet as an application.
//     *
//     * @param args
//     */
//    public static void main( String[] args ) {
//        SDGApplet applet = new SDGApplet( );
//        applet.init( );
//
//        JFrame frame = new JFrame( );
//        frame.getContentPane( ).add( applet );
//        frame.setTitle( "JGraphT Adapter to JGraph Demo" );
//        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//        frame.pack( );
//        frame.setVisible(true);
//    }
//
//
//    /**
//     * adjusts the display settings
//     *
//     * @param jg a JGraph
//     */
//    private void adjustDisplaySettings( JGraph jg ) {
//      jg.setPreferredSize( DEFAULT_SIZE );
//
//      Color  c        = DEFAULT_BG_COLOR;
//      String colorStr = null;
//
//      try {
//        colorStr = getParameter("bgcolor");
//      }
//      catch ( Exception e ) {
//        e.printStackTrace();
//      }
//
//      if( colorStr != null ) {
//        c = Color.decode( colorStr );
//      }
//
//      jg.setBackground( c );
//    }
//
//    /**
//     * display node of a graph at position with coordinates x, y in layout
//     *
//     * @param vertex node that is displayed
//     *
//     * @param x x-coordinate of node
//     *
//     * @param y y-coordinate of node
//     */
//    private void positionVertexAt( Object vertex, int x, int y ) {
//
//        DefaultGraphCell cell = m_jgAdapter.getVertexCell( vertex );
//        Map              attr = cell.getAttributes( );
//
//        // size of displayed node in layout
//        GraphConstants.setBounds( attr, new Rectangle( x, y, 20, 20 ) );
//        Map cellAttr = new HashMap( );
//        cellAttr.put( cell, attr );
//        m_jgAdapter.edit( cellAttr );
//    }
//
//    /**
//     * display vertices of graph at random position in layout
//     *
//     * @param g directed multigraph
//     */
//    private void drawGraphRandom ( DirectedPseudograph<SDGNode,SDGEdge> g ) {
//      long xc = 1000;
//      long yc = 700;
//      int xcs,ycs;
//      Random xrand = new Random(xc);
//      Random yrand = new Random(yc);
//      for( Iterator i = new BreadthFirstIterator( g ); i.hasNext(  ); ) {
//        // x-coordinate in 10 - 1010
//        xcs = Math.abs((xrand.nextInt( ) % 1000)+10);
//        // y-coordinate in 10 - 690
//        ycs = Math.abs((yrand.nextInt( ) % 680)+10);
//        positionVertexAt(i.next( ), xcs, ycs);
//      }
//    }
//
//    /**
//     * display vertices of graph in a grid layout
//     *
//     * @param g directed multigraph
//     */
//    private void drawGraphInGrid ( DirectedPseudograph<SDGNode,SDGEdge> g ) {
//      int xcounter = 10;
//      int ycounter = 0;
//      int columns = 0;
//      for( Iterator i = new BreadthFirstIterator( g ); i.hasNext( ); ) {
//        columns++;
//        if (columns == 12) {    // 12 nodes each row
//          ycounter += 90; xcounter = 10; columns = 0;
//        }   // y-coordinate + 90, set x-coordinate to start
//        positionVertexAt( i.next( ), xcounter, ycounter );
//        xcounter += 80;
//      }
//    }
//
//}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.algorithms;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;


/** This class parses the Algorithms.xml file that contains all available <br>
 * slicing and chopping algorithms.
 * It uses the DOM XML parser library.
 * The parsed data is represented in an Algorithms-object.
 * To parse the file, call getAvailableAlgorithms().
 *
 * @author giffhorn
 * @see Algorithms
 */
public final class DomAlgorithmUnmarshaller {
	// The path to the XML file Algorithms.xml
	private static final String XML = "/config/Algorithms.xml";

	// this is a utility class
	private DomAlgorithmUnmarshaller() { }

	/** Parses the Algorithms.xml file that contains all available slicing and chopping algorithms.
	 *
	 * @return  The algorithms as an Algorithms-object or null, if the XML file was corrupted.
	 */
	public static Algorithms getAvailableAlgorithms() {
		Algorithms algs = null;

		try {
			// open the file
//			File file = Activator.getDefault().getLocalFile(XML);
//			InputSource src = new InputSource(new FileInputStream(file));

			InputStream in =  Activator.getDefault().getClass().getResourceAsStream(XML);
			InputSource src = new InputSource(in);

		    // parse it...
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(src);
//			DOMParser prsr = new DOMParser();
//			prsr.parse(src);
//			Document doc = prsr.getDocument();

			// ... and create the data structure
			algs  = unmarshallAlgorithms(doc.getDocumentElement());

		} catch(IOException exc ) {
		    exc.printStackTrace();

		} catch (SAXException sexc) {
			sexc.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return algs;
	}

	/** Creates the Algorithms-object that represents the parsed data.
	 *
	 * @param rootNode  The enclosing XML tag.
	 * @return  The Algorithms-object.
	 */
    private static Algorithms unmarshallAlgorithms(Node rootNode) {
    	Algorithms algs = new Algorithms();

    	Node n;
    	NodeList nodes = rootNode.getChildNodes();

    	// iterate over all childs of rootNode
    	for (int i = 0; i < nodes.getLength(); i++) {
    		n = nodes.item(i);

    		if (n.getNodeType() == Node.ELEMENT_NODE) {

    			// all childs are named "algorithm"
    			if (n.getNodeName().equals("algorithm")) {
    				algs.addAlgorithm(unmarshallAlgorithm(n, null));

    			}

    		}
    	}

    	return algs;
    }

    /** Parses an "algorithm" node of the XML data file.
     *
     * @param algorithmNode Represents the "algorithm" node.
     * @param parent The "algorithm" parent. Can be null.
     * @return An Algorithm object representing the algorithm node.
     */
    private static Algorithm unmarshallAlgorithm(Node algorithmNode, Algorithm parent) {
    	Algorithm alg = new Algorithm(parent);

    	// an algorithm node can have up to three attributes:
    	// 'name' for the name of the algorithm
    	// 'description' for a brief description
    	// 'class' containing the name of the Java class that implements the algorithm
    	if(algorithmNode.hasAttributes() == true) {
    		alg.setName(unmarshallAttribute(algorithmNode, "name", "unknown"));
    		alg.setDescription(unmarshallAttribute(algorithmNode, "description", "no description"));
    		alg.setClassName(unmarshallAttribute(algorithmNode, "class", "no class defined"));
    	}

    	Node n;
    	NodeList nodes = algorithmNode.getChildNodes();

    	// iterate over all childs of algorithmNode and call this method recursively
    	for (int i = 0; i < nodes.getLength(); i++) {
    		n = nodes.item(i);

    		if (n.getNodeType() == Node.ELEMENT_NODE){

    			// all childs are algorithm nodes
    			if (n.getNodeName().equals("algorithm")) {
    				alg.addAlgorithm(unmarshallAlgorithm(n, alg));
    			}
    		}
    	}

    	return alg;
    }

    /** A standard method to parse an XML attribute.
     *
     * @param node          The XML node.
     * @param name          The attribute name.
     * @param defaultValue  A default value if the attribute is not set.
     * @return              The value of the attribute or the default value.
     */
    private static String unmarshallAttribute(Node node, String name, String defaultValue) {
    	Node n = node.getAttributes().getNamedItem(name);
    	return (n != null) ? (n.getNodeValue()) : (defaultValue);
    }
}

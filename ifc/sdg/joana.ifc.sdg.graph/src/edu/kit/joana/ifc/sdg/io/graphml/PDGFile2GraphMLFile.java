/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.graphml;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import edu.kit.joana.ifc.sdg.graph.SDG;

/**
 * TODO: @author Add your name here.
 */
public class PDGFile2GraphMLFile {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws IOException, XMLStreamException {
		if (args.length != 1) {
			System.out.println("Format: java -cp <CLASSPATH> edu.kit.joana.ifc.sdg.io.graphml.PDGFile2GraphMLFile <pdgfile>");
			System.out.println();
			System.out.println("..writes to: <pdgfile>.graphml");
			return;
		}
		final String file = args[0];
		final String outFile = file + ".graphml";
		final SDG sdg = SDG.readFrom(file);

		final BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outFile));
		SDG2GraphML.convertHierachical(sdg, bOut);
		System.out.println("Written to: " + outFile);
	}

}

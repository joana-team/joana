/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.graphml;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import javanet.staxutils.IndentingXMLStreamWriter;


/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class SDG2GraphML {
	/**
	 * Constants for names of JOANA specific node- and edge-attributes
	 */
	private static final String JOANA_EDGE_LABEL = "edgeLabel";
	private static final String JOANA_EDGE_KIND = "edgeKind";
	private static final String JOANA_NODE_EC = "nodeEc";
	private static final String JOANA_NODE_ER = "nodeEr";
	private static final String JOANA_NODE_SC = "nodeSc";
	private static final String JOANA_NODE_SR = "nodeSr";
	private static final String JOANA_NODE_BC_INDEX = "nodeBCIndex";
	private static final String JOANA_NODE_BC_NAME = "nodeBcName";
	private static final String JOANA_NODE_LABEL = "nodeLabel";
	private static final String JOANA_NODE_OPERATION = "nodeOperation";
	private static final String JOANA_NODE_PROC = "nodeProc";
	private static final String JOANA_NODE_SOURCE = "nodeSource";
	private static final String JOANA_NODE_KIND = "nodeKind";
	private static final String JOANA_NODE_LOCALDEF = "nodeLocalDef";
	private static final String JOANA_NODE_LOCALUSE = "nodeLocalUse";
	
	/**
	 * Constants for names from the GraphML namespace
	 */
	private static final String GRAPHML_DIRECTED = "directed";
	private static final String GRAPHML_EDGEDEFAULT = "edgedefault";
	private static final String GRAPHML_GRAPH = "graph";
	private static final String GRAPHML_GRAPHML = "graphml";
	private static final String GRAPHML_XMLNS = "http://graphml.graphdrawing.org/xmlns";
	private static final String GRAPHML_INT = "int";
	private static final String GRAPHML_STRING = "string";
	private static final String GRAPHML_ATTR_TYPE = "attr.type";
	private static final String GRAPHML_ATTR_NAME = "attr.name";
	private static final String GRAPHML_FOR = "for";
	private static final String GRAPHML_SOURCE = "source";
	private static final String GRAPHML_EDGE = "edge";
	private static final String GRAPHML_KEY = "key";
	private static final String GRAPHML_DATA = "data";
	private static final String GRAPHML_ID = "id";
	private static final String GRAPHML_NODE = "node";
	
	
	private static final boolean OUTPUT_EDGELABELS=false;
	private static final String ENCODING = "UTF-8";
	
	private static final Escaper xmlEscaper = XmlEscapers.xmlContentEscaper();
	
	private static void writeNode(SDGNode n, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(GRAPHML_NODE); {
			writer.writeAttribute(GRAPHML_ID, Integer.toString(n.getId()));
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_KIND);
				writer.writeCharacters(n.getKind().toString());
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_SOURCE);
				writer.writeCharacters(n.getSource());
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_PROC);
				writer.writeCharacters(Integer.toString(n.getProc()));
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_OPERATION);
				writer.writeCharacters(n.getOperation().toString());
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_LABEL);
				writer.writeCharacters(xmlEscaper.escape(n.getLabel()));
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_BC_NAME);
				writer.writeCharacters(n.getBytecodeName());
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_BC_INDEX);
				writer.writeCharacters(Integer.toString(n.getBytecodeIndex()));
			} writer.writeEndElement();
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_SR);
				writer.writeCharacters(Integer.toString(n.getSr()));
			} writer.writeEndElement();
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_SC);
				writer.writeCharacters(Integer.toString(n.getSc()));
			} writer.writeEndElement();
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_ER);
				writer.writeCharacters(Integer.toString(n.getEr()));
			} writer.writeEndElement();
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_EC);
				writer.writeCharacters(Integer.toString(n.getEc()));
			} writer.writeEndElement();
			
			if (n.getLocalDefNames() != null) {
				writer.writeStartElement(GRAPHML_DATA); {
					writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_LOCALDEF);
					writer.writeCharacters(Arrays.toString(n.getLocalDefNames()));
				} writer.writeEndElement();
			}
			
			if (n.getLocalUseNames() != null) {
				writer.writeStartElement(GRAPHML_DATA); {
					writer.writeAttribute(GRAPHML_KEY, JOANA_NODE_LOCALUSE);
					writer.writeCharacters(Arrays.toString(n.getLocalUseNames()));
				} writer.writeEndElement();
			}
			
		} writer.writeEndElement();
	}
	
	private static void writeEdge(SDGEdge e, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement(GRAPHML_EDGE); {
			writer.writeAttribute(GRAPHML_SOURCE, Integer.toString(e.getSource().getId()));
			writer.writeAttribute("target", Integer.toString(e.getTarget().getId()));
			
			writer.writeStartElement(GRAPHML_DATA); {
				writer.writeAttribute(GRAPHML_KEY, JOANA_EDGE_KIND);
				writer.writeCharacters(e.getKind().toString());
			} writer.writeEndElement();
			
			if (OUTPUT_EDGELABELS) { 
				writer.writeStartElement(GRAPHML_DATA); {
					writer.writeAttribute(GRAPHML_KEY, JOANA_EDGE_LABEL);
					writer.writeCharacters(e.getLabel());
				} writer.writeEndElement();
			}
			
		} writer.writeEndElement();
	}
	
	private static void writeKeyDeclarations(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_KIND);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_KIND);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_SOURCE);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_SOURCE);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_PROC);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_PROC);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_OPERATION);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_OPERATION);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_LABEL);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_LABEL);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_BC_NAME);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_BC_NAME);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_BC_INDEX);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_BC_INDEX);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_SR);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_SR);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_SC);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_SC);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_ER);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_ER);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_EC);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_EC);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_INT);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_LOCALDEF);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_LOCALDEF);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_NODE_LOCALUSE);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_NODE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_NODE_LOCALUSE);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}

		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_EDGE_KIND);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_EDGE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_EDGE_KIND);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
		
		writer.writeEmptyElement(GRAPHML_KEY); {
			writer.writeAttribute(GRAPHML_ID, JOANA_EDGE_LABEL);
			writer.writeAttribute(GRAPHML_FOR, GRAPHML_EDGE);
			writer.writeAttribute(GRAPHML_ATTR_NAME, JOANA_EDGE_LABEL);
			writer.writeAttribute(GRAPHML_ATTR_TYPE, GRAPHML_STRING);
		}
	}
	
	private static void writeStartRoot(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartDocument(ENCODING, "1.0");
		writer.setPrefix("gr", GRAPHML_XMLNS);
		writer.setDefaultNamespace(GRAPHML_XMLNS);
		
		writer.writeStartElement(GRAPHML_XMLNS, GRAPHML_GRAPHML); {
			writer.writeDefaultNamespace(GRAPHML_XMLNS);
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
		}
	}
	
	private static void writeEndRoot(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEndElement();
	}

	/**
	 * Write a GraphML representation of the the given {@link SDG} into the the given {@link OutputStream}.
	 * The written graph is non-hierarchical.
	 * 
	 * @see <a href="http://graphml.graphdrawing.org/">http://graphml.graphdrawing.org/</a>
	 * @param sdg
	 * @param out
	 * @throws XMLStreamException
	 */
	public static void convert(SDG sdg, OutputStream out) throws XMLStreamException {
		XMLOutputFactory output = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = new IndentingXMLStreamWriter(output.createXMLStreamWriter(out, ENCODING));

		writeStartRoot(writer);
		writeKeyDeclarations(writer);
		
		writer.writeStartElement(GRAPHML_GRAPH); {
			writer.writeAttribute(GRAPHML_ID, "G");
			writer.writeAttribute(GRAPHML_EDGEDEFAULT, GRAPHML_DIRECTED);
			
			for (SDGNode n : sdg.vertexSet()) {
				writeNode(n, writer);
			}
			
			for (SDGEdge e : sdg.edgeSet()) {
				writeEdge(e, writer);
			}
		} writer.writeEndElement();
		
		writeEndRoot(writer);
		writer.flush();
	}
	
	/**
	 * Write a GraphML representation of the the given {@link SDG} into the the given {@link OutputStream}.
	 * The written graph is hierarchical: Nodes are grouped by the procedure they belong to.
	 * 
	 * @see <a href="http://graphml.graphdrawing.org/">http://graphml.graphdrawing.org/</a>
	 * @param sdg
	 * @param out
	 * @throws XMLStreamException
	 */
	
	public static void convertHierachical(SDG sdg, OutputStream out) throws XMLStreamException {
		XMLOutputFactory output = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = new IndentingXMLStreamWriter(output.createXMLStreamWriter(out, ENCODING));

		writeStartRoot(writer);
		writeKeyDeclarations(writer);
		
		writer.writeStartElement(GRAPHML_GRAPH); {
			writer.writeAttribute(GRAPHML_ID, "callgraph");
			writer.writeAttribute(GRAPHML_EDGEDEFAULT, GRAPHML_DIRECTED);
			
			for (Entry<SDGNode, Set<SDGNode>> entry : sdg.sortByProcedures().entrySet()) {
				final SDGNode entryNode = entry.getKey();
				final Set<SDGNode> nodes = entry.getValue();
				writer.writeStartElement(GRAPHML_NODE); {
					// TODO: possibly find a nicer naming scheme?
					writer.writeAttribute(GRAPHML_ID, "proc" + Integer.toString(entryNode.getId()));
					
					writer.writeStartElement(GRAPHML_GRAPH); {
						writer.writeAttribute(GRAPHML_ID, "proc" + Integer.toString(entryNode.getId()) + ":");
						writer.writeAttribute(GRAPHML_EDGEDEFAULT, GRAPHML_DIRECTED);
						
						for (SDGNode n : nodes) {
							writeNode(n, writer);
						}
						
					} writer.writeEndElement();
				} writer.writeEndElement();
			}
			
			for (SDGEdge e : sdg.edgeSet()) {
				writeEdge(e, writer);
			}
		} writer.writeEndElement();
		
		writeEndRoot(writer);
		writer.flush();
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.io.graphml;

import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import javanet.staxutils.IndentingXMLStreamWriter;


/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class SDG2GraphML {
	private static final boolean OUTPUT_EDGELABELS=false;
	private static final String ENCODING = "UTF-8";
	
	private static void writeNode(SDGNode n, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("node"); {
			writer.writeAttribute("id", Integer.toString(n.getId()));
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeKind");
				writer.writeCharacters(n.getKind().toString());
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeSource");
				writer.writeCharacters(n.getSource());
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeProc");
				writer.writeCharacters(Integer.toString(n.getProc()));
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeOperation");
				writer.writeCharacters(n.getOperation().toString());
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeLabel");
				writer.writeCharacters(n.getLabel());
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeBcName");
				writer.writeCharacters(n.getBytecodeName());
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeBCIndex");
				writer.writeCharacters(Integer.toString(n.getBytecodeIndex()));
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeBCIndex");
				writer.writeCharacters(Integer.toString(n.getBytecodeIndex()));
			} writer.writeEndElement();
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeSr");
				writer.writeCharacters(Integer.toString(n.getSr()));
			} writer.writeEndElement();
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeSc");
				writer.writeCharacters(Integer.toString(n.getSc()));
			} writer.writeEndElement();
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeEr");
				writer.writeCharacters(Integer.toString(n.getEr()));
			} writer.writeEndElement();
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "nodeEc");
				writer.writeCharacters(Integer.toString(n.getEc()));
			} writer.writeEndElement();
			
		} writer.writeEndElement();
	}
	
	private static void writeEdge(SDGEdge e, XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("edge"); {
			writer.writeAttribute("source", Integer.toString(e.getSource().getId()));
			writer.writeAttribute("target", Integer.toString(e.getTarget().getId()));
			
			writer.writeStartElement("data"); {
				writer.writeAttribute("key", "edgeKind");
				writer.writeCharacters(e.getKind().toString());
			} writer.writeEndElement();
			
			if (OUTPUT_EDGELABELS) { 
				writer.writeStartElement("data"); {
					writer.writeAttribute("key", "edgeLabel");
					writer.writeCharacters(e.getLabel());
				} writer.writeEndElement();
			}
			
		} writer.writeEndElement();
	}
	
	private static void writeKeyDeclarations(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeKind");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeKind");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeSource");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeSource");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeProc");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeProc");
			writer.writeAttribute("attr.type", "int");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeOperation");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeOperation");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeLabel");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeLabel");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeBcName");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeBcName");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeBCIndex");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeBCIndex");
			writer.writeAttribute("attr.type", "int");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeSr");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeSr");
			writer.writeAttribute("attr.type", "int");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeSc");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeSc");
			writer.writeAttribute("attr.type", "int");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeEr");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeEr");
			writer.writeAttribute("attr.type", "int");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "nodeEc");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "nodeEc");
			writer.writeAttribute("attr.type", "int");
		}
		
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "edgeKind");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "edgeKind");
			writer.writeAttribute("attr.type", "string");
		}
		
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "edgeKind");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "edgeKind");
			writer.writeAttribute("attr.type", "string");
		}
		writer.writeEmptyElement("key"); {
			writer.writeAttribute("id", "edgeLabel");
			writer.writeAttribute("for", "node");
			writer.writeAttribute("attr.name", "edgeLabel");
			writer.writeAttribute("attr.type", "string");
		}
	}
	
	private static void writeStartRoot(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartDocument(ENCODING, "1.0");
		writer.setPrefix("gr", "http://graphml.graphdrawing.org/xmlns");
		writer.setDefaultNamespace("http://graphml.graphdrawing.org/xmlns");
		
		writer.writeStartElement("http://graphml.graphdrawing.org/xmlns", "graphml"); {
			writer.writeDefaultNamespace("http://graphml.graphdrawing.org/xmlns");
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");
		}
	}
	
	private static void writeEndRoot(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEndElement();
	}

	public static void convert(SDG sdg, OutputStream out) throws XMLStreamException {
		XMLOutputFactory output = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = new IndentingXMLStreamWriter(output.createXMLStreamWriter(out, ENCODING));

		writeStartRoot(writer);
		writeKeyDeclarations(writer);
		
		writer.writeStartElement("graph"); {
			writer.writeAttribute("id", "G");
			writer.writeAttribute("edgedefault", "directed");
			
			for (SDGNode n : sdg.vertexSet()) {
				writeNode(n, writer);
			}
			
			for (SDGEdge e : sdg.edgeSet()) {
				writeEdge(e, writer);
			}
		}
		
		writeEndRoot(writer);
		writer.flush();
	}
	
	public static void convertHierachical(SDG sdg, OutputStream out) throws XMLStreamException {
		XMLOutputFactory output = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = new IndentingXMLStreamWriter(output.createXMLStreamWriter(out, ENCODING));

		writeStartRoot(writer);
		writeKeyDeclarations(writer);
		
		writer.writeStartElement("graph"); {
			writer.writeAttribute("id", "callgraph");
			writer.writeAttribute("edgedefault", "directed");
			
			for (Entry<SDGNode, Set<SDGNode>> entry : sdg.sortByProcedures().entrySet()) {
				final SDGNode entryNode = entry.getKey();
				final Set<SDGNode> nodes = entry.getValue();
				writer.writeStartElement("node"); {
					// TODO: possibly find a nicer naming scheme?
					writer.writeAttribute("id", "proc" + Integer.toString(entryNode.getId()));
					
					writer.writeStartElement("graph"); {
						writer.writeAttribute("id", "proc" + Integer.toString(entryNode.getId()) + ":");
						writer.writeAttribute("edgedefault", "directed");
						
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
	
	public static void main(String[] args) throws XMLStreamException {
		SDG2GraphML.convert(null, System.out);
	}

}

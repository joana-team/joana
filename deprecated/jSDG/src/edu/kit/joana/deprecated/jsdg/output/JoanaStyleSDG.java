/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.output;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;

import edu.kit.joana.deprecated.jsdg.nontermination.NonTerminationSensitive;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryComputationOptimizer;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.IPDGNodeVisitor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.wala.util.VerboseProgressMonitor;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class JoanaStyleSDG {

	// system specific newline character
	private final static String NL = System.getProperty("line.separator");

	private JoanaStyleSDG() {}

	private static class NodeOutputVisitor implements IPDGNodeVisitor {
		private final PrintWriter out;
		private final SDG sdg;
		private final boolean addControlFlow;

		public NodeOutputVisitor(PrintWriter out, SDG sdg, boolean addControlFlow) {
			this.out = out;
			this.sdg = sdg;
			this.addControlFlow = addControlFlow;
		}

		private void printEdges(JDependencyGraph graph, AbstractPDGNode from, AbstractPDGNode to) {
			Set<? extends EdgeType> edges = graph.getEdgeLabels(from, to);
			for (EdgeType type : edges) {
				switch(type) {
				case CD_TRUE:
					out.print("CD " + to.getUniqueId() + ":\"true\";" + NL);
					break;
				case CD_FALSE:
					out.print("CD " + to.getUniqueId() + ":\"false\";" + NL);
					break;
				case FORK_OUT: // create interference edge
                    out.print("ID " + to.getUniqueId() + ";" + NL);
                    break;
				case CD_EX:
					out.print("UN " + to.getUniqueId() + ";" + NL);
					break;
				case CF: // Control flow - add only if addControlFLow is set
					if (!addControlFlow) {
						break;
					}
				default:
					out.print(type.name() + " " + to.getUniqueId() + ";" + NL);
				}
			}
		}

		/**
		 * Print the list of dependencies a node is source of to the output
		 * print stream. A node may have outgoing dependencies withing its own
		 * pdg graph or within the sdg.
		 * At first all dependencies going out from the nodes pdg are printed.
		 * Secondly all dependencies from the sdg are printed - iff there are any.
		 * @param node
		 */
		private void printDependencies(AbstractPDGNode node) {
			if (node.getPdgId() != sdg.getId()) {
				PDG pdg = sdg.getPdgForId(node.getPdgId());
				if (pdg.containsNode(node)) {
					Iterator<? extends AbstractPDGNode> it = pdg.getSuccNodes(node);
					while (it.hasNext()) {
						AbstractPDGNode toNode = it.next();
						printEdges(pdg, node, toNode);
					}
				} else {
					Log.warn("Homeless node " + node + " in " + pdg);
				}
			}

			if (sdg.containsNode(node)) {
				Iterator<? extends AbstractPDGNode> it = sdg.getSuccNodes(node);
				while (it.hasNext()) {
					AbstractPDGNode toNode = it.next();
					printEdges(sdg, node, toNode);
				}
			}
		}

		private void printLocation(AbstractPDGNode node) {
			SourceLocation sloc = sdg.getLocation(node);
			if (sloc != null) {
				out.print("S ");
				out.print(sloc);
				out.print(";" + NL);
			} else if (node.isParameterNode()) {
				AbstractParameterNode param = (AbstractParameterNode) node;
				final int id = node.getPdgId();
				if (id == sdg.getId()) {
					// root sdg has no matching call location
					return;
				}

				PDG pdg = sdg.getPdgForId(id);
				if (pdg == null) {
					Log.warn("No pdg with id " + id);
					return;
				}

				if (param.isActual()) {
					CallNode call = pdg.getCall(param);
					if (call != null) {
						printLocation(call);
					}
				} else {
					printLocation(pdg.getRoot());
				}
			}

			BytecodeLocation bloc = sdg.getBytecodeLocation(node);
			if (bloc != null) {
				out.print("B ");
				out.print(bloc);
				out.print(";" + NL);
			}
		}

		public void visitParameter(AbstractParameterNode node) {
			if (node.isActual()) {
				out.print(node.isIn() ? "ACTI" : "ACTO");
				out.print(" " + node.getUniqueId() + " {" + NL);
				out.print("O " + (node.isIn() ? "act-in" : "act-out") + ";" + NL);
			} else if (node.isException()) {
				out.print("FRMO");
				out.print(" " + node.getUniqueId() + " {" + NL);
				out.print("O form-out;" + NL);
			}  else if (node.isExit()) {
				out.print("EXIT");
				out.print(" " + node.getUniqueId() + " {" + NL);
				out.print("O exit;" + NL);
			} else {
				out.print(node.isIn() ? "FRMI" : "FRMO");
				out.print(" " + node.getUniqueId() + " {" + NL);
				out.print("O " + (node.isIn() ? "form-in" : "form-out") + ";" + NL);
			}

			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitCall(CallNode node) {
			out.print("CALL");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O call;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitEntry(EntryNode node) {
			out.print("ENTR");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O entry;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);

			// print thread ids
			if (node.getPdgId() != sdg.getId()) {
				PDG pdg = sdg.getPdgForId(node.getPdgId());
				IntSet threadIds = pdg.getThreadIds();
				final StringBuilder threadList = new StringBuilder();
				threadIds.foreach(new IntSetAction() {

					public void act(int x) {
						threadList.append(x);
						threadList.append(',');
					}

				});

				if (!threadIds.isEmpty()) {
					out.print("Z ");
					// delete last ','
					threadList.deleteCharAt(threadList.length() - 1);
					out.print(threadList.toString());
					out.print(";" + NL);
				}

				// print class loader info
				IMethod im = pdg.getMethod();

				if (im != null) {
					IClass cls = im.getDeclaringClass();

					if (cls != null) {
						final String clsLoader = cls.getClassLoader().toString();
						out.print("C \"" + clsLoader + "\";" + NL);
					}
				}
			}
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitExpression(ExpressionNode node) {
			out.print("EXPR");
			out.print(" " + node.getUniqueId() + " {" + NL);

			if (node.isGet()) {
				out.print("O " + SDGNode.Operation.REFERENCE + ";" + NL);
			} else if (node.isSet()) {
				out.print("O " + SDGNode.Operation.MODIFY + ";" + NL);
			} else {
				out.print("O " + SDGNode.Operation.ASSIGN + ";" + NL);
			}

			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitCatch(CatchNode node) {
			out.print("EXPR");
			out.print(" " + node.getUniqueId() + " {" + NL);
			// compound or jump -> look in old sdg code for possibilities
			out.print("O assign;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitNormal(NormalNode node) {
			out.print("NORM");
			out.print(" " + node.getUniqueId() + " {" + NL);
			// compound or jump -> look in old sdg code for possibilities
			out.print("O compound;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitPredicate(PredicateNode node) {
			out.print("PRED");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O IF;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitSync(SyncNode node) {
			out.print("SYNC");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O monitor;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitConstPhiValue(ConstantPhiValueNode node) {
			out.print("EXPR");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O assign;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}

		public void visitPhiValue(PhiValueNode node) {
			out.print("EXPR");
			out.print(" " + node.getUniqueId() + " {" + NL);
			out.print("O assign;" + NL);
			if (node.getLabel() != null) {
				out.print("V \"" + node.getLabel() + "\";" + NL);
			}
			out.print("P " + node.getPdgId() + ";" + NL);
			printLocation(node);

			printDependencies(node);

			out.print("}" + NL);
		}
	}

	private static class NodeConverterVisitor implements IPDGNodeVisitor {
        private final edu.kit.joana.ifc.sdg.graph.SDG g;
        private final SDG sdg;
        private final Set<CallNode> mayNotTerminate;
        private final Set<AbstractPDGNode> toInline;

        public NodeConverterVisitor(edu.kit.joana.ifc.sdg.graph.SDG g, SDG sdg, Set<CallNode> mayNotTerminate,
        		Set<AbstractPDGNode> toInline) {
            this.g = g;
            this.sdg = sdg;
            this.mayNotTerminate = mayNotTerminate;
            this.toInline = toInline;
        }

        private Tuple sourceInfo(AbstractPDGNode node) {
            Tuple t = new Tuple();

            SourceLocation sloc = sdg.getLocation(node);
            if (sloc != null) {
                t.source = sloc.getSourceFile();
                t.sr = sloc.getStartRow();
                t.sc = sloc.getStartColumn();
                t.er = sloc.getEndRow();
                t.ec = sloc.getEndColumn();
            } else if (node.isParameterNode()) {
				AbstractParameterNode param = (AbstractParameterNode) node;
				final int id = node.getPdgId();
				if (id == sdg.getId()) {
					// root sdg has no matching call location
					return t;
				}

				PDG pdg = sdg.getPdgForId(id);
				if (pdg == null) {
					Log.warn("No pdg with id " + id);
					return t;
				}

				if (param.isActual()) {
					CallNode call = pdg.getCall(param);
					if (call != null) {
						t = sourceInfo(call);
					}
				} else {
					t = sourceInfo(pdg.getRoot());
				}
			}

            BytecodeLocation bcloc = sdg.getBytecodeLocation(node);
            if (bcloc != null) {
            	t.bcMethod = bcloc.bcMethod;
            	t.bcIndex = bcloc.bcIndex;
            }

            return t;
        }

        public void visitParameter(AbstractParameterNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = null;
            if (node.isActual() && node.isIn()) {
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ACTUAL_IN;

            } else if (node.isActual() && node.isOut()) {
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ACTUAL_OUT;

            } else if (node.isException()) {
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.FORMAL_OUT;

            }  else if (node.isExit()) {
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.EXIT;

            } else if (node.isIn()){
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.FORMAL_IN;

            } else if (node.isOut()){
                op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.FORMAL_OUT;
            }

            if (toInline != null && toInline.contains(node)) {
            	op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.COMPOUND;
            }

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitCall(CallNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.CALL;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            if (mayNotTerminate != null && mayNotTerminate.contains(node)) {
            	n.setMayBeNonTerminating(true);
            }

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitEntry(EntryNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ENTRY;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            // print thread ids
            if (node.getPdgId() != sdg.getId()) {
                PDG pdg = sdg.getPdgForId(node.getPdgId());
                IntSet threadIds = pdg.getThreadIds();
                int[] array = new int[threadIds.size()];
                IntIterator ii = threadIds.intIterator();
                int index = 0;

                while (ii.hasNext()) {
                    int r = ii.next();
                    array[index] = r;
                    index++;
                }

                n.setThreadNumbers(array);

				// set class loader info
				IMethod im = pdg.getMethod();

				if (im != null) {
					IClass cls = im.getDeclaringClass();

					if (cls != null) {
						final String clsLoader = cls.getClassLoader().toString();
						n.setClassLoader(clsLoader);
					}
				}
            }

            final PDG pdg = sdg.getPdgForId(node.getPdgId());
            if (pdg != null && pdg.getCallGraphNode() != null) {
	            final int cgNumber = sdg.getCallGraph().getNumber(pdg.getCallGraphNode());
	            n.tmp = cgNumber;
            }

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitExpression(ExpressionNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op;
			if (node.isGet()) {
				op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.REFERENCE;
			} else if (node.isSet()) {
				op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.MODIFY;
			} else {
				op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ASSIGN;
			}

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitCatch(CatchNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ASSIGN;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitNormal(NormalNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.COMPOUND;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitPredicate(PredicateNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.IF;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitSync(SyncNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.MONITOR;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

        public void visitConstPhiValue(ConstantPhiValueNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ASSIGN;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
        }

		public void visitPhiValue(PhiValueNode node) {
            int id = node.getUniqueId();
            int proc = node.getPdgId();
            String value = node.getLabel() != null ? node.getLabel() : "";
            String type = "";

            Operation op = edu.kit.joana.ifc.sdg.graph.SDGNode.Operation.ASSIGN;

            Tuple t = sourceInfo(node);
            edu.kit.joana.ifc.sdg.graph.SDGNode n = new edu.kit.joana.ifc.sdg.graph.SDGNode(id, op, value, proc,
                    type, t.source, t.sr, t.sc, t.er, t.ec, t.bcMethod, t.bcIndex);

            g.addVertex(n);
            nodeMap.put(node, n);
		}
    }

	private static class Tuple {
	    String source = null;
        int sr = 0;
        int sc = 0;
        int er = 0;
        int ec = 0;

        String bcMethod = null;
        int bcIndex = -1;
	}

	private static class NodeEdgesVisitor implements IPDGNodeVisitor {
        private final edu.kit.joana.ifc.sdg.graph.SDG g;
        private final SDG sdg;
        private final boolean addControlFlow;
        // inlining to speed up summary edge computation for recursive methods
        private final Set<edu.kit.joana.ifc.sdg.graph.SDGNode> toInline;


        public NodeEdgesVisitor(edu.kit.joana.ifc.sdg.graph.SDG g, SDG sdg, boolean addControlFlow, Set<edu.kit.joana.ifc.sdg.graph.SDGNode> toInline) {
            this.g = g;
            this.sdg = sdg;
            this.addControlFlow = addControlFlow;
            this.toInline = toInline;
        }

        private void printEdges(Set<? extends EdgeType> edges, edu.kit.joana.ifc.sdg.graph.SDGNode source, edu.kit.joana.ifc.sdg.graph.SDGNode target) {
            for (EdgeType type : edges) {
                switch(type) {
                case CD_TRUE:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_COND));
                    break;
                case CD_FALSE:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_COND));
                    break;
                case CD_EX:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND));
                    break; //TODO this break has not been here before -> this seemed to be broken, but it has to be checked
                case CF: // Control flow - add only if addControlFLow is set
                    if (addControlFlow) {
                        g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW));
                    }
                    break;
                case CL:
                	if (toInline != null && toInline.contains(source)) {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND));
                		if (addControlFlow) {
                    		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW));
                		}
                	} else {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CALL));
                	}
                    break;
                case SU:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.SUMMARY));
                    break;
                case DD:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP));
                    break;
                case DH:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_HEAP));
                    break;
                case CE:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_EXPR));
                    break;
                case HE:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP));
                    break;
                case UN:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND));
                    break;
                case VD:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP_EXPR_VALUE));
                    break;
                case PS:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_STRUCTURE));
                    break;
                case PI:
                	if (toInline != null && toInline.contains(source)) {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP));
                	} else {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_IN));
                	}
                    break;
                case PO:
                	if (toInline != null && toInline.contains(target)) {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP));
                	} else {
                		g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_OUT));
                	}
                    break;
                case RD:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE));
                    break;
                case CC:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_CALL));
                    break;
                case ID:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE));
                    break;
                case IW:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE_WRITE));
                    break;
                case FORK:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.FORK));
                    break;
                case FORK_IN:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.FORK_IN));
                    break;
                case FORK_OUT: // create interference edge
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE));
                    break;
                case NTSCD:
                    g.addEdge(new edu.kit.joana.ifc.sdg.graph.SDGEdge(source, target, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.NTSCD));
                    break;
                default:
                    throw new RuntimeException("unknown edge type: "+type);
                }
            }
        }

        /**
         * Print the list of dependencies a node is source of to the output
         * print stream. A node may have outgoing dependencies withing its own
         * pdg graph or within the sdg.
         * At first all dependencies going out from the nodes pdg are printed.
         * Secondly all dependencies from the sdg are printed - iff there are any.
         * @param node
         */
        private void printDependencies(AbstractPDGNode node) {
            edu.kit.joana.ifc.sdg.graph.SDGNode source = nodeMap.get(node);
            if (node.getPdgId() != sdg.getId()) {
                PDG pdg = sdg.getPdgForId(node.getPdgId());
                if (pdg.containsNode(node)) {
                    Iterator<? extends AbstractPDGNode> it = pdg.getSuccNodes(node);
                    while (it.hasNext()) {
                        AbstractPDGNode toNode = it.next();
                        edu.kit.joana.ifc.sdg.graph.SDGNode target = nodeMap.get(toNode);
                        if (target != null) {
                        	printEdges(pdg.getEdgeLabels(node, toNode), source, target);
                        } else {
                        	System.out.println("No node in map for " + toNode);
                        }
                    }
                } else {
                    Log.warn("Homeless node " + node + " in " + pdg);
                }
            }

            if (sdg.containsNode(node)) {
                Iterator<? extends AbstractPDGNode> it = sdg.getSuccNodes(node);
                while (it.hasNext()) {
                    AbstractPDGNode toNode = it.next();
                    edu.kit.joana.ifc.sdg.graph.SDGNode target = nodeMap.get(toNode);
                    if (target != null) {
                    	printEdges(sdg.getEdgeLabels(node, toNode), source, target);
                    } else {
                    	System.out.println("No node in map for " + toNode);
                    }
                }
            }
        }

        public void visitParameter(AbstractParameterNode node) {
            printDependencies(node);
        }

        public void visitCall(CallNode node) {
            printDependencies(node);
        }

        public void visitEntry(EntryNode node) {
            printDependencies(node);
        }

        public void visitExpression(ExpressionNode node) {
            printDependencies(node);
        }

        public void visitCatch(CatchNode node) {
            printDependencies(node);
        }

        public void visitNormal(NormalNode node) {
            printDependencies(node);
        }

        public void visitPredicate(PredicateNode node) {
            printDependencies(node);
        }

        public void visitSync(SyncNode node) {
            printDependencies(node);
        }

        public void visitConstPhiValue(ConstantPhiValueNode node) {
            printDependencies(node);
        }

		public void visitPhiValue(PhiValueNode node) {
            printDependencies(node);
		}
    }

	private static void addAllNodesToArray(final JDependencyGraph graph, final BitSet nodesVisited,
			final Collection<AbstractPDGNode> nodes) {
		for (AbstractPDGNode node : graph) {
//			if (node.getPdgId() != graph.getId()) {
//				continue;
//			}

			if (!nodesVisited.get(node.getUniqueId())) {
				nodes.add(node);
				nodesVisited.set(node.getUniqueId());

				//XXX quick and dirty fix - is it needed anymore?
				if (!(graph instanceof SDG) && !(node instanceof EntryNode) && node.getPdgId() == graph.getId() && countControlDeps(node, graph) == 0) {
//					System.err.println("No control deps for: " + node + "(" + node.getUniqueId() + ")"+  " in " + graph + ": quick and dirty fix -> add dependency to entry node");
					graph.addControlDependency(graph.getRoot(), node, true);
				}
			}
		}
	}

	private final static int countControlDeps(AbstractPDGNode node, JDependencyGraph graph) {
		int sum = 0;
		sum += graph.getPredNodeCount(node, EdgeType.CD_EX);
		sum += graph.getPredNodeCount(node, EdgeType.CD_FALSE);
		sum += graph.getPredNodeCount(node, EdgeType.CD_TRUE);
		sum += graph.getPredNodeCount(node, EdgeType.CE);
		sum += graph.getPredNodeCount(node, EdgeType.CC);
		sum += graph.getPredNodeCount(node, EdgeType.UN);

		return sum;
	}

	private static AbstractPDGNode[] getAllNodes(final SDG sdg) {
		final BitSet nodesVisited = new BitSet();
		final ArrayList<AbstractPDGNode> nodes = new ArrayList<AbstractPDGNode>();

		// get all nodes of the sdg
		addAllNodesToArray(sdg, nodesVisited, nodes);

		// get all nodes of the pdgs
		for (final PDG pdg : sdg.getAllContainedPDGs()) {
			addAllNodesToArray(pdg, nodesVisited, nodes);
		}

		final AbstractPDGNode[] result = new AbstractPDGNode[nodes.size()];
		nodes.toArray(result);

		return result;
	}

	private static void addUtilityEdgesForNode(JDependencyGraph graph, AbstractPDGNode node) {
		Iterator<? extends AbstractPDGNode> succsTrue = graph.getSuccNodes(node, EdgeType.CD_TRUE);
		while (succsTrue.hasNext()) {
			AbstractPDGNode succ = succsTrue.next();
			graph.addUtilityEdge(node, succ);
		}

		Iterator<? extends AbstractPDGNode> succsFalse = graph.getSuccNodes(node, EdgeType.CD_FALSE);
		while (succsFalse.hasNext()) {
			AbstractPDGNode succ = succsFalse.next();
			graph.addUtilityEdge(node, succ);
		}
	}

	private static void addUtilityEdges(SDG sdg, AbstractPDGNode[] allNodes) {
		for (int i = 0; i < allNodes.length; i++) {
			AbstractPDGNode node = allNodes[i];
			if (node.getPdgId() != sdg.getId()) {
				PDG pdg = sdg.getPdgForId(node.getPdgId());
				if (!pdg.containsNode(node)) {
					Log.error("Node " + node + " not part of " + pdg + "  - but id is the same...");
				} else {
					addUtilityEdgesForNode(pdg, node);
				}
			}

			if (sdg.containsNode(node)) {
				addUtilityEdgesForNode(sdg, node);
			}
		}
	}

	/**
	 * @deprecated use createJoanaSDG() and write SDG afterwards to disc
	 */
	@SuppressWarnings("unused")
	private static void writeTo(SDG sdg, boolean addControlFlow, IProgressMonitor progress, PrintWriter out) throws CancelException {
		progress.beginTask("Writing SDG to joana style text representation", -1);

		progress.subTask("Sorting all nodes by their id");
		AbstractPDGNode allNodes[] = getAllNodes(sdg);
		progress.worked(1);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		Arrays.sort(allNodes, new Comparator<AbstractPDGNode>() {

			public int compare(AbstractPDGNode o1, AbstractPDGNode o2) {
				return o1.getUniqueId() - o2.getUniqueId();
			}});
		progress.worked(1);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}
		progress.done();

		progress.subTask("Building utility edges");
		addUtilityEdges(sdg, allNodes);
		progress.worked(1);
		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}
		progress.done();

		progress.subTask("Writing graphs to file");
		out.write("SDG \"" + Util.methodName(sdg.getMain()) + "\" {" + NL);
		NodeOutputVisitor visitor = new NodeOutputVisitor(out, sdg, addControlFlow);

		for (int i = 0; i < allNodes.length; i++) {
			AbstractPDGNode node = allNodes[i];

			assert (i == 0 || allNodes[i-1].getUniqueId() < allNodes[i].getUniqueId());

			node.accept(visitor);

			progress.worked(1);
			if (progress.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}
		}

		out.write("}" + NL);

		progress.done();
	}

	public static HashMap<AbstractPDGNode, edu.kit.joana.ifc.sdg.graph.SDGNode> nodeMap = new HashMap<AbstractPDGNode, edu.kit.joana.ifc.sdg.graph.SDGNode>();

	public static edu.kit.joana.ifc.sdg.graph.SDG createJoanaSDG(SDG sdg, boolean addControlFlow,
			boolean nonTermination, boolean optimizeSummary, IProgressMonitor progress)
	throws CancelException {
	    edu.kit.joana.ifc.sdg.graph.SDG g = new edu.kit.joana.ifc.sdg.graph.SDG();

	    Set<CallNode> mayNotTerminate = null;

	    if (nonTermination) {
	    	progress.beginTask("Compute interprocedural nontermination sensitive control dependencies", -1);
	    	mayNotTerminate = NonTerminationSensitive.run(sdg, progress);
	    	progress.done();
	    }

        progress.beginTask("Creating Joana-style SDG", -1);

        progress.subTask("Sorting all nodes by their id");
        AbstractPDGNode allNodes[] = getAllNodes(sdg);
        progress.worked(1);
        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        Arrays.sort(allNodes, new Comparator<AbstractPDGNode>() {

            public int compare(AbstractPDGNode o1, AbstractPDGNode o2) {
                return o1.getUniqueId() - o2.getUniqueId();
            }});
        progress.worked(1);
        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }
        progress.done();

        progress.subTask("Building utility edges");
        addUtilityEdges(sdg, allNodes);
        progress.worked(1);
        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }
        progress.done();

        Set<AbstractPDGNode> inlinePDGNode = null;
        if (optimizeSummary) {
	    	progress.beginTask("Optimizing recursive calls for summary edge computation", -1);
	    	inlinePDGNode = SummaryComputationOptimizer.run(sdg, progress);
	    	progress.done();
        }

        progress.subTask("Inserting " + allNodes.length + " nodes");

        g.setName(Util.methodName(sdg.getMain()));

        // add nodes
        IPDGNodeVisitor visitor = new NodeConverterVisitor(g, sdg, mayNotTerminate, inlinePDGNode);
        boolean console = (progress instanceof VerboseProgressMonitor);

        PDG curPDG = null;

        for (int i = 0; i < allNodes.length; i++) {
            AbstractPDGNode node = allNodes[i];

            assert (i == 0 || allNodes[i-1].getUniqueId() < allNodes[i].getUniqueId());

            node.accept(visitor);

            if (curPDG == null || curPDG.getId() != node.getPdgId()) {
            	curPDG = sdg.getPdgForId(node.getPdgId());
            }

            if (curPDG != null && !node.isParameterNode() && !(node instanceof EntryNode)) {
            	final SSAInstruction instr = curPDG.getInstructionForNode(node);
            	if (instr != null) {
            		final SDGNode sn = nodeMap.get(node);
            		sn.tmp = instr.iindex;
            	}
            }

            if (i % 100 == 0) {
            	if (!console) {
            		progress.done();
                	progress.subTask("Inserting " + (allNodes.length - i) + " nodes");
            	}

                progress.worked(1);
            }


            if (progress.isCanceled()) {
                throw CancelException.make("Operation aborted.");
            }
        }
        progress.done();

	    Set<edu.kit.joana.ifc.sdg.graph.SDGNode> toInline = null;

	    if (optimizeSummary) {
	    	toInline = HashSetFactory.make();
	    	for (AbstractPDGNode node : inlinePDGNode) {
	    		toInline.add(nodeMap.get(node));
	    	}
	    }

        progress.subTask("Inserting edges for " + allNodes.length + " nodes");
        // add edges
        visitor = new NodeEdgesVisitor(g, sdg, addControlFlow, toInline);

        for (int i = 0; i < allNodes.length; i++) {
            AbstractPDGNode node = allNodes[i];

            assert (i == 0 || allNodes[i-1].getUniqueId() < allNodes[i].getUniqueId());

            node.accept(visitor);

            if (i % 100 == 0) {
            	if (!console) {
            		progress.done();
            		progress.subTask("Inserting edges for " + (allNodes.length - i) + " nodes");
            	}

                progress.worked(1);
            }

            if (progress.isCanceled()) {
                throw CancelException.make("Operation aborted.");
            }
        }

        progress.done();
        progress.done();
        return g;
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.io.File;
import java.io.FileWriter;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.Value;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;

public class DotWriter {
    static final boolean WRITE_EXCEPTIONS = true;
    static final boolean WRITE_PI_PHI = false;

    static private SymbolTable symbols;
    static private FlowGraph graph;
    static private FlowAnalisys flow;

    static void setFlowAnalizer(FlowAnalisys flow) {
        DotWriter.flow = flow;
    }

    static void setSymbols(SymbolTable symbols) {
        DotWriter.symbols = symbols;
    }

    static void write(FlowGraph graph, String dotFile) {
        StringBuffer buf = new StringBuffer();
        buf.append("digraph \"DirectedGraph\" {\n");
        buf.append("node [color=blue, shape=box, fontcolor=black]\n");
        buf.append("edge [color=black, fontcolor=black];\n");
        buf.append("\n");

        DotWriter.graph = graph;
        writeNodes(graph.getLoops(), buf, 0);
        buf.append("\n");
        for (Node node : graph.getNodes()) {
            for (Node child : node.getChildren()) {
                if (!graph.containsNode(child.getIndex())) {
                    continue;
                }
                buf.append("Block_" + node.getBlock().getNumber() + " -> " +
                        "Block_" + child.getBlock().getNumber());
                if (node.hasCondClause()) {
                    //System.out.println(node.index + " " + node.condClause.yesNode.index + " " + node.condClause.noNode.index);
                    if (child.equals(node.getCondClause().yesNode)) {
                        buf.append(" [label=\"(y)\"]");
                    } else {
                        buf.append(" [label=\"(n)\"]");
                    }
                }
                buf.append("\n");
            }
            if (WRITE_EXCEPTIONS) {
                for (Entry<TypeReference, Node> entry : node.getThrown().entrySet()) {
                    buf.append("Block_" + node.getBlock().getNumber() + " -> " +
                            "Block_" + entry.getValue().getBlock().getNumber());
                    String exception = entry.getKey().getName().getClassName().toString();
                    if (exception.equals("NullPointerException")) exception = "<NPE>";
                    buf.append(" [label=\"" + exception +
                            "\" color=orange fontcolor=orange]\n");
                }
            }
        }
        buf.append("}\n");

        // now write the stuff down!
        try {
            File f = new File(dotFile);
            FileWriter fw = new FileWriter(f);
            fw.write(buf.toString());
            fw.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
    }

    static void writeNodes(LoopList cycleList, StringBuffer buf, int depth) {
        String tabulator = "";
        TreeSet<Integer> remainingNodes = new TreeSet<Integer>(cycleList.getNodeIndices());

        for (int i = 0; i < depth; i++) {
            tabulator += "\t";
        }
        buf.append(tabulator + "subgraph cluster_" + cycleList.hashCode() + " {\n");
        buf.append(tabulator + "\tcolor = gray\n");
        for (LoopList subgraph : cycleList.subLoops)  {
            writeNodes(subgraph, buf, depth + 1);
            remainingNodes.removeAll(subgraph.getNodeIndices());
        }
        for (Integer index : remainingNodes) {
            buf.append(tabulator + "\tBlock_" + index + " [label=\"" +
                    getLabel(graph.getNode(index))
                    + "\"]\n");
        }
        buf.append(tabulator + "}\n");
    }

    public static String getLabel(Node node) {
        String result = getLabel(node.getBlock());

        if (WRITE_PI_PHI) {
            if (flow.getPiDefinitions().containsKey(node)) {
                //System.out.println("Got here!");
                for (int val : flow.getPiDefinitions().get(node)) {
                    SSAPiInstruction pi = (SSAPiInstruction)flow.getDefUses().getDef(val);
                    result += "\\n[$" + pi.getDef() + " = pi(" + wrap(pi.getUse(0)) + ")]";
                }
            }
            if (flow.getPhiDefinitions().containsKey(node)) {
                for (int val : flow.getPhiDefinitions().get(node)) {
                    SSAPhiInstruction phi = (SSAPhiInstruction)flow.getDefUses().getDef(val);
                    result += "\\n[$" + phi.getDef() + " = phi(";
                    for (int i = 0; i < phi.getNumberOfUses() - 1; i++) {
                        result += wrap(phi.getUse(i)) + ", ";
                    }
                    result += wrap(phi.getUse(phi.getNumberOfUses() - 1)) + ")]";
                }
            }
        }
        return result;
    }

    public static String getLabel(IExplodedBasicBlock bb){
            if (bb == null) {
                return "invalid - null"; // this is an error!
            }
            String bbNum = "(" + bb.getNumber() + ") ";
            SSAInstruction istr = bb.getInstruction();
            int v1, v2, dest;
            if (istr instanceof SSAGetInstruction) {
                v1 = ((SSAGetInstruction)istr).getRef();
                if (istr.hasDef()) {
                    bbNum += wrap(istr.getDef()) + " = ";
                }
                String n = ((SSAGetInstruction)istr).getDeclaredField().getName().toString();
                return bbNum + wrap(v1) + "." + n;
            }
            if (istr instanceof SSAPutInstruction) {
                dest = ((SSAPutInstruction)istr).getRef();
                v2 = istr.getUse(1);
                String n = ((SSAPutInstruction)istr).getDeclaredField().getName().toString();
                return bbNum + wrap(dest) + "." + n + " = " + wrap(v2);
            }
            if (istr instanceof SSANewInstruction) {
                dest = istr.getDef();
                String n = ((SSANewInstruction)istr).getNewSite().getDeclaredType().getName().getClassName().toString();
                return bbNum + wrap(dest) + " = " + "New " +  n;
            }
            if (istr instanceof SSABinaryOpInstruction) {
                v1 = istr.getUse(0);
                v2 = istr.getUse(1);
                dest = ((SSABinaryOpInstruction)istr).getDef();
                return bbNum + wrap(dest) + " = " + ((SSABinaryOpInstruction)istr).getOperator()
                + "(" + wrap(v1) + ", " + wrap(v2) + ")";
            }
            if (istr instanceof SSAConditionalBranchInstruction) {
                v1 = istr.getUse(0);
                v2 = istr.getUse(1);
                return bbNum + "if " + ((SSAConditionalBranchInstruction)istr).getOperator()
                + "(" + wrap(v1) + ", " + wrap(v2) + ")";
            }
            if (istr instanceof SSAArrayLoadInstruction) {
                v1 = ((SSAArrayLoadInstruction)istr).getArrayRef();
                v2 = ((SSAArrayLoadInstruction)istr).getIndex();
                return bbNum + wrap(((SSAArrayLoadInstruction)istr).getDef()) +
                    " = " + wrap(v1) + "(" + wrap(v2) + ")";
            }
            if (istr instanceof SSAArrayStoreInstruction) {
                v1 = ((SSAArrayStoreInstruction)istr).getArrayRef();
                v2 = ((SSAArrayStoreInstruction)istr).getIndex();
                return bbNum + wrap(v1) + "(" + wrap(v2) + ") = "
                    + wrap(((SSAArrayStoreInstruction)istr).getDef()) ;
            }
            if (istr instanceof SSAInvokeInstruction) {
                if (istr.hasDef()) {
                    bbNum += wrap(istr.getDef()) + " = ";
                }
                String nv1;
                if (istr.getNumberOfUses() == 0) {
                    nv1 = "$?";
                } else {
                    nv1 = wrap(istr.getUse(0));
                }
                String n = ((SSAInvokeInstruction)istr).getDeclaredTarget().getName().toString();
                String params = "";
                for (int i = 1; i < ((SSAInvokeInstruction)istr).getNumberOfUses(); i++) {
                    params += wrap(((SSAInvokeInstruction)istr).getUse(i)) + ", ";
                }
                if (params.length() > 0) {
                    params = params.substring(0, params.length() - 2);
                }
                return bbNum + nv1 + "." + n + "(" + params + ")";
            }

            if (istr != null) {
                return bbNum + istr.toString();
            } else if (bb.isExitBlock()) {
                return bbNum + "EXIT";
            } else if (bb.isEntryBlock()) {
                return bbNum + "ENTRY";
            } else if (bb.isCatchBlock()) {
                return bbNum + (bb.getCatchInstruction() == null ?
                        "?" :
                        bb.getCatchInstruction().toString());
            } else {
                return bbNum + "nope";
            }
    }

    public static String wrap(int i) {
        if (i == -1 || i > symbols.getMaxValueNumber()) return "$?";
        Value v = symbols.getValue(i);
        if (v == null) return "$" + i;
        //if (v.isNullConstant()) return ("$" + i + "=null");
        //if (v.isStringConstant()) return ("$" + i + "='" + v.toString() + "'");
        //if (v instanceof ConstantValue) return "$" + i + "=" + v.toString();
        if (v.isNullConstant()) return "null";
        if (v.isStringConstant()) return "'" + v.toString() + "'";
        if (v instanceof ConstantValue) return v.toString();
        return "$" + i;
    }
}

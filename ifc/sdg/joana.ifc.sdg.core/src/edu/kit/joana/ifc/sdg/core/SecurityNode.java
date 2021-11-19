/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.SourceLocation;


/**
 * A security node is a {@link SDGNode node of an sdg} with two security levels: The <i>required</i> level and the
 * <i>provided</i> level. Both levels can either be {@link #UNDEFINED} or a string describing the security level. Security
 * nodes are used by the information flow control algorithms to propagate security levels. Basically, the required
 * level of a node controls the maximal security level which can enter the node through incoming edges and the provided level
 * controls the minimal security level which can leave the node through outgoing edges. The terms 'maximal' and 'minimal'
 * are given by the underlying security lattice which the security levels are assumed to be elements
 * of.
 * @author giffhorn, hammer
 */
public class SecurityNode extends SDGNode {

	/** undefined security level - equivalent to the bottom element in the underlying security lattice */
    public static final String UNDEFINED = null;//"edu.kit.joana.ifc.sdg.iflowundefined"; //de.naxan.NJSec.iflow.iflowundefined";
    public static final String ABSTRACT = "";// abstract security level for modular IFC

    private String required = SecurityNode.UNDEFINED;
    private String provided = SecurityNode.UNDEFINED;

    public SecurityNode(int id, Operation op, String value, int proc,
            String type, SourceLocation sourceLocation, String bcName, int bcIndex,
            String[] localDefNames, String[] localUseNames,
            String unresolvedCallTarget,
            int[] allocationSites,
            String clsLoader) {
        super(id, op, value, proc, type, sourceLocation, bcName, bcIndex, localDefNames, localUseNames, unresolvedCallTarget, allocationSites, clsLoader);
    }

    public SecurityNode(int kind, int id, Operation op, String value, int proc,
            String type, SourceLocation sourceLocation, String bcName, int bcIndex,
            String[] localDefNames, String[] localUseNames,
            String unresolvedCallTarget,
            int[] allocationSites,
            String clsLoader) {
        super(op.getKind(kind), id, op, value, proc, type, sourceLocation, bcName, bcIndex, localDefNames, localUseNames, unresolvedCallTarget, allocationSites, clsLoader);
    }

    public SecurityNode(SDGNode n) {
    	super(n.getId(), n.getOperation(), n.getLabel(), n.getProc(), n.getType(), n.getSourceLocation(), n.getBytecodeName(), n.getBytecodeIndex(), n.getLocalDefNames(), n.getLocalUseNames(), n.getUnresolvedCallTarget(), n.getAllocationSites(), n.getClassLoader());
    }

    public SecurityNode(Kind kind, int id, Operation op, int proc, String label){
        super(kind, id, op, proc, label);
    }

    /**
     * A constructor for synthetic nodes that are added to an existing SDG.
     * Receives only a kind, an ID and a procedure ID and sets the other attributes to default values.
     * @param kind  The node's kind.
     * @param id    The node's ID.
     * @param proc  The procedure ID.
     */
    public SecurityNode(Kind kind, int id, int proc, String label){
        this(kind, id, Operation.EMPTY, proc, label);
    }

    public SecurityNode clone() {
        SecurityNode ret = new SecurityNode(0, getId(), operation, getLabel(), getProc(), getType(), getSourceLocation(), getBytecodeName(), getBytecodeIndex(), getLocalDefNames(), getLocalUseNames(), getUnresolvedCallTarget(), getAllocationSites(), getClassLoader());
        ret.setRequired(getRequired());
        ret.setProvided(getProvided());
        return ret;
    }

    /**
     * Returns the required level of this security node.
     * @return the required level of this security node
     */
    public String getRequired() {
      return required;
    }


    /**
     * Sets the required level of this security node to the given value. If the given value is {@code null}, then
     * the required level of this security node becomes {@link #UNDEFINED}.
     * @param iflow new required level of this security node
     */
    public void setRequired(String iflow) {
      if (iflow == null) this.required = SecurityNode.UNDEFINED;
      else this.required = iflow.intern();
    }

    /**
     * Returns the provided level of this security node.
     * @return the provided level of this security node
     */
    public String getProvided() {
      return provided;
    }

    /**
     * Returns the required level, if this node is an information sink, the
     * provided level if this node is an information source and
     * {@link #SecurityLevel.UNDEFINED}, if neither of these two cases apply, i.e. if this
     * node is either a declassification node or has no annotations at all.
     *
     * @return the required level, if this node is an information sink, the
     *         provided level if this node is an information source and
     *         {@link #SecurityLevel.UNDEFINED}, if neither of these two cases apply
     */
    public String getLevel() {
        if (isInformationSource()) {
            return getProvided();
        } else if (isInformationSink()) {
            return getRequired();
        } else {
            return UNDEFINED;
        }
    }

    /**
     * Sets the provided level of this security node to the given value. If the given value is {@code null}, then
     * the required level of this security node becomes {@link #UNDEFINED}.
     * @param iflow new required level of this security node
     */
    public void setProvided(String iflow) {
      if (iflow == null) this.provided = SecurityNode.UNDEFINED;
      else this.provided = iflow.intern();
    }

    /**
     * Returns whether this node is an information source. This is the case if and only if the {@link #getRequired() required level} is
     * {@link #UNDEFINED} and the {@link #getProvided() provided level} is not.
     * @return {@code true} if this node is an information source, {@code false} if not
     */
    public boolean isInformationSource() {
        return (required == UNDEFINED && provided != UNDEFINED);
    }

    /**
     * Returns whether this node is an information sink. This is the case if and only if the {@link #getProvided() provided level} is
     * {@link #UNDEFINED} and the {@link #getRequired() required level} is not.
     * @return {@code true} if this node is an information source, {@code false} if not
     */
    public boolean isInformationSink() {
        return (required != UNDEFINED && provided == UNDEFINED);
    }

    /**
     * Returns whether this node is an information source or an information sink.
     * @return {@code true} if this node is an information source or an information sink, {@code false}
     * if this node is either unannotated or a declassification node.
     */
    public boolean isInformationEndpoint() {
        return isInformationSource() || isInformationSink();
    }

    /**
     * Returns whether this node is a declassification node. This is the case if and only if both the {@link #getRequired() required level} and
     * the {@link #getProvided() provided level} are not {@link #UNDEFINED}. Note that there is an additional condition, if a concrete security lattice
     * is given: The provided level has to be lower than or equal to the required level (where the term 'lower than or equal to' is given by the
     * lattice).
     * @return {@code true} if this node is a declassification node, {@code false} if not
     */
    public boolean isDeclassification() {
        return (provided != UNDEFINED && required != UNDEFINED);
    }

    /**
     * Returns whether this node is totally unannotated. This is the case if and only if both the {@link #getRequired() required level} and the
     * {@link #getProvided() provided level} are {@link #UNDEFINED}.
     * @return
     */
    public boolean isUnannotated() {
        return (provided == UNDEFINED && required == UNDEFINED);
    }


    /**
     * Factory for security nodes
     * @see NodeFactory
     */
    public static final class SecurityNodeFactory extends NodeFactory {


    	/**
    	 * @see edu.kit.joana.ifc.sdg.graph.SDGNode.NodeFactory#createNode(edu.kit.joana.ifc.sdg.graph.SDGNode.Operation, int, int, java.lang.String, int, java.lang.String, java.lang.String, int, int, int, int, java.lang.String, int)
    	 */
        public SDGNode createNode(Operation op, int kind, int id, String value, int proc,
                String type, SourceLocation sourceLocation,
                String bcName, int bcIndex,
                String[] localDefNames, String[] localUseNames,
                String unresolvedCallTarget,
                int[] allocationSites,
                String clsLoader) {
            return new SecurityNode(kind, id, op, value, proc, type,
                    sourceLocation, bcName, bcIndex, localDefNames, localUseNames, unresolvedCallTarget, allocationSites, clsLoader);
        }
    }
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice.impl;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.lattice.ILatticeOperations;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;


/**
 * This class implements a static lattice using the representation from "A
 * space-and-time effiecient coding algorithm for lattice computations" by
 * Ganguly, Mohan and Ranka.
 *
 * Using this bitfield encoding, glb and lub operations can pe performed in O(n)
 * where n is the number of elements in the graph.
 *
 * @param <ElementType>
 *            the type of the elements contained in the lattice.
 */
public class StaticLatticeBitset<ElementType> implements IStaticLattice<ElementType> {

	private static BigInteger first(final int c, final BigInteger n, final int nlen) {
		BigInteger i = twoPow(c).subtract(BigInteger.ONE).shiftLeft(nlen - c);
		return n.and(i);
	}

	private static int leftMost1(BigInteger i) {
		assert i.bitCount() > 0;
		int index = 0;
		for (int c = 0; c < i.bitCount(); index++) {
			if (i.testBit(index))
				c++;
		}
		return index - 1;
	}

	private static int secondLeftMost1(BigInteger i) {
		return leftMost1(i.clearBit(leftMost1(i)));
	}

	private final ElementType top;

	private final ElementType bottom;

	private final Map<ElementType, BigInteger> code;

	private transient Map<ElementType, Integer> codeLen;

	private final List<ElementType> elements;

	private BigInteger impureLS = ZERO;

	private final Map<Integer, ElementType> impurePtrs = new HashMap<Integer, ElementType>();

	private transient ILatticeOperations<ElementType> ops;

	/**
	 * Constructor. Builds the efficient representation of the lattice.
	 *
	 * @param elements
	 *            the elements in the lattice.
	 * @param ops
	 *            the <code>ILatticeOperations</code> object providing the
	 *            relations between the lattice's elements.
	 */
	public StaticLatticeBitset(Collection<ElementType> elements, ILatticeOperations<ElementType> ops) {
		assert elements != null;
		assert !elements.isEmpty();
		assert ops != null;

		this.ops = ops;
		ArrayList<ElementType> elementsCopy = new ArrayList<ElementType>(elements);
		this.elements = elementsCopy;

		Set<ElementType> virtualNodes = new HashSet<ElementType>();
		List<Collection<ElementType>> layers = buildLayers(this.elements, virtualNodes);
		bottom = layers.get(0).iterator().next();
		top = layers.get(layers.size() - 1).iterator().next();
		final int NODE_COUNT = elements.size();
		code = new HashMap<ElementType, BigInteger>(NODE_COUNT);
		codeLen = new HashMap<ElementType, Integer>(NODE_COUNT);

		pass1(layers);
 		pass2(layers);
 		for (ElementType virtual : virtualNodes) {
			code.remove(virtual);
		}
 		code.put(bottom, ZERO);
		Collections.sort(elementsCopy, new Comparator<ElementType>() {
			public int compare(ElementType arg0, ElementType arg1) {
				BigInteger code0 = code.get(arg0);
				BigInteger code1 = code.get(arg1);
				return code0.compareTo(code1);
			}
		});

		codeLen = null;
		if (StaticLatticeBitset.class.desiredAssertionStatus()) {
			EditableLatticeSimple<ElementType> l = (EditableLatticeSimple<ElementType>) ops;
			for (ElementType el1 : elements) {
				for (ElementType el2 : elements) {
					assert greatestLowerBound(el1, el2).equals(l.greatestLowerBound(el1, el2));
					assert greatestLowerBound(el2, el1).equals(l.greatestLowerBound(el2, el1)) : greatestLowerBound(el2, el2) + " " + l.greatestLowerBound(el2, el1);
					assert leastUpperBound(el1, el2).equals(l.leastUpperBound(el1, el2));
					assert leastUpperBound(el2, el1).equals(l.leastUpperBound(el2, el1)) : leastUpperBound(el2, el2) + " " + l.leastUpperBound(el2, el1);
				}
			}
		}
		this.ops = null;
	}

	private List<Collection<ElementType>> buildLayers(List<ElementType> elements, Set<ElementType> virtualNodes) {
		assert elements != null;

		List<Collection<ElementType>> layers = new ArrayList<Collection<ElementType>>();

		Collection<ElementType> touched = new HashSet<ElementType>();
		Set<ElementType> queue = new HashSet<ElementType>();
		queue.add(findBottom(elements));
		EditableLatticeSimple<ElementType> lat = new EditableLatticeSimple<ElementType>(elements);
		HashMap<ElementType, Integer> layer = new HashMap<ElementType, Integer>();
		int i = 0;
		while (!queue.isEmpty()) {
			List<ElementType> thisLayer = new ArrayList<ElementType>();
			Collection<ElementType> newElements = new HashSet<ElementType>();
			for (ElementType e : queue) {
				thisLayer.add(e);
				layer.put(e, i);
				touched.add(e);
				newElements.addAll(ops.getImmediatelyGreater(e));
			}
			queue.clear();
			i++;
			layers.add(thisLayer);
			for (ElementType e : newElements) {
				Collection<ElementType> children = ops.getImmediatelyLower(e);
				if (touched.containsAll(children)) {
					assert !touched.contains(e);
					queue.add(e);
					for (int l = 0; l < i; l++) {
						List<ElementType> elems = new ArrayList<ElementType>(layers.get(l));
						elems.retainAll(children);
						nextChild: for (ElementType child : elems) {
							int d = i - l;
							if (d > 1) {
								for (ElementType p : children) {
									if (layer.get(p) > l) {
										if (ops.collectAllLowerElements(p).contains(child))
											continue nextChild;
									}
								}
								for (int j = 1; j < d; j++) {
									@SuppressWarnings("unchecked")
									ElementType v = (ElementType) new Object();
									virtualNodes.add(v);
									lat.addElement(v);
									lat.setImmediatelyGreater(child, v);
									layers.get(l + j).add(v);
									child = v;
								}
							}
							lat.setImmediatelyGreater(child, e);
						}
					}
				}
			}
		}
		ops = lat;
		return layers;
	}

	private ElementType child(ElementType element) {
		assert element != null;

		return ops.getImmediatelyLower(element).iterator().next();
	}

	private Collection<ElementType> children(ElementType e) {
		assert e != null;

		return ops.getImmediatelyLower(e);
	}

	private Collection<ElementType> collectPureNodes(Collection<ElementType> layer) {
		assert layer != null;

		Collection<ElementType> pureNodes = new ArrayList<ElementType>();
		for (ElementType e : layer)
			if (isPure(e))
				pureNodes.add(e);
		return pureNodes;
	}

	private int computePrefixLen(int i, List<Collection<ElementType>> layers) {
		assert layers != null;

		// len <- number of impure nodes in layer i
		int len = countImpureNodes(layers.get(i));

		// If no pure node at layer i has a sibling, then
		if (noPureNodeHasSibling(layers.get(i), layers.get(i + 1))) {
			// len <- len + 2
			len += 2;
		}
		// else len <- len + max_pure_siblings(i)
		else {
			len += maxPureSiblings(i, layers);
		}
		return len;
	}

	private BigInteger concatenate(final BigInteger a, final BigInteger b) {
		assert a != null;
		assert b != null;

		return b.or(a);
	}

	private int countImpureNodes(Collection<ElementType> layer) {
		assert layer != null;

		int count = 0;
		for (ElementType e : layer)
			if (!isPure(e))
				count++;
		return count;
	}

	private int countPureChildren(ElementType e) {
		assert e != null;

		int count = 0;
		for (ElementType c : ops.getImmediatelyLower(e))
			if (isPure(c))
				count++;
		return count;
	}

	private ElementType findBottom(List<ElementType> elements) {
		assert elements != null;

		for (ElementType e : elements)
			if (ops.getImmediatelyLower(e).size() == 0)
				return e;
		assert false : "Not a lattice";
		return null;
	}

	private int getChildCount(ElementType e) {
		assert e != null;

		return ops.getImmediatelyLower(e).size();
	}

	public ElementType greatestLowerBound(ElementType a, ElementType b) throws NotInLatticeException {
		if (subsumes(a, b))
			return b;
		if (subsumes(b, a))
			return a;

		final BigInteger aAndBAndImpureLS = code.get(a).and(code.get(b)).and(impureLS);
		if (aAndBAndImpureLS.equals(ZERO))
			return bottom;

		ElementType impureNode = impurePtrs.get(leftMost1(aAndBAndImpureLS));
		if (!subsumes(code.get(impureNode), code.get(a).or(code.get(b))))
			return impureNode;

		if (aAndBAndImpureLS.bitCount() == 1)
			return bottom;

		return impurePtrs.get(secondLeftMost1(aAndBAndImpureLS));
	}

	private boolean hasSibling(ElementType n, Collection<ElementType> pLayer) {
		assert n != null;

		if (ops.getImmediatelyGreater(n).size() == 0)
			return false;
		for (ElementType p : ops.getImmediatelyGreater(n))
			if (pLayer.contains(p) && collectPureNodes(ops.getImmediatelyLower(p)).size() > 1)
				return true;
		return false;
	}

	private int indegree(ElementType e) {
		assert e != null;

		return ops.getImmediatelyGreater(e).size();
	}

	private boolean isPure(ElementType element) {
		assert element != null;

		return indegree(element) <= 1;
	}

	public ElementType leastUpperBound(ElementType s, ElementType t) throws NotInLatticeException {
		assert s != null;
		assert t != null;

		BigInteger lubCode = code.get(s).or(code.get(t)); // O(1)
		for (ElementType elm : elements)
//			// O(n)
			if (code.get(elm).or(lubCode).equals(code.get(elm))) // O(1)?
				return elm; // O(1)
		throw new AssertionError("something is wrong here");
	}

	private int maxPureSiblings(int j, List<Collection<ElementType>> layers) {
		assert layers != null;

		// noof_siblings <- 0
		int noOfSiblings = 0;
		// For each node, m, in layer j+1 do
		for (ElementType m : layers.get(j + 1)) {
			int pureChildCount = countPureChildren(m);
			// If noof_pure_children(m) > noof_siblings
			if (pureChildCount > noOfSiblings)
				// noof_siblings <- noof_pure_children(m)
				noOfSiblings = pureChildCount;
		}
		return noOfSiblings;
	}

	private void nameChildren(ElementType parent, int encodeStartIndex, int currCodeLen) {
		assert parent != null;
		assert encodeStartIndex > 0;
		assert currCodeLen >= 0;

		// j <- unik[layer]
		int j = encodeStartIndex;

		// For each node n in children(parent) do
		for (ElementType n : ops.getImmediatelyLower(parent)) {
			// If indegree(n) = 1 then
			if (indegree(n) == 1) {
				// n.code <- 2^j OR n.code
				// DIFFERENCE
				// code[nIndex] = code[nIndex].or(twoPow(j - 1));
				// n.codeLen <- currCodeLen
				// codeLen[nIndex] = currCodeLen;
				code.put(n, code.get(n).or(twoPow(j - 1)));
				codeLen.put(n, currCodeLen);
				// j <- j-1
				j -= 1;
			}
		}
	}

	private boolean noPureNodeHasSibling(Collection<ElementType> layer, Collection<ElementType> pLayer) {
		assert layer != null;
		Collection<ElementType> pureNodes = collectPureNodes(layer);
		for (ElementType e : pureNodes)
			if (hasSibling(e, pLayer))
				return false;
		return true;
	}

	private ElementType parent(ElementType element) {
		assert element != null;
		return ops.getImmediatelyGreater(element).iterator().next();
	}

	private void pass1(List<Collection<ElementType>> layers) {
		assert layers != null;

		final int MAX_LAYER = layers.size() - 1;

		// curr_code_len <- 0
		int currCodeLen = 0;

		// For layer_no <- 1 to max_layer do
		for (int layerNo = 1; layerNo <= MAX_LAYER; layerNo++) {
			// For each node, n, in the layer numbered layer_no do
			for (ElementType n : layers.get(layerNo)) {
				// If layer_no = 1 then n.code = 0
				if (layerNo == 1) {
					code.put(n, ZERO);
					codeLen.put(n, 0);
				} else {
					ElementType child = child(n);
					if (getChildCount(n) == 1 && isPure(child)) {
						// i <- left_most_1(child(n))
						int i = leftMost1(code.get(child));
						assert code.get(child).and(twoPow(i - 1)).equals(ZERO);
						code.put(n, code.get(child).or(twoPow(i - 1)));
						codeLen.put(n, codeLen.get(child));
					}
					// else n.code <- bitwise OR of the codes of all children of n
					else {
						BigInteger cod = ZERO;
						int codeLn = 0;
						for (ElementType c : children(n)) {
							cod = cod.or(code.get(c));
							codeLn = Math.max(codeLn, codeLen.get(c));
						}
						code.put(n, cod);
						codeLen.put(n, codeLn);
					}
				}
			}

			// If layer_no != max_layer then
			if (layerNo != MAX_LAYER) {
				// curr_code_len <- curr_code_len + compute_prefix_len(layer_no)
				currCodeLen += computePrefixLen(layerNo, layers);
				// i <- curr_code_len /* impure nodes will be first coded, starting from bit position i */
				int i = currCodeLen;
				// S <- emptyset /* pure nodes' parents at layerNo + 1, initally empty */
				Set<ElementType> s = new HashSet<ElementType>();
				// For each node n in the layer numbered layer_no, do
				for (ElementType n : layers.get(layerNo)) {
					// If indegree(n) > 1 then
					if (!isPure(n)) {
						// n.code <- 2^(i-1) OR n.code
						// n.code_len <- curr_code_len
						code.put(n, code.get(n).or(twoPow(i - 1)));
						codeLen.put(n, currCodeLen);
						// impure-ls <- 2^(i-1) OR impure-ls
						impureLS = impureLS.or(twoPow(i - 1));
						// impure-ptrs[i] <- n
						impurePtrs.put(i - 1, n);
						// i = i-1
						i -= 1;
					}
					// else If parent(n) is in the layer numbered layer_no + 1
					// then
					else if (layers.get(layerNo + 1).contains(parent(n))) {
						// S <- S UNION {parent(n)}
						s.add(parent(n));
					}
				}

				// For each node m in S, do
				for (ElementType m : s) {
					// name_children(m, layer_no)
					nameChildren(m, i, currCodeLen);
				}
			}
		}
	}

	private void pass2(List<Collection<ElementType>> layers) {
		assert layers != null;

		final int MAX_LAYER = layers.size() - 1;
		// For layer_no <- max_layer - 1 downto 0 do
		for (int layerNo = MAX_LAYER - 1; layerNo >= 1; layerNo--) {
			// For each node, n, in layer_no do
			for (ElementType n : layers.get(layerNo)) {
				// d <- parent(n).code_len - n.code_len
				ElementType parent = parent(n);
				int d = codeLen.get(parent) - codeLen.get(n);
				BigInteger prefix;
				if (indegree(n) > 1)
					prefix = ZERO;
				else
					prefix = first(d, code.get(parent), codeLen.get(parent));
				code.put(n, concatenate(prefix, code.get(n)));
				codeLen.put(n, codeLen.get(n) + d);
			}
		}
	}

	private boolean subsumes(BigInteger a, BigInteger b) {
		assert a != null;
		assert b != null;
		return a.or(b).equals(a);
	}

	private boolean subsumes(ElementType a, ElementType b) {
		return subsumes(code.get(a), code.get(b));
	}

	public ElementType getBottom() {
		return bottom;
	}

	public ElementType getTop() {
		return top;
	}

    public Collection<ElementType> getElements() {
        return elements;
    }

    private static BigInteger twoPow(int exp) {
    	return BigInteger.ZERO.setBit(exp);
    }
}

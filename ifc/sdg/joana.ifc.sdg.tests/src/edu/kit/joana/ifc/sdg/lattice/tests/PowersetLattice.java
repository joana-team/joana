/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice.tests;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;


public class PowersetLattice {
	private static final int LEN = 8;
	private static final double PROB = .2;
	static EditableLatticeSimple<String> name = new EditableLatticeSimple<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BitSet bs = new BitSet(8);
		bs.set(0, LEN);
		Set<BitSet> old = new HashSet<BitSet>();
		Set<BitSet> all = new HashSet<BitSet>();
		old.add(bs);
		Set<BitSet> news = new HashSet<BitSet>();
		while (!old.isEmpty()) {
			while (!old.isEmpty()) {
				Iterator<BitSet> iterator = old.iterator();
				bs = iterator.next();
				iterator.remove();
				for (int i = 0; i < LEN; i++) {
					BitSet nbs = (BitSet) bs.clone();
					nbs.clear(i);
					if (!all.contains(nbs))
						if (Math.random() <= PROB) {
							print(nbs, bs);
							if (!nbs.equals(bs))
								news.add(nbs);
						}
				}
			}
			old = news;
			all.addAll(news);
			news = new HashSet<BitSet>();
		}
		if (LatticeValidator.validateIncremental(name) == null)
			save();
		else {
			System.out.print('.');
			main(args);
		}
	}

	private static void save() {
		StringBuffer latticeSource = new StringBuffer();
		String top = name.getTop();
		Collection<String> worklist = new LinkedHashSet<String>();
		worklist.add(top);

		while (!worklist.isEmpty()) {
			Iterator<String> iterator = worklist.iterator();
			top = iterator.next();
			iterator.remove();
			for (String lower : name.getImmediatelyLower(top)) {
				latticeSource.append(lower).append("<=").append(top).append('\n');
				worklist.add(lower);
			}
		}
	}

	private static void print(BitSet nbs, BitSet bs) {
		String lower = getName(nbs);
		String higher = getName(bs);
		name.addElement(lower);
		name.addElement(higher);
		name.setImmediatelyLower(higher, lower);
	}

	@SuppressWarnings("unused")
    private static void print1(BitSet nbs, BitSet bs) {
		System.out.print(getName(nbs));
		System.out.print("<=");
		System.out.println(getName(bs));
	}

	private static String getName(BitSet nbs) {
		StringBuilder lower = new StringBuilder(nbs.toString().replaceAll(", ", ""));
		lower.setCharAt(0, 'x');
		String lower1 = lower.substring(0, lower.length() - 1);
		return lower1;
	}
}

package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterprocDominators2<V,E> {
	protected AbstractCFG<V,E> icfg;
	private Map<V, NodeSetValue<V>> dom = new HashMap<V, NodeSetValue<V>>();

	public InterprocDominators2(AbstractCFG<V,E> icfg) {
		this.icfg = icfg;
	}

	public void run() {
		LinkedList<V> worklist = new LinkedList<V>();
		V root = icfg.getRoot();
		worklist.add(root);
		dom.put(root, new NodeSetValue.SetBased<V>(Collections.singleton(root)));
		for (V v : icfg.vertexSet()) {
			if (v.equals(root)) continue;
			dom.put(v, new NodeSetValue.NotSet<V>());
		}
		boolean changed;
		do {
			changed = false;
			System.out.println("doing it again...");
			for (V next : icfg.vertexSet()) {
				NodeSetValue<V> domNext = dom.get(next);
				NodeSetValue<V> domJoins = intersectDomsOf(incJoins(next));
				NodeSetValue<V> domNonJoins = intersectDomsOf(incNonJoins(next));
				NodeSetValue<V> newValue = domJoins.unionWith(domNonJoins).add(next);
				if (!domNext.equals(newValue)) {
					dom.put(next, newValue);
					changed = true;
				}
			}

		} while (changed);

		assert check();
	}

	public void runWorklist() {
		LinkedList<V> worklist = new LinkedList<V>();
		V root = icfg.getRoot();
		worklist.add(root);
		dom.put(root, new NodeSetValue.SetBased<V>(Collections.emptySet()));
		for (V v : icfg.vertexSet()) {
			if (v.equals(root)) continue;
			dom.put(v, new NodeSetValue.NotSet<V>());
		}
		while (!worklist.isEmpty()) {
			V next = worklist.poll();
			NodeSetValue<V> domNext = dom.get(next);
			NodeSetValue<V> domJoins = intersectDomsOf(incJoins(next));
			NodeSetValue<V> domNonJoins = intersectDomsOf(incNonJoins(next));
			NodeSetValue<V> newValue = domJoins.unionWith(domNonJoins).add(next);
			if (!domNext.isUnset() && newValue.size() > domNext.size()) {
				System.out.println(String.format("%s: %s -> %s", next, domNext.size(), newValue.size()));
			}
			if (!domNext.equals(newValue)) {
				dom.put(next, newValue);
				for (E out : icfg.out(next)) {
					if (!worklist.containsAll(icfg.atail(out))) {
						worklist.addAll(icfg.atail(out));
					}
				}
			}
		}

		assert check();
	}

	private List<E> incJoins(V v) {
		List<E> joins = new LinkedList<E>();
		for (E e : icfg.inc(v)) {
			if (icfg.isJoinEdge(e)) {
				joins.add(e);
			}
		}
		return joins;
	}

	private List<E> incNonJoins(V v) {
		List<E> nonJoins = new LinkedList<E>();
		for (E e : icfg.inc(v)) {
			if (!icfg.isJoinEdge(e)) {
				nonJoins.add(e);
			}
		}
		return nonJoins;
	}

	private NodeSetValue<V> intersectDomsOf(Collection<? extends E> edges) {
		if (edges.isEmpty()) {
			return new NodeSetValue.SetBased<V>(Collections.emptySet());
		} else {
			NodeSetValue<V> ret = new NodeSetValue.NotSet<V>();
			for (E e : edges) {
				ret = ret.intersectWith(dom(icfg.ahead(e)));
			}
			return ret;
		}
	}

	private boolean check() {
		boolean ret = true;
		for (V v : icfg.vertexSet()) {
			NodeSetValue<V> shouldDom = intersectDomsOf(incJoins(v)).unionWith(intersectDomsOf(incNonJoins(v))).add(v);
			if (!dom.get(v).equals(shouldDom)) {
				//furtherInspectAndLog(v, dom(v), shouldDom);
				ret = false;
			}
		}
		return ret;
	}

//	private void furtherInspectAndLog(V v, NodeSetValue<V> actualDom, NodeSetValue<V> expectedDom) {
//		Set<V> d12 = actualDom.diff(actualDom, expectedDom);
//		Set<V> d21 = diff(expectedDom, actualDom);
//		if (!d12.isEmpty()) {
//			System.out.println("The following elements should not be in the dom set of " + v + ": " + d12);
//		}
//		if (!d21.isEmpty()) {
//			System.out.println("The following elements should be but are not in the dom set of " + v + ": " + d12);
//		}
//	}

	@SuppressWarnings("unused")
	private <T> Set<T> diff(Set<T> s1, Set<T> s2) {
		Set<T> ret = new HashSet<T>();
		ret.addAll(s1);
		ret.removeAll(s2);
		return ret;
	}

	public Set<V> dom(V n) {
		NodeSetValue<V> ret = dom(Collections.singleton(n));
		if (ret.isUnset()) {
			return new HashSet<V>(icfg.vertexSet());
		} else {
			return ret.getElements();
		}
	}

	public Set<V> strictDom(V n) {
		Set<V> ret = new HashSet<V>(dom(n));
		ret.remove(n);
		return ret;
	}

	public Set<V> idoms(V n) {
		Set<V> ret = new HashSet<V>();
		// strict dominators of strict dominators of n
		Set<V> strictDom = strictDom(n);
		Set<V> domDom = new HashSet<V>();
		for (V d : strictDom) {
			domDom.addAll(strictDom(d));
		}
		ret.addAll(strictDom);
		ret.removeAll(domDom);
		return ret;
	}

	private NodeSetValue<V> dom(Collection<? extends V> c) {
		NodeSetValue<V> ret = new NodeSetValue.SetBased<V>(Collections.emptySet());
		for (V n : c) {
			ret = ret.unionWith(dom.get(n));
		}
		return ret;
	}
}

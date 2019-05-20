package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.BasicLogger.*;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.set;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.html.HtmlEscapers;

import edu.kit.joana.ifc.sdg.qifc.nildumu.util.DefaultMap;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.TriConsumer;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box;
import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.model.Graph;

/**
 * Dominator calculation on arbitrary graphs.
 * 
 * Loosely based on the loop-nest tree implementation from p. 382 of "modern compiler
 * implementation in Java" (2nd edition) by Andrew w. Appel 
 */
public class Dominators<T> {

    public static class Node<T> {
        final T elem;
        private final Set<Node<T>> outs;
        private final Set<Node<T>> ins;
        final boolean isEntryNode;

		public Node(T elem, Set<Node<T>> outs, Set<Node<T>> ins, boolean isEntryNode) {
			assert (elem == null) == isEntryNode;
			this.elem = elem;
			this.outs = outs;
			this.ins = ins;
			this.isEntryNode = isEntryNode;
		}

		public Node(T elem){
            this(elem, new HashSet<>(), new HashSet<>(), false);
        }

        private void addOut(Node<T> node){
		    if (!outs.contains(node)) {
                outs.add(node);
            }
            node.ins.add(this);
        }

        @Override
        public String toString() {
            return elem == null ? "$abstract entry$" : elem.toString();
        }

        public Set<Node<T>> transitiveOutHull(){
            Set<Node<T>> alreadyVisited = new LinkedHashSet<>();
            Queue<Node<T>> queue = new ArrayDeque<>();
            queue.add(this);
            while (queue.size() > 0){
                Node<T> cur = queue.poll();
                if (!alreadyVisited.contains(cur)){
                    alreadyVisited.add(cur);
                    queue.addAll(cur.outs);
                }
            }
            return alreadyVisited;
        }

        public Set<Node<T>> transitiveOutHullAndSelf() {
            Set<Node<T>> nodes = transitiveOutHull();
            nodes.add(this);
            return nodes;
        }

        public List<Node<T>> transitiveOutHullAndSelfInPostOrder(){
            return transitiveOutHullAndSelfInPostOrder(new HashSet<>());
        }

        private List<Node<T>> transitiveOutHullAndSelfInPostOrder(Set<Node<T>> alreadyVisited){
            alreadyVisited.add(this);
            return Stream.concat(outs.stream()
                    .filter(n -> !alreadyVisited.contains(n))
                    .flatMap(n -> n.transitiveOutHullAndSelfInPostOrder(alreadyVisited).stream()),
                    Stream.of(this)).collect(Collectors.toList());
        }

        public Graph createDotGraph(Function<Node<T>, Attributes> attrSupplier){
            return graph().graphAttr().with(RankDir.TOP_TO_BOTTOM).directed().with((guru.nidi.graphviz.model.Node[])transitiveOutHullAndSelf()
                    .stream().map(n -> node(n.toString())
                            .link((String[])n.outs.stream()
                            .map(m -> m.toString()).toArray(i -> new String[i]))
                            .with(n.isEntryNode ? Records.of("$abstract entry$") : attrSupplier.apply(n))
                     ).toArray(i -> new guru.nidi.graphviz.model.Node[i]));
        }

        public Set<Node<T>> getOuts() {
            return Collections.unmodifiableSet(outs);
        }

        public Set<Node<T>> getIns() {
            return Collections.unmodifiableSet(ins);
        }

        public T getElem() {
            return elem;
        }

		@Override
		public int hashCode() {
			return elem == null ? 0 : elem.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj.getClass() == Node.class && ((elem == null && ((Node<T>)obj).elem == null) || (((Node<T>)obj).elem != null && ((Node<T>)obj).elem.equals(elem)));
		}

		public boolean isEntryNode() {
			return isEntryNode;
		}
        
    }
    
    final Node<T> entryNode;
    final Function<T, Collection<T>> outs;
    final Map<T, Node<T>> elemToNode;
    final Map<Node<T>, Set<Node<T>>> dominators;
    final Map<Node<T>, Integer> loopDepths;
    /**
     * Contains the previous loop headers of every node that has one
     */
    final Map<Node<T>, Node<T>> loopHeaderPerNode;
    final Node<T> rootNode;

    public Dominators(T rootElem, Function<T, Collection<T>> outs) {
        this.entryNode =
                new Node<T>(null,
                        new HashSet<>(),
                        Collections.emptySet(),
                        true);
		this.outs = e -> e == null ? set(rootElem) : outs.apply(e);
        this.elemToNode =
                Stream.concat(
                		Stream.concat(Stream.of(rootElem), transitiveHull(rootElem, outs).stream()).map(Node<T>::new),
                		Stream.of(entryNode))
                        .collect(Collectors.toMap(n -> n.elem, n -> n));
        elemToNode
                .entrySet()
                .forEach(
                        e -> {
                            this.outs.apply(e.getKey())
                                    .forEach(m -> e.getValue().addOut(elemToNode.get(m)));
                        });
        rootNode = elemToNode.get(rootElem);
        dominators = dominators(entryNode);
        Pair<Map<Node<T>, Integer>, Map<Node<T>, Node<T>>> p = calcLoopDepthAndHeaders(entryNode, dominators);
        loopDepths = p.first;
        loopHeaderPerNode = p.second;
    } 
    
    public void registerDotGraph(String topic, String name) {
    	registerDotGraph(topic, name, Object::toString);
    }
  
    public void registerDotGraph(String topic, String name, Function<T, String> labeler) {
        DotRegistry.get().store(topic, name,
                () -> () -> {
                	Graph g = entryNode.createDotGraph(n -> Records.of(loopDepths.get(n) + "",
                    		HtmlEscapers.htmlEscaper().escape(labeler.apply(n.elem))));
                	return g;
                }
                );
    }
    
    static <T> Set<T> transitiveHull(T elem, Function<T, Collection<T>> outs){
    	 Set<T> alreadyVisited = new LinkedHashSet<>();
         Queue<T> queue = new ArrayDeque<>();
         queue.add(elem);
         while (queue.size() > 0){
             T cur = queue.poll();
             if (!alreadyVisited.contains(cur)){
                 alreadyVisited.add(cur);
                 queue.addAll(outs.apply(cur));
             }
         }
         alreadyVisited.remove(elem);
         return alreadyVisited;
    }

    public <R> Map<Node<T>, R> worklist(
            BiFunction<Node<T>, Map<Node<T>, R>, R> action,
            Function<Node<T>, R> bot,
            Function<Node<T>, Set<Node<T>>> next,
            Map<Node<T>, R> state) {
        return worklist(rootNode, action, bot, next, loopDepths::get, state);
    }

    public Set<T> dominators(T elem){
        return dominators.get(elemToNode.get(elem)).stream().filter(n -> n.elem != null).map(Node<T>::getElem).collect(Collectors.toSet());
    }

    public int loopDepth(T elem){
        return loopDepths.get(elemToNode.get(elem));
    }

    public boolean containsLoops(){
        return loopDepths.values().stream().anyMatch(l -> l > 0);
    }
    
    public boolean isPartOfLoop(T elem) {
    	return loopDepth(elem) > 0;
    }

    /**
     * Returns the previous loop header of a node, or {@code null} if isn't dominated by a header
     */
    public T loopHeader(T elem) {
    	return loopHeaderPerNode.getOrDefault(elemToNode.get(elem), null).elem;
    }
    
    public static <T> Map<Node<T>, Set<Node<T>>> dominators(Node<T> entryNode) {
        Set<Node<T>> bot = entryNode.transitiveOutHullAndSelf();
        return worklist(
        		entryNode,
                (n, map) -> {
                        Set<Node<T>> nodes = new HashSet<>(n.ins.stream().filter(n2 -> n != n2).map(map::get).reduce((s1, s2) -> {
                            Set<Node<T>> intersection = new HashSet<>(s1);
                            intersection.retainAll(s2);
                            return intersection;
                        }).orElseGet(() -> new HashSet<>()));
                        nodes.add(n);
                        return nodes;
                },
                n -> n == entryNode ? Collections.singleton(n) : bot,
                Node<T>::getOuts,
                n -> 1,
                new HashMap<>());
    }

    public static <T> Pair<Map<Node<T>, Integer>, Map<Node<T>, Node<T>>> calcLoopDepthAndHeaders(Node<T> mainNode, Map<Node<T>, Set<Node<T>>> dominators){
        Set<Node<T>> loopHeaders = new HashSet<>();
        dominators.forEach((n, dom) -> {
            dom.forEach(d -> {
                if (n.outs.contains(d)){
                    loopHeaders.add(d);
                }
            });
        });
        Map<Node<T>, Set<Node<T>>> dominates = new DefaultMap<>((n, map) -> new HashSet<>());
        dominators.forEach((n, dom) -> {
            dom.forEach(d -> dominates.get(d).add(n));
        });
        Map<Node<T>, Set<Node<T>>> dominatesDirectly = new DefaultMap<>((n, map) -> new HashSet<>());
        dominates.entrySet().forEach(e -> {
            for (Node<T> dominated : e.getValue()){
                if (e.getValue().stream().filter(d -> d != dominated && d != e.getKey()).allMatch(d -> !(dominates.get(d).contains(dominated)))){
                    dominatesDirectly.get(e.getKey()).add(dominated);
                }
            }
        });
        Map<Node<T>, Integer> loopDepths = new HashMap<>();
        // contains the previous header for each node
        Map<Node<T>, Node<T>> loopHeaderPerNode = new HashMap<>();
        Box<TriConsumer<Node<T>, Node<T>, List<Pair<Node<T>, Integer>>>> action = new Box<>(null);
        action.val = (node, header, depth) -> {
            if (loopDepths.containsKey(node)){
                return;
            }
            if (loopHeaders.contains(node)) {
                depth = new ArrayList<>(depth);
                depth.add(0, new Pair<>(node, depth.get(0).second + 1));
            }
            for (int i = 0; i < depth.size(); i++) {
            	if (i == depth.size() - 1 || node.transitiveOutHullAndSelf().contains(depth.get(i).first)) {
            		loopDepths.put(node, depth.get(i).second);
            		loopHeaderPerNode.put(node, depth.get(i).first);
            		break;
            	}
            }
            for (Node<T> Node : dominatesDirectly.get(node)) {
                if (node != Node) {
                    action.val.accept(Node, loopHeaders.contains(node) ? node : header, depth);
                }
            }
        };
        action.val.accept(mainNode, null, new ArrayList<>(Collections.singletonList(new Pair<>(null, 0))));
        return new Pair<>(loopDepths, loopHeaderPerNode);
    }

    /**
     * Basic extendable worklist algorithm implementation
     *
     * @param mainNode node to root (only methods that this node transitively calls, are considered)
     * @param action transfer function
     * @param bot root element creator
     * @param next next nodes for current node
     * @param priority priority of each node, usable for an inner loop optimization of the iteration
     *     order
     * @param <T> type of the data calculated for each node
     * @return the calculated values
     */
    public static <T, R> Map<Node<T>, R> worklist(
            Node<T> entryNode,
            BiFunction<Node<T>, Map<Node<T>, R>, R> action,
            Function<Node<T>, R> bot,
            Function<Node<T>, Set<Node<T>>> next,
            Function<Node<T>, Integer> priority,
            Map<Node<T>, R> state) {
        PriorityQueue<Node<T>> queue =
                new PriorityQueue<>(new TreeSet<>(Comparator.comparingInt(n -> -priority.apply((Node<T>)n))));
        queue.addAll(entryNode.transitiveOutHullAndSelfInPostOrder());
        log(() -> String.format("Initial order: %s", queue.toString()));
        queue.forEach(n -> state.put(n, bot.apply(n)));
        while (queue.size() > 0) {
            Node<T> cur = queue.poll();
            R newRes = action.apply(cur, state);
            if (!state.get(cur).equals(newRes)) {
                state.put(cur, newRes);
                queue.addAll(next.apply(cur));
            }
        }
        return state;
    }
    
    /**
     * Returns the root element
     */
    public T getRootElem() {
    	return rootNode.elem;
    }
    
    public Set<T> getElements(){
    	return elemToNode.keySet().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }
    
    /**
     * Returns the elements that the passed element has an edge to
     */
    public Collection<T> getNextElems(T elem){
    	return outs.apply(elem);
    }
    
	/**
	 * Based on the depth-first algorithm (ignores cycles): 
	 * https://en.wikipedia.org/wiki/Topological_sorting
	 */
	private List<Node<T>> topOrder(){
		Set<Node<T>> unmarked = new HashSet<>(elemToNode.values());
		List<Node<T>> l = new ArrayList<>();
		Box<Consumer<Node<T>>> visit = new Box<>(null);
		visit.val = n -> {
			if (!unmarked.contains(n)) {
				return;
			}
			unmarked.remove(n);
			n.outs.forEach(visit.val::accept);
			l.add(n);
		};
		while (!unmarked.isEmpty()) {
			visit.val.accept(Util.get(unmarked));
		}
		Collections.reverse(l);
		return l;
	}
	
	public List<T> getElementsInTopologicalOrder(){
		return topOrder().stream().map(Node::getElem).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
	
	public Node<T> getNodeForElement(T elem) {
		return elemToNode.get(elem);
	}
}

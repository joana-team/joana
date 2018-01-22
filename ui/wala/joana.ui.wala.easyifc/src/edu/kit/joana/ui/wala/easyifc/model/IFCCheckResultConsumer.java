/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.IMethod;

import com.google.common.collect.Multimap;

import edu.kit.joana.api.SPos;
import edu.kit.joana.api.annotations.AnnotationTypeBasedNodeCollector;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.wala.easyifc.model.IFCResultFilter.LeakType;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;
import edu.kit.joana.util.Pair;

public interface IFCCheckResultConsumer {

	/**
	 * Handle information flow check results.
	 * @param ifcres information flow check results.
	 */
	public void consume(IFCResult ifcres);
	
	public void inform(EntryPointConfiguration discovered);
	
	public static IFCCheckResultConsumer DEFAULT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final IFCResult ifcres) {
			// do nothing
		}
		
		public void inform(EntryPointConfiguration discovered) {
			// do nothing
		};
	};

	public static IFCCheckResultConsumer STDOUT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final IFCResult ifcres) {
			System.out.println(ifcres);
		}
		
		public void inform(EntryPointConfiguration discovered) {
			System.out.println("discovered: " + discovered);
		};
	};
	
	public class IFCResult {
		
		private final EntryPointConfiguration mainMethod;
		
		private final SortedSet<SLeak> leaks = new TreeSet<SLeak>();  
		private final SortedSet<SLeak> directLeaks = new TreeSet<SLeak>();  
		private final SortedSet<SLeak> excLeaks = new TreeSet<SLeak>();  
		private final SortedSet<SLeak> noExcLeaks = new TreeSet<SLeak>();
		private       Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations;
		private       Collection<IFCAnnotation> annotations2;
		private final IFCResultFilter filter;
		private final AnnotationTypeBasedNodeCollector collector;
		
		public IFCResult(final EntryPointConfiguration mainMethod, final IFCResultFilter filter, AnnotationTypeBasedNodeCollector collector) {
			this.mainMethod = mainMethod;
			this.collector = collector;
			this.filter = (filter == null ? IFCResultFilter.DEFAULT : filter);
		}
		
		public EntryPointConfiguration getEntryPointConfiguration() {
			return mainMethod;
		}
		
		public IMethod getEntryPoint() {
			return mainMethod.getEntryPointMethod();
		}

		public boolean hasLeaks() {
			return !(excLeaks.isEmpty() && noExcLeaks.isEmpty() && directLeaks.isEmpty());
		}
		
		public boolean hasImportantLeaks() {
			return !(noExcLeaks.isEmpty() && directLeaks.isEmpty());
		}
		
		public void addDirectLeak(final SLeak leak) {
			if (filter.isOk(LeakType.DIRECT, leak)) {
				leaks.add(leak);
				directLeaks.add(leak);
			}
		}

		public void addExcLeak(final SLeak leak) {
			if (filter.isOk(LeakType.EXCEPTION, leak)) {
				leaks.add(leak);
				excLeaks.add(leak);
			}
		}

		public void addNoExcLeak(final SLeak leak) {
			if (filter.isOk(LeakType.INDIRECT, leak)) {
				leaks.add(leak);
				noExcLeaks.add(leak);
			}
		}
		
		public String toString() {
			final int dir = directLeaks.size();
			final int excl = excLeaks.size();
			final int total = dir + excl + noExcLeaks.size();
			
			if (total > 0) {
				return "is UNSAFE: " + total + " leak" + (total > 1 ? "s" : "") + " found."
						+ (excl > 0 ? " " + excl + " due to indirect flow caused by exceptions." : "")
						+ (dir > 0 ? " " + dir + " due to direct flow." : "");
			} else {
				return "is SECURE.";
			}
		}
		
		public SortedSet<SLeak> getLeaks() {
			return Collections.unmodifiableSortedSet(leaks);
		}

		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> getAnnotations() {
			return annotations;
		}

		public void setAnnotations(Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations) {
			this.annotations = annotations;
		}
		
		public Collection<IFCAnnotation> getAnnotations2() {
			return annotations2;
		}

		public void setAnnotations2(Collection<IFCAnnotation> annotations2) {
			this.annotations2 = annotations2;
		}

		public AnnotationTypeBasedNodeCollector getCollector() {
			return collector;
		}

	}
	
	public static enum Reason { 
		DIRECT_FLOW(1), INDIRECT_FLOW(2), BOTH_FLOW(3), EXCEPTION(4), THREAD(5), THREAD_DATA(6), THREAD_ORDER(7),
		THREAD_EXCEPTION(8), UNKNOWN(9);

		public final int importance;
	
		private Reason(final int importance) {
			this.importance = importance;
		}
	
	}

	public static class SLeak implements Comparable<SLeak> {
		private final SPos source;
		private final SPos sink;
		private final Reason reason;
		private final Set<SPos> trigger = new TreeSet<>();
		private final SortedMap<SPos, ? extends Set<? extends SDGNode>> chop;

		public SLeak(final SPos source, final SPos sink, final Reason reason, final SortedMap<SPos, ? extends Set<? extends SDGNode>> chop) {
			this(source, sink, reason, chop, null);
		}
		
		public SLeak(final SPos source, final SPos sink, final Reason reason, final SortedMap<SPos, ? extends Set<? extends SDGNode>> chop, final SPos trigger) {
			if (trigger != null) {
				switch (reason) {
				case THREAD:
				case THREAD_DATA:
				case THREAD_EXCEPTION:
				case THREAD_ORDER:
					break;
				default:
					throw new IllegalArgumentException("A trigger position only makes sense for a thread conflict.");
				}
			}

			this.source = source;
			this.sink = sink;
			this.reason = reason;
			this.chop = chop;
		}
		
		public void addTrigger(final SPos tpos) {
			trigger.add(tpos);
		}
		
		public Set<SPos> getTrigger() {
			return Collections.unmodifiableSet(trigger);
		}
		
		public boolean hasTrigger() {
			return !trigger.isEmpty();
		}
		
		public SortedMap<SPos, Set<? extends SDGNode>> getChop() {
			return Collections.unmodifiableSortedMap(chop);
		}
		
		public SPos getSource() {
			return source;
		}
		
		public SPos getSink() {
			return sink;
		}
		
		public Reason getReason() {
			return reason;
		}
		
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + reason.hashCode();
			result = prime * result + sink.hashCode();
			result = prime * result + source.hashCode();
			result = prime * result + chop.keySet().hashCode();
			result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			if (o instanceof SLeak) {
				final SLeak l = (SLeak) o;
				return reason == l.reason
				    && sink.equals(l.sink)
				    && source.equals(l.source)
				    && chop.keySet().equals(l.chop.keySet());
			}
			
			return false;
		}

		public String toString() {
			String info = "";
			switch (reason) {
			case BOTH_FLOW:
				info = "direct and indirect flow ";
				break;
			case DIRECT_FLOW:
				info = "direct flow ";
				break;
			case INDIRECT_FLOW:
				info = "indirect flow ";
				break;
			case EXCEPTION:
				info = "flow caused by exceptions ";
				break;
			case THREAD:
				info = "critical thread interference ";
				break;
			case THREAD_DATA:
				info = "critical thread interference (data) ";
				break;
			case THREAD_ORDER:
				info = "critical thread interference (order) ";
				break;
			case THREAD_EXCEPTION:
				info = "critical thread interference caused by exceptions ";
				break;
			case UNKNOWN: //no action
				break;
			}
			
			switch (reason) {
			case THREAD_ORDER:
			case THREAD:
			case THREAD_DATA:
			case THREAD_EXCEPTION:
				if (hasTrigger()) {
					info += "caused by '" + trigger.toString() + "' ";
				}
				info += "between '" + source.toString() + "' and '" + sink.toString() + "'";
				break;
			default:
				info += "from '" + source.toString() + "' to '" + sink.toString() + "'";
			}
			
			if (hasTrigger()) {
				info += " (" + trigger.size() + " trigger)";
			}
			
			return info;
		}
		
		public String toString(final File srcFile) {
			StringBuffer sbuf = new StringBuffer();
			switch (reason) {
			case DIRECT_FLOW:
				sbuf.append("direct flow:\n");
				break;
			case INDIRECT_FLOW:
				sbuf.append("indirect flow:\n");
				break;
			case BOTH_FLOW:
				sbuf.append("direct and indirect flow:\n");
				break;
			case EXCEPTION:
				sbuf.append("flow due to exceptions:\n");
				break;
			case THREAD:
				sbuf.append("possibilistic or probabilistic flow:\n");
				break;
			case THREAD_DATA:
				sbuf.append("possibilistic flow:\n");
				break;
			case THREAD_ORDER:
				sbuf.append("probabilistic flow:\n");
				break;
			case THREAD_EXCEPTION:
				sbuf.append("possibilistic or probabilistic flow caused by exceptions:\n");
				break;
			default:
				sbuf.append("reason: " + reason + "\n");
			}
			
			sbuf.append("from '" + source.toString() + "' to '" + sink.toString() + "'\n");
			
			for (final SPos pos : chop.keySet()) {
				sbuf.append(pos.toString() + "\t");
				final String code = pos.getSourceCode(srcFile);
				sbuf.append(code + "\n");
			}
			
			return sbuf.toString();
		}
	
		@Override
		public int compareTo(final SLeak o) {
			if (o == this || this.equals(o)) {
				return 0;
			}
			
			if (!sink.equals(o.sink)) {
				return sink.compareTo(o.sink);
			}
			
			if (!source.equals(o.source)) {
				return source.compareTo(o.source);
			}
			
			// FIXME: this does not quite do what we would want to do: some unqueal SLeaks will be lumped together.
			// on the other hand, we cannot easily totally order sets of SPos.
			return Integer.compare(chop.hashCode(), o.chop.hashCode());
		}
		
	}

}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.IMethod;

import edu.kit.joana.ui.wala.easyifc.model.IFCResultFilter.LeakType;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;

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
		private final IFCResultFilter filter;
		
		public IFCResult(final EntryPointConfiguration mainMethod, final IFCResultFilter filter) {
			this.mainMethod = mainMethod;
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
		private final SortedSet<SPos> chop;
		private final Set<SPos> trigger = new TreeSet<>();

		public SLeak(final SPos source, final SPos sink, final Reason reason, final SortedSet<SPos> chop) {
			this(source, sink, reason, chop, null);
		}
		
		public SLeak(final SPos source, final SPos sink, final Reason reason, final SortedSet<SPos> chop, final SPos trigger) {
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
		
		public SortedSet<SPos> getChop() {
			return Collections.unmodifiableSortedSet(chop);
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
		
		public int hashCode() {
			return source.hashCode() + 23 * sink.hashCode() + 49 * reason.hashCode()
					+ (trigger != null ? 4711 * trigger.hashCode() : 0);
		}
		
		public boolean equals(Object o) {
			if (o instanceof SLeak) {
				final SLeak l = (SLeak) o;
				return source.equals(l.source) && sink.equals(l.sink) && reason == l.reason;
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
			}
			
			switch (reason) {
			case THREAD_ORDER:
			case THREAD:
			case THREAD_DATA:
			case THREAD_EXCEPTION:
				if (trigger != null) {
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
			
			for (final SPos pos : chop) {
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
	
			if (!source.equals(o.source)) {
				return source.compareTo(o.source);
			}
			
			if (!sink.equals(o.sink)) {
				return sink.compareTo(o.sink);
			}
			
			return 0;
		}
		
	}
	
	public static class SPos implements Comparable<SPos> {
		public final String sourceFile;
		public final String simpleSource;
		public final int startChar;
		public final int endChar;
		public final int startLine;
		public final int endLine;
		
		public SPos(final String sourceFile, final int startLine, final int endLine, final int startChar,
				final int endChar) {
			this.sourceFile = sourceFile;
			if (sourceFile.contains("/")) {
				this.simpleSource = sourceFile.substring(sourceFile.lastIndexOf("/") + 1);
			} else if (sourceFile.contains("\\")) {
				this.simpleSource = sourceFile.substring(sourceFile.lastIndexOf("\\") + 1);
			} else {
				this.simpleSource = sourceFile;
			}
			this.startLine = startLine;
			this.endLine = endLine;
			this.startChar = startChar;
			this.endChar = endChar;
		}
		
		public int hashCode() {
			return sourceFile.hashCode() + 13 * startLine;
		}
		
		public boolean isAllZero() {
			return startLine == 0 && endLine == 0 && startChar == 0 && endChar == 0;
		}
		
		public boolean hasCharPos() {
			return !(startChar == 0 && startChar == endChar);
		}
		
		public boolean isMultipleLines() {
			return startLine != endLine;
		}
		
		public boolean equals(Object o) {
			if (o instanceof SPos) {
				final SPos spos = (SPos) o;
				return sourceFile.equals(spos.sourceFile) && startLine == spos.startLine && endLine == spos.endLine
						&& startChar == spos.startChar && endChar == spos.endChar;
			}
			
			return false;
		}
	
		@Override
		public int compareTo(SPos o) {
			if (sourceFile.compareTo(o.sourceFile) != 0) {
				return sourceFile.compareTo(o.sourceFile);
			}
			
			if (startLine != o.startLine) {
				return startLine - o.startLine;
			}
			
			if (endLine != o.endLine) {
				return endLine - o.endLine;
			}
			
			if (startChar != o.startChar) {
				return startChar - o.startChar;
			}
			
			if (endChar != o.endChar) {
				return endChar - o.endChar;
			}
			
			return 0;
		}
		
		public String toString() {
			if (hasCharPos() && isMultipleLines()) {
				return simpleSource + ":(" + startLine + "," + startChar + ")-(" + endLine + "," + endChar +")"; 
			} else if (hasCharPos()) {
				return simpleSource + ":(" + startLine + "," + startChar + "-" + endChar +")"; 
			} else if (isMultipleLines()) {
				return simpleSource + ":" + startLine + "-" + endLine; 
			} else {
				return simpleSource + ":" + startLine; 
			}
		}
		
		public String getSourceCode(final File sourceFile) {
			final File f = sourceFile;
			try {
				String code = "";
				final BufferedReader read = new BufferedReader(new FileReader(f));
				for (int i = 0; i < startLine-1; i++) {
					read.readLine();
				}
	
				if (!isMultipleLines()) {
					final String line = read.readLine();
					if (hasCharPos()) {
						code = line.substring(startChar, endChar);
					} else {
						code = line;
					}
				} else {
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code = line.substring(startChar);
						} else {
							code = line;
						}
					}
					
					for (int i = startLine; i < endLine-1; i++) {
						code += read.readLine();
					}
					
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code += line.substring(0, endChar);
						} else {
							code += line;
						}
					}
				}
	
				read.close();
				
				return code;
			} catch (IOException e) {}
			
			return  "error getting source";
		}
	}

}

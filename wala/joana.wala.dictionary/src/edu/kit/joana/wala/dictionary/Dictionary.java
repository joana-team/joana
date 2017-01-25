/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ibm.wala.classLoader.IMethod;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PartialOrder.Cmp;

public class Dictionary {

	private static class Entry {
		private final MayAliasGraph context;
		private final SDGSummaryReference sdg;

		private Entry(MayAliasGraph context, SDGSummaryReference sdg) {
			this.context = context;
			this.sdg = sdg;
		}

		public int hashCode() {
			return context.hashCode();
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof Entry) {
				Entry other = (Entry) obj;

				return context.equals(other.context) && sdg.equals(other.sdg);
			}

			return false;
		}
	}

	public static final class SDGSummaryReference {

		private WeakReference<SDG> sdg;

		private final String filename;

		public SDGSummaryReference(final String filename) {
			this.filename = filename;
			this.sdg = new WeakReference<SDG>(null);
		}

		public SDG load() throws IOException {
			SDG result = sdg.get();

			if (result == null) {
				// reload
				result = SDG.readFrom(filename);
				sdg = new WeakReference<SDG>(result);
			}

			return result;
		}

		public int hashCode() {
			return filename.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (obj instanceof SDGSummaryReference) {
				final SDGSummaryReference other = (SDGSummaryReference) obj;
				return filename.equals(other.filename);
			}

			return false;
		}

		public String toString() {
			return filename;
		}
	}

	private final Map<String, List<Entry>> map = new HashMap<String, List<Entry>>();

	public SDGSummaryReference getSDGfor(IMethod method, MayAliasGraph aliascfg) {
		return getSDGfor(extractSignature(method), aliascfg);
	}

	public SDGSummaryReference getSDGfor(String methodsig, MayAliasGraph aliascfg) {
		if (methodsig == null || aliascfg == null) {
			throw new IllegalArgumentException("Arguments may not be null.");
		}

		List<Entry> entries = map.get(methodsig);

		if (entries == null || entries.isEmpty()) {
			throw new NoSuchElementException("No entries for method '" + methodsig + "' found.");
		}

		// search lowest upper bound
		Entry found = null;
		for (Entry e : entries) {
			final Cmp cmp = e.context.compareTo(aliascfg);
			if (cmp == Cmp.BIGGER) {
				if (found == null || found.context.compareTo(e.context) == Cmp.BIGGER) {
					found = e;
				}
			} else if (cmp == Cmp.EQUAL) {
				found = e;
				break;
			}
		}

		if (found == null) {
			throw new NoSuchElementException("No entry with a context >= '" + aliascfg + "' found.");
		}

		return found.sdg;
	}

	public void putSDGFor(IMethod method, MayAliasGraph aliascfg, SDGSummaryReference sdg) {
		putSDGFor(extractSignature(method), aliascfg, sdg);
	}

	public void putSDGFor(String methodsig, MayAliasGraph aliascfg, SDGSummaryReference sdg) {
		if (methodsig == null || aliascfg == null || sdg == null) {
			throw new IllegalArgumentException("Arguments may not be null.");
		}

		List<Entry> entries = map.get(methodsig);
		if (entries == null) {
			entries = new LinkedList<Entry>();
			map.put(methodsig, entries);
		}

		final Entry e = new Entry(aliascfg, sdg);
		entries.add(e);
	}

	public String toString() {
		return "Dictionary of size " + map.size();
	}

	public static String extractSignature(final IMethod im) {
		return sanitizeSignature(im.getSignature());
	}

	private static String sanitizeSignature(String sig) {
		sig = sig.replace(';', '#');
		sig = sig.replace('/', '.');
		sig = sig.replace('$', '.');

		return sig;
	}
}

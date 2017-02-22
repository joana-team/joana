/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;

/**
 *
 * @author Martin Mohr
 *
 */
public class SDGMethodSelector {

	// private SDG sdg;
	private SDGMethod activeMethod = null;
	private List<SDGMethod> lastMethodSearchResults = new ArrayList<SDGMethod>();
	private final IFCConsole console;

	public SDGMethodSelector(IFCConsole console) {
		this.console = console;
	}

	private void init() {
		unselectMethod();
		lastMethodSearchResults.clear();
	}

	private SDG getSDG() {
		return console.getSDG();
	}

	/**
	 * Searches for entry nodes for methods, in whose name the given string is
	 * contained. Updates last search result list, if search was successful.
	 * Returns whether search was successful.
	 *
	 * @param methodName
	 *            name to search
	 * @return whether search was successful.
	 */
	public boolean searchMethodsContainingName(String methodName) {
		Pattern searchPattern = Pattern.compile(".*?" + methodName
				+ ".*?\\(.*\\).*");
		return search(searchPattern);
	}

	public boolean searchMethod(String methodSig) {
		Pattern searchPattern = Pattern.compile(Pattern.quote(methodSig));
		return search(searchPattern);
	}

	/**
	 * Returns the first method in list with given signature.
	 *
	 * @param sig
	 *            signature to find
	 * @return the first method in list with given signature, null if no such
	 *         method exists.
	 */
	public SDGMethod getMethod(JavaMethodSignature sig) {
		for (SDGMethod m : console.getProgram().getAllMethods()) {
			if (m.getSignature().equals(sig))
				return m;
		}

		return null;
	}

	private boolean search(Pattern searchPattern) {
		// Debug.println("searching for "+searchPattern);
		List<SDGMethod> newResults = new ArrayList<SDGMethod>();
		for (SDGMethod m : console.getProgram().getAllMethods()) {
			if (searchPattern.matcher(m.getSignature().toBCString()).matches()) {
				newResults.add(m);
			}
		}
		if (newResults.isEmpty()) {
			return false;
		}
		Collections.sort(newResults, new Comparator<SDGMethod>() {

			@Override
			public int compare(SDGMethod o1, SDGMethod o2) {
				return o1.getSignature().getMethodName()
						.compareTo(o2.getSignature().getMethodName());
			}

		});
		lastMethodSearchResults = newResults;
		unselectMethod();

		return true;

	}

	public boolean lastSearchResultEmpty() {
		return lastMethodSearchResults.isEmpty();
	}

	public boolean indexValid(int i) {
		return (i >= 0 && i < lastMethodSearchResults.size());
	}

	public void selectMethod(int i) {
		activeMethod = lastMethodSearchResults.get(i);
	}

	public SDGMethod getMethod(int i) {
		return lastMethodSearchResults.get(i);
	}

	public void unselectMethod() {
		activeMethod = null;
	}

	public SDGMethod getActiveMethod() {
		return activeMethod;
	}

	public int numberOfSearchResults() {
		return lastMethodSearchResults.size();
	}

	public void reset() {
		init();
	}

	public boolean isReady() {
		return getSDG() != null;
	}

	public boolean noSearchResults() {
		return numberOfSearchResults() == 0;
	}

	public boolean methodSelected() {
		return activeMethod != null;
	}

	public int getIndex(SDGMethod m) {
		for (int i = 0; i < lastMethodSearchResults.size(); i++) {
			if (lastMethodSearchResults.get(i).equals(m))
				return i;
		}
		throw new IllegalArgumentException("Given method not in list!");
	}
}

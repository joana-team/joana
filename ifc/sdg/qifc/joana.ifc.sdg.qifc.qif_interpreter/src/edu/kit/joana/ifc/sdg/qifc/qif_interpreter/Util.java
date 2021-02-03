package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Util {

	public static <T> List<T> asList(Iterator<T> it) {
		return asList(() -> it);
	}

	public static <T> List<T> asList(Iterable<T> it) {
		return StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());
	}
}

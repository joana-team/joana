package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class Util {

	public static <T> List<T> asList(Iterator<T> it) {
		return asList(() -> it);
	}

	public static <T> List<T> asList(Iterable<T> it) {
		return StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());
	}

	public static <T> Set<T> newHashSet(T... objs) {
		Set<T> set = new HashSet<T>();
		Collections.addAll(set, objs);
		return set;
	}

	public static void dumpToFile(String dest, StringBuilder sb) throws IOException {
		File targetFile = new File(dest);
		FileUtils.touch(targetFile);
		byte[] buffer = sb.toString().getBytes(StandardCharsets.UTF_8);
		FileUtils.writeByteArrayToFile(targetFile, buffer);
	}

	public static <T> List<T> prepend(List<T> base, List<? extends T> toPrepend) {
		for (int i = toPrepend.size() - 1; i >= 0; i--) {
			base.add(0, toPrepend.get(i));
		}
		return base;
	}

	public static <T> Collection<T> combine(Collection<T> a, Collection<T> b) {
		a.addAll(b);
		return a;
	}

	public static <T, S extends Collection<T>> S combine(S... collections) {
		Optional<S> first = Arrays.stream(collections).findFirst();

		if (!first.isPresent()) {
			return null;
		}
		;

		for (S collection : collections) {
			if (!collection.equals(first.get())) {
				first.get().addAll(collection);
			}
		}
		return first.get();
	}

	public static int[] removeDuplicates(int[] array) {
		Set<Integer> asSet = new HashSet<>();
		Arrays.stream(array).forEach(asSet::add);
		List<Integer> asList = new ArrayList<>(asSet);
		int[] noDups = new int[asSet.size()];
		IntStream.range(0, noDups.length).forEach(i -> noDups[i] = asList.get(i));

		return noDups;
	}
}
package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

	public static void dumpToFile(String dest, StringBuilder sb) throws IOException {
		File targetFile = new File(dest);
		FileUtils.touch(targetFile);
		byte[] buffer = sb.toString().getBytes(StandardCharsets.UTF_8);
		FileUtils.writeByteArrayToFile(targetFile, buffer);
	}
}

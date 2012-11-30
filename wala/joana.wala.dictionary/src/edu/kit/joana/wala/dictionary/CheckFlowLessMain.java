/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias.CheckFlowConfig;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer;

public class CheckFlowLessMain {

	public static final String TMP_OUT_DIR = "./checkflow_tmp/";
	public static final String LOG_FILE = TMP_OUT_DIR + "checkflow.log";

	public static final String[] FILES_NEEDED = { "natives_empty.xml", "jSDG-stubs-jre1.4.jar" };

	public static void main(final String[] args) throws ClassHierarchyException, IllegalArgumentException,
			IOException, CancelException, UnsoundGraphException {
		if (args.length < 2) {
			printUsage();
			return;
		}

		final String srcDir = args[0];
		final String binDir = args[1];

		if (!checkDirExistsAndRWable(srcDir)) {
			System.out.println("'" + srcDir +"' is not a read and writeable directory. Aborting.");
			return;
		}

		if (!checkDirExistsAndRWable(binDir)) {
			System.out.println("'" + binDir +"' is not a read and writeable directory. Aborting.");
			return;
		}

		if (!checkDirExistsOrCreate(TMP_OUT_DIR)) {
			System.out.println("Could not find or create temporary output directory '" + binDir +"'. Aborting.");
			return;
		}

		for (final String f : FILES_NEEDED) {
			if (!checkFileExists(TMP_OUT_DIR + f)) {
				try {
					copyFromBundle(f, TMP_OUT_DIR);
				} catch (IOException exc) {
					System.out.println("Could not copy file '" + f + "' from jar to '" + TMP_OUT_DIR +"'. Aborting.");
					return;
				}
			}
		}

		final CheckFlowConfig cfc = new CheckFlowConfig(binDir, srcDir, TMP_OUT_DIR, TMP_OUT_DIR,
				CheckFlowLessWithAlias.createPrintStream(LOG_FILE), FlowCheckResultConsumer.STDOUT,
				NullProgressMonitor.INSTANCE);

		final CheckFlowLessWithAlias cflwa = new CheckFlowLessWithAlias(cfc);
		cflwa.runCheckFlowLess();
	}

	private static boolean checkFileExists(final String name) {
		final File f = new File(name);

		return f.exists() && f.isFile() && f.canRead();
	}

	private static void copyFromBundle(final String name, final String toPath) throws IOException {
		final URL url = CheckFlowLessMain.class.getClassLoader().getResource(name);
		final BufferedInputStream bIn = new BufferedInputStream(url.openStream());
		final BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(toPath + name));
		byte[] buf = new byte[1024 * 10];

		while (bIn.available() > 0) {
			int read = bIn.read(buf);
			if (read > 0) {
				bOut.write(buf, 0, read);
			}
		}

		bIn.close();
		bOut.close();
	}

	private static boolean checkDirExistsOrCreate(final String dir) {
		final File f = new File(dir);

		if (!f.exists()) {
			if (!f.mkdirs()) {
				return false;
			}
		}

		return f.exists() && f.canRead() && f.canWrite() && f.isDirectory();
	}

	private static boolean checkDirExistsAndRWable(final String dir) {
		final File f = new File(dir);

		return f.exists() && f.canRead() && f.canWrite() && f.isDirectory();
	}

	private static void printUsage() {
		System.out.println("java -jar checkflow.jar <src-dir> <bin-dir>");
		System.out.println("");
		System.out.println("Searches for flowless annotations in all .java files in the <src-dir> directory");
		System.out.println("then loads all .class files in the <bin-dir> and checks if the flow statements");
		System.out.println("are satisfied.");
		System.out.println("");
		System.out.println("Log output is written to '" + LOG_FILE + "'.");
		System.out.println("Temporary sdg files are written to '" + TMP_OUT_DIR + "'.");
	}

}

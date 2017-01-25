/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.ParseException;

import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class EntryPointCache {

	public static class LoadEntryPointException extends Exception {

		private static final long serialVersionUID = 4688131245614666376L;

		public LoadEntryPointException(String msg) {
			super(msg);
		}

		public LoadEntryPointException(String msg, Throwable cause) {
			super(msg, cause);
		}

	}

	public static class StoreEntryPointException extends Exception {

		private static final long serialVersionUID = -7781970062400268423L;

		public StoreEntryPointException(String msg) {
			super(msg);
		}

		public StoreEntryPointException(String msg, Throwable cause) {
			super(msg, cause);
		}

	}

	private final TIntObjectHashMap<EntryPoint> data = new TIntObjectHashMap<EntryPoint>();
	private final String directory;
	private static final String ENTRYPOINT_FILE_SUFFIX = ".sum";

	private EntryPointCache(String directory) {
		this.directory = directory;
	}

	public EntryPoint getEntryPoint(int entryId) throws LoadEntryPointException {
		EntryPoint ep = data.get(entryId);

		if (ep == null) {
			ep = readFromFile(entryId);

			data.put(entryId, ep);
		}

		assert ep != null;

		return ep;
	}

	private EntryPoint readFromFile(final int entryId) throws LoadEntryPointException {
		EntryPoint ep = null;
		File epFile = getFileOfEntryPoint(entryId);

		if (!epFile.exists() || !epFile.isFile()) {
			throw new LoadEntryPointException(epFile.getAbsolutePath() + " is not an existing file.");
		} else if (!epFile.canRead()) {
			throw new LoadEntryPointException(epFile.getAbsolutePath() + " exists but is not readable.");
		}

		try {
			BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(epFile));

			ep = EntryPoint.readIn(bIn);
		} catch (FileNotFoundException e) {
			throw new LoadEntryPointException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new LoadEntryPointException(e.getMessage(), e);
		}

		return ep;
	}

	private void writeToFile(EntryPoint ep) throws StoreEntryPointException {
		File epFile = getFileOfEntryPoint(ep.getEntryId());

		try {
			PrintWriter pw = new PrintWriter(epFile);

			EntryPoint.writeOut(pw, ep);

			pw.flush();
			pw.close();
		} catch (FileNotFoundException exc) {
			throw new StoreEntryPointException(exc.getMessage(), exc);
		}
	}

	private File getFileOfEntryPoint(final int entryId) {
		final String filename = directory + File.separator + entryId + ENTRYPOINT_FILE_SUFFIX;
		File epFile = new File(filename);

		return epFile;
	}

	public static EntryPointCache create(String directory) {
		File dir = new File(directory);

		if (dir.exists() && dir.isFile()) {
			throw new IllegalArgumentException(directory + " is a file and not a directory.");
		}

		if (!dir.exists()) {
			dir.mkdirs();
			if (!dir.exists()) {
				dir.mkdir();
			}
		}

		if (!dir.exists() || dir.isFile()) {
			throw new IllegalArgumentException("Could not create a dir with name: " + directory);
		}

		return new EntryPointCache(directory);
	}

	public void put(EntryPoint ep) throws StoreEntryPointException {
		if (ep == null) {
			throw new IllegalArgumentException("Parameter is null.");
		} else if (data.contains(ep.getEntryId())) {
			throw new IllegalArgumentException("Entrypoint already in cache.");
		}

		data.put(ep.getEntryId(), ep);
		writeToFile(ep);
	}

}

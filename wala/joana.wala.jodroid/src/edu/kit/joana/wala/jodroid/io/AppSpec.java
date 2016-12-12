package edu.kit.joana.wala.jodroid.io;

import java.io.File;
import java.io.IOException;

import brut.apktool.Main;
import brut.common.BrutException;
import edu.kit.joana.wala.jodroid.io.apktool.APKToolException;



public class AppSpec {
	public final File apkFile;
	public final File manifestFile;

	public AppSpec(File apkFile, File manifestFile) {
		this.apkFile = apkFile;
		this.manifestFile = manifestFile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((apkFile == null) ? 0 : apkFile.hashCode());
		result = prime * result + ((manifestFile == null) ? 0 : manifestFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AppSpec)) {
			return false;
		}
		AppSpec other = (AppSpec) obj;
		if (apkFile == null) {
			if (other.apkFile != null) {
				return false;
			}
		} else if (!apkFile.equals(other.apkFile)) {
			return false;
		}
		if (manifestFile == null) {
			if (other.manifestFile != null) {
				return false;
			}
		} else if (!manifestFile.equals(other.manifestFile)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return apkFile.getAbsolutePath();
	}

	/**
	 * Uses apktool to extract the given apk file and unpack its content, in particular its manifest.
	 * WARNING: This method assumes that the parent directory of the given apk is writable and
	 * creates a new directory named 'apktool' in which it stores all the files. Afterwards, the file
	 * '&lt;parent-of-apk&gt;/apktool/AndroidManifest.xml' will contain the manifest of the given apk.
	 * This file has to stay where it is!
	 * @param apkFile the apk to extract
	 * @param quiet whether apktool shall perform output
	 * @return a pair (apkFile, manifestFile) where apkFile is the given parameter and manifestFile
	 * refers to &lt;parent-of-apk&gt;/apktool/AndroidManifest.xml
	 * @throws APKToolException if apktool fails
	 */
	public static AppSpec make(File apkFile, boolean quiet) throws APKToolException {
		String[] args;
		if (quiet) {
			args = new String[] {"-q", "d", "-f", apkFile.getAbsolutePath(), "-o", apkFile.getParent() + "/apktool"};
		} else {
			args = new String[] {"d", "-f", apkFile.getAbsolutePath(), "-o", apkFile.getParent() + "/apktool"};
		}
		try {
			Main.main(args);
		} catch (BrutException | InterruptedException | IOException e) {
			throw new APKToolException(e);
		}
		return new AppSpec(apkFile, new File(apkFile.getParent() + "/apktool/AndroidManifest.xml"));
	}
}
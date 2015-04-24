/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.wala.eval.util.HeavySlicer;
import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;
import edu.kit.joana.wala.util.jobber.client.ManagerClient;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class HeavySlicingManager extends ManagerClient {

	private static final String SDG_REGEX = ".*\\.pdg";
	private static final String NAME_MUST_CONTAIN = null;
	private static final boolean SKIP_SDGS_WITH_PREVIOUS_ERROR = false;

	public static final int POLL_FOR_JOBS_FINISHED_MS = 200;
	public static final int SLEEP_IN_BETWEEN_SINGLE_POLL = 30;


	private final PrintStream log;
	private final LinkedList<File> worklist = new LinkedList<File>(); 
	
	public HeavySlicingManager(final String serverIp, final int port, final PrintStream log) {
		super(serverIp, port);
		this.log = log;
	}
	
	public static void main(final String args[]) {
		final HeavySlicingManager hsm = new HeavySlicingManager("localhost", JobberServer.PORT, System.out);
		
		boolean recursive = false;
		boolean lazy = false;
		final List<File> filelist = new LinkedList<File>();
		
		
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-recursive")) {
					recursive = true;
				} else if (args[i].equals("-lazy")) {
					lazy = true;
				} else if (args[i].equals("-help")) {
					System.out.println("Usage: progname [-variant [new|old|delete]] [-runs <numberofruns>] [-recursive] [-help] <files or dir>");
					return;
				} else {
					// must be a file or directory name
					final String filename = args[i];
					
					final File f = new File(filename);
					if (!f.exists()) {
						hsm.displayError("File does not exist: '" + filename + "' - skipping");
					} else if (!f.canRead()) {
						hsm.displayError("File is not readable: '" + filename + "' -skipping");
					} else {
						filelist.add(f);
					}
					
					i++;
				}
			}
		}

		for (final File f : filelist) {
			hsm.addDirectoryToWorklist(f.getAbsolutePath(), SDG_REGEX, recursive, lazy);
		}
		
		hsm.start();
	}
	
	private static boolean nonEmptyFileExists(final String filename) {
		final File lf = new File(filename);

		if (lf.exists() && lf.isFile() && lf.length() > 0) {
			if (SKIP_SDGS_WITH_PREVIOUS_ERROR) {
				try {
					final BufferedReader br = new BufferedReader(new FileReader(lf));
					final String line = br.readLine();
					br.close();
					return !line.contains("ERROR");
				} catch (IOException exc) {}
			}
			return true;
		}
		
		return false;
	}
	
	private boolean needsToBeComputed(final boolean lazy, final String sdgFile, final String logFile) {
		if (NAME_MUST_CONTAIN != null && !sdgFile.contains(NAME_MUST_CONTAIN)) {
			return false;
		} else 	if (!lazy) {
			return true;
		}
		
		if (nonEmptyFileExists(logFile)) {
			final File sdg = new File(sdgFile);
			final File log = new File(logFile);
			try {
				final Map<String,Object> sdgAttr = Files.readAttributes(sdg.toPath(), "lastModifiedTime");
				final Map<String,Object> logAttr = Files.readAttributes(log.toPath(), "lastModifiedTime");
				if (sdgAttr.containsKey("lastModifiedTime") && logAttr.containsKey("lastModifiedTime")) {
					final FileTime ftSdg = (FileTime) sdgAttr.get("lastModifiedTime");
					final FileTime ftLog = (FileTime) logAttr.get("lastModifiedTime");
					if (ftSdg.compareTo(ftLog) <= 0) {
						// sdg before after log file -> no rerun needed
						System.out.println("skipping " + sdg.getName() + " as it is older or same date then summary log.");
						return false;
					}
				}
			} catch (IOException e) {
				displayError(e);
			}
		}
		
		return true;
	}
	
	public void addFileToWorklist(final String filename, final boolean lazy) {
		final File f = new File(filename);
		if (f.canRead() && !f.isDirectory()) {
			final String logFile = HeavySlicer.defaultLogFileName(f.getAbsolutePath());
			if (needsToBeComputed(lazy, filename, logFile)) {
				worklist.add(f);
			}
		} else {
			displayError("ignoring unreadable file: '" + filename + "'");
		}
	}
	
	public void addDirectoryToWorklist(final String dir, final String regex, final boolean recursive,
			final boolean lazy) {
		final File f = new File(dir);
		if (!f.isDirectory()) {
			addFileToWorklist(dir, lazy);
		} else {
			final Collection<File> result = FileUtil.listFiles(f.getAbsolutePath(), regex, recursive);
			for (final File found : result) {
				addFileToWorklist(found.getAbsolutePath(), lazy);
			}
		}
	}
	
	public void run() {
		int jobNum = 0;
		final LinkedList<Job> joblist = new LinkedList<Job>();
		final String processName = ManagementFactory.getRuntimeMXBean().getName();
		
		// create jobs
		while (!worklist.isEmpty()) {
			final File f = worklist.removeFirst();
			jobNum++;
			final CharBuffer data = CharBuffer.wrap(f.getAbsolutePath());

			
			try {
				final Job job = sendJob(HeavySlicingWorker.TYPE, "job-" + processName + "-" + jobNum, f.getName(), data);
				joblist.add(job);
				log.println("added job: " + job);
			} catch (UnknownHostException e) {
				throw new IllegalStateException("Could not send job: " + f.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new IllegalStateException("Could not send job: " + f.getAbsolutePath(), e);
			} catch (MessageParseException e) {
				throw new IllegalStateException("Could not send job: " + f.getAbsolutePath(), e);
			}
		}
		
		// monitor status of jobs until all are done or failed.
		while (!joblist.isEmpty()) {
			try {
				sleep(POLL_FOR_JOBS_FINISHED_MS);
			} catch (InterruptedException e) {}
			
			final List<Job> toRemove = new LinkedList<Job>();
			for (final Job job : joblist) {
				final int id = job.getId();
				try {
					try {
						sleep(SLEEP_IN_BETWEEN_SINGLE_POLL);
					} catch (InterruptedException e) {}
					final JobState state = checkStatus(id);
					switch (state) {
					case DONE:
						log.println("job finished: " + job);
						toRemove.add(job);
						break;
					case FAILED:
						log.println("job failed: " + job);
						toRemove.add(job);
						break;
					default:
						// nop
					}
				} catch (UnknownHostException e) {
					displayError(e);
				} catch (IOException e) {
					displayError(e);
				} catch (MessageParseException e) {
					displayError(e);
				}
			}
			
			joblist.removeAll(toRemove);
		}
		
		log.println("all jobs done, manager terminates.");
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.ManagerClient#displayError(java.lang.String)
	 */
	@Override
	public void displayError(final String msg) {
		log.println(msg);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.util.jobber.client.ManagerClient#displayError(edu.kit.joana.wala.util.jobber.io.RespMessage)
	 */
	@Override
	public void displayError(final RespMessage msg) {
		log.println(msg);
	}
	
	public void displayError(final Throwable t) {
		t.printStackTrace(log);
	}

}

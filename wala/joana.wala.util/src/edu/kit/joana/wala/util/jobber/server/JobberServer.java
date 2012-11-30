/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.wala.util.jobber.Job;
import edu.kit.joana.wala.util.jobber.JobState;


public class JobberServer extends Thread {

	public static final int PORT = 7654;

	private Set<Worker> workers = new HashSet<Worker>();
	private Map<Worker, Assignment> assignments = new HashMap<Worker, Assignment>();
	private List<Assignment> results = new LinkedList<Assignment>();
	private List<Job> jobs = new LinkedList<Job>();
	private Set<Integer> workersToCancel = new HashSet<Integer>();
	private ServerSocket in;

	private JobberServer() {}

	public static JobberServer create() throws IOException {
		return create(PORT);
	}

	public static JobberServer create(final int port) throws IOException {
		final JobberServer jd = new JobberServer();
		jd.in = new ServerSocket(port);

		return jd;
	}

	public static void main(final String[] argv) throws IOException {
		final JobberServer jd = JobberServer.create();
		jd.start();
	}

	@Override
	public void run() {
		log("started");

		try {
			while (in != null) {
				final Socket soc = in.accept();
				process(soc);
			}
		} catch (IOException exc) {
			log(exc);
		}

		log("finished");
	}

	public int getPort() {
		return this.in.getLocalPort();
	}

	private void process(final Socket soc) throws IOException {
		final CmdInterpreter interp = new CmdInterpreter(this);
		interp.setSocket(soc);
		interp.start();
	}

	protected void log(Exception exc) {
		exc.printStackTrace();
	}

	protected void log(String str) {
		System.out.println("Jobber[" + in.getLocalPort() + "] " + str);
	}

	protected Worker newWorker(final String name, final String type) {
		Worker worker = null;

		synchronized (workers) {
			worker = new Worker(getWorkerId(), name, type);
			workers.add(worker);
		}

		log("newWorker: " + worker);

		return worker;
	}

	protected Job newJob(final String type, final  String name, final String comment, final CharBuffer data) {
		Job job = null;

		synchronized (jobs) {
			job = new Job(getJobId(), type, name, comment, data);
			jobs.add(job);
		}

		log("newJob: " + job);

		return job;
	}

	protected Job cancelJob(int jobId) {
		Job job = null;

		synchronized (jobs) {
			for (Job current : jobs) {
				if (current.getId() == jobId) {
					job = current;
					break;
				}
			}

			if (job != null) {
				jobs.remove(job);
			}
		}

		if (job != null) {
			log("cancelJob: " + job);
		}

		return job;
	}

	protected void finishedAssignment(final int wId, final JobState state, final CharBuffer cbuf) throws ServerException {
		Assignment assign = null;

		synchronized (workers) {
			Worker worker = getWorker(wId);

			if (worker == null) {
				throw new ServerException("No worker with id " + wId);
			}

			synchronized (assignments) {
				assign = assignments.get(worker);
				if (assign == null) {
					throw new ServerException("Worker has no current assignment");
				}

				// remove current assignment
				assignments.put(worker, null);
			}

			switch (state) {
			case DONE:
				assign.setDone();
				break;
			case FAILED:
				assign.setFailed();
			default:
				throw new ServerException("Assignment may only be DONE or FAILED and not: " + state);
			}

			assign.setData(cbuf);
		}


		synchronized (results) {
			results.add(assign);
		}

		log("finishedAssignment: " + assign);
	}

	protected List<Job> listAllQueuedJobs(final String type) {
		final List<Job> queued = new LinkedList<Job>();

		synchronized (jobs) {
			for (Job job : jobs) {
				if (type != null && type.equals(job.getType())) {
					queued.add(job);
				} else if (type == null || type.isEmpty()) {
					queued.add(job);
				}
			}
		}

		log("listAllQueuedJobs: " + type);

		return queued;
	}

	protected List<Assignment> listAllWorking(final String type) {
		final List<Assignment> matches = new LinkedList<Assignment>();

		synchronized (assignments) {
			for (Assignment assign : assignments.values()) {
				if (assign != null) {
					if (type != null && type.equals(assign.getJob().getType())) {
						matches.add(assign);
					} else if (type == null || type.isEmpty()) {
						matches.add(assign);
					}
				}
			}
		}

		log("listAllWorking: " + type);

		return matches;
	}

	protected List<Assignment> listAllResults(final String type) {
		final List<Assignment> matches = new LinkedList<Assignment>();

		synchronized (results) {
			for (Assignment assign : results) {
				if (assign != null) {
					if (type != null && type.equals(assign.getJob().getType())) {
						matches.add(assign);
					} else if (type == null || type.isEmpty()) {
						matches.add(assign);
					}
				}
			}
		}

		log("listAllResults: " + type);

		return matches;
	}

	protected Job assignNewJobTo(final int wId) throws ServerException {
		Job job = null;

		boolean cancelThisWorker = false;
		synchronized (workersToCancel) {
			if (workersToCancel.contains(wId)) {
				workersToCancel.remove(wId);
				cancelThisWorker = true;
			}
		}

		synchronized (workers) {
			Worker worker = getWorker(wId);

			if (worker == null) {
				throw new ServerException("No worker with id " + wId);
			}

			final String type = worker.getType();

			if (!cancelThisWorker) {
				job = getNextJob(type);

				if (job != null) {
					newAssignment(job, worker);
				}
			} else {
				job = cancelWorkerJob(type);
			}
		}

		if (job != null && !cancelThisWorker) {
			log("assignNewJobTo: " + wId + " <- " + job);
		}

		return job;
	}

	protected void unregisterWorker(final int wId) throws ServerException {
		synchronized (workers) {
			Worker worker = getWorker(wId);

			if (worker != null) {
				removeWorker(worker);
			} else {
				throw new ServerException("No such worker with id " + wId);
			}
		}

		log("unregisterWorker: " + wId);
	}


	protected Assignment getResult(int jobId) {
		Assignment assign = null;

		synchronized (results) {
			for (Assignment current : results) {
				final Job job = current.getJob();

				if (job.getId() == jobId) {
					assign = current;
					break;
				}
			}

			if (assign != null) {
				results.remove(assign);
			}
		}

		if (assign != null) {
			log("getResult: " + assign);
		}

		return assign;
	}

	protected JobState checkState(int jobId) {
		log("checkState: " + jobId);

		// search in joblist for job
		synchronized (jobs) {
			for (Job job : jobs) {
				if (job.getId() == jobId) {
					return JobState.NEW;
				}
			}
		}

		// not there? search in currently assigned jobs
		synchronized (assignments) {
			for (Assignment assign : assignments.values()) {
				if (assign != null && assign.getJob().getId() == jobId) {
					return JobState.RUNNING;
				}
			}
		}

		// not running? search in finished jobs...
		synchronized (results) {
			for (Assignment assign : results) {
				if (assign != null && assign.getJob().getId() == jobId) {
					return assign.getState();
				}
			}
		}

		// no such job in system
		return JobState.UNKNOWN;
	}

	protected void shutDownWorkers(String type) {
		List<Integer> toCancel = new LinkedList<Integer>();

		log("shut down workers: " + type);

		synchronized (workers) {
			for (Worker work : workers) {
				if (type.equals(work.getType())) {
					log("shut down worker " + work.getId() + ":" + work.getType());
					toCancel.add(work.getId());
				} else {
					log("NOT shut down worker " + work.getId() + ":" + work.getType());
				}
			}
		}

		synchronized (workersToCancel) {
			workersToCancel.addAll(toCancel);
		}
	}

	private void newAssignment(Job job, Worker worker) throws ServerException {
		final Assignment assign = new Assignment(job, worker);

		synchronized (assignments) {
			Assignment old = assignments.get(worker);
			if (old != null) {
				throw new ServerException("Worker has already an assignment.");
			}

			assignments.put(worker, assign);
		}

		assign.setRunning();
	}

	private void removeWorker(Worker worker) throws ServerException {
		synchronized (assignments) {
			Assignment assign = assignments.get(worker);

			if (assign == null) {
				workers.remove(worker);
			} else {
				throw new ServerException("Can not remove worker. He has an assignment: " + assign);
			}
		}
	}

	private Worker getWorker(int wId) {
		for (Worker work : workers) {
			if (work.getId() == wId) {
				return work;
			}
		}

		return null;
	}

	private Job cancelWorkerJob(String type) {
		Job cancel = new Job(Job.CANCEL_ID, type, "CANCEL", "shut down this worker", CharBuffer.wrap(""));

		return cancel;
	}

	private Job getNextJob(String type) {
		Job result = null;

		synchronized (jobs) {
			for (Job job : jobs) {
				if (job.getType().equals(type)) {
					result = job;
					break;
				}
			}

			if (result != null) {
				jobs.remove(result);
			}
		}

		return result;
	}

	private int currentJobId = Job.START_ID;

	private int getJobId() {
		return currentJobId++;
	}

	private int currentWorkerId = 0;

	private int getWorkerId() {
		return currentWorkerId++;
	}

}

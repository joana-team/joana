/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.PDGs;


public class Setup {
	private static final String[] pdgs = PDGs.javagrande;

	private Algorithm.Kind choice;
	private final List<Algorithm> algs;
	private final List<String> progs;
	private final List<Report> reports;
	private int criteria;
	private boolean random;

	public Setup() {
		random = false;
		algs = new LinkedList<Algorithm>();
		progs = new LinkedList<String>();
		reports = new LinkedList<Report>();
	}

	/**
	 * -k S | C
	 * -a <list of algorithms>
	 * -p <list of programs>
	 * -c [r] <number of criteria>  (option `r' for random criteria)
	 * -r <list of reports>
	 *
	 * @param args
	 */
	public Evaluator configure(String[] args) {
		int kindStart = 0;
		int algsStart = 0;
		int progsStart = 0;
		int critStart = 0;
		int reportStart = 0;
		int algsEnd = 0;
		int progsEnd = 0;
		int reportEnd = 0;

		// parse the command line parameters
		int pos = 0;
		int start = pos;
		if (args[start].charAt(0) != '-') throw new IllegalArgumentException();
		pos++;

		while (pos < args.length) {
			if (args[pos].charAt(0) == '-') {
				if ("-k".equals(args[start])) {
					kindStart = start;

				} else if ("-a".equals(args[start])) {
					algsStart = start;
					algsEnd = pos-1;

				} else if ("-p".equals(args[start])) {
					progsStart = start;
					progsEnd = pos-1;

				} else if ("-c".equals(args[start])) {
					critStart = start;

				} else if ("-r".equals(args[start])) {
					reportStart = start;
					reportEnd = pos-1;

				} else {
					throw new IllegalArgumentException();
				}

				start = pos;


			} else if (pos == args.length-1) {
				if ("-k".equals(args[start])) {
					kindStart = start;

				} else if ("-a".equals(args[start])) {
					algsStart = start;
					algsEnd = pos;

				} else if ("-p".equals(args[start])) {
					progsStart = start;
					progsEnd = pos;

				} else if ("-c".equals(args[start])) {
					critStart = start;

				} else if ("-r".equals(args[start])) {
					reportStart = start;
					reportEnd = pos;

				} else {
					throw new IllegalArgumentException();
				}
			}

			pos++;
		}

		// evaluate the command line parameters
		parseChoice(args, kindStart);
		parseAlgorithms(args, algsStart, algsEnd);
		parsePrograms(args, progsStart, progsEnd);
		parseCrits(args, critStart);
		parseReports(args, reportStart, reportEnd);

		// initialize the evaluation
		Evaluator eval = new Evaluator();
		eval.setAlgorithms(choice, algs);
		eval.setSDGFiles(progs);
		eval.setReports(reports);
		eval.setCriteria(criteria, random);
		return eval;
	}

	private void parseChoice(String[] args, int pos) {
		String str = args[pos+1];
		for (Algorithm.Kind k : Algorithm.Kind.values()) {
			if (k.toString().equals(str)) {
				choice = k;
				break;
			}
		}

		if (choice == null) {
			throw new IllegalArgumentException();
		}
	}

	private void parseAlgorithms(String[] args, int start, int end) {
		int pos = start+1;

		while (pos <= end) {
			String arg = args[pos];
			for (Algorithm.Algo a : Algorithm.Algo.values()) {
				if (a.getValue().equals(arg) && a.getKind() == this.choice) {
					algs.add(a.instantiate());
					break;
				}
			}
			pos++;
		}
	}

	private void parsePrograms(String[] args, int start, int end) {
		int pos = start+1;

		while (pos <= end) {
			String arg = args[pos];
			try {
				int nr = Integer.parseInt(arg);
				progs.add(pdgs[nr]);

			} catch(NumberFormatException nex) { }
			pos++;
		}
	}

	private void parseCrits(String[] args, int pos) {
		if ("r".equals(args[pos+1])) {
			random = true;
			try {
				criteria = Integer.parseInt(args[pos+2]);

			} catch(NumberFormatException nex) { }

		} else {
			try {
				criteria = Integer.parseInt(args[pos+1]);

			} catch(NumberFormatException nex) { }
		}
	}

	private void parseReports(String[] args, int start, int end) {
		int pos = start+1;

		while (pos <= end) {
			String arg = args[pos];
			for (Report.Kind r : Report.Kind.values()) {
				if (r.getValue().equals(arg)) {
					reports.add(r.instantiate(algs));
					break;
				}
			}
			pos++;
		}
	}


	public static void main(String[] args) throws IOException {
		Setup conf = new Setup();
		Evaluator eval = conf.configure(args);
		eval.evaluate();
	}
}

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class Evaluator {
	private Algorithm.Kind choice;
	private List<Algorithm> algorithms;
	private List<String> sdgFiles;
	private List<Report> reports;
	private int criteria;
	private boolean random;

	public Evaluator() {
		algorithms = new LinkedList<Algorithm>();
		sdgFiles = new LinkedList<String>();
	}

	public void setAlgorithms(Algorithm.Kind kind, List<Algorithm> l) {
		choice = kind;
		algorithms = l;
	}

	public void setSDGFiles(List<String> files) {
		sdgFiles = files;
	}

	public void setCriteria(int crits, boolean rand) {
		criteria = crits;
		random = rand;
	}

	public void setReports(List<Report> rs) {
		reports = rs;
	}

	public void evaluate() throws IOException {
		System.out.println("Algorithms:");
		for (Algorithm a : algorithms) {
	        System.out.println("	"+a);
		}
		System.out.println("Programs:");
		for (String file : sdgFiles) {
	        System.out.println("	"+file);
		}
		System.out.println("Criteria: "+criteria+" (random: "+random+")");

		for (String file : sdgFiles) {
			SDG g = SDG.readFrom(file);
	        System.out.println(file);

	        singleProgram(g);

			for (Report r : reports) {
				System.out.println(r);
				r.clear();
			}
		}
	}

	private void singleProgram(SDG g) {
        System.out.println("initialize the algorithms");

        for (Algorithm alg : algorithms) {
			alg.setSDG(g);
		}

        System.out.println("build the criteria");

        List<Criterion> crits = new LinkedList<Criterion>();
		if (choice == Algorithm.Kind.CHOPPER) {
			crits = Criterion.createNRandomChoppingCriteria(criteria, g);

		} else if (choice == Algorithm.Kind.SLICER) {
			crits = (random ? Criterion.createNCriteriaRandomly(criteria, g) : Criterion.createNCriteria(criteria, 15, g));
		}

        // begin the evaluation
        int ctr = 0;
		for (Criterion crit : crits) {
			ctr++;

			for (Report r : reports) {
				r.nextIteration();
			}

			try {
				for (Algorithm alg : algorithms) {
					long time = System.currentTimeMillis();
					Collection<SDGNode> nodes = alg.run(crit);
					time = System.currentTimeMillis() - time;

					for (Report r : reports) {
						r.update(alg, crit, time, nodes);
					}
				}

				for (Report r : reports) {
					r.iterationSucceeded();
				}

			} catch(RuntimeException ex) {
				for (Report r : reports) {
					r.iterationAborted(ex, crit);
				}
			}

			if (ctr % 10 == 0) {
				System.out.print(".");
			}
			if (ctr % 100 == 0) {
				System.out.print(ctr);
			}
			if (ctr % 1000 == 0) {
				System.out.println();
			}
		}

		System.out.println();

		for (Report r : reports) {
			r.evaluationFinished();
		}
	}
}

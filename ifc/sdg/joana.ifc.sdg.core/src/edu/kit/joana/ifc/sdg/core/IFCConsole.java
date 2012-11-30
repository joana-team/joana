/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.ifc.sdg.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.core.conc.PossibilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNIChecker;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;


/**
 * @author giffhorn
 *
 */
public class IFCConsole {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static String in;

    private static IStaticLattice<String> l;
    private static String lFile;
    private static SDG g;
    private static String gFile;
    private static List<SecurityNode> nodes = new LinkedList<SecurityNode>();
    private static String kind;

    private IFCConsole() { }

    public static void main(String[] args) throws IOException, WrongLatticeDefinitionException {
        System.out.println("Run the analysis using a prepared configuration, ");
        System.out.println("or press <return> to input your data manually.");
        System.out.print("> ");
        in = reader.readLine();
        String[] command = in.split(" ");

        if (command[0].equals("")) {
            interactive();

        } else {
            parseConfiguration(command[0]);
            StringBuffer content = new StringBuffer();
            content.append(gFile+"\n");
            content.append(lFile+"\n");
            for (SecurityNode n : nodes) {
                content.append(n.getId()+" "+n.getRequired()+" "+n.getProvided()+"\n");
            }
            System.out.println(content);
            run();
        }
//    	System.out.print("> ");
//        in = reader.readLine();
//        String[] command = in.split(" ");
//    	for (int i = 0; i < 12; i++) {
//	    	parseConfiguration(command[0]);
////	    	StringBuffer content = new StringBuffer();
////	    	content.append(gFile+"\n");
////	    	content.append(lFile+"\n");
////	    	for (SecurityNode n : nodes) {
////	    		content.append(n.getId()+" "+n.getRequired()+" "+n.getProvided()+"\n");
////	    	}
////	    	System.out.println(content);
//	    	run();
//    	}
    }

    public static void interactive() throws IOException, WrongLatticeDefinitionException {
        System.out.println("Querying SDG: Enter the path to a .pdg file");
        System.out.print("> ");
        in = reader.readLine();
        String[] command = in.split(" ");
        readSDG(command[0]);

        System.out.println("Querying security lattice: Enter the path to a .lat file");
        System.out.print("> ");
        in = reader.readLine();
        command = in.split(" ");
        readLattice(command[0]);

        System.out.println("Choose your kind of analysis: `Seq (for sequential IFC), `Prob' (for Probabilistic IFC) or `Poss' (for Possibilistic IFC)");
        System.out.print("> ");
        in = reader.readLine();
        command = in.split(" ");
        kind = command[0];

        readCommands();
    }

    private static void readSDG(String path) throws IOException {
        g = SDG.readFrom(path, new SecurityNodeFactory());
        gFile = path;
    }

    private static void readLattice(String path) throws IOException, WrongLatticeDefinitionException {
        File f = new File(path);
        BufferedInputStream bfr = new BufferedInputStream(new FileInputStream(f));
        IEditableLattice<String> el = LatticeUtil.loadLattice(bfr);
        bfr.close();
        l = LatticeUtil.compileBitsetLattice(el);
        lFile = path;
    }

    private static void readCommands() throws IOException {
        boolean quit = false;
        System.out.println("Input your annotations, in the format ");
        System.out.println("     source <id> <level>");
        System.out.println("     sink <id> <level>");
        System.out.println("     declass <id> <incoming level> <outgoing level>");
        System.out.println("or use  ");
        System.out.println("     print (<file>)");
        System.out.println("to print your current input to a file or to the terminal");
        System.out.println("or use  ");
        System.out.println("     run");
        System.out.println("to start the information flow control.");

        // repeat until user wants to quit
        while (!quit) {
            System.out.print("> ");
            in = reader.readLine();
            String[] command = in.split(" ");
            // get the method for this command

            if ("source".equals(command[0])) {
                int id = Integer.parseInt(command[1]);
                String level = command[2];
                if (l.getElements().contains(level)) {
                    SecurityNode n = (SecurityNode) g.getNode(id);
                    n.setProvided(level);
                    nodes.add(n);
                } else {
                    System.out.println("`"+level+"' is not an element in the given lattice");
                }

            } else if ("sink".equals(command[0])) {
                int id = Integer.parseInt(command[1]);
                String level = command[2];
                if (l.getElements().contains(level)) {
                    SecurityNode n = (SecurityNode) g.getNode(id);
                    n.setRequired(level);
                    nodes.add(n);
                } else {
                    System.out.println("`"+level+"' is not an element in the given lattice");
                }

            } else if ("declass".equals(command[0])) {
                int id = Integer.parseInt(command[1]);
                String level1 = command[2];
                String level2 = command[3];

                SecurityNode n = (SecurityNode) g.getNode(id);
                if (l.getElements().contains(level1)) {
                    n.setRequired(level1);
                } else {
                    System.out.println("`"+level1+"' is not an element in the given lattice");
                }

                if (l.getElements().contains(level2)) {
                    n.setProvided(level2);
                    nodes.add(n);
                } else {
                    System.out.println("`"+level2+"' is not an element in the given lattice");
                }

            } else if ("print".equals(command[0])) {
                StringBuffer content = new StringBuffer();
                content.append(gFile+"\n");
                content.append(lFile+"\n");
                content.append(kind+'\n');
                for (SecurityNode n : nodes) {
                    content.append(n.getId()+" "+n.getRequired()+" "+n.getProvided()+"\n");
                }

                if (command.length > 1) {
                    File f = new File(command[1]);
                    BufferedWriter bfw = new BufferedWriter(new FileWriter(f));
                    bfw.write(content.toString());
                    bfw.flush();
                    bfw.close();

                    System.out.println("Current input status has been written to file "+command[1]);

                } else {
                    System.out.println(content);
                }

            } else if ("run".equals(command[0])) {
                run();
            }
        }
    }

    private static void run() {
        Collection<Violation> vios = null;
        long time = 0L;
        if (kind.equals("Prob")) {
            System.out.println("");
            System.out.println("Running probabilistic IFC...");
            time = System.currentTimeMillis();
            ProbabilisticNIChecker ifc = new ProbabilisticNIChecker(g, l);
            vios = ifc.checkIFlow();
            time = System.currentTimeMillis() - time;
            System.out.println("Time: "+ time);
            System.out.println("Init: "+ ifc.probInit);
            System.out.println("Check prob: "+ ifc.probCheck);
            System.out.println("	Order: "+ ifc.orderChannels);
            System.out.println("	Data: "+ ifc.dataChannels);
            System.out.println("Check flow: "+ ifc.flowCheck);

        } else if (kind.equals("Poss")) {
            System.out.println("Running possibilistic IFC...");
            time = System.currentTimeMillis();
            PossibilisticNIChecker ifc = new PossibilisticNIChecker(g, l);
            vios = ifc.checkIFlow();
            time = System.currentTimeMillis() - time;

        } else if (kind.equals("Seq")) {
            System.out.println("Running sequential IFC...");
            time = System.currentTimeMillis();
            PossibilisticNIChecker ifc = new PossibilisticNIChecker(g, l);
            vios = ifc.checkIFlow();
            time = System.currentTimeMillis() - time;

        } else {
        	throw new UnsupportedOperationException("This IFC algorithm is currently not supported: "+kind);
        }

        System.out.print("done, found "+vios.size()+" security violations");

        if (vios.size() > 0) {
            System.out.println(":");

        } else {
            System.out.println();
        }

        for (Violation v : vios) {
            System.out.println(v);
        }

        System.out.println("Time needed (sec): "+ (((double) time) / 1000));
    }

    private static void parseConfiguration(String path) throws IOException, WrongLatticeDefinitionException {
        BufferedReader bfr = new BufferedReader(new FileReader(path));

        // parse the graph
        readSDG(bfr.readLine());

        // parse the lattice
        readLattice(bfr.readLine());

        // parse the algorithm
        kind = bfr.readLine();

        while (bfr.ready()) {
            String line = bfr.readLine();
            String[] tokens = line.split(" ");

            int id = Integer.parseInt(tokens[0]);
            SecurityNode n = (SecurityNode) g.getNode(id);
            String level1 = tokens[1];
            String level2 = tokens[2];

            if (!"null".equals(level1)) {
                n.setRequired(level1);
            }

            if (!"null".equals(level2)) {
                n.setProvided(level2);
            }

            nodes.add(n);
        }
    }
}

package edu.kit.joana.SuSi2Joana;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

import java.util.List;
import java.util.ArrayList;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
//import edu.kit.joana.wala.jodroid.CliProgressMonitor;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.FileNotFoundException;

public class SuSi2Joana {
    private static final String SUSI2JOANA_INVOCATION_NAME = "<PROGRAM>";
    private static final int LINE_WIDTH = 80;
    
    private static File sdgFile = null;
    private static File sources = null;
    private static File sinks = null;
    private static PrintWriter outWriter = new PrintWriter(System.out, true);
    private static ILatticeBuilder lb;
    private static File latFile;

    private static void printUsage(Options options) {
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();

        formatter.printUsage(pw, LINE_WIDTH, "[OPTIONS] " + SUSI2JOANA_INVOCATION_NAME +"\n");
        formatter.printWrapped(pw, LINE_WIDTH, 0, "\nPossible Options are:");
        formatter.printOptions(pw, LINE_WIDTH, options, 4, 8);
    }

    private static void handleOptions(String[] args) throws FileNotFoundException {
        CommandLine commandLine;
        Options options = new Options();
        CommandLineParser parser = new GnuParser();

        options.addOption(
                OptionBuilder.withLongOpt( "binary" )
                .withDescription(
                    "Use a binary lattice and ignore SuSi-Cathegories"
                    )
                .create("b"));
        options.addOption(
                OptionBuilder.withLongOpt( "lattice" )
                .withDescription(
                    "Read the lattice from a file. It must contain SuSi-Cathegories"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("l"));
        options.addOption(
                OptionBuilder.withLongOpt( "create-lattice" )
                .withDescription(
                    "Prepare a lattice-file to be edited manually later"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("c"));
        options.addOption(
                OptionBuilder.withLongOpt( "sdg" )
                .withDescription(
                    "File containing a System-Dependence-Graph in Joanas .pdg-Format"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("s") );
        options.addOption(
                OptionBuilder.withLongOpt( "out" )
                .withDescription(
                    "File to write the IFC-Script to"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("o") );
        options.addOption(
                OptionBuilder.withLongOpt( "sources" )
                .withDescription(
                    "File in SuSi/FlowDrod-Format containing source specifications"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("u") );
        options.addOption(
                OptionBuilder.withLongOpt( "sinks" )
                .withDescription(
                    "File in SuSi/FlowDrod-Format containing sink specifications"
                    )
                .hasArg()
                .withArgName("FILE")
                .create("i") );
        options.addOption( "h", "help", false, "show help message" );

        try {
            commandLine = parser.parse(options, args);
            boolean allOk = true;
           
            if ( commandLine.hasOption( "binary" ) ) {
                if (commandLine.hasOption( "lattice" ) || commandLine.hasOption( "create-lattice" )) {
                    System.err.println("binary, lattice and create-lattice are mutually exclusive!");
                    System.exit(20);
                }

                SuSi2Joana.lb = new BinaryLattice();
            }

            if ( commandLine.hasOption( "lattice" ) ) {
                if (commandLine.hasOption( "binary" ) || commandLine.hasOption( "create-lattice" )) {
                    System.err.println("binary, lattice and create-lattice are mutually exclusive!");
                    System.exit(20);
                }

                final File latFile = new File(commandLine.getOptionValue("lattice"));
                if (latFile.isFile()) {
                    SuSi2Joana.lb = new LoadedLattice(latFile);
                } else {
                    System.err.println("Can't read the lattice from " + commandLine.getOptionValue("lattice"));
                    System.exit(30);
                }
            }

            if ( commandLine.hasOption( "create-lattice" ) ) {
                if (commandLine.hasOption( "lattice" ) || commandLine.hasOption( "binary" )) {
                    System.err.println("binary, lattice and create-lattice are mutually exclusive!");
                    System.exit(20);
                }

                SuSi2Joana.latFile = new File(commandLine.getOptionValue("create-lattice"));
                SuSi2Joana.lb = new LatticeBuilder();
            }

            if (! commandLine.hasOption( "binary" ) || commandLine.hasOption( "lattice" ) || commandLine.hasOption( "create-lattice" )) {
                System.err.println("One of the options binary, lattice or create-lattice has to be set");
                allOk = false;
            }

            if ( commandLine.hasOption( "help" ) ) {
                printUsage(options);
                System.exit(0);
            }

            if ( commandLine.hasOption( "sdg" ) ) {
                final String sdg = commandLine.getOptionValue("sdg");
                SuSi2Joana.sdgFile = new File(sdg);
                if (! SuSi2Joana.sdgFile.isFile() ) {
                    System.err.println("Cannot access the file '" + sdg + "' given as SDG");
                    System.exit(10);
                }
            } else {
                System.err.println("Cannot proceed without SDG-File.");
                allOk = false;
            }

            if ( commandLine.hasOption( "out" ) ) {
                final String out = commandLine.getOptionValue("out");
                final File outFile = new File(out);
                SuSi2Joana.outWriter = new PrintWriter(outFile);
            } else {
                System.err.println("No out-file given. Writing to stdout");
            }

            if ( commandLine.hasOption( "sources" ) ) {
                final String srcs = commandLine.getOptionValue("sources");
                SuSi2Joana.sources = new File(srcs);
                 if (! SuSi2Joana.sources.isFile() ) {
                    System.err.println("Cannot access the file '" + srcs + "' given for sources");
                    System.exit(11);
                 }
            } else {
                System.err.println("Cannot proceed without Sources-File.");
                allOk = false;
            }

            if ( commandLine.hasOption( "sinks" ) ) {
                final String sinks = commandLine.getOptionValue("sinks");
                SuSi2Joana.sinks = new File(sinks);
                 if (! SuSi2Joana.sinks.isFile() ) {
                    System.err.println("Cannot access the file '" + sinks + "' given for sinks");
                    System.exit(12);
                 }
            } else {
                System.err.println("Cannot proceed without Sinks-File.");
                allOk = false;
            }

            if (! allOk) {
                System.err.println("Not all requirements for running were met!");
                System.err.println("Try the --help option for more information.");
                System.exit(2);
            }
        } catch (ParseException exception) {
            System.err.println(exception.getMessage());
            printUsage(options);
            System.exit(1);
        }
    }

    private static void prepareLogging() {
        final Level defaultLevel = Level.INFO;
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(defaultLevel);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("SuSi2Joana");

        prepareLogging();
        handleOptions(args);
        //final IProgressMonitor mon = new CliProgressMonitor(System.out);
        final IProgressMonitor mon = new NullProgressMonitor(); // TODO: 

        final SuSiFile susi = new SuSiFile(mon);
        susi.readSources(SuSi2Joana.sources);
        susi.readSinks(SuSi2Joana.sinks);

        SuSi2Joana.lb.extract(susi.sources);
        SuSi2Joana.lb.extract(susi.sinks);

        final SdgHandler hSDG = new SdgHandler(mon);
        hSDG.readSDG(SuSi2Joana.sdgFile);

        final IfcScript ifcS = new IfcScript(lb);
        hSDG.matchSources(susi.sources, ifcS);
        hSDG.matchSinks(susi.sinks, ifcS);

        if (SuSi2Joana.lb instanceof LatticeBuilder) {
            SuSi2Joana.lb.write(SuSi2Joana.latFile);
        }

        ifcS.write(SuSi2Joana.outWriter);

        if (! SuSi2Joana.outWriter.equals(System.out)) {
            SuSi2Joana.outWriter.close();
        }
    }

}

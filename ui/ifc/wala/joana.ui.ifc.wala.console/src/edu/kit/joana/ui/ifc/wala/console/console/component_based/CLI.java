package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import edu.kit.joana.component.connector.JoanaCall;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.zip.ZipInputStream;

import static picocli.CommandLine.Command;

@Command(name = "component_based_cli", mixinStandardHelpOptions = true)
public class CLI implements Callable<Integer> {

  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @Override public Integer call() throws Exception {
    throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  @Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze and store result as json-io serialized JoanaCallReturn")
  static class Analyze implements Callable<Integer> {

    @CommandLine.Parameters(description = "path to json-io serialized JoanaCall object or zip", index="0")
    private Path input;

    @CommandLine.Parameters(description = "path to generated json-io serialized JoanaCallResult object", index="1")
    private Path output;

    @Override public Integer call() throws Exception {
      if (new ZipInputStream(Files.newInputStream(input)).getNextEntry() != null){
        JoanaCall.loadZipFile(input, c -> {
          new BasicFlowAnalyzer().processJoanaCall(c).store(output);
        });
      } else {
        new BasicFlowAnalyzer().processJoanaCall(JoanaCall.load(input)).store(output);
      }
      return 0;
    }
  }

  public static void main(String[] args) {
    new CommandLine(new CLI()).addSubcommand(new Analyze()).execute(args);
  }

}

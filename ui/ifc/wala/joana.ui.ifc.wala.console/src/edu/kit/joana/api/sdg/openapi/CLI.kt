package edu.kit.joana.api.sdg.openapi

import edu.kit.joana.api.sdg.openapi.CLI.BareCommand
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * Basic CLI for multisdg
 */
@CommandLine.Command(
    subcommands = [BareCommand::class, CLI.MultiCommand::class],
    description = [
        """
Runs collections of commands, that each define a console usage (i.e. run commands in succession to run the analysis).
A collection of commands can be given with the following syntax: "COMMAND_1; …; COMMAND_N; ...; COMMAND_M1; …; COMMAND_MM" 
where "..." tells the evaluator when to run the SDG computation. Configure the SDG computation before the "..." and
run the analysis after them.
 """
    ],
    mixinStandardHelpOptions = true,
    hidden = true
)
class CLI : Runnable {

    @CommandLine.Command(
        name = "bare",
        description = ["Run all arguments as individuals, one after another, ignoring \"...\""],
        mixinStandardHelpOptions = true
    )
    class BareCommand : Runnable {

        @CommandLine.Parameters
        var consoles: List<String> = ArrayList()

        override fun run() {
            consoles.toIFCConsoleWithMore(ignoreDots = true)
        }
    }

    @CommandLine.Command(
        name = "multi",
        description = ["Run the commands and use the MultiSDGComputation"],
        mixinStandardHelpOptions = true
    )
    class MultiCommand : Runnable {
        @CommandLine.Parameters
        var consoles: List<String> = ArrayList()

        override fun run() {
            consoles.toIFCConsoleWithMore().runWithMultiSDGComputatation()
        }
    }

    @CommandLine.Spec
    var spec: CommandLine.Model.CommandSpec? = null

    override fun run() = spec!!.commandLine().usage(System.err)
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(CLI()).execute(*args))
}

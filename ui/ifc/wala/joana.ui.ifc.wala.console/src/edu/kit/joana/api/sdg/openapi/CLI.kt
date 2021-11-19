package edu.kit.joana.api.sdg.openapi

import edu.kit.joana.api.sdg.SDGProgram.createSDGBuilder
import edu.kit.joana.api.sdg.openapi.CLI.BareCommand
import edu.kit.joana.ui.ifc.sdg.graphviewer.GraphViewer
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import picocli.CommandLine.Spec
import kotlin.system.exitProcess

/**
 * Basic CLI for multisdg
 */
@Command(
    subcommands = [BareCommand::class, CLI.MultiCommand::class, CLI.PropagateCommand::class],
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

    @Command(
        name = "bare",
        description = ["Run all arguments as individuals, one after another, ignoring \"...\""],
        mixinStandardHelpOptions = true
    )
    class BareCommand : Runnable {

        @Parameters
        var consoles: List<String> = ArrayList()

        override fun run() {
            consoles.toIFCConsoleWithMore(ignoreDots = true)
        }
    }

    @Command(
        name = "propagate",
        description = ["Run the commands and propagate some variables from a given method"],
        mixinStandardHelpOptions = true
    )
    class PropagateCommand : Runnable {
        @Parameters(index = "0")
        var console: String = ""

        @Parameters(index = "1")
        var variable: String = ""

        @Parameters(index = "2")
        var entry: String = ""

        @Parameters(index = "3", defaultValue = "true")
        var addException = true

        override fun run() {
            val ifcConsoleWithMore = console.toIFCConsoleWithMore()
            ifcConsoleWithMore.ifcConsole.createSDGBuilder().get().let {
                val builder = it.first.builder
                val exSDG = ExSDG(builder.sdg, builder.callGraph)
                val entry = exSDG.getEntryNode(builder.callGraph.vertexSet().first { node -> node.node.method.name.toString().equals(entry) }.node)
                println("Entry node is ${entry.label}")
                Propagation(exSDG).propagateSDGNodesForVariables(entry, setOf(variable), addException)
                GraphViewer.launch(exSDG.sdg)
                readLine()
            }
        }
    }

    @Command(
        name = "multi",
        description = ["Run the commands and use the MultiSDGComputation"],
        mixinStandardHelpOptions = true
    )
    class MultiCommand : Runnable {
        @Parameters
        var consoles: List<String> = ArrayList()

        override fun run() {
            consoles.toIFCConsoleWithMore(parallel = true).runWithMultiSDGComputatation()
        }
    }

    @Spec
    var spec: CommandLine.Model.CommandSpec? = null

    override fun run() = spec!!.commandLine().usage(System.err)
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(CLI()).execute(*args))
}

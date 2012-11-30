package edu.kit.joana.ifc.wala.sdpn.benchmark
import scala.collection.JavaConversions._
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey
import de.wwu.sdpn.wala.dpngen.symbols.DPNAction
import de.wwu.sdpn.wala.dpngen.symbols.GlobalState
import de.wwu.sdpn.wala.dpngen.symbols.StackSymbol
import com.ibm.wala.ipa.callgraph.CGNode

object DebugRunner {
    val JRE14_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";
    val DO_CACHE = false;
    val SKIP_PRIMORDIAL = true;
    val INTERPRET_KILL = true;
    val UNSAFE_KILL = false;
    val IGNORE_WAIT = false;
    val NO_EXCEPTIONS = false;
    val ITERABLE_ANALYSIS = false
    val THREAD_CONTEXTS = true
    val XSB_TIMEOUT = 1000 * 60 * 2;
    val JSDG_TIMEOUT = 1000 * 60 * 30;

    def main(args: Array[String]): Unit = {
        val out = System.out
        //        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream("/tmp/joana.out.txt")))
        val s = Setting("../Tests/bin", "Lconc/kn/Knapsack5", JRE14_LIB, DO_CACHE, SKIP_PRIMORDIAL, INTERPRET_KILL, UNSAFE_KILL, IGNORE_WAIT, NO_EXCEPTIONS, ITERABLE_ANALYSIS,THREAD_CONTEXTS, XSB_TIMEOUT,JSDG_TIMEOUT);
        val (cg, pa) = CGProvider.getCGnPA(s);
        //        System.setOut(out)
        val xeqrun = cg.getNode(262)
        println("IR for Xeq.run: " + xeqrun)
        println(xeqrun.getIR)
        println("Succ nodes: " + cg.getSuccNodeCount(xeqrun))
        for (n <- cg.getSuccNodes(xeqrun)) {
            println(n.getGraphNodeId + " : " + n)
        }

        val dpna = new DPN4IFCAnalysis(cg, pa)
        dpna.init()

        val init = cg.getNode(13)
        println("<init>: " + init)

        val searchRun = cg.getNode(196)
        println("Serach.run: " + searchRun)

        val sInit = dpna.getSS4Node(init)
        val sSearchRun = dpna.getSS4Node(searchRun)

        import de.wwu.sdpn.core.dpn.monitor.MonitorDPN

        val dpn: MonitorDPN[GlobalState, StackSymbol, DPNAction, InstanceKey] = dpna.genMDPN(Set(init, searchRun))
        import de.wwu.sdpn.core.ta.xsb.HasTermRepresentation

        implicit def da2tr(da: DPNAction) = new de.wwu.sdpn.core.ta.xsb.HasTermRepresentation { def toTerm = "0" }
        import cg.getNode

        def reach(n: CGNode): Boolean = de.wwu.sdpn.core.analyses.DPNReachability.runIRCheck(dpn, List(Set(dpna.getSS4Node(n))), false)
        def reachS(n: StackSymbol): Boolean = de.wwu.sdpn.core.analyses.DPNReachability.runIRCheck(dpn, List(Set(n)), false)
        //        val resIRLS = de.wwu.sdpn.core.analyses.DPNReachability.runIRCheck(dpn, List(Set(sInit), Set(sSearchRun)), true)
        //        println("MHS IT LS    " + resIRLS)
        //
        //        val resIRLIS = de.wwu.sdpn.core.analyses.DPNReachability.runIRCheck(dpn, List(Set(sInit), Set(sSearchRun)), false)
        //        println("MHS IT LIS   " + resIRLIS)
        //
        //        val resdpn4ifc = dpna.mayHappenSuccessively(sInit, sSearchRun)
        //        println("DPN4IFC      " + resdpn4ifc._1)
        //

        //
        //        println("Run reach    " + reach(searchRun))
        //        println("Init reach   " + reach(init))
        //
        //        val start = cg.getNode(112)
        //        println("Start reach  " + reach(start))

        // Search for where it breakes: 

        //        assert(!reach(searchRun),"I thought Search.run wasn't reachable!")
        //        
        //        var callList = List((start, searchRun))
        //        var checked = Set(searchRun)
        //        while (!callList.isEmpty) {
        //            val (n, o) :: t = callList
        //            callList = t
        //            if (!checked(n)) {
        //                checked += n                
        //                if (!reach(n)) {
        //                    println("Not reachable:  " + n.getGraphNodeId + ": " + n)
        //                    for (pre <- cg.getPredNodes(n)) {
        //                    	println("     Pred node: " + pre.getGraphNodeId + ": " + pre)
        //                        callList ::= (pre, n)
        //                    }
        //                } else {
        //                    println("Reachable:      " + n.getGraphNodeId + ": " + n)
        //                    println("----------")
        //                    println("Reachable:     " + n.getGraphNodeId + ": "+ n)
        //                    println("Not reachable: " + o.getGraphNodeId + ": "+ o)
        //                    println("----------")
        //                }
        //            }
        //
        //        }
        //
        //        Output:
        //Not reachable:  112: Node: < Primordial, Ljava/lang/Thread, start()V > Context: Everywhere
        //     Pred node: 74: Node: < Application, Lconc/kn/PriorityRunQueue, put(Ljava/lang/Runnable;D)V > Context: Everywhere
        //     Pred node: 256: Node: < Application, Lconc/kn/RunQueue, put(Ljava/lang/Runnable;)V > Context: Everywhere
        //Not reachable:  256: Node: < Application, Lconc/kn/RunQueue, put(Ljava/lang/Runnable;)V > Context: Everywhere
        //     Pred node: 241: Node: < Application, Lconc/kn/RunQueue, run(Ljava/lang/Runnable;)V > Context: Everywhere
        //Not reachable:  241: Node: < Application, Lconc/kn/RunQueue, run(Ljava/lang/Runnable;)V > Context: Everywhere
        //     Pred node: 225: Node: < Application, Lconc/kn/Future, setValue(Ljava/lang/Object;)V > Context: Everywhere
        //Not reachable:  225: Node: < Application, Lconc/kn/Future, setValue(Ljava/lang/Object;)V > Context: Everywhere
        //     Pred node: 210: Node: < Application, Lconc/kn/SharedTerminationGroup, terminate()V > Context: Everywhere
        //Not reachable:  210: Node: < Application, Lconc/kn/SharedTerminationGroup, terminate()V > Context: Everywhere
        //     Pred node: 196: Node: < Application, Lconc/kn/Knapsack5$Search, run()V > Context: Everywhere
        //Reachable:      74: Node: < Application, Lconc/kn/PriorityRunQueue, put(Ljava/lang/Runnable;D)V > Context: Everywhere
        //----------
        //Reachable:     74: Node: < Application, Lconc/kn/PriorityRunQueue, put(Ljava/lang/Runnable;D)V > Context: Everywhere
        //Not reachable: 112: Node: < Primordial, Ljava/lang/Thread, start()V > Context: Everywhere
        //----------

        // ok this one breaks it!
        val prioPut = getNode(74)
        println(prioPut.getIR)
        println("PP: " + prioPut + "succs: " + cg.getSuccNodeCount(prioPut))
        for (s <- cg.getSuccNodes(prioPut))
            println("   " + s.getGraphNodeId + " : " + s)

        println("DPN Rules for prioPut: ")
        for (r <- dpn.getTransitions.filter(_.inSymbol.node == prioPut))
            println("----\n" + r)

        val prioNodes = dpn.getStackSymbols.filter(_.node == prioPut)

        import dpna.getSS4Node;
        import de.wwu.sdpn.wala.dpngen.symbols.NState

        val newInit = (NState, getSS4Node(prioPut))
        def lmap(x: Any): Option[Int] = None
        val trans = dpn.getTransitions.filter(_.inSymbol.node == prioPut)
        val dpnPP = de.wwu.sdpn.core.dpn.monitor.DPNUtil.createMDPNfromRules(newInit, trans, lmap)

        val afterMon = StackSymbol(prioPut, 1, -1)
        def reachSPP(n: StackSymbol): Boolean = de.wwu.sdpn.core.analyses.DPNReachability.runIRCheck(dpnPP, List(Set(n)), false)
        println("After exit in DPNPP: " + reachSPP(afterMon))

        {
            import java.io._

            val aIdx = Map() ++ dpnPP.actions.zipWithIndex
            implicit def int2HTR(i: Int) = new HasTermRepresentation { def toTerm = i.toString }
            implicit def da2tr(da: DPNAction) = new HasTermRepresentation { def toTerm = aIdx(da).toString }

            val po = new PrintWriter(new FileWriter("/tmp/PrioPut.txt"))
            po.println(de.wwu.sdpn.core.dpn.monitor.DPNUtil.printMDPN(dpnPP))
            po.close
        }

        //        val nrNodes = prioNodes.filter(x => {
        //            val r = reachS(x)
        //            if (r) {
        //                println("Reachable:     " + x)
        //            } else {
        //                println("Not reachable: " + x)
        //            }
        //            !r
        //        })
        //
        //        println("\nNot reachable: ")
        //        for (node <- nrNodes) {
        //            println("    " + node)
        //        }
        //Not reachable: 
        //    ((74,put),50,0)
        //    ((74,put),56,0)
        //    ((74,put),55,1)
        //    ((74,put),48,1)
        //    ((74,put),52,0)
        //    ((74,put),53,0)
        //    ((74,put),53,1)
        //    ((74,put),50,1)
        //    ((74,put),55,0)
        //    ((74,put),58,0)
        //    ((74,put),49,3)
        //    ((74,put),57,0)
        //    ((74,put),1,-1)
        //    ((74,put),56,1)
        //    ((74,put),47,3)
        //    ((74,put),48,0)
        //    ((74,put),51,1)
        //    ((74,put),51,0)
        //    ((74,put),54,0)
        //    ((74,put),52,1)
        //    ((74,put),54,1)
        //    ((74,put),57,1)

    }

}
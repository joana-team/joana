package edu.kit.joana.ifc.wala.sdpn.benchmark
import java.io.IOException
import scala.annotation.implicitNotFound
import scala.collection.JavaConversions.asScalaIterator
import com.ibm.wala.classLoader.IClass
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis
import com.ibm.wala.ipa.callgraph.CGNode
import com.ibm.wala.ipa.callgraph.CallGraph
import com.ibm.wala.types.ClassLoaderReference
import com.ibm.wala.util.MonitorUtil.IProgressMonitor
import com.ibm.wala.util.MonitorUtil.beginTask
import com.ibm.wala.util.MonitorUtil.done
import com.ibm.wala.util.MonitorUtil.subTask
import com.ibm.wala.util.MonitorUtil.worked
import com.ibm.wala.util.CancelException
import de.wwu.sdpn.core.analyses.TwoSetReachability
import de.wwu.sdpn.core.dpn.monitor.MonitorDPN
import de.wwu.sdpn.core.ta.xsb.cuts.CutAcqStructComplTA
import de.wwu.sdpn.core.ta.xsb.cuts.CutAcqStructPrecutTA
import de.wwu.sdpn.core.ta.xsb.cuts.CutReleaseStructTA
import de.wwu.sdpn.core.ta.xsb.cuts.CutWellFormed
import de.wwu.sdpn.core.ta.xsb.cuts.FwdCutLockSet
import de.wwu.sdpn.core.ta.xsb.cuts.IFlowReading
import de.wwu.sdpn.core.ta.xsb.cuts.IFlowWriting
import de.wwu.sdpn.core.ta.xsb.cuts.MDPN2CutTA
import de.wwu.sdpn.core.ta.xsb.IntersectionEmptinessCheck
import de.wwu.sdpn.core.ta.xsb.IntersectionTA
import de.wwu.sdpn.core.ta.xsb.ScriptTreeAutomata
import de.wwu.sdpn.core.ta.xsb.XSBInterRunner
import de.wwu.sdpn.core.util.WPMWrapper
import de.wwu.sdpn.wala.dpngen.symbols.DPNAction
import de.wwu.sdpn.wala.dpngen.symbols.GlobalState
import de.wwu.sdpn.wala.dpngen.symbols.StackSymbol
import de.wwu.sdpn.wala.dpngen.MonitorDPNFactory
import de.wwu.sdpn.wala.util.BackwardSliceFilter
import de.wwu.sdpn.wala.util.LockWithOriginLocator
import de.wwu.sdpn.wala.util.SubProgressMonitor
import de.wwu.sdpn.wala.util.UniqueInstanceLocator
import de.wwu.sdpn.wala.util.WaitMap
import com.ibm.wala.ssa.SSAPutInstruction
import com.ibm.wala.ssa.SSAGetInstruction
import de.wwu.sdpn.wala.util.FieldUtil
import de.wwu.sdpn.core.ta.xsb.cuts.DPNAnnotater
import de.wwu.sdpn.core.dpn.monitor._
import de.wwu.sdpn.wala.dpngen.symbols.SSAAction
import de.wwu.sdpn.core.ta.xsb.cuts.IFlowNoOverwrite
import de.wwu.sdpn.wala.analyses.MyPreAnalysis

/**
 * Interface class to use for integration of sDPN with Joana.
 *
 * After creating an instance of this class the `init` method should be called to
 * perform preanalyses which will compute information used in subsequent interference
 * analyses.
 *
 * An interference analysis can than be performed by using the `runWeakCheck` method.
 *
 *
 * Some facts:
 *
 *  - The generated DPN uses `getSS4Node(cg.getFakeRootNode)` as starting configuration.
 *  - Calls to `java.lang.Thread.start()` are modeled as new processes in the DPN.
 *  - The call graph is pruned for every new analysis by only considering interesting nodes.
 *   - The nodes containing read and write positions are ''interesting''.
 *   - If `includeLockLocations` is true than all nodes containing possible lock usages are ''interesting'' too.
 *   - All nodes calling ''interesting'' nodes are ''interesting''.
 *  - The set of used locks can be configured by setting the variable `lockFilter`.
 *  The default accepts only locks corresponding to the class loader ''Application''.
 *   - Currently at most eight locks may be used on a 64bit System as the acquisition graph is modeled as
 *   a bit string.
 *  - The XSB-executable (obtainable from [[http://xsb.sf.net XSB web site]]) and a directory with write permission
 *  where temporary files will be stored need to be specified in the file `sdpn.properties`
 *  which needs to be on class path during execution.
 *
 *
 * @param cg A call graph representing the control flow of a program
 * @param pa The associated pointer analysis.
 *
 * @author Benedikt Nordhoff
 */
class DPN4IFCAnalysis(cg: CallGraph,
                      pa: PointerAnalysis,
                      unsafe_kill: Boolean = false,
                      ignore_wait: Boolean = false,
                      noExceptions: Boolean = false,
                      iterableAnalysis: Boolean = false,
                      timeout: Long = 0) {
    type MDPN = MonitorDPN[GlobalState, StackSymbol, DPNAction, InstanceKey]

    private val printWitness = true

    protected var possibleLocks: Set[InstanceKey] = null
    protected var lockUsages: Map[InstanceKey, (Set[CGNode], Boolean)] = null
    protected var waitMap: Map[CGNode, scala.collection.Set[InstanceKey]] = null
    protected var includeLockLocations = true
    protected var uniqueInstances: Set[InstanceKey] = null

    protected var lockFilter: InstanceKey => Boolean = defaultLockFilter

    protected def defaultLockFilter: InstanceKey => Boolean = {
        case x: ConstantKey[_] =>
            x.getValue() match {
                case x: IClass => ClassLoaderReference.Application.equals(x.getClassLoader().getReference())
                case _         => false
            }
        case x: InstanceKey =>
            ClassLoaderReference.Application.equals(x.getConcreteType().getClassLoader().getReference())
        case _ => false
    }

    protected def isConstantKey: InstanceKey => Boolean = _.isInstanceOf[ConstantKey[_]]

    /**
     * Should locations of used locks be factored in when pruning the call graph for DPN generation.
     * This increases precision and cost of the analysis.
     * @param value The new value for includeLockLocations.
     */
    def setIncludeLockLocations(value: Boolean) { includeLockLocations = value }

    /**
     * Set a filter which decides for abstractable locks whether they should
     * be considered for DPN generation.  This doesn't effect the identification of possible locks.
     * The default value is
     * {{{
     * x: InstanceKey =>
     *   ClassLoaderReference.Application.equals(x.getConcreteType().getClassLoader().getReference())
     * }}}
     * This can be implemented in Java by extending `scala.Function1<InstanceKey,Boolean>` and
     * implementing the `Boolean apply(InstanceKey ik)` method.
     *
     * @param value A new filter used to select locks.
     */
    def setLockFilter(value: InstanceKey => Boolean) { lockFilter = value }

    /**
     * Reset the lock filter to it's default value.
     */
    def resetLockFilter() {
        lockFilter = defaultLockFilter
    }

    /**
     * Set the lock filter to ignore all locks.
     */
    def setLockInsensitive() {
        lockFilter = _ => false
    }

    /**
     * @return The set of instance keys which have been identified to be unique
     * in the sense that there exists at most one concrete instance
     */
    def getUniqueInstances = uniqueInstances

    /**
     * @return The set of possible locks.
     */
    def getPossibleLocks = possibleLocks

    /**
     * @return The wait map, mapping a `CGNode` to a set of instance keys on which a `wait()` call may occur within.
     */
    def getWaitMap = waitMap

    /**
     * @return A map which maps `InstanceKeys` to a set of `CGNodes` where they are used as locks.
     * If includeLockLocations is set to true these will be included for pruning.
     */
    def getLockUsages = lockUsages

    /**
     * @return the value of includeLockLocations
     */
    def getIncludeLockLocations = includeLockLocations

    /**
     * @return the currently used lock filter
     */
    def getLockFilter = lockFilter

    /**
     * Initialize this analysis by calculating possible locks and the wait map.
     * @param pm0 The progress monitor used to report progress, with default value null.
     */
    def init(pm: IProgressMonitor = null) {

        try {
            beginTask(pm, "Initialyizing DPN-based analyses", 3)

            subTask(pm, "Identifying unique instances")
            val ui = UniqueInstanceLocator.instances(cg, pa)
            uniqueInstances = ui
            worked(pm, 1)

            subTask(pm, "Identifying lock usages")
            val loi = LockWithOriginLocator.instances(cg, pa)
            lockUsages = loi.filter(p => (p._2._2 || ui(p._1)))
            possibleLocks = lockUsages.keySet
            worked(pm, 1)

            if (!ignore_wait) {
                subTask(pm, "Locating wait() calls")
                val wmc = new WaitMap(new MyPreAnalysis(cg, pa), possibleLocks)
                waitMap = wmc.waitMap
                worked(pm, 1)
            } else {
                subTask(pm, "Ignoring wait() calls")
                waitMap = Map().withDefaultValue(Set())
                worked(pm, 1)
            }
        } finally { done(pm) }
    }

    /**
     * Generate a Monitor DPN which models all nodes from which a node of pruneSet can be reached.
     * @param pruneSet A set of interesting nodes
     * @return a Monitor DPN
     */
    def genMDPN(pruneSet: Set[CGNode]): MDPN = {
        var ss0 = pruneSet
        if (includeLockLocations) {
            for ((ik, (nodes, _)) <- lockUsages; node <- nodes)
                if (lockFilter(ik)
                    && !waitMap(node)(ik))
                    ss0 += node
        }
        val prea = new MyPreAnalysis(cg, pa) with BackwardSliceFilter {
            override def initialSet = ss0
            override def safeLock(ik: InstanceKey, node: CGNode) = possibleLocks(ik) && lockFilter(ik) && !waitMap(node)(ik)
        }
        val dpnFac = new MonitorDPNFactory(prea, false)
        val dpn = dpnFac.getDPN

        // ignore exceptions THIS IS VERY UNSOUND!
        import de.wwu.sdpn.wala.dpngen.symbols._

        if (noExceptions) {
            val trans = dpn.getTransitions
            val tf = trans.filter {
                case BaseRule(_, _, _, EState, _)        => false
                case PopRule(_, _, _, EState)            => false
                case SpawnRule(_, _, _, EState, _, _, _) => false
                case PushRule(_, _, _, EState, _, _)     => false
                case BaseRule(EState, _, _, _, _)        => false
                case PopRule(EState, _, _, _)            => false
                case SpawnRule(EState, _, _, _, _, _, _) => false
                case PushRule(EState, _, _, _, _, _)     => false
                case _                                   => true
            }
            val init = (dpn.initialState, dpn.initialStack)
            val lm = dpn.usedLock _

            return de.wwu.sdpn.core.dpn.monitor.DPNUtil.createMDPNfromRules(init, tf, lm)
        }
        // end ignore exceptions

        return dpn
    }

    /**
     * Run an interference check between '''writePos''' and '''readPos''' assuming weak updates or the absence of killing definitions.
     * This means we check if it is possible to reach '''readPos''' after reaching '''writePos'''.
     *
     * Throws an '''IOException''' if an error occurs while interacting with the XSB process
     * (e.g. more than five locks on a 32bit system)
     *
     * Throws an '''IllegalArgumentException''' if more than eight locks are used.
     *
     * Throws a '''CancelException''' or '''RuntimeException''' if `pm0.isCanceled` is set to true during the execution.
     *
     * @param writePos A stack symbol representing a point where a variable is written.
     * @param readPos A stack symbol representing a point where the written variable is read.
     * @param pm0 A progress monitor used to report progress with default value null.
     * @return True if '''readPos''' can be reached after reaching '''writePos''' in the DPN-model.
     *
     */
    @throws(classOf[IOException])
    @throws(classOf[IllegalArgumentException])
    @throws(classOf[RuntimeException])
    @throws(classOf[CancelException])
    def mayHappenSuccessively(writePos: StackSymbol, readPos: StackSymbol, ifr: IFResult, pm: IProgressMonitor = null): Boolean = {
        beginTask(pm, "Running DPN-based interference check", 3)
        try {
            val pm1 = new SubProgressMonitor(pm, 1)
            val (td, bu, dpnSize, lockNum) = genWeakAutomata(writePos, readPos, pm1)
            val icheck = new IntersectionEmptinessCheck(td, bu) { override val name = "ifccheck" }
            //debug
//            import java.io._
//            val x = new PrintStream(new FileOutputStream("/tmp/dpn.txt"))
//            x.print(icheck.emptiness)
//            x.close
//            println("Write pos: " + writePos)
//            println("Read pos: " + readPos)
            //end debug
            val pm2 = new SubProgressMonitor(pm, 2)

            val start = System.currentTimeMillis
            val result = !XSBInterRunner.runCheck(icheck, new WPMWrapper(pm2), timeout)
            val timeXSB = System.currentTimeMillis - start

            ifr.possible = result
            ifr.dpnSize = dpnSize
            ifr.timeXSB = timeXSB
            ifr.lockNumber = lockNum
            ifr.overwrite = false

            return result
        } finally {
            done(pm)
        }

    }

    /**
     * Run a may happen in parallel check between '''posOne''' and '''posTwo'''.
     * Note: If '''posOne == posTwo''' two processes must reach '''posOne'''.
     *
     * Throws an '''IOException''' if an error occurs while interacting with the XSB process
     * (e.g. more than five locks on a 32bit system)
     *
     * Throws an '''IllegalArgumentException''' if more than eight locks are used.
     *
     * Throws a '''CancelException''' or '''RuntimeException''' if `pm0.isCanceled` is set to true during the execution.
     *
     * @param posOne A stack symbol representing a point where a variable is written.
     * @param posTwo A stack symbol representing a point where the written variable is read.
     * @param pm0 A progress monitor used to report progress with default value null.
     * @return True if '''posOne''' and '''posTwo''' may happen in parallel in the DPN-model.
     *
     */
    @throws(classOf[IOException])
    @throws(classOf[IllegalArgumentException])
    @throws(classOf[RuntimeException])
    @throws(classOf[CancelException])
    def mayHappenInParallel(posOne: StackSymbol, posTwo: StackSymbol, pm: IProgressMonitor = null, timeout: Long = 0): Boolean = {
        beginTask(pm, "Running DPN-based MHP check", 5)
        try {
            subTask(pm, "Generating MonitorDPN")
            val dpn = genMDPN(Set(posOne.node, posTwo.node))
            worked(pm, 1)
            val lockSens = !dpn.locks.isEmpty

            subTask(pm, "Generating tree automata")
            val (td, bu) = TwoSetReachability.genAutomata(dpn, Set(posOne), Set(posTwo), lockSens)
            val icheck = new IntersectionEmptinessCheck(td, bu) { override val name = "mhpcheck" }
            worked(pm, 1)
            val pm2 = new SubProgressMonitor(pm, 3)

            return !XSBInterRunner.runCheck(icheck, new WPMWrapper(pm2), timeout)
        } finally {
            done(pm)
        }
    }

    /**
     * Helper method used by runWeakCheck.
     *
     * @param writePos A stack symbol representing a point where a variable is written.
     * @param readPos A stack symbol representing a point where the written variable is read.
     * @param pm0 A progress monitor used to report progress.
     * @return Two [[de.wwu.sdpn.ta.ScriptTreeAutomata]] the first one to be evaluated top down the second one to be evaluated bottom up
     * by an intersection emptiness test.
     */
    protected def genWeakAutomata(writePos: StackSymbol, readPos: StackSymbol, pm: IProgressMonitor = null): (ScriptTreeAutomata, ScriptTreeAutomata, Int, Int) = {

        try {
            beginTask(pm, "Generating automata for interference check", 2)

            subTask(pm, "Generating MonitorDPN")
            val dpn = genMDPN(Set(readPos.node, writePos.node))
            worked(pm, 1)
            val lockSens = !dpn.locks.isEmpty

            subTask(pm, "Generating tree automata")

            //The automata representing the lock insensitive  control flow of the DPN 
            val cflow = new MDPN2CutTA(dpn)

            val topDown: ScriptTreeAutomata = if (lockSens) {
                //An top down automata calculating lock sets to identify reentrant operations
                val fwdLS = new FwdCutLockSet("fwdLS", dpn.locks.size)

                //The control flow and the lock sets are evaluated top down
                new IntersectionTA(cflow, fwdLS) {
                    override val name = "flowls"
                }
            } else {
                cflow
            }

            //Now we build an bottom up tree automata which checks for conflicts and
            //assures that the execution tree can be scheduled lock sensitive

            //the stack symbols where a variable is read/written
            var writeStack = Set(writePos)
            var readStack = Set(readPos)

            //Automatons which check for a conflict      
            val ifwrite = new IFlowWriting("ifwrite", writeStack)
            val ifread = new IFlowReading("ifread", readStack)

            val conflict = new IntersectionTA(ifwrite, ifread) { override val name = "ifconf" }

            //A automata which ensures that the tree is cut well formed
            val cwf = new CutWellFormed("cwf")
            val cwfc = new IntersectionTA(conflict, cwf) {
                override val name = "cwfc"
            }

            val bottomUp: ScriptTreeAutomata = if (lockSens) {

                //Automatons which ensure lock sensitive schedulability      

                val relstr = new CutReleaseStructTA("crs", dpn.locks.size)
                val inter1 = new IntersectionTA(cwfc, relstr) {
                    override val name = "crf"
                }

                val lockTA = new CutAcqStructComplTA("compacq", dpn.locks.size)
                val inter2 = new IntersectionTA(inter1, lockTA) {
                    override val name = "craf"
                }

                val lockPreTA = new CutAcqStructPrecutTA("precutacq", dpn.locks.size)
                new IntersectionTA(inter2, lockPreTA)
            } else { cwfc }

            worked(pm, 1)

            return (topDown, bottomUp, dpn.transitions.size, dpn.locks.size)

        } finally { done(pm) }

    }

    def mayFlowFromTo(writeNode: CGNode, writeIdx: Int, readNode: CGNode, readIdx: Int, ifr: IFResult, pm: IProgressMonitor = null): Boolean = {
        beginTask(pm, "Running DPN-based interference check", 5)
        try {
            subTask(pm, "Identifying fields")
            val writePos = getSS4NodeAndIndex(writeNode, writeIdx, true)
            val readPos = getSS4NodeAndIndex(readNode, readIdx)

            val wi = writeNode.getIR().getInstructions()(writeIdx)
            val ri = readNode.getIR().getInstructions()(readIdx)

            val isRegularFieldAccess = wi.isInstanceOf[SSAPutInstruction] && ri.isInstanceOf[SSAGetInstruction]

            ifr.regularField = isRegularFieldAccess

            // this must be some kind of 
            if (!isRegularFieldAccess) {
                System.err.println("Interference between non regular fields!")
                System.err.println("   from: " + wi)
                System.err.println("   to: " + ri)
                val pm1 = new SubProgressMonitor(pm, 4)
                val result = mayHappenSuccessively(writePos, readPos, ifr, pm1)
                return result
            }

            require(wi.isInstanceOf[SSAPutInstruction], "Write instruction isn't instance of SSAPutInstruction: " + wi)
            require(ri.isInstanceOf[SSAGetInstruction], "Read instruction isn't instance of SSAGetInstruction: " + ri)

            val writeInstr = wi.asInstanceOf[SSAPutInstruction]
            val readInstr = ri.asInstanceOf[SSAGetInstruction]

            val readName = readInstr.getDeclaredField().getName()
            val writeName = writeInstr.getDeclaredField().getName()

            require(readName == writeName, "Instructions refer to differently named fields read: " + readName + " write: " + writeName)
            val fieldName = readName

            val writeObs = FieldUtil.getIKS4FieldInstr(pa, writeNode, writeInstr)
            val readObs = FieldUtil.getIKS4FieldInstr(pa, readNode, readInstr)

            val interObs = writeObs intersect readObs

            //no shared instance keys means no flow possible, but we would wan't joana to check for this!
            require(!(interObs isEmpty), "No shared instance keys for field found!")

            val uniqueInterObs = interObs filter (key => uniqueInstances(key) || isConstantKey(key))
            if (!unsafe_kill && ((interObs size) > 1 || (uniqueInterObs isEmpty))) {
                System.err.println("No safe killings, running weak check. %n interObs: %s %n uniqueInterObs: %s".
                    format(interObs.mkString("\n\t", "\n\t", ""),
                        uniqueInterObs.mkString("\n\t", "\n\t", "\n")))
                val pm1 = new SubProgressMonitor(pm, 4)
                // There may be multiple instances which correspond to this interference we can't interpret any killing definitions
                val result = mayHappenSuccessively(writePos, readPos, ifr, pm1)
                return result
            }

            assert(unsafe_kill || uniqueInterObs.size == 1, "I'm with stupid. I've written rubish above.")

            val fieldObj = uniqueInterObs.headOption.getOrElse(interObs.head) // use some other if no unique one

            if (!iterableAnalysis) {

                object FieldOverwriteAnnotater extends DPNAnnotater[GlobalState, StackSymbol, DPNAction] {
                    override type RuleAnnotation = String
                    override def annotateRule(rule: DPNRule[GlobalState, StackSymbol, DPNAction]): String = {
                        rule match {
                            case BaseRule(_, StackSymbol(node, _, _), SSAAction(instr: SSAPutInstruction), _, _) =>
                                val owrObs = FieldUtil.getIKS4FieldInstr(pa, node, instr)
                                if ((
                                    (unsafe_kill && !owrObs.intersect(interObs).isEmpty) || // if unsafe kill we want only some intersection  
                                    (owrObs.size == 1 && owrObs(fieldObj)) // else 
                                ) &&
                                    instr.getDeclaredField().getName() == fieldName)
                                    //TODO this is unsound when there are different fields with the same name as juergen talked about on the mailing list
                                    return "write"
                                else
                                    return "none"
                            case _ =>
                                return "none"
                        }
                    }

                }

                worked(pm, 1)

                val pm1 = new SubProgressMonitor(pm, 1)
                val (td, bu, dpnSize, lockNum) = genStrongAutomata(writePos, readPos, FieldOverwriteAnnotater, pm1)
                val icheck = new IntersectionEmptinessCheck(td, bu) { override val name = "ifccheck" }
                val pm2 = new SubProgressMonitor(pm, 3)

                val startXSB = System.currentTimeMillis

                val result = !XSBInterRunner.runCheck(icheck, new WPMWrapper(pm2), timeout)
                val timeXSB = System.currentTimeMillis - startXSB

                ifr.possible = result
                ifr.dpnSize = dpnSize
                ifr.timeXSB = timeXSB
                ifr.lockNumber = lockNum
                ifr.overwrite = true

                return result
            } else { // iterableAnalysis 
                val pruneSet = Set(writeNode, readNode)
                val dpn = genMDPN(pruneSet)
                val owTrans = dpn.transitions.filter({
                    case BaseRule(_, StackSymbol(node, _, _), SSAAction(instr: SSAPutInstruction), _, _) =>
                        val owrObs = FieldUtil.getIKS4FieldInstr(pa, node, instr)
                        if ((
                            (unsafe_kill && !owrObs.intersect(interObs).isEmpty) || // if unsafe kill we want only some intersection  
                            (owrObs.size == 1 && owrObs(fieldObj)) // else 
                        ) &&
                            instr.getDeclaredField().getName() == fieldName)
                            //TODO this is unsound when there are different fields with the same name as juergen talked about on the mailing list
                            true
                        else
                            false
                    case _ =>
                        false
                })

                val firstConf = Set(writePos)
                val confs = List((owTrans, Set(readPos)))
                val lockSens = !dpn.locks.isEmpty

                val startXSB = System.currentTimeMillis
                val result = de.wwu.sdpn.core.analyses.DPNReachability.runAIRCheck(dpn, firstConf, confs, true)
                val timeXSB = System.currentTimeMillis - startXSB

                ifr.possible = result
                ifr.dpnSize = dpn.transitions.size
                ifr.timeXSB = timeXSB
                ifr.lockNumber = dpn.locks.size
                ifr.overwrite = true

                return result
            }
        } finally {
            done(pm)
        }
    }

    /**
     * Helper method used by mayFlowFromTo
     *
     * @param writePos A stack symbol representing a point where a variable is written.
     * @param readPos A stack symbol representing a point where the written variable is read.
     * @param pm0 A progress monitor used to report progress.
     * @return Two [[de.wwu.sdpn.ta.ScriptTreeAutomata]] the first one to be evaluated top down the second one to be evaluated bottom up
     * by an intersection emptiness test.
     */
    protected def genStrongAutomata(writePos: StackSymbol, readPos: StackSymbol, annotater: DPNAnnotater[GlobalState, StackSymbol, DPNAction], pm: IProgressMonitor = null): (ScriptTreeAutomata, ScriptTreeAutomata, Int, Int) = {

        try {
            beginTask(pm, "Generating automata for interference check", 2)

            subTask(pm, "Generating MonitorDPN")
            val dpn = genMDPN(Set(readPos.node, writePos.node))
            worked(pm, 1)

            val lockSens = !dpn.locks.isEmpty

            subTask(pm, "Generating tree automata")

            //The automata representing the lock insensitive  control flow of the DPN 
            val cflow = new MDPN2CutTA(dpn, annotater = annotater)

            val topDown: ScriptTreeAutomata = if (lockSens) {
                //An top down automata calculating lock sets to identify reentrant operations
                val fwdLS = new FwdCutLockSet("fwdLS", dpn.locks.size)

                //The control flow and the lock sets are evaluated top down
                new IntersectionTA(cflow, fwdLS) {
                    override val name = "flowls"
                }
            } else {
                cflow
            }

            //Now we build an bottom up tree automata which checks for conflicts and
            //assures that the execution tree can be scheduled lock sensitive

            //the stack symbols where a variable is read/written
            var writeStack = Set(writePos)
            var readStack = Set(readPos)

            //Automatons which check for a conflict      
            val ifwrite = new IFlowWriting("ifwrite", writeStack)
            val ifread = new IFlowReading("ifread", readStack)
            val ifowr = new IFlowNoOverwrite("ifowr")

            val confl1 = new IntersectionTA(ifwrite, ifread, "ifreadwrite")
            val conflict = new IntersectionTA(ifowr, confl1, "ifconfl")

            //A automata which ensures that the tree is cut well formed
            val cwf = new CutWellFormed("cwf")
            val cwfc = new IntersectionTA(conflict, cwf) {
                override val name = "cwfc"
            }

            val bottomUp: ScriptTreeAutomata = if (lockSens) {

                //Automatons which ensure lock sensitive schedulability      

                val relstr = new CutReleaseStructTA("crs", dpn.locks.size)
                val inter1 = new IntersectionTA(cwfc, relstr) {
                    override val name = "crf"
                }

                val lockTA = new CutAcqStructComplTA("compacq", dpn.locks.size)
                val inter2 = new IntersectionTA(inter1, lockTA) {
                    override val name = "craf"
                }

                val lockPreTA = new CutAcqStructPrecutTA("precutacq", dpn.locks.size)
                new IntersectionTA(inter2, lockPreTA)
            } else { cwfc }

            worked(pm, 1)

            return (topDown, bottomUp, dpn.transitions.size, dpn.locks.size)

        } finally { done(pm) }

    }
    /**
     * Convert a CGNode plus BasicBlock index into the StackSymbol(node,bbNr,0) representing
     * the entry point of the basic block within the control flow graph corresponding to the node.
     *
     * @param node A node from the call graph
     * @param bbNr A basic block number from the control flow graph corresponding to node
     * @return StackSymbol(node,bbNr,0)
     */
    def getSS4NodeAndBB(node: CGNode, bbNr: Int) = StackSymbol(node, bbNr, 0)

    /**
     * Convert a CGNode into the StackSymbol(node,0,0) representing the entry point of that method.
     *
     * @param node A node from the call graph
     * @return StackSymbol(node,0,0)
     */
    def getSS4Node(node: CGNode) = StackSymbol(node, 0, 0)

    /**
     * Obtains the StackSymbol representing the point just ''before'' the
     * instruction corresponding to the given instructionIndex or ''after'' if ''afterInstruction'' is true.
     *
     * @param node A CGNode
     * @param instructionIndex An index of an instruction within the array node.getIR.getInstructions
     * @return A corresponding stack symbol
     */
    def getSS4NodeAndIndex(node: CGNode, instructionIndex: Int): StackSymbol = getSS4NodeAndIndex(node, instructionIndex, false)

    /**
     * Obtains the StackSymbol representing the point just ''before'' the
     * instruction corresponding to the given instructionIndex or ''after'' if ''afterInstruction'' is true.
     *
     * @param node A CGNode
     * @param instructionIndex An index of an instruction within the array node.getIR.getInstructions
     * @return A corresponding stack symbol
     */
    def getSS4NodeAndIndex(node: CGNode, instructionIndex: Int, afterInstruction: Boolean): StackSymbol = {
        val ir = node.getIR
        val cfg = ir.getControlFlowGraph

        val bb = cfg.getBlockForInstruction(instructionIndex)
        //val bb = cfg.filter(x => x.getFirstInstructionIndex <= instructionIndex && instructionIndex <= x.getLastInstructionIndex).first

        var index = 0
        for (instr <- bb.iteratePhis()) {
            index += 1
        }
        if (bb.isCatchBlock())
            index += 1
        val start = bb.getFirstInstructionIndex

        val instrArr = ir.getInstructions
        for (i <- start until instructionIndex) {
            if (instrArr(i) != null) {
                index += 1
            }
        }
        if (afterInstruction)
            index += 1
        return StackSymbol(node, bb.getNumber, index)
    }

    def shutdown() {
        XSBInterRunner.shutdown();
    }

    def noException: Option[Exception] = None
    def someException(a: Exception) = Some(a)

}
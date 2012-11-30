package edu.kit.joana.ifc.wala.sdpn.benchmark

object TestsSuite extends Suite {
    def name = "Tests"
    outputDir = "results/tests"

    val JRE14_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";
    val J2ME_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-j2me2.0.jar";
    val JAVACARD_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-javacard.jar";
    var RUNTIME_LIB = JRE14_LIB
    var DO_CACHE = false
    var SKIP_PRIMORDIAL = true
    var INTERPRET_KILL = true
    var UNSAFE_KILL = false // this is unsound
    var IGNORE_WAIT = false // this is unsound
    var NO_EXCEPTIONS = false // this is unsound
    var ITERABLE_ANALYSIS = true
    var THREAD_CONTEXTS = true
    var XSB_TIMEOUT = 1000 * 60 * 5
    var TIMEOUT = 1000 * 60 * 60
    var BIN = "../Tests/bin"

    def note = //format: OFF
"Runtimelib         " + RUNTIME_LIB + "\n" + 
"Caching            " + DO_CACHE + "\n" + 
"Skip primordial    " + SKIP_PRIMORDIAL + "\n" + 
"Interpret kill     " + INTERPRET_KILL + "\n" + 
"Unsafe kill        " + UNSAFE_KILL + "\n" +
"Ignore wait()      " + IGNORE_WAIT + "\n" + 
"No exceptions      " + NO_EXCEPTIONS + "\n" + 
"Iterable analysis  " + ITERABLE_ANALYSIS + "\n" + 
"Thread contexts    " + THREAD_CONTEXTS + "\n" + 
("XSB timeout      %d.%03ds".format(XSB_TIMEOUT/1000,XSB_TIMEOUT%1000))  + "\n" +
("Timeout          %d.%03ds".format(TIMEOUT/1000,TIMEOUT%1000))
    //format: ON

    def setting(main: String) = Setting(
        BIN,
        main,
        RUNTIME_LIB,
        DO_CACHE,
        SKIP_PRIMORDIAL,
        INTERPRET_KILL,
        UNSAFE_KILL,
        IGNORE_WAIT,
        NO_EXCEPTIONS,
        ITERABLE_ANALYSIS,
        THREAD_CONTEXTS,
        XSB_TIMEOUT,
        TIMEOUT)

    def mains =
        "Ltests/Synchronization" ::
            "Ltests/ConcPasswordFile" ::
            "Ltests/Mantel00Page10" ::
            "Ltests/IndirectRecursiveThreads" ::
            "Ltests/ProbPasswordFile" ::
            "Ltests/RecursiveThread" ::
            "Ltests/ThreadJoining" ::
            "Ltests/ThreadSpawning" ::
            "Ltests/VolpanoSmith98Page3" ::
            "Lconc/ds/DiskSchedulerDriver" ::
            "Lconc/bb/ProducerConsumer" ::
            "Lconc/ac/AlarmClock" ::
            "Lconc/lg/LaplaceGrid" ::
            "Lconc/sq/SharedQueue" ::
            "Lconc/TimeTravel" ::
            "Lconc/dp/DiningPhilosophers" ::
            "Lconc/kn/Knapsack5" ::
            "Lconc/daisy/DaisyTest" ::
            "Ltests/FixedProbChannel" ::
            "Ltests/ProbChannel" ::
            Nil
    //            "Lconc/cliser/kk/Main" ::  // kill's everything

    def settings = mains.map(setting)

    def main(args: Array[String]) {        
        Benchmark.runOn(this)
        //        UNSAFE_KILL = false
        //        Benchmark.runOn(this, "results/safe_kill/tests")
    }

}
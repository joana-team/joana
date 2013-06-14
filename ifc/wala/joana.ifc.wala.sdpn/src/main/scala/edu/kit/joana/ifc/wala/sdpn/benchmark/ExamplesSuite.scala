package edu.kit.joana.ifc.wala.sdpn.benchmark

object ExamplesSuite extends Suite {
    def name = "Examples"

    outputDir = "results/iterable/examples"

    val JRE14_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";
    var RUNTIME_LIB = JRE14_LIB
    var DO_CACHE = false;
    var SKIP_PRIMORDIAL = true;
    var INTERPRET_KILL = true;
    var UNSAFE_KILL = false;
    var IGNORE_WAIT = false
    var NO_EXCEPTIONS = false
    var ITERABLE_ANALYSIS = true
    var THREAD_CONTEXTS = true
    var XSB_TIMEOUT = 1000 * 60 * 5;
    var TIMEOUT = 1000 * 60 * 30;
    var BIN = "bin"

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
("XSB timeout      %d.%03ds".format(XSB_TIMEOUT/1000,XSB_TIMEOUT%1000)) + "\n" +
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

    def mains = "Lexamples/A" ::
        "Lexamples/B" ::
        "Lexamples/C" ::
        "Lexamples/CutTest" ::
        "Lexamples/BSP01" ::
        "Lexamples/BSP02" ::
        "Lexamples/BSP03" ::
        "Lexamples/BSP04" ::
        "Lexamples/BSP05" ::
        "Lexamples/BSP06" ::
        "Lexamples/MyThread" ::
        "Lexamples/testdata/A" ::
        "Lexamples/testdata/B" ::
        "Lexamples/testdata/C" ::
        "Lexamples/testdata/D" ::
        "Lexamples/testdata/Killing01" ::
        "Lexamples/testdata/Killing02" ::
        "Lexamples/testdata/Killing03" ::
        "Lexamples/testdata/Killing04" ::
        "Lexamples/testdata/Wait01" ::
        "Lexamples/testdata/Test01" :: Nil

    def settings:List[Setting] = mains.map(setting)

    def main(args: Array[String]) {
        Benchmark.runOn(this)
    }

}
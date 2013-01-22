package edu.kit.joana.ifc.wala.sdpn.benchmark

import edu.kit.joana.deprecated.jsdg.util.Log

object GiffhornSuite extends Suite {
    def name = "GiffhornExamples"

    outputDir = "results/giffhorn"

    var NEW_WORKSPACE_LOC = "../../giffhorn-suite/runtime-EclipseApplication";
    //var NEW_WORKSPACE_LOC = "/ben/giffhorn/Desktop/eclipse/runtime-EclipseApplication";
    var DO_CACHE = false;
    var SKIP_PRIMORDIAL = true;
    var INTERPRET_KILL = true;
    var UNSAFE_KILL = false;
    var IGNORE_WAIT = false
    var NO_EXCEPTIONS = false
    var ITERABLE_ANALYSIS = true
    var THREAD_CONTEXTS = true
    var XSB_TIMEOUT = 1000 * 60 * 10;
    var TIMEOUT = 1000 * 60 * 60;

    def note = //format: OFF         
"Caching            " + DO_CACHE + "\n" + 
"Skip primordial    " + SKIP_PRIMORDIAL + "\n" + 
"Interpret kill     " + INTERPRET_KILL + "\n" + 
"Unsafe kill        " + UNSAFE_KILL + "\n" +
"Ignore wait()      " + IGNORE_WAIT + "\n" + 
"No exceptions      " + NO_EXCEPTIONS + "\n" + 
"Iterable analysis  " + ITERABLE_ANALYSIS + "\n" + 
"Thread contexts    " + THREAD_CONTEXTS + "\n" + 
("XSB timeout        %d.%03ds".format(XSB_TIMEOUT/1000,XSB_TIMEOUT%1000))  + "\n" +
("Timeout            %d.%03ds".format(TIMEOUT/1000,TIMEOUT%1000))
    //format: ON

    def setting(app: String, cfgFile: String) = {
        val nwl = NEW_WORKSPACE_LOC
        import java.io.File

import edu.kit.joana.ifc.wala.sdpn.benchmark.Benchmark;
import edu.kit.joana.ifc.wala.sdpn.benchmark.GiffhornSuite;
        Setting(
            new File(nwl + "/" + app + "/jSDG/" + cfgFile),
            new File(nwl),
            new File(outputDir, "jSDG"),
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
    }
    var mains =
        ("Java Grande", "section1.JGFBarrierBench.cfg") :: //ca. 175 LOC
            ("Java Grande", "section1.JGFForkJoinBench.cfg") :: //ca. 133 LOC // no edges checked
            ("Java Grande", "section1.JGFSyncBench.cfg") :: //ca. 177 LOC
            ("Java Grande", "section2.JGFCryptBenchSizeC.cfg") :: //ca. 221 LOC
            ("Java Grande", "section2.JGFLUFactBenchSizeC.cfg") :: //ca. 503 LOC
            ("Java Grande", "section2.JGFSeriesBenchSizeC.cfg") :: //ca. 176 LOC
            ("Java Grande", "section2.JGFSORBenchSizeC.cfg") ::	//ca. 185 LOC
            ("Java Grande", "section2.JGFSparseMatmultBenchSizeC.cfg") :: //ca. 186 LOC
            ("Java Grande", "section3.JGFMolDynBenchSizeB.cfg") ::	//ca. 531 LOC // Timeout //Checked 3115 edges
            ("Java Grande", "section3.JGFMonteCarloBenchSizeB.cfg") :: //ca. 1176 LOC
            ("Java Grande", "section3.JGFRayTracerBenchSizeB.cfg") :: //ca. 697 LOC
            ("Tests", "conc.ac.AlarmClock.cfg") :: //ca. 187 LOC
            ("Tests", "conc.cliser.dt.Main.cfg") :: //ca. 963 LOC
            ("Tests", "conc.lg.LaplaceGrid.cfg") :: //ca. 175 LOC // StackOverflowError in WALA CallGraph
            ("Tests", "conc.sq.SharedQueue.cfg") :: //ca. 357 LOC
            ("Tests", "conc.TimeTravel.cfg") :: //ca. 29 LOC
            ("Tests", "tests.FixedProbChannel.cfg") :: //ca. 125 LOC
            ("Tests", "tests.Mantel00Page10.cfg") :: //ca. 122 LOC
            ("Tests", "tests.PasswordFile.cfg") :: //ca. 20 LOC // no edges checked
            ("Tests", "tests.ProbChannel.cfg") :: //ca. 87 LOC // IllegalStateException
            ("Tests", "tests.ProbPasswordFile.cfg") ::  //ca. 37 LOC // no edges checked
            ("Tests", "tests.VolpanoSmith98Page3.cfg") :: //ca. 64 LOC
            ("Tests 1.5", "conc.auto.EnvDriver.cfg") ::// ca. 2677 LOC
            ("Tests 1.5", "seq.ami.Shell.cfg") :: //ca. 678 LOC // no edges checked 
            ("Tests 1.5", "seq.dijkstra.Shell.cfg") :: //ca. 209 LOC // no edges checked 
            ("Tests 1.5", "seq.dinic.Dinic.cfg") :: //ca. 636 LOC // no edges checked 
            ("Tests 1.5", "seq.enigma.Shell.cfg") :: //ca. 192 LOC // no edges checked 
            ("Tests 1.5", "seq.matrix.Shell.cfg") :: //ca. 305 LOC // no edges checked 
            ("Tests 1.5", "seq.sudoku.Sudoku.cfg") :: //ca. 604 LOC // no edges checked 
            ("BluetoothLogger", "MainEmulator.cfg") :: //ca. 279 LOC //checked 14
            ("CellSafe", "cellsafe.MainEmulator.cfg") :: //ca. 6252 LOC // Timeout while checking //Checked 1709 edges
            ("GoldenSMS", "pt.uminho.msm.goldensms.midlet.MessageEmulator.cfg") :: //ca. 3099 LOC //Checked 5
            ("Guitar", "it.denzosoft.denzoGuitarSoft.midp.MainEmulator.cfg") :: //ca. 1269 LOC // No edges checked
            ("Hyper-M", "edu.hit.nus.can.gui.MainEmulator.cfg") ::  //ca. 2683 LOC // rem 3 edges // ran debug code for this
            ("J2MESafe", "MainEmulator.cfg") :: //ca. 2262 LOC // No edges checked
            ("JRemCntl", "Emulator.cfg") :: //ca. 16465 LOC // Wala Error model missing
            ("KeePassJ2ME", "MainEmulator.cfg") :: //ca. 4984 LOC // Timeout //Checked 1695 edges
            ("maza", "Sergi.MainEmulator.cfg") :: //ca. 1888 LOC // no edges checked
            ("MobileCube", "mobileCube.MainEmulator.cfg") :://ca. 5009 LOC // no edges checked
            ("MobilePodcast", "com.ma.j2mepodcast.ui.MainEmulator.cfg") :: //ca. 3505 LOC
            ("OneTimePass", "MainEmulator.cfg") :: //ca. 1279 LOC // Wala Error model missing
            ("Barcode", "MainEmulator.cfg") :: //ca. 3462 LOC // jSDG takes very long and prints java.lang.NegativeArraySizeException
            ("bExplore", "MainEmulator.cfg") :: //ca. 5634 LOC // Timeout // Checked 80 edges 
            ("VNC", "tk.wetnet.j2me.vnc.MainEmulator.cfg") :: //ca. 4078 LOC // Wala Error model missing
            Nil

    def settings = mains.map { case (x, y) => setting(x, y) } collect settingFilter

    var settingFilter: PartialFunction[Setting, Setting] = { case x: Setting => {x.jsdgConf.logLevel = Log.LogLevel.WARN;x} }

    def main(args: Array[String]) {
        Benchmark.runOn(this)
    }

}
object RunGS_NTC_SAFE {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/ntc_safe"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = false;
        s.IGNORE_WAIT = false
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = false
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}
object RunGS_NTC_UNSAFE_NWAIT {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/ntc_unsafe_nwait"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = false
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}
object RunGS_NTC_UNSAFE_NWAIT_NEX {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/ntc_unsafe_nwait_nex"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = true
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = false
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}
object RunGS_TC_SAFE {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/tc_safe"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = false;
        s.IGNORE_WAIT = false
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}
object RunGS_TC_UNSAFE_NWAIT {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/tc_unsafe_nwait"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}
object RunGS_TC_UNSAFE_NWAIT_NEX {
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/tc_unsafe_nwait_nex"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = true
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        Benchmark.runOn(s)
    }
}

object RunGS_VANILLA_TC_UNSAFE_NWAIT_NEX {
    import edu.kit.joana.deprecated.jsdg.SDGFactory
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/vanilla_tc_unsafe_nwait_nex"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = true
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        s.settingFilter = {
            case st: Setting =>
                st.jsdgConf.pointsTo = SDGFactory.Config.PointsToType.VANILLA_ZERO_ONE_CFA
                st.jsdgConf.objTree = SDGFactory.Config.ObjTreeType.PTS_GRAPH;
                st.jsdgConf.logLevel = Log.LogLevel.WARN
                st
        }
        Benchmark.runOn(s)
    }
}
object RunGS_VANILLA_TC_UNSAFE_NWAIT {
    import edu.kit.joana.deprecated.jsdg.SDGFactory
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/vanilla_tc_unsafe_nwait"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = true;
        s.IGNORE_WAIT = true
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        s.settingFilter = {
            case st: Setting =>
                st.jsdgConf.pointsTo = SDGFactory.Config.PointsToType.VANILLA_ZERO_ONE_CFA
                st.jsdgConf.objTree = SDGFactory.Config.ObjTreeType.PTS_GRAPH;
                st.jsdgConf.logLevel = Log.LogLevel.WARN
                st
        }
        Benchmark.runOn(s)
    }
}
object RunGS_VANILLA_TC_SAFE {
import edu.kit.joana.deprecated.jsdg.SDGFactory
    def main(args: Array[String]) {
        val s = GiffhornSuite
        s.outputDir = "results/giffhorn/vanilla_tc_safe"
        s.INTERPRET_KILL = true;
        s.UNSAFE_KILL = false;
        s.IGNORE_WAIT = false
        s.NO_EXCEPTIONS = false
        s.ITERABLE_ANALYSIS = true
        s.THREAD_CONTEXTS = true
        s.XSB_TIMEOUT = 1000 * 60 * 10;
        s.TIMEOUT = 1000 * 60 * 60;
        s.settingFilter = {
            case st: Setting =>
                st.jsdgConf.pointsTo = SDGFactory.Config.PointsToType.VANILLA_ZERO_ONE_CFA
                st.jsdgConf.objTree = SDGFactory.Config.ObjTreeType.PTS_GRAPH;
                st.jsdgConf.logLevel = Log.LogLevel.WARN
                st
        }
        Benchmark.runOn(s)
    }
}
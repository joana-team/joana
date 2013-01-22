package edu.kit.joana.ifc.wala.sdpn.benchmark

import edu.kit.joana.deprecated.jsdg.SDGFactory
import scala.collection.JavaConversions._
import java.io.File
import edu.kit.joana.deprecated.jsdg.util.Log

//final String bin, final String main, final String runtimeLib,final boolean do_cache, final boolean skip_primordial, final boolean interpret_kill, final long xsbtimeout
case class Setting(
        jsdgConf: SDGFactory.Config,
        do_cache: Boolean,
        skip_primordial: Boolean,
        interpret_kill: Boolean,
        unsafe_kill: Boolean,
        ignore_wait: Boolean,
        no_exceptions: Boolean,
        iterable_analysis: Boolean,
        thread_contexts: Boolean,
        xsb_timeout: Long,
        timeout: Long) {
    def makeString = {
        "Main:              " + main + "\n" +
            "Classpath:         " + classpath + "\n" +
            "Scope:             " + scope + "\n" +
            "Caching:           " + do_cache + "\n" +
            "No Pimordial:      " + skip_primordial + "\n" +
            "Killings:          " + interpret_kill + "\n" +
            "Unsafe killings:   " + unsafe_kill + "\n" +
            "Ignore wait():     " + ignore_wait + "\n" +
            "No exceptions:     " + no_exceptions + "\n" +
            "Iterable analysis: " + iterable_analysis + "\n" +
            "Thread contexts:   " + iterable_analysis + "\n" +
            "XSB timeout:       " + secs(xsb_timeout) + "\n" +
            "Timeout:           " + secs(timeout) + "\n" +
            "--- jSDG conf ---\n" +
            jsdgConf
    }

    def main = if (jsdgConf != null) { jsdgConf.mainClass } else null

    def scope = if (jsdgConf != null) { jsdgConf.scopeData.mkString("  ,  ") } else null

    def classpath = if (jsdgConf != null) { jsdgConf.classpath } else null

    def secs(x: Long) = "%ds".format(x / 1000)
}
object Setting {
    def apply(
        bin: String,
        main: String,
        runtimeLib: String,
        do_cache: Boolean,
        skip_primordial: Boolean,
        interpret_kill: Boolean,
        unsafe_kill: Boolean,
        ignore_wait: Boolean,
        no_exceptions: Boolean,
        iterable_analysis: Boolean,
        thread_contexts: Boolean,
        xsb_timeout: Long,
        timeout: Long): Setting =
        {
            new Setting(
                defaultConfig(bin, main, runtimeLib),
                do_cache,
                skip_primordial,
                interpret_kill,
                unsafe_kill,
                ignore_wait,
                no_exceptions,
                iterable_analysis,
                thread_contexts,
                xsb_timeout,
                timeout
            )

        }

    def apply(
        jsdgConfFile: File,
        new_workspace_loc: File,
        output_dir: File,
        do_cache: Boolean,
        skip_primordial: Boolean,
        interpret_kill: Boolean,
        unsafe_kill: Boolean,
        ignore_wait: Boolean,
        no_exceptions: Boolean,
        iterable_analysis: Boolean,
        thread_contexts: Boolean,
        xsb_timeout: Long,
        timeout: Long): Setting =
        apply(
            jsdgConfFile: File,
            List(new File("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-EclipseApplication"),
                new File("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration")
            ),
            new_workspace_loc: File,
            output_dir: File,
            do_cache: Boolean,
            skip_primordial: Boolean,
            interpret_kill: Boolean,
            unsafe_kill: Boolean,
            ignore_wait: Boolean,
            no_exceptions: Boolean,
            iterable_analysis: Boolean,
            thread_contexts: Boolean,
            xsb_timeout: Long,
            timeout: Long)

    def apply(
        jsdgConfFile: File,
        new_workspace_loc: File,
        do_cache: Boolean,
        skip_primordial: Boolean,
        interpret_kill: Boolean,
        unsafe_kill: Boolean,
        ignore_wait: Boolean,
        no_exceptions: Boolean,
        iterable_analysis: Boolean,
        thread_contexts: Boolean,
        xsb_timeout: Long,
        timeout: Long): Setting =
        apply(
            jsdgConfFile: File,
            new_workspace_loc: File,
            new File("/tmp"),
            do_cache: Boolean,
            skip_primordial: Boolean,
            interpret_kill: Boolean,
            unsafe_kill: Boolean,
            ignore_wait: Boolean,
            no_exceptions: Boolean,
            iterable_analysis: Boolean,
            thread_contexts: Boolean,
            xsb_timeout: Long,
            timeout: Long)

    def apply(
        jsdgConfFile: File,
        old_workspace_locs: List[File],
        new_workspace_loc: File,
        output_dir: File,
        do_cache: Boolean,
        skip_primordial: Boolean,
        interpret_kill: Boolean,
        unsafe_kill: Boolean,
        ignore_wait: Boolean,
        no_exceptions: Boolean,
        iterable_analysis: Boolean,
        thread_contexts: Boolean,
        xsb_timeout: Long,
        timeout: Long): Setting =
        {
            import java.io._

            val cfg = SDGFactory.Config.readFrom(new BufferedInputStream(new FileInputStream(jsdgConfFile)))
            val nwl = new_workspace_loc.getPath + "/"
            val od = output_dir.getPath + "/"

            for (old_workspace_loc <- old_workspace_locs) {
                val owl = old_workspace_loc.getPath + "/"
                cfg.classpath = cfg.classpath.replace(owl, nwl)
                cfg.scopeData = cfg.scopeData.map(_.replace(owl, nwl))
                cfg.nativesXML = cfg.nativesXML.replace(owl, nwl)
                cfg.logFile = cfg.logFile.replace(owl, od)
                cfg.outputDir = cfg.outputDir.replace(owl, od)
                cfg.outputSDGfile = cfg.outputSDGfile.replace(owl, od)
            }

            new Setting(
                cfg,
                do_cache,
                skip_primordial,
                interpret_kill,
                unsafe_kill,
                ignore_wait,
                no_exceptions,
                iterable_analysis,
                thread_contexts,
                xsb_timeout,
                timeout
            )

        }

    def defaultConfig(bin: String, main: String, runtimeLib: String): SDGFactory.Config = {

        val mainClassSimpleName = main.replace('/', '.').replace('$', '.').substring(1);
        val cfg = new SDGFactory.Config();
        cfg.addControlFlow = true;
        cfg.classpath = bin;
        cfg.mainClass = main;
        cfg.computeInterference = true;
        cfg.computeSummaryEdges = true;
        cfg.ignoreExceptions = false;
        cfg.exclusions = new java.util.LinkedList[String]();
        cfg.exclusions.add("java/awt/.*");
        cfg.exclusions.add("java/security/.*");
        cfg.exclusions.add("javax/swing/.*");
        cfg.exclusions.add("sun/awt/.*");
        cfg.exclusions.add("sun/swing/.*");
        cfg.exclusions.add("com/sun/.*");
        cfg.exclusions.add("sun/.*");
        cfg.logFile = mainClassSimpleName + ".log";
        cfg.outputDir = "./";
        cfg.outputSDGfile = mainClassSimpleName + ".pdg";
        cfg.logLevel = Log.LogLevel.INFO;
        cfg.pointsTo = SDGFactory.Config.PointsToType.VANILLA_ZERO_ONE_CFA;
        //		cfg.pointsTo = PointsToType.OBJ_SENS;
        cfg.objTree = SDGFactory.Config.ObjTreeType.PTS_GRAPH;
        cfg.useWalaSdg = false;
        cfg.scopeData = new java.util.LinkedList[String]();
        //		String[] stdlibs = getJarsInDirectory(j2seLibs); 		
        //		for (String lib: stdlibs) {
        //			cfg.scopeData.add("Primordial,Java,jarFile," + lib);
        //			System.out.println("adding library to scope: " + lib);
        //		}
        //		cfg.scopeData.add("Primordial,Java,stdlib,none");
        cfg.scopeData.add(runtimeLib);
        cfg.nativesXML = "../jSDG/lib/natives_empty.xml";
        cfg

    }

}


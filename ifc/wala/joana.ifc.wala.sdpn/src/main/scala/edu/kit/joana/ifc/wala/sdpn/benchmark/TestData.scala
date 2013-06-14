/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn.benchmark

object TestData {
    val giffhornSuite = Map(
        "Java Grande/section1/JGFBarrierBench" -> TestApp(
            "Lsection1/JGFBarrierBench",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section1/JGFForkJoinBench" -> TestApp(
            "Lsection1/JGFForkJoinBench",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section1/JGFSyncBench" -> TestApp(
            "Lsection1/JGFSyncBench",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section2/JGFCryptBenchSizeC" -> TestApp(
            "Lsection2/JGFCryptBenchSizeC",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section2/JGFLUFactBenchSizeC" -> TestApp(
            "Lsection2/JGFLUFactBenchSizeC",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section2/JGFSeriesBenchSizeC" -> TestApp(
            "Lsection2/JGFSeriesBenchSizeC",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section2/JGFSORBenchSizeC" -> TestApp(
            "Lsection2/JGFSORBenchSizeC",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section2/JGFSparseMatmultBenchSizeC" -> TestApp(
            "Lsection2/JGFSparseMatmultBenchSizeC",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section3/JGFMolDynBenchSizeB" -> TestApp(
            "Lsection3/JGFMolDynBenchSizeB",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section3/JGFMonteCarloBenchSizeB" -> TestApp(
            "Lsection3/JGFMonteCarloBenchSizeB",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Java Grande/section3/JGFRayTracerBenchSizeB" -> TestApp(
            "Lsection3/JGFRayTracerBenchSizeB",
            "../../giffhorn-suite/runtime-EclipseApplication/Java Grande/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Java Grande/jSDG/lib/primordial.jar.model")),
        "Tests/conc/ac/AlarmClock" -> TestApp(
            "Lconc/ac/AlarmClock",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/conc/cliser/dt/Main" -> TestApp(
            "Lconc/cliser/dt/Main",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/conc/lg/LaplaceGrid" -> TestApp(
            "Lconc/lg/LaplaceGrid",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/conc/sq/SharedQueue" -> TestApp(
            "Lconc/sq/SharedQueue",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/conc/TimeTravel" -> TestApp(
            "Lconc/TimeTravel",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/tests/FixedProbChannel" -> TestApp(
            "Ltests/FixedProbChannel",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/tests/Mantel00Page10" -> TestApp(
            "Ltests/Mantel00Page10",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/tests/PasswordFile" -> TestApp(
            "Ltests/PasswordFile",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/tests/ProbChannel" -> TestApp(
            "Ltests/ProbChannel",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model",
                "Primordial,Java,stdlib,none")),
        "Tests/tests/ProbPasswordFile" -> TestApp(
            "Ltests/ProbPasswordFile",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests/tests/VolpanoSmith98Page3" -> TestApp(
            "Ltests/VolpanoSmith98Page3",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/conc/auto/EnvDriver" -> TestApp(
            "Lconc/auto/EnvDriver",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/ami/Shell" -> TestApp(
            "Lseq/ami/Shell",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/dijkstra/Shell" -> TestApp(
            "Lseq/dijkstra/Shell",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/dinic/Dinic" -> TestApp(
            "Lseq/dinic/Dinic",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/enigma/Shell" -> TestApp(
            "Lseq/enigma/Shell",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/matrix/Shell" -> TestApp(
            "Lseq/matrix/Shell",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "Tests 1.5/seq/sudoku/Sudoku" -> TestApp(
            "Lseq/sudoku/Sudoku",
            "../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Tests 1.5/jSDG/lib/primordial.jar.model")),
        "BluetoothLogger/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/BluetoothLogger/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/BluetoothLogger/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/BluetoothLogger/jSDG/lib/primordial.jar.model")),
        "CellSafe/cellsafe/MainEmulator" -> TestApp(
            "Lcellsafe/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/CellSafe/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/CellSafe/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/CellSafe/jSDG/lib/primordial.jar.model")),
        "GoldenSMS/pt/uminho/msm/goldensms/midlet/MessageEmulator" -> TestApp(
            "Lpt/uminho/msm/goldensms/midlet/MessageEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/GoldenSMS/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/GoldenSMS/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/GoldenSMS/jSDG/lib/primordial.jar.model")),
        "Guitar/it/denzosoft/denzoGuitarSoft/midp/MainEmulator" -> TestApp(
            "Lit/denzosoft/denzoGuitarSoft/midp/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/Guitar/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Guitar/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Guitar/jSDG/lib/primordial.jar.model")),
        "Hyper-M/edu/hit/nus/can/gui/MainEmulator" -> TestApp(
            "Ledu/hit/nus/can/gui/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/Hyper-M/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Hyper-M/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Hyper-M/jSDG/lib/primordial.jar.model")),
        "J2MESafe/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/J2MESafe/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/J2MESafe/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/J2MESafe/jSDG/lib/primordial.jar.model")),
        "JRemCntl/Emulator" -> TestApp(
            "LEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/JRemCntl/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/JRemCntl/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/JRemCntl/jSDG/lib/primordial.jar.model")),
        "KeePassJ2ME/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/KeePassJ2ME/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/KeePassJ2ME/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/KeePassJ2ME/jSDG/lib/primordial.jar.model")),
        "maza/Sergi/MainEmulator" -> TestApp(
            "LSergi/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/maza/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/maza/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/maza/jSDG/lib/primordial.jar.model")),
        "MobileCube/mobileCube/MainEmulator" -> TestApp(
            "LmobileCube/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/MobileCube/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/MobileCube/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/MobileCube/jSDG/lib/primordial.jar.model")),
        "MobilePodcast/com/ma/j2mepodcast/ui/MainEmulator" -> TestApp(
            "Lcom/ma/j2mepodcast/ui/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/MobilePodcast/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/MobilePodcast/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/MobilePodcast/jSDG/lib/primordial.jar.model")),
        "OneTimePass/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/OneTimePass/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/OneTimePass/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/OneTimePass/jSDG/lib/primordial.jar.model")),
        "Barcode/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/Barcode/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Barcode/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/Barcode/jSDG/lib/primordial.jar.model")),
        "bExplore/MainEmulator" -> TestApp(
            "LMainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/bExplore/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/bExplore/jSDG/lib/jSDG-stubs-j2me2.0.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/bExplore/jSDG/lib/primordial.jar.model")),
        "VNC/tk/wetnet/j2me/vnc/MainEmulator" -> TestApp(
            "Ltk/wetnet/j2me/vnc/MainEmulator",
            "../../giffhorn-suite/runtime-EclipseApplication/VNC/bin",
            List(
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/VNC/jSDG/lib/jSDG-stubs-jre1.4.jar",
                "Primordial,Java,jarFile,../../giffhorn-suite/runtime-EclipseApplication/VNC/jSDG/lib/primordial.jar.model"))
    )

    val examplesSuite = Map(
        "examples/A" -> TestApp(
            "Lexamples/A",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/B" -> TestApp(
            "Lexamples/B",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/C" -> TestApp(
            "Lexamples/C",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/CutTest" -> TestApp(
            "Lexamples/CutTest",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP01" -> TestApp(
            "Lexamples/BSP01",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP02" -> TestApp(
            "Lexamples/BSP02",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP03" -> TestApp(
            "Lexamples/BSP03",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP04" -> TestApp(
            "Lexamples/BSP04",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP05" -> TestApp(
            "Lexamples/BSP05",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/BSP06" -> TestApp(
            "Lexamples/BSP06",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/MyThread" -> TestApp(
            "Lexamples/MyThread",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/A" -> TestApp(
            "Lexamples/testdata/A",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/B" -> TestApp(
            "Lexamples/testdata/B",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/C" -> TestApp(
            "Lexamples/testdata/C",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/D" -> TestApp(
            "Lexamples/testdata/D",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Killing01" -> TestApp(
            "Lexamples/testdata/Killing01",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Killing02" -> TestApp(
            "Lexamples/testdata/Killing02",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Killing03" -> TestApp(
            "Lexamples/testdata/Killing03",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Killing04" -> TestApp(
            "Lexamples/testdata/Killing04",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Wait01" -> TestApp(
            "Lexamples/testdata/Wait01",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar")),
        "examples/testdata/Test01" -> TestApp(
            "Lexamples/testdata/Test01",
            "bin",
            List(
                "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar"))
    )

}

object PrintSuite {
    def main(args: Array[String]) {
        var usedNames = Set[String]()
        var first = true
        val settings = ExamplesSuite.settings
        println("Map(")
        for (x <- settings) {
            val conf = x.jsdgConf
            if (!first) { println(",") }
            first = false
            import scala.collection.JavaConversions._
            val dir = conf.classpath //.split("/").dropRight(1).last
            val mc = conf.mainClass.drop(1)
            println("  \"" /* +dir + "/" */ + mc + "\" -> TestApp(")
            println("    \"" + conf.mainClass + "\",")
            println("    \"" + conf.classpath + "\",")
            println(conf.scopeData.mkString("    List(\n      \"", "\",\n      \"", "\"))"))

        }
        println(")")
    }

}
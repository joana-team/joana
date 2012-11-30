name := "jSDG-sdpn"

version := "1.0"

organization := "de.wwu"

scalaVersion := "2.9.2"

parallelExecution in Test := false

fork in run := true

javaOptions in run += "-Xmx6G"

javaOptions in run += "-XX:MaxPermSize=521M"

//ivyXML :=
//  <resolvers>
//        <ibiblio name="Local Maven Repository" changingPattern="*-SNAPSHOT" m2compatible="true" root="~/.m2"/>
//  </resolvers>

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies += "de.wwu" %% "sdpn.core" % "1.0-SNAPSHOT"

libraryDependencies += "de.wwu" %% "sdpn.wala" % "1.0-SNAPSHOT"

libraryDependencies += "org.eclipse.core" % "runtime" % "[3.5.0,)"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" //% "test"
scalaVersion := "2.9.2"

name := "NetLogo"

onLoadMessage := ""

resourceDirectory in Compile <<= baseDirectory(_ / "resources")

scalacOptions ++=
  "-deprecation -unchecked -Xfatal-warnings -Xcheckinit -encoding us-ascii"
  .split(" ").toSeq

javacOptions ++=
  "-bootclasspath dist/java5/classes.jar:dist/java5/ui.jar -g -deprecation -encoding us-ascii -Werror -Xlint:all -Xlint:-serial -Xlint:-fallthrough -Xlint:-path -source 1.5 -target 1.5"
  .split(" ").toSeq

// only log problems plz
ivyLoggingLevel := UpdateLogging.Quiet

// this makes jar-building and script-writing easier
retrieveManaged := true

// we're not cross-building for different Scala versions
crossPaths := false

scalaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

javaSource in Compile <<= baseDirectory(_ / "src" / "main")

javaSource in Test <<= baseDirectory(_ / "src" / "test")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "src" / "tools")

unmanagedResourceDirectories in Compile <+= baseDirectory { _ / "resources" }

mainClass in (Compile, run) := Some("org.nlogo.app.App")

mainClass in (Compile, packageBin) := Some("org.nlogo.app.App")

sourceGenerators in Compile <+= Autogen.sourceGeneratorTask

resourceGenerators in Compile <+= I18n.resourceGeneratorTask

Extensions.extensionsTask

InfoTab.infoTabTask

ModelIndex.modelIndexTask

NativeLibs.nativeLibsTask

Depend.dependTask

threed := { System.setProperty("org.nlogo.is3d", "true") }

nogen  := { System.setProperty("org.nlogo.noGenerator", "true") }

moduleConfigurations += ModuleConfiguration("javax.media", JavaNet2Repository)

netlogoVersion <<= (testLoader in Test) map {
  _.loadClass("org.nlogo.api.Version")
   .getMethod("version")
   .invoke(null).asInstanceOf[String]
   .replaceFirst("NetLogo ", "")
}

scalacOptions in (Compile, doc) <++= (baseDirectory, netlogoVersion) map {
  (base, version) =>
    Seq("-encoding", "us-ascii") ++
    Opts.doc.title("NetLogo") ++
    Opts.doc.version(version) ++
    Opts.doc.sourceUrl("https://github.com/NetLogo/NetLogo/blob/" +
                       version + "/src/main€{FILE_PATH}.scala")
}

// compensate for issues.scala-lang.org/browse/SI-5388
doc in Compile ~= NetLogoBuild.mungeScaladocSourceUrls

// The regular doc task includes doc for the entire main source tree.  But for bundling with the
// User Manual, in docs/scaladoc/, we want to document only select classes.  So I copy and pasted
// the code for the main doc task and tweaked it. - ST 6/29/12, 7/18/12
// sureiscute.com/images/cutepictures/I_Have_No_Idea_What_I_m_Doing.jpg
docSmaller <<= (baseDirectory, cacheDirectory, scalacOptions in (Compile, doc), compileInputs in Compile, netlogoVersion, streams) map {
  (base, cache, options, inputs, version, s) =>
    val apiSources = Seq(
      "app/App.scala", "headless/HeadlessWorkspace.scala",
      "lite/InterfaceComponent.scala", "lite/Applet.scala", "lite/AppletPanel.scala",
      "api/", "agent/", "workspace/", "nvm/")
    val sourceFilter: File => Boolean = path =>
      apiSources.exists(ok => path.toString.containsSlice("src/main/org/nlogo/" + ok))
    val out = base / "docs" / "scaladoc"
    Doc(inputs.config.maxErrors, inputs.compilers.scalac)
      .cached(cache / "docSmaller", "NetLogo",
              inputs.config.sources.filter(sourceFilter),
              inputs.config.classpath, out, options, s.log)
    NetLogoBuild.mungeScaladocSourceUrls(out)
  }

libraryDependencies ++= Seq(
  "asm" % "asm-all" % "3.3.1",
  "org.picocontainer" % "picocontainer" % "2.13.6",
  "log4j" % "log4j" % "1.2.16",
  "javax.media" % "jmf" % "2.1.1e",
  "org.pegdown" % "pegdown" % "1.1.0",
  "org.parboiled" % "parboiled-java" % "1.0.2",
  "steveroy" % "mrjadapter" % "1.2" from "http://ccl.northwestern.edu/devel/mrjadapter-1.2.jar",
  "org.jhotdraw" % "jhotdraw" % "6.0b1" from "http://ccl.northwestern.edu/devel/jhotdraw-6.0b1.jar",
  "ch.randelshofer" % "quaqua" % "7.3.4" from "http://ccl.northwestern.edu/devel/quaqua-7.3.4.jar",
  "ch.randelshofer" % "swing-layout" % "7.3.4" from "http://ccl.northwestern.edu/devel/swing-layout-7.3.4.jar",
  "org.jogl" % "jogl" % "1.1.1" from "http://ccl.northwestern.edu/devel/jogl-1.1.1.jar",
  "org.gluegen-rt" % "gluegen-rt" % "1.1.1" from "http://ccl.northwestern.edu/devel/gluegen-rt-1.1.1.jar",
  "org.jmock" % "jmock" % "2.5.1" % "test",
  "org.jmock" % "jmock-legacy" % "2.5.1" % "test",
  "org.jmock" % "jmock-junit4" % "2.5.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
  "org.scalatest" %% "scalatest" % "1.8" % "test"
)

all <<= (baseDirectory, streams) map { (base, s) =>
  s.log.info("making resources/system/dict.txt and docs/dict folder")
  IO.delete(base / "docs" / "dict")
  Process("python bin/dictsplit.py").!!
}

all <<= all.dependsOn(
  Extensions.extensions,
  NativeLibs.nativeLibs,
  ModelIndex.modelIndex,
  InfoTab.infoTab)

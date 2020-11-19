import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}
lazy val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

lazy val `scalaprops-shapeless` =
  project.in(file(".")).aggregate(coreJVM, coreJS, testJVM, testJS).settings(commonSettings).settings(noPublishSettings)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .settings(
    name := coreName,
    moduleName := coreName,
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.github.scalaprops" %%% "scalaprops-core" % scalapropsVersion.value
    )
  )
  .jsSettings(
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalaprops/scalaprops-shapeless/" + tagOrHash.value
      s"-P:scalajs:mapSourceURI:$a->$g/"
    },
    scalaJSStage in Test := FastOptStage
  )
  .nativeSettings(
    crossScalaVersions := Scala211 :: Nil
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val test = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    libraryDependencies += "com.github.scalaprops" %%% "scalaprops" % scalapropsVersion.value % "test"
  )
  .settings(noPublishSettings)
  .jsSettings(
    scalaJSStage in Test := FastOptStage
  )
  .nativeSettings(
    crossScalaVersions := Scala211 :: Nil,
    scalapropsNativeSettings
  )

lazy val testJVM = test.jvm
lazy val testJS = test.js
lazy val testNative = test.native

lazy val coreName = "scalaprops-shapeless"

def Scala211 = "2.11.12"

lazy val commonSettings = Seq(
  scalaVersion := Scala211,
  crossScalaVersions := Scala211 :: "2.12.12" :: "2.13.4" :: Nil,
  publishTo := sonatypePublishToBundle.value,
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  commands += Command.command("updateReadme")(updateReadmeTask),
  organization := "com.github.scalaprops",
  scalapropsVersion := "0.8.1"
) ++ compileSettings ++ publishSettings ++ scalapropsCoreSettings

lazy val unusedWarnings = Seq("-Ywarn-unused")

lazy val compileSettings = Seq(
  scalacOptions in (Compile, doc) ++= {
    val tag = tagOrHash.value
    Seq(
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalaprops/scalaprops-shapeless/tree/${tag}€{FILE_PATH}.scala"
    )
  },
  scalacOptions ++= unusedWarnings,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Yno-adapted-args")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-Xfuture",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions"
  )
) ++ Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/scalaprops/scalaprops-shapeless")),
  licenses := Seq(
    "Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalaprops/scalaprops-shapeless.git"),
      "scm:git:github.com/scalaprops/scalaprops-shapeless.git",
      Some("scm:git:git@github.com:scalaprops/scalaprops-shapeless.git")
    )
  ),
  developers := List(
    Developer(
      "xuwei-k",
      "Kenji Yoshida",
      "",
      url("https://github.com/xuwei-k")
    )
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  credentials ++= {
    Seq("SONATYPE_USER", "SONATYPE_PASSWORD").map(sys.env.get) match {
      case Seq(Some(user), Some(pass)) =>
        Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
      case _ =>
        Seq()
    }
  }
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishArtifact := false
)

lazy val updateReadmeTask = { state: State =>
  val extracted = Project.extract(state)
  val v = extracted get version
  val org = extracted get organization
  val modules = coreName :: Nil
  val snapshotOrRelease = if (extracted get isSnapshot) "snapshots" else "releases"
  val readme = "README.md"
  val readmeFile = file(readme)
  val newReadme = Predef
    .augmentString(IO.read(readmeFile))
    .lines
    .map { line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      if (line.startsWith("libraryDependencies") && matchReleaseOrSnapshot) {
        val i = modules.map("\"" + _ + "\"").indexWhere(line.contains)
        s"""libraryDependencies += "$org" %% "${modules(i)}" % "$v""""
      } else line
    }
    .mkString("", "\n", "\n")
  IO.write(readmeFile, newReadme)
  val git = new sbtrelease.Git(extracted get baseDirectory)
  git.add(readme) ! state.log
  git.commit(message = "update " + readme, sign = false, signOff = false) ! state.log
  sys.process.Process("git diff HEAD^") ! state.log
  state
}

lazy val updateReadmeProcess: ReleaseStep = updateReadmeTask

import ReleaseTransformations._

val SetScala211 = releaseStepCommand("++ 2.11.12")

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  updateReadmeProcess,
  tagRelease,
  ReleaseStep(
    action = { state =>
      val extracted = Project extract state
      extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
    },
    enableCrossBuild = true
  ),
  SetScala211,
  releaseStepCommand("coreNative/publishSigned"),
  releaseStepCommandAndRemaining("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  updateReadmeProcess,
  pushChanges
)

// build.sbt shamelessly inspired by https://github.com/fthomas/refined/blob/master/build.sbt

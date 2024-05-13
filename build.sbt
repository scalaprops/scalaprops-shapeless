import sbtcrossproject.CrossPlugin.autoImport.crossProject

lazy val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}
lazy val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

commonSettings
noPublishSettings

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .settings(
    name := coreName,
    moduleName := coreName,
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq(
          "com.github.scalaprops" %%% "scalaprops-gen" % scalapropsVersion.value,
          "org.typelevel" %%% "shapeless3-deriving" % "3.4.1"
        )
      } else {
        Seq(
          "com.github.scalaprops" %%% "scalaprops-core" % scalapropsVersion.value,
          "com.chuusai" %%% "shapeless" % "2.3.10"
        )
      }
    }
  )
  .jsSettings(
    scalacOptions += {
      val a = (LocalRootProject / baseDirectory).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalaprops/scalaprops-shapeless/" + tagOrHash.value
      val key = scalaBinaryVersion.value match {
        case "3" =>
          "-scalajs-mapSourceURI"
        case _ =>
          "-P:scalajs:mapSourceURI"
      }
      s"${key}:$a->$g/"
    },
    Test / scalaJSStage := FastOptStage
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
    Test / scalaJSStage := FastOptStage
  )
  .nativeSettings(
    scalapropsNativeSettings
  )

lazy val testJVM = test.jvm
lazy val testJS = test.js
lazy val testNative = test.native

lazy val coreName = "scalaprops-shapeless"

def Scala212 = "2.12.19"
def Scala3 = "3.4.2"

lazy val commonSettings = Def.settings(
  scalaVersion := Scala212,
  crossScalaVersions := Scala212 :: "2.13.14" :: Scala3 :: Nil,
  publishTo := sonatypePublishToBundle.value,
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  commands += Command.command("updateReadme")(updateReadmeTask),
  organization := "com.github.scalaprops",
  scalapropsVersion := "0.9.1"
) ++ compileSettings ++ publishSettings ++ scalapropsCoreSettings

lazy val unusedWarnings = Seq("-Ywarn-unused")

lazy val compileSettings = Seq(
  (Compile / doc / scalacOptions) ++= {
    val tag = tagOrHash.value
    Seq(
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalaprops/scalaprops-shapeless/tree/${tag}€{FILE_PATH}.scala"
    )
  },
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Nil
    } else {
      unusedWarnings ++ Seq(
        "-Xlint",
        "-Xfuture"
      )
    }
  },
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
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions"
  )
) ++ Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings)

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
      extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
    },
    enableCrossBuild = true
  ),
  releaseStepCommandAndRemaining("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  updateReadmeProcess,
  pushChanges
)

// build.sbt shamelessly inspired by https://github.com/fthomas/refined/blob/master/build.sbt

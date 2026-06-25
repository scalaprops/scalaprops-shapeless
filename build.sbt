lazy val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}
lazy val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lazyLines_!.head
  else tagName.value
}

val scalaVersions = Seq("2.12.21", "2.13.18", "3.3.8")

val scalapropsShapelessRoot = rootProject.autoAggregate.settings(
  commonSettings,
  noPublishSettings,
  TaskKey[Unit]("testSequential") := Def.uncached(
    Def
      .sequential(
        (core.projectRefs ++ test.projectRefs).map(_ / Test / testFull)
      )
      .value
  ),
  autoScalaLibrary := false
)

lazy val core = projectMatrix
  .in(file("core"))
  .defaultAxes()
  .settings(
    commonSettings,
    name := coreName,
    moduleName := coreName,
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq(
          "com.github.scalaprops" %% "scalaprops-gen" % scalapropsVersion.value,
          "org.typelevel" %% "shapeless3-deriving" % "3.6.0"
        )
      } else {
        Seq(
          "com.github.scalaprops" %% "scalaprops-core" % scalapropsVersion.value,
          "com.chuusai" %% "shapeless" % "2.3.13"
        )
      }
    }
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings(
    )
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
    )
  )
  .jsPlatform(
    scalaVersions,
    Def.settings(
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
  )

lazy val test = projectMatrix
  .defaultAxes()
  .dependsOn(core)
  .settings(
    commonSettings,
    libraryDependencies += "com.github.scalaprops" %% "scalaprops" % scalapropsVersion.value % "test",
    noPublishSettings
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings(
    )
  )
  .jsPlatform(
    scalaVersions,
    Def.settings(
      Test / scalaJSStage := FastOptStage
    )
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
      scalapropsNativeSettings
    )
  )

lazy val coreName = "scalaprops-shapeless"

lazy val commonSettings = Def.settings(
  publishTo := (if (isSnapshot.value) None else localStaging.value),
  releaseTagName := tagName.value,
  commands += Command.command("updateReadme")(updateReadmeTask),
  organization := "com.github.scalaprops",
  scalapropsVersion := "0.11.0"
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
        "-Xlint"
      )
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Yno-adapted-args",
          "-Xsource:3",
          "-language:higherKinds",
          "-Xfuture"
        )
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-language:existentials",
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
  pomIncludeRepository := { _ => false }
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishArtifact := false
)

lazy val updateReadmeTask: State => State = { state =>
  val extracted = Project.extract(state)
  val v = extracted.get(version)
  val org = extracted.get(organization)
  val modules = coreName :: Nil
  val snapshotOrRelease = if (extracted.get(isSnapshot)) "snapshots" else "releases"
  val readme = "README.md"
  val readmeFile = file(readme)
  val newReadme = IO
    .readLines(readmeFile)
    .map { line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      if (line.startsWith("libraryDependencies") && matchReleaseOrSnapshot) {
        val i = modules.map("\"" + _ + "\"").indexWhere(line.contains)
        s"""libraryDependencies += "$org" %% "${modules(i)}" % "$v""""
      } else line
    }
    .mkString("", "\n", "\n")
  IO.write(readmeFile, newReadme)
  val git = new sbtrelease.Git(extracted.get(baseDirectory))
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
  releaseStepCommandAndRemaining(PgpKeys.publishSigned.key.label),
  releaseStepCommandAndRemaining("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  updateReadmeProcess,
  pushChanges
)

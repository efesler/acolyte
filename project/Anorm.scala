import sbt._
import Keys._

trait Anorm { deps: Dependencies ⇒
  // Dependencies
  def scalacPlugin: Project
  def jdbcScala: Project

  lazy val anorm = 
    Project(id = "anorm", base = file("anorm")).settings(
      name := "anorm",
      javacOptions in Test ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
      scalacOptions in Compile ++= Seq("-unchecked", "-deprecation"),
      scalacOptions in Compile <++= (scalaVersion in ThisBuild).map { v =>
        if (v startsWith "2.11") Seq("-Ywarn-unused-import")
        else Nil
      },
      scalacOptions in Test <++= (version in ThisBuild).
        zip(scalaVersion in ThisBuild).
        zip(baseDirectory in (scalacPlugin, Compile)).
        zip(name in (scalacPlugin, Compile)) map { d =>
          val (((v, sv), b), n) = d
          val msv = 
            if (sv startsWith "2.10") "2.10" 
            else if (sv startsWith "2.11") "2.11" 
            else sv

          val td = b / "target" / s"scala-$msv"
          val j = td / s"${n}_${msv}-$v.jar"

          Seq("-feature", "-deprecation", s"-Xplugin:${j.getAbsolutePath}")
        },
      compile in Test <<= (compile in Test).
        dependsOn(compile in (scalacPlugin, Test)), // make sure plugin is there
      libraryDependencies ++= Seq(
        "org.eu.acolyte" % "jdbc-driver" % (version in ThisBuild).value, 
        specs2Test)
    ).dependsOn(scalacPlugin, jdbcScala)

}

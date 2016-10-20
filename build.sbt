name := "heisenberg"

organization := "com.github.gigurra"

version := "0.2.8-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest"   %%  "scalatest"     % "2.2.6"       % "test",
  "org.mockito"     %   "mockito-core"  % "1.10.19"     % "test",
  "org.scala-lang"  %   "scala-reflect" % scalaVersion.value
)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)


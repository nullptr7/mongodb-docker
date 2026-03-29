addSbtPlugin("com.thesamet"  % "sbt-protoc"   % "1.0.8")
addSbtPlugin("com.eed3si9n"  % "sbt-assembly" % "2.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")
addSbtPlugin("org.typelevel" % "sbt-fs2-grpc" % "3.0.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"

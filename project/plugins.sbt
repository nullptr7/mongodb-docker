addSbtPlugin("com.thesamet"  % "sbt-protoc"   % "1.0.7")
addSbtPlugin("com.eed3si9n"  % "sbt-assembly" % "2.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")
addSbtPlugin("org.typelevel" % "sbt-fs2-grpc" % "2.7.4")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"

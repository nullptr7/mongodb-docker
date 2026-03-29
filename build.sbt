ThisBuild / name := "mongodb-grpc"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.1"

val catsVersion       = "2.13.0"
val catsEffectVersion = "3.7.0"
val mongoScalaVersion = "5.6.4"
val grpcVersion       = "1.80.0"
val scalapbVersion    = "0.11.20"

enablePlugins(fs2.grpc.codegen.Fs2Grpc)
enablePlugins(ScalafmtPlugin)

lazy val root = (project in file("."))
  .settings(
    name := "mongodb-grpc",
    version := "0.1.0",
    scalaVersion := "3.3.7",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Wunused:all",
    libraryDependencies ++= Seq(
      // Cats Effect - Functional Effects
      "org.typelevel" %% "cats-effect"        % catsEffectVersion,
      "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
      "org.typelevel" %% "cats-core"          % catsVersion,

      // MongoDB Scala Driver (works with Scala 3 via Scala 2.13 cross-build)
      "org.mongodb.scala" % "mongo-scala-driver_2.13" % mongoScalaVersion,

      // FS2 gRPC runtime (Scala 3 version)
      "org.typelevel" %% "fs2-grpc-runtime" % "3.0.0",

      // gRPC/Protobuf dependencies
      "io.grpc"               % "grpc-netty-shaded"    % grpcVersion,
      "io.grpc"               % "grpc-protobuf"        % grpcVersion,
      "io.grpc"               % "grpc-stub"            % grpcVersion,
      "com.google.protobuf"   % "protobuf-java"        % "4.34.1",
      "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion,

      // Logging
      "ch.qos.logback" % "logback-classic" % "1.5.32",
      "org.typelevel" %% "log4cats-slf4j"  % "2.8.0",

      // Scala 3 compatibility
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0"
    ),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true, flatPackage = true) -> (Compile / sourceManaged).value / "scalapb"
    ),
    assembly / assemblyJarName := "mongodb-grpc-assembly-0.1.0.jar",
    assembly / mainClass := Some("com.example.Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")         =>
        MergeStrategy.discard

      // Ignore module-info classes
      case x if x.endsWith("module-info.class")        =>
        MergeStrategy.discard

      // For conflicting MongoDB native-image metadata, just take the first one
      case PathList("META-INF", "native-image", _ @_*) =>
        MergeStrategy.first

      case x =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )

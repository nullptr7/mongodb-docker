addCommandAlias("l", "projects")
addCommandAlias("ll", "projects")
addCommandAlias("ls", "projects")
addCommandAlias("cd", "project")
addCommandAlias("root", "cd mongodb-grpc")
addCommandAlias("c", "compile")
addCommandAlias("cc", "clean; compile")
addCommandAlias("all", "reload; update; clean; compile")
addCommandAlias("ca", "Test / compile")
addCommandAlias("t", "test")
addCommandAlias("r", "run")
addCommandAlias("rs", "reStart")
addCommandAlias("s", "reStop")
addCommandAlias(
  "checkFmt",
  "scalafmtSbtCheck; scalafmtCheckAll; scalafixAll --check"
)
addCommandAlias(
  "fmtAll",
  "scalafixAll; scalafmtSbt; scalafmtAll"
)
addCommandAlias(
  "up2date",
  "reload plugins; dependencyUpdates; reload return; dependencyUpdates"
)

package com.example

import cats.effect.*
import com.example.api.UserServiceFs2Grpc
import com.example.grpc.UserServiceImpl
import com.example.services.MongoService
import fs2.grpc.syntax.all.*
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    val mongoUri = sys.env.getOrElse("MONGO_URI", "mongodb://root:password@localhost:27017")
    val grpcPort = sys.env.getOrElse("GRPC_PORT", "50051").toInt

    val app = for
      mongoService <- MongoService.make[IO](mongoUri)
      server       <- startGrpcServer(grpcPort, mongoService)
    yield server

    app
      .use(server => IO.pure(server.start()) *> IO.never)
      .handleErrorWith { e =>
        logger.error(e)(s"Failed to start gRPC server: ${e.getMessage}") *> IO.pure(ExitCode.Error)
      }

  private def startGrpcServer(port: Int, mongoService: MongoService[IO]): Resource[IO, Server] =
    for
      userServiceImpl <- Resource.eval(IO.delay(new UserServiceImpl(mongoService)))
      serviceDef      <- UserServiceFs2Grpc.bindServiceResource[IO](userServiceImpl)
      server <- NettyServerBuilder
        .forPort(port)
        .addService(serviceDef)
        .resource[IO]
      _ <- Resource.eval(logger.info(s"gRPC server started on port $port"))
    yield server

  end startGrpcServer

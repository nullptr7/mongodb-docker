package com.example.api

import cats.effect.*
import cats.effect.std.Dispatcher
import cats.syntax.all.*

import io.grpc.stub.{ServerCalls, StreamObserver}
import io.grpc.{ServerMethodDefinition, ServerServiceDefinition, ServiceDescriptor}

import org.typelevel.log4cats.Logger

trait Fs2GrpcService[F[_]]:
  def descriptor: ServiceDescriptor

  def methodDefinitions(
      dispatcher: Dispatcher[F]
  )(using Async[F], Logger[F]): List[ServerMethodDefinition[?, ?]]

object Fs2GrpcService:
  def toUnaryMethod[F[_]: Async: Logger, Req, Res](
      serviceCall: Req => F[Res],
      dispatcher:  Dispatcher[F]
  ): ServerCalls.UnaryMethod[Req, Res] =
    (request: Req, responseObserver: StreamObserver[Res]) =>
      dispatcher.unsafeRunAndForget(
        serviceCall(request)
          .flatMap { response =>
            Async[F].delay(responseObserver.onNext(response)) *>
              Async[F].delay(responseObserver.onCompleted())
          }
          .handleErrorWith(e =>
            Logger[F].error(e)(s"Error occurred while processing request: ${e.getMessage}")
              *> Async[F].delay(responseObserver.onError(e))
          )
      )

  def bindServiceResources[F[_]: Async: Logger](
      services: List[Fs2GrpcService[F]]
  ): Resource[F, List[ServerServiceDefinition]] =
    Dispatcher.parallel[F].map { dispatcher =>
      services
        .map { service =>
          service
            .methodDefinitions(dispatcher)
            .foldLeft(ServerServiceDefinition.builder(service.descriptor)) { (builder, methodDef) =>
              builder.addMethod(
                methodDef.asInstanceOf[ServerMethodDefinition[Any, Any]]
              ) // scalafix:ok DisableSyntax.asInstanceOf
            }
            .build()
        }
    }

end Fs2GrpcService

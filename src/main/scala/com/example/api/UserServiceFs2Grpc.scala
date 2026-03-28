package com.example.api

import cats.effect.*
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.example.api.UserServiceGrpc
import io.grpc.ServerServiceDefinition
import io.grpc.stub.{ServerCalls, StreamObserver}

trait UserServiceFs2Grpc[F[_]]:
  def createUser(request:  CreateUserRequest):  F[CreateUserResponse]
  def getUser(request:     GetUserRequest):     F[GetUserResponse]
  def updateUser(request:  UpdateUserRequest):  F[UpdateUserResponse]
  def deleteUser(request:  DeleteUserRequest):  F[DeleteUserResponse]
  def getAllUsers(request: GetAllUsersRequest): F[GetAllUsersResponse]

object UserServiceFs2Grpc:
  private def toUnaryMethod[F[_]: Async, Req, Res](
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
          .handleErrorWith(e => Async[F].delay(responseObserver.onError(e)))
      )

  def bindServiceResource[F[_]: Async](
      impl: UserServiceFs2Grpc[F]
  ): Resource[F, ServerServiceDefinition] =
    Dispatcher.parallel[F].map { dispatcher =>
      ServerServiceDefinition
        .builder(UserServiceGrpc.SERVICE)
        .addMethod(
          UserServiceGrpc.METHOD_CREATE_USER,
          ServerCalls.asyncUnaryCall(toUnaryMethod(impl.createUser, dispatcher))
        )
        .addMethod(
          UserServiceGrpc.METHOD_GET_USER,
          ServerCalls.asyncUnaryCall(toUnaryMethod(impl.getUser, dispatcher))
        )
        .addMethod(
          UserServiceGrpc.METHOD_UPDATE_USER,
          ServerCalls.asyncUnaryCall(toUnaryMethod(impl.updateUser, dispatcher))
        )
        .addMethod(
          UserServiceGrpc.METHOD_DELETE_USER,
          ServerCalls.asyncUnaryCall(toUnaryMethod(impl.deleteUser, dispatcher))
        )
        .addMethod(
          UserServiceGrpc.METHOD_GET_ALL_USERS,
          ServerCalls.asyncUnaryCall(toUnaryMethod(impl.getAllUsers, dispatcher))
        )
        .build()
    }

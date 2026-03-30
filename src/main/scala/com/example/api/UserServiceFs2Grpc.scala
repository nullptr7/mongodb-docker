package com.example.api

import cats.effect.*
import cats.effect.std.Dispatcher

import io.grpc.stub.ServerCalls
import io.grpc.{ServerMethodDefinition, ServiceDescriptor}

import org.typelevel.log4cats.Logger

trait UserServiceFs2Grpc[F[_]] extends Fs2GrpcService[F]:
  def createUser(request:  CreateUserRequest): F[CreateUserResponse]
  def getUser(request:     GetUserRequest): F[GetUserResponse]
  def updateUser(request:  UpdateUserRequest): F[UpdateUserResponse]
  def deleteUser(request:  DeleteUserRequest): F[DeleteUserResponse]
  def getAllUsers(request: GetAllUsersRequest): F[GetAllUsersResponse]

  override def descriptor: ServiceDescriptor = UserServiceGrpc.SERVICE

  override def methodDefinitions(
      dispatcher: Dispatcher[F]
  )(using Async[F], Logger[F]): List[ServerMethodDefinition[?, ?]] =
    List(
      ServerMethodDefinition.create(
        UserServiceGrpc.METHOD_CREATE_USER,
        ServerCalls.asyncUnaryCall(Fs2GrpcService.toUnaryMethod(createUser, dispatcher))
      ),
      ServerMethodDefinition.create(
        UserServiceGrpc.METHOD_GET_USER,
        ServerCalls.asyncUnaryCall(Fs2GrpcService.toUnaryMethod(getUser, dispatcher))
      ),
      ServerMethodDefinition.create(
        UserServiceGrpc.METHOD_UPDATE_USER,
        ServerCalls.asyncUnaryCall(Fs2GrpcService.toUnaryMethod(updateUser, dispatcher))
      ),
      ServerMethodDefinition.create(
        UserServiceGrpc.METHOD_DELETE_USER,
        ServerCalls.asyncUnaryCall(Fs2GrpcService.toUnaryMethod(deleteUser, dispatcher))
      ),
      ServerMethodDefinition.create(
        UserServiceGrpc.METHOD_GET_ALL_USERS,
        ServerCalls.asyncUnaryCall(Fs2GrpcService.toUnaryMethod(getAllUsers, dispatcher))
      )
    )

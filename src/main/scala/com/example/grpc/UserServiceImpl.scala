package com.example.grpc

import cats.effect.*
import cats.implicits.*

import io.grpc.Status

import com.example.api.*
import com.example.db.{ToDocument, UserDTO}
import com.example.exceptions.*
import com.example.services.MongoService
import org.typelevel.log4cats.Logger

final class UserServiceImpl[F[_]: Async: Logger](mongoService: MongoService[F, UserDTO])
    extends UserServiceFs2Grpc[F]:

  private def toUser(userDTO: UserDTO) =
    User(
      id        = userDTO.id,
      name      = userDTO.name,
      email     = userDTO.email,
      age       = userDTO.age,
      city      = userDTO.city,
      createdAt = userDTO.createdAt,
      updatedAt = userDTO.updatedAt
    )

  private def getErrorStatus(exception: Throwable): Status =
    exception match
      case e: UserNotFoundException =>
        Status.NOT_FOUND.withDescription(e.message)
      case e: UserCreationException =>
        Status.INVALID_ARGUMENT.withDescription(e.message)
      case e: UserUpdateException   =>
        Status.NOT_FOUND.withDescription(e.message)
      case e: UserDeletionException =>
        Status.NOT_FOUND.withDescription(e.message)
      case e: FetchUsersException   =>
        Status.INTERNAL.withDescription(e.message)
      case e: AppException          =>
        Status.INTERNAL.withDescription(e.getMessage)
      case e                        =>
        Status.INTERNAL.withDescription(s"Unexpected error: ${e.getMessage}")

  private def handleException[A](exception: Throwable): F[A] =
    Logger[F].error(exception)(s"Error occurred: ${exception.getMessage}") *>
      Async[F].raiseError(getErrorStatus(exception).asRuntimeException())

  override def createUser(request: CreateUserRequest): F[CreateUserResponse] =
    mongoService
      .create(summon[ToDocument[CreateUserRequest]].to(request))
      .map { userDTO =>
        CreateUserResponse(
          user    = Some(toUser(userDTO)),
          success = true,
          message = "User created successfully"
        )
      }
      .handleErrorWith(handleException[CreateUserResponse])

  override def getUser(request: GetUserRequest): F[GetUserResponse] =
    mongoService
      .get(request.id)
      .map(userDTO => GetUserResponse(user = userDTO.map(toUser), found = true))
      .handleErrorWith {
        case _: UserNotFoundException =>
          Async[F].pure(GetUserResponse(user = None))
        case e                        => handleException[GetUserResponse](e)
      }

  override def updateUser(request: UpdateUserRequest): F[UpdateUserResponse] =
    mongoService
      .update(request.id, summon[ToDocument[UpdateUserRequest]].to(request))
      .map { userDTO =>
        UpdateUserResponse(
          user    = userDTO.map(toUser),
          success = true,
          message = "User updated successfully"
        )
      }
      .handleErrorWith(handleException[UpdateUserResponse])

  override def deleteUser(request: DeleteUserRequest): F[DeleteUserResponse] =
    mongoService
      .delete(request.id)
      .map(_ => DeleteUserResponse(success = true, message = "User deleted successfully"))
      .handleErrorWith(handleException[DeleteUserResponse])

  override def getAllUsers(request: GetAllUsersRequest): F[GetAllUsersResponse] =
    mongoService.getAll
      .map(userDTOs => GetAllUsersResponse(users = userDTOs.map(toUser)))
      .handleErrorWith(handleException[GetAllUsersResponse])

object UserServiceImpl:
  def make[F[_]: Async: Logger](mongoService: MongoService[F, UserDTO]): F[UserServiceImpl[F]] =
    Async[F].delay(new UserServiceImpl[F](mongoService))

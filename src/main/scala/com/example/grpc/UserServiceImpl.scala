package com.example.grpc

import cats.effect.*
import cats.implicits.*

import io.grpc.Status

import com.example.api.*
import com.example.db.UserDTO
import com.example.exceptions.*
import com.example.services.MongoService
import org.typelevel.log4cats.Logger

final class UserServiceImpl[F[_]: Async: Logger](mongoService: MongoService[F])
    extends UserServiceFs2Grpc[F]:

  private def toUser(UserDTO: UserDTO) =
    User(
      id        = UserDTO.id,
      name      = UserDTO.name,
      email     = UserDTO.email,
      age       = UserDTO.age,
      city      = UserDTO.city,
      createdAt = UserDTO.createdAt,
      updatedAt = UserDTO.updatedAt
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
      case e =>
        Status.INTERNAL.withDescription(s"Unexpected error: ${e.getMessage}")

  private def handleException[A](exception: Throwable): F[A] =
    Logger[F].error(exception)(s"Error occurred: ${exception.getMessage}") *>
      Async[F].raiseError(getErrorStatus(exception).asRuntimeException())

  override def createUser(request: CreateUserRequest): F[CreateUserResponse] =
    mongoService
      .createUser(request.name, request.email, request.age, request.city)
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
      .getUser(request.id)
      .map { userDTO =>
        GetUserResponse(user = Some(toUser(userDTO)), found = true)
      }
      .handleErrorWith {
        case _: UserNotFoundException =>
          Async[F].pure(GetUserResponse(user = None))
        case e => handleException[GetUserResponse](e)
      }

  override def updateUser(request: UpdateUserRequest): F[UpdateUserResponse] =
    mongoService
      .updateUser(request.id, request.name, request.email, request.age, request.city)
      .map { userDTO =>
        UpdateUserResponse(
          user    = Some(toUser(userDTO)),
          success = true,
          message = "User updated successfully"
        )
      }
      .handleErrorWith(handleException[UpdateUserResponse])

  override def deleteUser(request: DeleteUserRequest): F[DeleteUserResponse] =
    mongoService
      .deleteUser(request.id)
      .map(_ => DeleteUserResponse(success = true, message = "User deleted successfully"))
      .handleErrorWith(handleException[DeleteUserResponse])

  override def getAllUsers(request: GetAllUsersRequest): F[GetAllUsersResponse] =
    mongoService.getAllUsers
      .map(userDTOs => GetAllUsersResponse(users = userDTOs.map(toUser)))
      .handleErrorWith(handleException[GetAllUsersResponse])

object UserServiceImpl:
  def make[F[_]: Async: Logger: MongoService]: F[UserServiceImpl[F]] =
    Async[F].delay(new UserServiceImpl[F](summon[MongoService[F]]))

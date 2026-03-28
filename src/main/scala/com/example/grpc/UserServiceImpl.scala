package com.example.grpc

import cats.effect.*
import com.example.services.MongoService
import com.example.api.*
import com.example.exceptions.*
import io.grpc.Status

class UserServiceImpl(mongoService: MongoService[IO]) extends UserServiceFs2Grpc[IO]:

  private def toUser(userData: com.example.services.UserData) =
    User(
      id        = userData.id,
      name      = userData.name,
      email     = userData.email,
      age       = userData.age,
      city      = userData.city,
      createdAt = userData.createdAt,
      updatedAt = userData.updatedAt
    )

  private def handleException[A](exception: Throwable): IO[A] =
    exception match
      case e: UserNotFoundException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.NOT_FOUND.withDescription(e.message)
          )
        )
      case e: UserCreationException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.INVALID_ARGUMENT.withDescription(e.message)
          )
        )
      case e: UserUpdateException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.NOT_FOUND.withDescription(e.message)
          )
        )
      case e: UserDeletionException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.NOT_FOUND.withDescription(e.message)
          )
        )
      case e: FetchUsersException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.INTERNAL.withDescription(e.message)
          )
        )
      case e: AppException =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.INTERNAL.withDescription(e.getMessage)
          )
        )
      case e =>
        IO.raiseError(
          new io.grpc.StatusRuntimeException(
            Status.INTERNAL.withDescription(s"Unexpected error: ${e.getMessage}")
          )
        )

  override def createUser(request: CreateUserRequest): IO[CreateUserResponse] =
    mongoService
      .createUser(request.name, request.email, request.age, request.city)
      .map { userData =>
        CreateUserResponse(
          user    = Some(toUser(userData)),
          success = true,
          message = "User created successfully"
        )
      }
      .handleErrorWith(handleException[CreateUserResponse])

  override def getUser(request: GetUserRequest): IO[GetUserResponse] =
    mongoService
      .getUser(request.id)
      .map { userData =>
        GetUserResponse(user = Some(toUser(userData)), found = true)
      }
      .handleErrorWith {
        case e: UserNotFoundException =>
          IO.pure(GetUserResponse(user = None))
        case e => handleException[GetUserResponse](e)
      }

  override def updateUser(request: UpdateUserRequest): IO[UpdateUserResponse] =
    mongoService
      .updateUser(request.id, request.name, request.email, request.age, request.city)
      .map { userData =>
        UpdateUserResponse(
          user    = Some(toUser(userData)),
          success = true,
          message = "User updated successfully"
        )
      }
      .handleErrorWith(handleException[UpdateUserResponse])

  override def deleteUser(request: DeleteUserRequest): IO[DeleteUserResponse] =
    mongoService
      .deleteUser(request.id)
      .map { _ =>
        DeleteUserResponse(success = true, message = "User deleted successfully")
      }
      .handleErrorWith(handleException[DeleteUserResponse])

  override def getAllUsers(request: GetAllUsersRequest): IO[GetAllUsersResponse] =
    mongoService.getAllUsers
      .map(userDataList => GetAllUsersResponse(users = userDataList.map(toUser)))
      .handleErrorWith(handleException[GetAllUsersResponse])

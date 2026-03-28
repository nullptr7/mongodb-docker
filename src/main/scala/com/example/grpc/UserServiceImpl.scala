package com.example.grpc

import cats.effect.*
import com.example.services.MongoService
import com.example.api.*

// New fs2-grpc style service implementation
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
      .handleErrorWith(e =>
        IO.pure(
          CreateUserResponse(
            user    = None,
            message = s"Error creating user: ${e.getMessage}"
          )
        )
      )

  override def getUser(request: GetUserRequest): IO[GetUserResponse] =
    IO.consoleForIO.print("Getting User...") *> mongoService
      .getUser(request.id)
      .map { maybeUser =>
        maybeUser.fold(GetUserResponse(user = None, found = false))(userData =>
          GetUserResponse(user = Some(toUser(userData)), found = true)
        )
      }
      .handleErrorWith(_ => IO.pure(GetUserResponse(user = None, found = false)))

  override def updateUser(request: UpdateUserRequest): IO[UpdateUserResponse] =
    mongoService
      .updateUser(request.id, request.name, request.email, request.age, request.city)
      .map {
        case Some(userData) =>
          UpdateUserResponse(
            user    = Some(toUser(userData)),
            success = true,
            message = "User updated successfully"
          )
        case None =>
          UpdateUserResponse(
            user    = None,
            message = "User not found"
          )
      }
      .handleErrorWith(e =>
        IO.pure(
          UpdateUserResponse(
            user    = None,
            message = s"Error updating user: ${e.getMessage}"
          )
        )
      )

  override def deleteUser(request: DeleteUserRequest): IO[DeleteUserResponse] =
    mongoService
      .deleteUser(request.id)
      .map(success =>
        if success then DeleteUserResponse(success = true, message = "User deleted successfully")
        else DeleteUserResponse(message            = "User not found")
      )
      .handleErrorWith(e =>
        IO.pure(
          DeleteUserResponse(message = s"Error deleting user: ${e.getMessage}")
        )
      )

  override def getAllUsers(request: GetAllUsersRequest): IO[GetAllUsersResponse] =
    mongoService.getAllUsers
      .map(userDataList => GetAllUsersResponse(users = userDataList.map(toUser)))
      .handleErrorWith(_ => IO.pure(GetAllUsersResponse(users = List.empty)))

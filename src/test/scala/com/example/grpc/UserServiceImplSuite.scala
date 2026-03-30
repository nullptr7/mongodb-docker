package com.example.grpc

import cats.effect.IO

import io.grpc.Status

import com.example.api.*
import com.example.db.UserDTO
import com.example.exceptions.UserCreationException
import com.example.services.MongoService
import munit.CatsEffectSuite
import org.mongodb.scala.Document
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

final class UserServiceImplSuite extends CatsEffectSuite:

  given Logger[IO] = NoOpLogger[IO]

  private val user =
    UserDTO(
      id        = "user-1",
      name      = "Ada",
      email     = "ada@example.com",
      age       = 32,
      city      = "London",
      createdAt = 1000L,
      updatedAt = 2000L
    )

  test("createUser returns a successful response with the mapped user") {
    val service = new UserServiceImpl[IO](StubMongoService(createResult = IO.pure(user)))

    service
      .createUser(
        CreateUserRequest(
          name  = "Ada",
          email = "ada@example.com",
          age   = 32,
          city  = "London"
        )
      )
      .map { response =>
        assertEquals(response.success, true)
        assertEquals(response.message, "User created successfully")
        assertEquals(response.user.map(_.id), Some("user-1"))
        assertEquals(response.user.map(_.createdAt), Some(1000L))
        assertEquals(response.user.map(_.updatedAt), Some(2000L))
      }
  }

  test("getUser marks found=false when the user does not exist") {
    val service = new UserServiceImpl[IO](StubMongoService(getResult = IO.pure(None)))

    service.getUser(GetUserRequest(id = "missing")).map { response =>
      assertEquals(response.user, None)
      assertEquals(response.found, false)
    }
  }

  test("getAllUsers returns all mapped users") {
    val secondUser = user.copy(id = "user-2", name = "Grace")
    val service    =
      new UserServiceImpl[IO](StubMongoService(getAllResult = IO.pure(List(user, secondUser))))

    service.getAllUsers(GetAllUsersRequest()).map { response =>
      assertEquals(response.users.map(_.id), Seq("user-1", "user-2"))
      assertEquals(response.users.map(_.name), Seq("Ada", "Grace"))
    }
  }

  test("createUser converts domain exceptions into gRPC status errors") {
    val service = new UserServiceImpl[IO](
      StubMongoService(
        createResult = IO.raiseError(
          UserCreationException(
            name    = "Ada",
            email   = "ada@example.com",
            message = "invalid email"
          )
        )
      )
    )

    interceptIO[io.grpc.StatusRuntimeException](
      service.createUser(
        CreateUserRequest(
          name  = "Ada",
          email = "ada@example.com",
          age   = 32,
          city  = "London"
        )
      )
    ).map { exception =>
      assertEquals(exception.getStatus.getCode, Status.INVALID_ARGUMENT.getCode)
      assertEquals(exception.getStatus.getDescription, "invalid email")
    }
  }

private final case class StubMongoService(
    createResult: IO[UserDTO]         = IO.raiseError(new AssertionError("unexpected create call")),
    getResult:    IO[Option[UserDTO]] = IO.raiseError(new AssertionError("unexpected get call")),
    updateResult: IO[Option[UserDTO]] = IO.raiseError(new AssertionError("unexpected update call")),
    deleteResult: IO[Unit]            = IO.raiseError(new AssertionError("unexpected delete call")),
    getAllResult: IO[List[UserDTO]]   = IO.raiseError(new AssertionError("unexpected getAll call"))
) extends MongoService[IO, UserDTO]:

  override def create(document: Document): IO[UserDTO] = createResult

  override def get(id: String): IO[Option[UserDTO]] = getResult

  override def update(id: String, document: Document): IO[Option[UserDTO]] = updateResult

  override def delete(id: String): IO[Unit] = deleteResult

  override def getAll: IO[List[UserDTO]] = getAllResult

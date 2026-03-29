package com.example.services

import cats.effect._
import cats.syntax.all._

import com.example.exceptions._
import org.bson.types.{ObjectId => BsonObjectId}
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._

case class UserData(
    id:        String,
    name:      String,
    email:     String,
    age:       Int,
    city:      String,
    createdAt: Long,
    updatedAt: Long
)

trait MongoService[F[_]]:
  def createUser(name: String, email: String, age: Int, city: String): F[UserData]
  def getUser(id:      String): F[UserData] // Throws UserNotFoundException if not found
  def updateUser(
      id:    String,
      name:  String,
      email: String,
      age:   Int,
      city:  String
  ): F[UserData] // Throws UserUpdateException if not found
  def deleteUser(id:   String): F[Unit] // Throws UserDeletionException if not found
  def getAllUsers: F[List[UserData]]

object MongoService:
  def make[F[_]: Async](mongoUri: String): Resource[F, MongoService[F]] =
    Resource.make(
      Async[F].delay {
        val mongoClient = MongoClient(mongoUri)
        val database    = mongoClient.getDatabase("mongodb_grpc")
        val collection  = database.getCollection("users")
        new MongoServiceImpl(collection, mongoClient)
      }
    )(service =>
      Async[F].delay {
        service.close()
      }
    )

  private[services] def fromSingleObservable[F[_]: Async, A](obs: Observable[A]): F[A] =
    Async[F].fromFuture(
      Async[F].executionContext.flatMap { ec =>
        Async[F].delay(obs.toFuture().map(_.head)(ec))
      }
    )

  private[services] def fromOptionObservable[F[_]: Async, A](obs: Observable[A]): F[Option[A]] =
    Async[F].fromFuture(
      Async[F].executionContext.flatMap { ec =>
        Async[F].delay(obs.toFuture().map(_.headOption)(ec))
      }
    )

  private[services] def fromListObservable[F[_]: Async, A](obs: Observable[A]): F[List[A]] =
    Async[F].fromFuture(
      Async[F].executionContext.flatMap { ec =>
        Async[F].delay(obs.toFuture().map(_.toList)(ec))
      }
    )

private class MongoServiceImpl[F[_]: Async](
    collection:  MongoCollection[Document],
    mongoClient: MongoClient
) extends MongoService[F]:

  def createUser(name: String, email: String, age: Int, city: String): F[UserData] =
    for
      id  <- Async[F].delay(new BsonObjectId().toString)
      now <- Async[F].delay(System.currentTimeMillis())
      doc = Document(
        "_id"       -> id,
        "name"      -> name,
        "email"     -> email,
        "age"       -> age,
        "city"      -> city,
        "createdAt" -> now,
        "updatedAt" -> now
      )
      _ <- MongoService.fromSingleObservable(collection.insertOne(doc))
    yield UserData(id, name, email, age, city, now, now)

  def getUser(id: String): F[UserData] =
    MongoService
      .fromOptionObservable(collection.find(equal("_id", id)).first())
      .flatMap {
        case Some(doc) => Async[F].pure(documentToUserData(doc))
        case None => Async[F].raiseError(UserNotFoundException(id, s"User with id '$id' not found"))
      }

  def updateUser(
      id:    String,
      name:  String,
      email: String,
      age:   Int,
      city:  String
  ): F[UserData] =
    for
      now      <- Async[F].delay(System.currentTimeMillis())
      maybeDoc <- MongoService.fromOptionObservable(
        collection
          .findOneAndUpdate(
            equal("_id", id),
            combine(
              set("name", name),
              set("email", email),
              set("age", age),
              set("city", city),
              set("updatedAt", now)
            )
          )
          .toObservable()
      )
      result   <- maybeDoc match
        case Some(doc) =>
          Async[F].pure(
            UserData(
              id,
              doc.getString("name"),
              doc.getString("email"),
              doc.getInteger("age"),
              doc.getString("city"),
              doc.getLong("createdAt"),
              now
            )
          )
        case None      =>
          Async[F].raiseError(UserUpdateException(id, s"Failed to update user with id '$id'"))
    yield result

  def deleteUser(id: String): F[Unit] =
    MongoService
      .fromSingleObservable(collection.deleteOne(equal("_id", id)))
      .flatMap { result =>
        if result.getDeletedCount > 0 then Async[F].unit
        else Async[F].raiseError(UserDeletionException(id, s"Failed to delete user with id '$id'"))
      }

  def getAllUsers: F[List[UserData]] =
    MongoService.fromListObservable(collection.find()).map(_.map(documentToUserData))

  def close(): Unit =
    mongoClient.close()

  private def documentToUserData(doc: Document): UserData =
    UserData(
      doc.getString("_id"),
      doc.getString("name"),
      doc.getString("email"),
      doc.getInteger("age"),
      doc.getString("city"),
      doc.getLong("createdAt"),
      doc.getLong("updatedAt")
    )

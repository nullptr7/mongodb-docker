package com.example.services

import scala.util.Try

import cats.effect.Resource
import cats.effect.kernel.Async
import cats.syntax.all.*

import com.example.db.FromDocument
import org.mongodb.scala.bson.{BsonInt64, BsonObjectId}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{Document, *}

sealed trait MongoService[F[_], A]:

  def create(document: Document): F[A]

  def get(id: String): F[Option[A]]

  def update(id: String, document: Document): F[Option[A]]

  def delete(id: String): F[Unit]

  def getAll: F[List[A]]

object MongoService:
  def make[F[_]: Async, A: FromDocument](
      mongoUri:       String,
      databaseName:   String,
      collectionName: String
  ): Resource[F, MongoService[F, A]] =
    Resource.make(
      Async[F].delay {
        val mongoClient = MongoClient(mongoUri)
        val database    = mongoClient.getDatabase(databaseName)
        val collection  = database.getCollection(collectionName)
        new MongoServiceImpl(collection, mongoClient)
      }
    )(service => Async[F].delay(service.close()))

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

final private class MongoServiceImpl[F[_]: Async, A: FromDocument](
    collection:  MongoCollection[Document],
    mongoClient: MongoClient
) extends MongoService[F, A] {

  override def create(document: Document): F[A] =
    for {
      id  <- Async[F].delay(new BsonObjectId().getValue.toString)
      now <- Async[F].delay(System.currentTimeMillis())
      insertedDocument = document + ("_id" -> id, "createdAt" -> now, "updatedAt" -> now)
      _             <- MongoService.fromSingleObservable(collection.insertOne(insertedDocument))
      insertedValue <- Async[F].fromTry(fromDoc(insertedDocument))
    } yield insertedValue

  override def get(id: String): F[Option[A]] =
    MongoService
      .fromOptionObservable(collection.find(equal("_id", id)).first())
      .flatMap {
        case Some(document) => Async[F].fromTry(fromDoc(document)).map(Some(_))
        case None           => Async[F].pure(None)
      }

  override def update(id: String, document: Document): F[Option[A]] =
    for {
      now          <- Async[F].delay(System.currentTimeMillis())
      maybeDoc     <- MongoService.fromOptionObservable(
        collection
          .findOneAndUpdate(
            equal("_id", id),
            Document("$set" -> document.toBsonDocument().updated("updatedAt", BsonInt64(now)))
          )
          .toObservable()
      )
      updatedValue <- maybeDoc match {
        case Some(document) => Async[F].fromTry(fromDoc(document)).map(Some(_))
        case None           => Async[F].pure(None)
      }
    } yield updatedValue

  override def delete(id: String): F[Unit] =
    MongoService
      .fromSingleObservable(collection.deleteOne(equal("_id", id)))
      .flatMap { result =>
        if result.getDeletedCount > 0 then Async[F].unit
        else Async[F].raiseError(new Exception(s"Failed to delete id '$id'"))
      }

  override def getAll: F[List[A]] =
    for {
      documents <- MongoService.fromListObservable(collection.find())
      listOfA   <- documents.map(document => Async[F].fromTry(fromDoc(document))).sequence
    } yield listOfA

  def close(): Unit =
    mongoClient.close()

  private[this] inline def fromDoc(doc: Document): Try[A] =
    summon[FromDocument[A]].from(doc)
}

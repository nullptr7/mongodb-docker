package com.example.db

import org.bson.types.ObjectId
import org.mongodb.scala.bson.*

trait BsonWriter[A]:
  def write(value: A): BsonValue

object BsonWriter:

  given BsonWriter[String] with
    def write(value: String): BsonValue = BsonString(value)

  given BsonWriter[Int] with
    def write(value: Int): BsonValue = BsonInt32(value)

  given BsonWriter[Long] with
    def write(value: Long): BsonValue = BsonInt64(value)

  given BsonWriter[Double] with
    def write(value: Double): BsonValue = BsonDouble(value)

  given BsonWriter[Boolean] with
    def write(value: Boolean): BsonValue = BsonBoolean(value)

  given BsonWriter[ObjectId] with
    def write(value: ObjectId): BsonValue = BsonObjectId(value)

  given [A](using writer: BsonWriter[A]): BsonWriter[Option[A]] with
    def write(value: Option[A]): BsonValue =
      value match
        case Some(v) => writer.write(v)
        case None    => BsonNull()

  given [A](using writer: BsonWriter[A]): BsonWriter[List[A]] with
    def write(value: List[A]): BsonValue =
      BsonArray(value.map(writer.write))

  given [A](using toDoc: ToDocument[A]): BsonWriter[A] with
    def write(value: A): BsonValue =
      toDoc.to(value).toBsonDocument

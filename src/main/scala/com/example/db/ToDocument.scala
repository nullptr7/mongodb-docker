package com.example.db

import scala.compiletime.*
import scala.deriving.*

import org.mongodb.scala.Document
import org.mongodb.scala.bson.*

trait ToDocument[A]:
  def to(a: A): Document

object ToDocument:

  private inline def collectFields[Elems <: Tuple, Labels <: Tuple](
      p:     Product,
      index: Int
  ): List[(String, BsonValue)] =
    inline (erasedValue[Elems], erasedValue[Labels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        Nil

      case _: (h *: t, l *: tl) =>
        val fieldName = constValue[l & String]

        val rest = collectFields[t, tl](p, index + 1)

        if fieldName == "unknownFields" then rest
        else
          val bsonKey = if fieldName == "id" then "_id" else fieldName
          val value   =
            p.productElement(index).asInstanceOf[h] // scalafix:ok DisableSyntax.asInstanceOf
          val bson = summonInline[BsonWriter[h]].write(value)
          (bsonKey, bson) :: rest

  inline def derived[A](using m: Mirror.ProductOf[A]): ToDocument[A] =
    (a: A) =>
      val fields =
        collectFields[m.MirroredElemTypes, m.MirroredElemLabels](
          a.asInstanceOf[Product], // scalafix:ok DisableSyntax.asInstanceOf
          0
        )

      Document(BsonDocument(fields))

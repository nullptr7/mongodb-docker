package com.example.db

import scala.compiletime.*
import scala.deriving.*
import scala.util.Try

import org.mongodb.scala.Document

trait FromDocument[A]:
  def from(doc: Document): Try[A]

object FromDocument:
  private inline def summonAll[Elems <: Tuple, Labels <: Tuple](doc: Document): List[Any] =
    inline (erasedValue[Elems], erasedValue[Labels]) match
      case _: (EmptyTuple, EmptyTuple) => Nil
      case _: (h *: t, l *: tl)        =>
        val fieldName = constValue[l & String]
        val bsonKey   =
          if fieldName == "id" then "_id" else fieldName
        summonInline[BsonField[h]].get(doc, bsonKey) :: summonAll[t, tl](doc)

  inline def derived[A](using m: Mirror.ProductOf[A]): FromDocument[A] =
    (doc: Document) =>
      Try {
        val values = summonAll[m.MirroredElemTypes, m.MirroredElemLabels](doc)
        m.fromProduct(Tuple.fromArray(values.toArray))
      }

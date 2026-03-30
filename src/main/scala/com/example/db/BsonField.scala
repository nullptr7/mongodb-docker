package com.example.db

import org.mongodb.scala.*

trait BsonField[A]:
  def get(doc: Document, key: String): A

object BsonField:
  given BsonField[String] with
    def get(doc: Document, key: String): String = doc.getString(key)

  given BsonField[Int] with
    def get(doc: Document, key: String): Int = doc.getInteger(key)

  given BsonField[Long] with
    def get(doc: Document, key: String): Long = doc.getLong(key)

  given BsonField[Double] with
    def get(doc: Document, key: String): Double = doc.getDouble(key)

  given BsonField[Boolean] with
    def get(doc: Document, key: String): Boolean = doc.getBoolean(key)

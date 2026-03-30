package com.example.db

import com.example.api.CreateUserRequest
import munit.CatsEffectSuite
import org.bson.types.ObjectId
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonInt32, BsonObjectId, BsonString}
import org.mongodb.scala.{Document, documentToUntypedDocument}

final class DocumentCodecSuite extends CatsEffectSuite:

  test("ToDocument[UserDTO] maps id to _id and preserves fields") {
    val user =
      UserDTO(
        id        = "user-1",
        name      = "Ada",
        email     = "ada@example.com",
        age       = 32,
        city      = "London",
        createdAt = 1000L,
        updatedAt = 2000L
      )

    val document = summon[ToDocument[UserDTO]].to(user)

    assertEquals(document.getString("_id"), "user-1")
    assertEquals(document.getString("name"), "Ada")
    assertEquals(document.getString("email"), "ada@example.com")
    assertEquals(document.getInteger("age").intValue, 32)
    assertEquals(document.getString("city"), "London")
    assertEquals(document.getLong("createdAt").longValue, 1000L)
    assertEquals(document.getLong("updatedAt").longValue, 2000L)
  }

  test("ToDocument derived for protobuf requests omits unknownFields") {
    val request =
      CreateUserRequest(
        name  = "Ada",
        email = "ada@example.com",
        age   = 32,
        city  = "London"
      )

    val document = summon[ToDocument[CreateUserRequest]].to(request)

    assertEquals(document.toBsonDocument.keySet.size(), 4)
    assert(!document.toBsonDocument.containsKey("unknownFields"))
    assertEquals(document.getString("name"), "Ada")
    assertEquals(document.getString("email"), "ada@example.com")
    assertEquals(document.getInteger("age").intValue, 32)
    assertEquals(document.getString("city"), "London")
  }

  test("FromDocument[UserDTO] reconstructs a case class from a Mongo document") {
    val document = Document(
      "_id"       -> "user-1",
      "name"      -> "Ada",
      "email"     -> "ada@example.com",
      "age"       -> 32,
      "city"      -> "London",
      "createdAt" -> 1000L,
      "updatedAt" -> 2000L
    )

    val result = summon[FromDocument[UserDTO]].from(document).get

    assertEquals(
      result,
      UserDTO(
        id        = "user-1",
        name      = "Ada",
        email     = "ada@example.com",
        age       = 32,
        city      = "London",
        createdAt = 1000L,
        updatedAt = 2000L
      )
    )
  }

  test("FromDocument[UserDTO] uses the driver's default for a missing numeric field") {
    val document = Document(
      "_id"       -> "user-1",
      "name"      -> "Ada",
      "email"     -> "ada@example.com",
      "age"       -> 32,
      "city"      -> "London",
      "createdAt" -> 1000L
    )

    val result = summon[FromDocument[UserDTO]].from(document).get

    assertEquals(result.updatedAt, 0L)
  }

  test("BsonWriter handles option, list, and ObjectId values") {
    val objectId = ObjectId.get()

    assert(summon[BsonWriter[Option[String]]].write(None).isNull)
    assertEquals(summon[BsonWriter[Option[String]]].write(Some("Ada")), BsonString("Ada"))
    assertEquals(
      summon[BsonWriter[List[Boolean]]].write(List(true, false)),
      BsonArray.fromIterable(List(BsonBoolean(true), BsonBoolean(false)))
    )
    assertEquals(summon[BsonWriter[Int]].write(32), BsonInt32(32))
    assertEquals(summon[BsonWriter[ObjectId]].write(objectId), BsonObjectId(objectId))
  }

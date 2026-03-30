package com.example.services

import com.example.db.UserDTO

trait UserDTOService[F[_]] {

  def createUser(name: String, email: String, age: Int, city: String): F[UserDTO]
  def getUser(id:      String): F[UserDTO] // Throws UserNotFoundException if not found
  def updateUser(
      id:    String,
      name:  String,
      email: String,
      age:   Int,
      city:  String
  ): F[UserDTO] // Throws UserUpdateException if not found
  def deleteUser(id:   String): F[Unit] // Throws UserDeletionException if not found
  def getAllUsers: F[List[UserDTO]]
}

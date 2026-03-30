package com.example.db

case class UserDTO(
    id:        String,
    name:      String,
    email:     String,
    age:       Int,
    city:      String,
    createdAt: Long,
    updatedAt: Long
)

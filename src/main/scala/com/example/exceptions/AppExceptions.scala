package com.example.exceptions

/** Base exception class for all application-specific exceptions
  */
sealed abstract class AppException(val message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

/** Exception thrown when a user is not found
  */
case class UserNotFoundException(id: String, override val message: String)
    extends AppException(message)

/** Exception thrown when user creation fails
  */
case class UserCreationException(
    name:                 String,
    email:                String,
    override val message: String,
    cause:                Option[Throwable] = None
) extends AppException(message, cause)

/** Exception thrown when user update fails
  */
case class UserUpdateException(
    id:                   String,
    override val message: String,
    cause:                Option[Throwable] = None
) extends AppException(message, cause)

/** Exception thrown when user deletion fails
  */
case class UserDeletionException(
    id:                   String,
    override val message: String,
    cause:                Option[Throwable] = None
) extends AppException(message, cause)

/** Exception thrown when retrieving all users fails
  */
case class FetchUsersException(
    override val message: String            = "Failed to fetch users",
    cause:                Option[Throwable] = None
) extends AppException(message, cause)

/** Exception thrown for invalid user data
  */
case class InvalidUserDataException(
    field:                String,
    value:                String,
    override val message: String
) extends AppException(message)

/** Exception thrown for database-related errors
  */
case class DatabaseException(
    operation:            String,
    override val message: String,
    cause:                Option[Throwable] = None
) extends AppException(message, cause)

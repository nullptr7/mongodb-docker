# MongoDB gRPC Service - Scala 3

A fully functional MongoDB CRUD service exposed via gRPC API, built with Scala 3, Cats Effect framework, and containerized with Docker.

## Technology Stack

- **Scala 3.3.1** - Modern functional programming language
- **Cats Effect 3.5.0** - Functional effects and asynchronous programming
- **MongoDB Scala Driver 5.0.0** - MongoDB connectivity
- **gRPC 1.59.0** - High-performance RPC framework
- **ScalaPB 0.11.13** - Protocol Buffers code generation
- **Docker & Docker Compose** - Containerization

## Project Structure

```
mongodb-docker/
├── build.sbt                          # SBT build configuration
├── project/
│   ├── build.properties              # SBT version
│   └── plugins.sbt                   # SBT plugins
├── src/
│   └── main/
│       ├── scala/com/example/
│       │   ├── Main.scala            # Application entry point
│       │   ├── services/
│       │   │   └── MongoService.scala # MongoDB CRUD operations
│       │   └── grpc/
│       │       └── UserServiceImpl.scala # gRPC service implementation
│       └── protobuf/
│           └── user.proto            # gRPC service definition
├── Dockerfile                         # Multi-stage Docker build
├── docker-compose.yml                 # Docker Compose setup (MongoDB + App)
└── README.md                          # This file
```

## Features

### CRUD Operations
- **Create User** - Add new users to MongoDB
- **Get User** - Retrieve a user by ID
- **Update User** - Update user information
- **Delete User** - Remove a user
- **Get All Users** - Retrieve all users

### Functional Architecture
- Pure functional code using Cats Effect `IO` monad
- Resource management with `Async` interface
- Non-blocking MongoDB operations using Scala Futures
- Error handling with functional composition

### gRPC API
The service exposes a `UserService` with the following RPC methods:
- `CreateUser(CreateUserRequest) -> CreateUserResponse`
- `GetUser(GetUserRequest) -> GetUserResponse`
- `UpdateUser(UpdateUserRequest) -> UpdateUserResponse`
- `DeleteUser(DeleteUserRequest) -> DeleteUserResponse`
- `GetAllUsers(GetAllUsersRequest) -> GetAllUsersResponse`

## Prerequisites

- **Docker** (v20.10+)
- **Docker Compose** (v1.29+)
- **SBT** (v1.9.7+) - for local development
- **Java 21** - for local development

## Quick Start

### Option 1: Using Docker Compose (Recommended)

```bash
# Build and start all services
docker-compose up --build

# The service will be available at localhost:50051
```

### Option 2: Local Development

```bash
# Ensure MongoDB is running locally
# mongodb://localhost:27017

# Build the project
sbt clean compile

# Run the application
sbt run

# The service will be available at localhost:50051
```

## Environment Variables

Configure the application using environment variables:

```env
MONGO_URI=mongodb://localhost:27017
GRPC_PORT=50051
```

For Docker Compose with authentication:
```env
MONGO_URI=mongodb://root:password@mongodb:27017
```

## Building the Project

### Local Build
```bash
# Compile Scala code and generate gRPC stubs
sbt compile

# Create assembly JAR
sbt assembly
```

### Docker Build
```bash
# Build and run with Docker Compose
docker-compose up --build

# Or build image only
docker build -t mongodb-grpc:latest .
```

## Testing the Service

### Using grpcurl

```bash
# Install grpcurl
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest

# List services
grpcurl -plaintext localhost:50051 list

# Create a user
grpcurl -plaintext -d '{"name":"John Doe","email":"john@example.com","age":30,"city":"New York"}' \
  localhost:50051 com.example.api.UserService/CreateUser

# Get a user (replace ID with actual user ID)
grpcurl -plaintext -d '{"id":"<user-id>"}' \
  localhost:50051 com.example.api.UserService/GetUser

# Get all users
grpcurl -plaintext -d '{}' \
  localhost:50051 com.example.api.UserService/GetAllUsers

# Update a user
grpcurl -plaintext -d '{"id":"<user-id>","name":"Jane Doe","email":"jane@example.com","age":28,"city":"Los Angeles"}' \
  localhost:50051 com.example.api.UserService/UpdateUser

# Delete a user
grpcurl -plaintext -d '{"id":"<user-id>"}' \
  localhost:50051 com.example.api.UserService/DeleteUser
```

### Using BloomRPC (GUI)
1. Download [BloomRPC](https://github.com/uw-labs/bloomrpc)
2. Import the proto file: `src/main/protobuf/user.proto`
3. Connect to `localhost:50051`
4. Test the endpoints

## Project Components

### MongoService
Trait defining CRUD operations with Cats Effect:
```scala
trait MongoService[F[_]]:
  def createUser(name: String, email: String, age: Int, city: String): F[UserData]
  def getUser(id: String): F[Option[UserData]]
  def updateUser(id: String, name: String, email: String, age: Int, city: String): F[Option[UserData]]
  def deleteUser(id: String): F[Boolean]
  def getAllUsers: F[List[UserData]]
```

### UserServiceImpl
gRPC service implementation that translates Protocol Buffer requests/responses to Cats Effect operations.

### Main Application
Manages resource lifecycle:
- Initializes MongoDB connection
- Starts gRPC server
- Handles graceful shutdown

## Dependencies

### Core Dependencies
- `cats-effect`: Functional effects and IO
- `mongo-scala-driver`: MongoDB client
- `grpc-netty-shaded`: gRPC transport
- `scalapb-runtime-grpc`: Protocol Buffer runtime

### Plugins
- `sbt-protoc`: Protocol Buffer code generation
- `sbt-assembly`: JAR assembly for Docker

## Database Schema

Documents are stored in the `mongodb_grpc.users` collection:

```json
{
  "_id": "object-id-string",
  "name": "string",
  "email": "string",
  "age": 30,
  "city": "string",
  "createdAt": 1234567890,
  "updatedAt": 1234567890
}
```

## Troubleshooting

### Port Already in Use
```bash
# Kill process on port 50051
# Linux/Mac
lsof -i :50051 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Windows
netstat -ano | findstr :50051
taskkill /PID <PID> /F
```

### MongoDB Connection Issues
```bash
# Check if MongoDB is running
docker ps | grep mongodb

# View logs
docker-compose logs mongodb

# Restart services
docker-compose restart
```

### See Application Logs
```bash
docker-compose logs -f grpc-service
```

## Performance Notes

- Non-blocking async operations using Cats Effect
- Connection pooling handled by MongoDB driver
- gRPC with HTTP/2 for efficient communication
- Resource management with automatic cleanup

## Architecture Decisions

1. **Cats Effect**: Used for pure functional error handling and resource management
2. **Scala 3**: Modern language features (given instances, match expressions)
3. **MongoDB Scala Driver**: Native Scala support for database operations
4. **gRPC**: Efficient binary protocol for client-server communication
5. **Docker Compose**: Orchestration of MongoDB and application services

## Extending the Project

### Add New gRPC Methods
1. Update `src/main/protobuf/user.proto`
2. Implement methods in `MongoService` trait
3. Implement methods in `UserServiceImpl`
4. Rebuild: `sbt compile`

### Modify Data Model
1. Update `UserData` case class in `MongoService.scala`
2. Update protobuf `User` message in `user.proto`
3. Update database mappings
4. Rebuild: `sbt compile`

## Building for Production

```bash
# Build optimized Docker image
docker build -t mongodb-grpc:latest .

# Run with resource constraints
docker run -m 512m --cpus 1.0 -p 50051:50051 mongodb-grpc:latest

# Push to registry
docker tag mongodb-grpc:latest <registry>/mongodb-grpc:latest
docker push <registry>/mongodb-grpc:latest
```

## License

MIT License - see LICENSE file for details

## Support

For issues, questions, or contributions, please open an issue or pull request.

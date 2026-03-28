FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the fat JAR built by sbt assembly
COPY target/scala-3.3.7/mongodb-grpc-assembly-0.1.0.jar ./app.jar

# Expose the gRPC port
EXPOSE 50051

# Environment variables (Compose overrides these)
ENV MONGO_URI=mongodb://mongodb:27017
ENV GRPC_PORT=50051

CMD ["java", "-jar", "app.jar"]
# Use OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy source files
COPY src/ ./src/
COPY data*.txt ./

# Compile Java files
RUN javac -d . src/*.java

# No default command - run manually with docker-compose run


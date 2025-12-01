# Use Eclipse Temurin base image (official replacement for OpenJDK)
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy source files
COPY src/ ./src/
COPY data*.txt ./

# Compile Java files
RUN javac -encoding UTF-8 -d . src/*.java

# No default command - run manually with docker-compose run


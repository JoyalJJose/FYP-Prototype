# FYP-Prototype
Prototype for FYP

## Overview

This repository contains two main components:
- **InferProto**: Python-based crowd counting module using YOLO for thermal images
- **Scheduler**: Java-based vehicle deployment scheduling system

## Quick Start with Docker

The easiest way to run this project without installing dependencies is using Docker.

### Prerequisites

- [Docker](https://www.docker.com/get-started) installed on your system
- [Docker Compose](https://docs.docker.com/compose/install/) (usually included with Docker Desktop)

### Setup

1. **Clone the repository or unzip**:
   ```bash
   git clone <repository-url>
   cd FYP-Prototype
   ```

2. **Build the Docker images**:
   ```bash
   docker-compose build
   ```
   Please note that this takes quite a while (approx 15 min).
   Recommend to look around code while waiting for build to finish.

### Running the Programs

**Open an interactive shell in the container:**

Try either of the following commands:
```bash
docker-compose run --rm fyp-prototype sh
```
or
```bash
docker-compose run --rm fyp-prototype /bin/sh
```

**Once inside the container, run:**

- **Python Inference Script:**
  ```bash
  python /app/InferProto/test_random_inference.py
  ```
  Press enter to save an image to the output folder with visualisation.

**Viewing Output Images:**

The output images are saved in two locations (they're synced via Docker volumes):

1. **From inside the container:**
   ```bash
   ls /app/InferProto/output
   ```
   Images are saved as `result_001.png`, `result_002.png`, etc.

2. **From your host machine (Windows):**
   - Navigate to: `FYP-Prototype\InferProto\output\`
   - Open the PNG files with any image viewer

- **Java Scheduler:**
  ```bash
  java -cp /app/Scheduler Scheduler
  ```
  Or with a specific data file:
  ```bash
  java -cp /app/Scheduler Scheduler dataIncrease.txt
  java -cp /app/Scheduler Scheduler dataDecrease.txt


  To exit from the interactive shell in the container, simply type:
  ```bash
  exit
  ```
  or press `Ctrl+D`. This will close the shell and return you to your host system's terminal.

  ```

For more detailed instructions, see the individual README files:
- [InferProto README](./InferProto/README.md) - for running the crowd counting inference
- [Scheduler README](./Scheduler/README.md) - for running the vehicle scheduling system

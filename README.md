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

For instructions on how to run each program, see the individual README files:
- [InferProto README](./InferProto/README.md) - for running the crowd counting inference
- [Scheduler README](./Scheduler/README.md) - for running the vehicle scheduling system

# Use PyTorch base image with CUDA support
FROM pytorch/pytorch:2.1.0-cuda11.8-cudnn8-runtime

# Install Java 17 (OpenJDK) and system dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Set Java environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Set working directory
WORKDIR /app

# Install Python dependencies
# Upgrade pip first
RUN pip install --no-cache-dir --upgrade pip

# Install PyTorch with CUDA 12.8 support (will fallback to CUDA 11.8 if unavailable)
# Try CUDA 12.8 first, then fallback to CUDA 11.8 (matching base image)
RUN pip install --no-cache-dir torch==2.9.1+cu128 torchvision==0.24.1+cu128 --index-url https://download.pytorch.org/whl/cu128 2>/dev/null || \
    (echo "CUDA 12.8 PyTorch not available, falling back to CUDA 11.8..." && \
     pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cu118)

# Install remaining dependencies (excluding torch and torchvision)
RUN pip install --no-cache-dir ultralytics==8.3.228 opencv-python==4.12.0.88 numpy==2.2.6 Pillow==12.0.0 matplotlib==3.10.7 pyyaml==6.0.3

# Copy Scheduler Java source files and data files
COPY Scheduler/src/ ./Scheduler/src/
COPY Scheduler/data*.txt ./Scheduler/

# Compile Java files (output to Scheduler directory)
RUN javac -encoding UTF-8 -d ./Scheduler ./Scheduler/src/*.java

# Copy InferProto application code (includes models/weights/best.pt from the repository)
COPY InferProto/ ./InferProto/

# Set Python path
ENV PYTHONPATH=/app/InferProto

# Set matplotlib backend for Docker (non-interactive)
ENV MPLBACKEND=Agg

# No default command - run manually with docker-compose run


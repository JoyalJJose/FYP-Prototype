# Use PyTorch base image with CUDA support
FROM pytorch/pytorch:2.1.0-cuda11.8-cudnn8-runtime

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements file
COPY requirements.txt .

# Install Python dependencies
# Upgrade pip first
RUN pip install --no-cache-dir --upgrade pip

# Install PyTorch with CUDA 12.8 support (will fallback to CUDA 11.8 if unavailable)
# Try CUDA 12.8 first, then fallback to CUDA 11.8 (matching base image)
RUN pip install --no-cache-dir torch==2.9.1+cu128 torchvision==0.24.1+cu128 --index-url https://download.pytorch.org/whl/cu128 2>/dev/null || \
    (echo "CUDA 12.8 PyTorch not available, falling back to CUDA 11.8..." && \
     pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cu118)

# Install remaining dependencies from requirements.txt (excluding torch and torchvision)
# Filter out torch and torchvision, then install the rest
RUN pip install --no-cache-dir ultralytics==8.3.228 opencv-python==4.12.0.88 numpy==2.2.6 Pillow==12.0.0 matplotlib==3.10.7 pyyaml==6.0.3

# Copy application code (includes models/weights/best.pt from the repository)
COPY . .

# Set Python path
ENV PYTHONPATH=/app

# Set matplotlib backend for Docker (non-interactive)
# This allows matplotlib to work in Docker without a display
ENV MPLBACKEND=Agg

# No default command - run manually with docker-compose run


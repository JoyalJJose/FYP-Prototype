"""
Test script that randomly selects an image and runs inference with visualization.
"""

import os
import random
from pathlib import Path
import matplotlib
# Set backend for Docker/non-interactive environments
if os.environ.get('MPLBACKEND'):
    matplotlib.use(os.environ.get('MPLBACKEND'))
import matplotlib.pyplot as plt
import numpy as np
import cv2 as cv
from inference import ThermalCrowdCounter

# Get base directory (Docker-compatible)
BASE_DIR = Path(__file__).parent

# Paths
MODEL_PATH = BASE_DIR / "models" / "weights" / "best.pt"
IMAGES_DIR = BASE_DIR / "data" / "sample_images"

# Get all images
image_files = list(IMAGES_DIR.glob("*.jpg"))

# Load model
print(f"Loading model from {MODEL_PATH}...")
counter = ThermalCrowdCounter(str(MODEL_PATH), conf_threshold=0.25)

# Counter for sequential file naming
file_counter = 0

# Loop through random images until user exits
while True:
    # Randomly select one image
    selected_image = random.choice(image_files)
    print(f"Selected image: {selected_image.name}")
    
    # Run inference
    print(f"Running inference...")
    img_with_detections = counter.visualize_detections(selected_image)
    
    # Convert BGR to RGB for matplotlib
    img_rgb = cv.cvtColor(img_with_detections, cv.COLOR_BGR2RGB)
    
    # Get count
    count, _ = counter.count_people(selected_image)
    
    # Display image
    plt.figure(figsize=(12, 8))
    plt.imshow(img_rgb)
    plt.title(f"{selected_image.name}\nPeople Count: {count}", fontsize=14, fontweight='bold')
    plt.axis('off')
    plt.tight_layout()
    
    # In Docker/non-interactive mode, save image instead of showing
    if matplotlib.get_backend().lower() == 'agg':
        output_dir = BASE_DIR / "output"
        output_dir.mkdir(exist_ok=True)
        file_counter += 1
        output_path = output_dir / f"result_{file_counter:03d}.png"
        plt.savefig(output_path, dpi=150, bbox_inches='tight')
        print(f"Saved visualization to {output_path}")
        plt.close()
    else:
        plt.show()
    
    print(f"Processed image with {count} people detected")
    
    # Ask user to continue or exit
    user_input = input("Press Enter for next image, or 'q' to quit: ")
    if user_input.lower() == 'q':
        break


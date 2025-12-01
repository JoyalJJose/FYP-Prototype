"""
Test script that randomly selects an image and runs inference with visualization.
"""

import random
from pathlib import Path
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
    plt.show()
    
    print(f"Displayed image with {count} people detected")
    
    # Ask user to continue or exit
    user_input = input("Press Enter for next image, or 'q' to quit: ")
    if user_input.lower() == 'q':
        break


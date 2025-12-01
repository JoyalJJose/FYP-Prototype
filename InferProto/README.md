# InferProto

Crowd counting module that allows to count the number of people in a thermal image

## Contents

- `inference.py` - 'ThermalCrowdCounter' class for running inference
- `test_random_inference.py` - Script to test inference on images
- `models/weights/best.pt` - Trained YOLO model (~62 epochs)
- `data/sample_images/` - 50 sample thermal images for demonstration

## Quick Start

Run the inference script:

```bash
python test_random_inference.py
```

**Note**: If you're using Docker, make sure the Docker images have been built first (see main [README](../README.md)). The script will loop through randomly selected images, running inference and displaying each with bounding boxes and people count. Press Enter to see the next image, or 'q' to quit.

## Visualizations

### Running in Docker

When running in Docker (non-interactive mode), visualizations are automatically saved to the `output/` directory instead of being displayed on screen. Each processed image is saved as:

- **Location**: `InferProto/output/`
- **File naming**: `result_001.png`, `result_002.png`, `result_003.png`, etc. (sequentially numbered)
- **Example**: The first processed image will be saved as `result_001.png`, the second as `result_002.png`, and so on

The output directory is mounted as a volume in Docker, so you can access the saved visualizations directly from your host machine at `./InferProto/output/`.

### Running Locally

When running locally (outside Docker), visualizations are displayed interactively using matplotlib's default backend. The images will appear in a window, and you can view them before proceeding to the next image.



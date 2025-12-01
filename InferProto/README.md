# InferProto

Crowd counting module that allows to count the number of people in a thermal image

## Contents

- `inference.py` - 'ThermalCrowdCounter' class for running inference
- `test_random_inference.py` - Script to test inference on images
- `models/weights/best.pt` - Trained YOLO model (~62 epochs)
- `data/sample_images/` - 50 sample thermal images for demonstration
- `requirements.txt` - Python dependencies

## Quick Start

Simply run the inference script (all dependencies are already included in the Docker container):

```bash
python test_random_inference.py
```

The script will loop through randomly selected images, running inference and displaying each with bounding boxes and people count. Press Enter to see the next image, or 'q' to quit.



"""
Minimal inference module for thermal image crowd counting.
"""

import cv2
import numpy as np
from pathlib import Path
from ultralytics import YOLO
from typing import Union, Tuple
import torch


class ThermalCrowdCounter:
    # Counting people in thermal images using YOLO model
    
    def __init__(self, model_path: str, conf_threshold: float = 0.25):
        self.model = YOLO(model_path)
        self.conf_threshold = conf_threshold
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
    
    def count_people(self, image_path: Union[str, Path]) -> Tuple[int, dict]:
        # Count people in image
        results = self.model.predict(
            source=str(image_path),
            conf=self.conf_threshold,
            device=self.device,
            verbose=False
        )
        
        result = results[0]
        count = len(result.boxes)
        
        detection_info = {'count': count, 'boxes': [], 'scores': []}
        
        if count > 0:
            boxes = result.boxes.xyxy.cpu().numpy()
            scores = result.boxes.conf.cpu().numpy()
            detection_info['boxes'] = boxes.tolist()
            detection_info['scores'] = scores.tolist()
        
        return count, detection_info
    
    def visualize_detections(self, image_path: Union[str, Path]) -> np.ndarray:
        # Visualize detections on image
        img = cv2.imread(str(image_path))
        
        count, detection_info = self.count_people(image_path)
        
        for box, score in zip(detection_info['boxes'], detection_info['scores']):
            x1, y1, x2, y2 = map(int, box)
            cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(img, f'{score:.2f}', (x1, y1 - 10),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
        
        cv2.putText(img, f"People Count: {count}", (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        
        return img


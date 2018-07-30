from __future__ import print_function, division
from imageprocessor import process
from camera import Camera
import time

X_FOV = 35.9
Y_FOV = 27.3

vision = Camera(process, X_FOV, Y_FOV)
print('Camera resolution: %s' % str(vision.resolution()))

fps_sum = 0
samples = 0

while True:
    before = time.time()
    frame, img = vision.get_vision_frame()
    after = time.time()
    
    if frame is None or img is None:
        print('No camera detected!')
        break
    
    elapsed = after - before
    fps = 1.0/elapsed
    fps_sum += fps
    samples += 1
    avg_fps = fps_sum / samples
    print('\rFPS: %.1f, Avg. FPS: %.1f, Samples: %d' % (fps, avg_fps, samples), end='')
from __future__ import print_function, division
from imageprocessor import process
from camera import Camera
import time

X_FOV = 35.9
Y_FOV = 27.3

vision = Camera(process, X_FOV, Y_FOV)
print('Camera resolution: %s' % str(vision.resolution()))

fps_sum = 0
readings = 0

while True:
    before = time.time()
    r, img = vision.get_vision_frame()
    after = time.time()
    
    if not r:
        print('No camera detected!')
        break
    
    elapsed = after - before
    fps = 1.0/elapsed
    fps_sum += fps
    readings += 1
    print('\rFPS: %.1f, Avg. FPS: %.1f' % (fps, fps_sum / readings), end='')
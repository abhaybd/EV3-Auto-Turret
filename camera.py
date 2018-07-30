from __future__ import with_statement
import time
import cv2

# The tag changes depending on the version of cv2
if cv2.__version__[0] == '3':
    from cv2 import CAP_PROP_FRAME_WIDTH as WIDTH_TAG
    from cv2 import CAP_PROP_FRAME_HEIGHT as HEIGHT_TAG
else:
    from cv2.cv import CAP_PROP_FRAME_WIDTH as WIDTH_TAG
    from cv2.cv import CAP_PROP_FRAME_HEIGHT as HEIGHT_TAG

with open('resolutions.txt') as f:
    lines = f.readlines()
    resolutions = [[int(x) for x in line.split(',')] for line in lines]
    resolutions = sorted(resolutions, key=lambda x:x[0]*x[1])

def get_resolution(cam):
    return cam.get(WIDTH_TAG), cam.get(HEIGHT_TAG)

def set_resolution(cam, width, height):
    cam.set(WIDTH_TAG, width)
    cam.set(HEIGHT_TAG, height)
    set_width, set_height = get_resolution(cam)
    return set_width == width and set_height == height

def setup_camera(cam, req_resolution=None):
    if req_resolution is not None:
        if set_resolution(cam, *req_resolution):
            return
    prev_dims = get_resolution(cam)
    for width,height in resolutions:
        if set_resolution(cam,width,height):
            return
    set_resolution(cam,*prev_dims)

def millis():
    return round(time.time()*1000)
    
class VisionFrame(object):
    pass

class Camera(object):
    def __init__(self, image_processor, x_fov, y_fov, cam_index=0):
        self.processor = image_processor
        self.x_fov = x_fov
        self.y_fov = y_fov
        self.cam = cv2.VideoCapture(cam_index)
        setup_camera(self.cam)
    
    def resolution(self):
        return get_resolution(self.cam)
    
    def get_vision_frame(self, visualize=False):
        r, img = self.cam.read()
        if r:
            last_frame = VisionFrame()
            x,y = self.processor(img, visualize=visualize)
            if x == -1 and y == -1:
                last_frame.isTargetPresent = False
                x_deg = 0
                y_deg = 0
            else:
                last_frame.isTargetPresent = True
                width = img.shape[1]
                height = img.shape[0]
                y = height-y
                x_deg = round(((x-width/2.0) / (width/2.0)) * self.x_fov/2.0)
                y_deg = round(((y-height/2.0) / (height/2.0)) * self.y_fov/2.0)
            last_frame.targetPitch = y_deg
            last_frame.targetYaw = x_deg
            last_frame.timestamp = millis()
            return last_frame, img
        else:
            return None, None
    
    def release(self):
        self.cam.release()
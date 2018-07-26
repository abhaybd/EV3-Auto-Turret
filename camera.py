from __future__ import with_statement
from cv2 import CAP_PROP_FRAME_WIDTH as WIDTH_TAG
from cv2 import CAP_PROP_FRAME_HEIGHT as HEIGHT_TAG

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
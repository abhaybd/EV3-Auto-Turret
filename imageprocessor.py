import cv2
import numpy as np

face_cascade = cv2.CascadeClassifier('haarcascade_facedetection.xml')

def process(img, visualize=False):
    if type(img) is not np.ndarray:
        img = np.array(img)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(30,30),
            flags=cv2.CASCADE_SCALE_IMAGE)
    if len(faces) == 0:
        return -1, -1
    x,y,w,h = max(faces,key=lambda x: x[2]*x[3])
    if visualize:
        cv2.rectangle(img,(x,y),(x+w,y+h),(0,255,0), 2)
    return x+w/2,y+h/2
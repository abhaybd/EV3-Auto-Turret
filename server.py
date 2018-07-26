from __future__ import print_function, division
import socket as s
import time
import threading
from boltons.socketutils import BufferedSocket
import cv2
import json
from imageprocessor import process
from camera import setup_camera, get_resolution

# x fov and y fov of the webcam
X_FOV = 35.9
Y_FOV = 27.3
# TODO: only use diagonal fov and calculate x and y fov from resolution

VISUALIZE_FEED = False

port = 4444
udp_port = 4445

serversocket = s.socket()
serversocket.bind((s.gethostname(), port))
serversocket.listen(1)

def udp_listener_thread():
     udp_sock = s.socket(s.AF_INET, s.SOCK_DGRAM)
     udp_sock.bind((s.gethostname(),4445))
     print('Waiting for udp ping!')
     while True:
         msg, addr = udp_sock.recvfrom(256)
         print('Recieved udp ping from {}!'.format(addr))
         udp_sock.sendto(msg, addr)
         return

def millis():
    return round(time.time()*1000)

udp_thread = threading.Thread(target=udp_listener_thread, args=())
udp_thread.daemon = True
udp_thread.start()

print('Waiting for TCP connection...')
socket, addr = serversocket.accept()
print('Recieved TCP connection from {}!'.format(addr))
serversocket.close()

socket = BufferedSocket(socket)

class SimpleNamespace():
    pass

last_frame = SimpleNamespace()
lock = threading.Lock()

def communicate_thread():
    try:
        while True:
            request = socket.recv_until(b'\n',timeout=None).decode('utf-8')
            request = json.loads(request)
            if request['id'] == 1:
                lock.acquire()
                response = json.dumps(last_frame.__dict__) + '\n'
                lock.release()
                socket.send(response.encode())
                socket.flush()
            elif request['id'] == -1:
                break;
    except:
        pass
    global running
    running = False
    

cam = cv2.VideoCapture(0)
setup_camera(cam) # Reduce resolution to minimum
print('Camera resolution: %s' % str(get_resolution(cam)))

img_thread = threading.Thread(target=communicate_thread)
img_thread.daemon = True
img_thread.start()

running = True

while True:
    if not running:
        break
    global img
    r, img = cam.read()
    if r:
        x,y = process(img, visualize=VISUALIZE_FEED)
        lock.acquire()
        if x == -1 and y == -1:
            last_frame.isTargetPresent = False
            x_deg = 0
            y_deg = 0
        else:
            last_frame.isTargetPresent = True
            width = img.shape[1]
            height = img.shape[0]
            y = height-y
            x_deg = round(((x-width/2.0) / (width/2.0)) * X_FOV/2.0)
            y_deg = round(((y-height/2.0) / (height/2.0)) * Y_FOV/2.0)
        last_frame.targetPitch = y_deg
        last_frame.targetYaw = x_deg
        last_frame.timestamp = millis()
        lock.release()
        if VISUALIZE_FEED:
            cv2.imshow('Image',img)
            cv2.waitKey(1)

cam.release()
cv2.destroyAllWindows()
from __future__ import print_function, division
import socket as s
import time
import threading
from boltons.socketutils import BufferedSocket
import cv2
import json
from imageprocessor import process
from camera import Camera
from manualcontrol import ManualController

# Diagonal fov of webcam in degrees. X fov will be calculated with aspect ratio.
DIAG_FOV = 83

VISUALIZE_FEED = True

port = 4444
udp_port = 4445

serversocket = s.socket()
serversocket.bind(('0.0.0.0', port))
serversocket.listen(1)

def udp_listener_thread():
     udp_sock = s.socket(s.AF_INET, s.SOCK_DGRAM)
     udp_sock.bind(('0.0.0.0',4445))
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
                print('\r%s'%response, end='')
                socket.send(response.encode())
                socket.flush()
            elif request['id'] == -1:
                break;
    except:
        pass
    global running
    running = False
    

vision = Camera(process, DIAG_FOV)
print('Camera resolution: %s' % str(vision.resolution()))

controller = ManualController()

img_thread = threading.Thread(target=communicate_thread)
img_thread.daemon = True
img_thread.start()

running = True

while True:
    if not running:
        break
    if controller.manual_control():
        lock.acquire()
        last_frame = controller.get_frame()
        lock.release()
    else:
        global img
        frame, img = vision.get_vision_frame(visualize=VISUALIZE_FEED)
        if frame is not None and img is not None:
            lock.acquire()
            last_frame = frame
            lock.release()
            if VISUALIZE_FEED:
                cv2.imshow('Image',img)
                cv2.waitKey(1)

vision.release()
controller.release()
cv2.destroyAllWindows()
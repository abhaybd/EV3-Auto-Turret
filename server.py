import socket as s
import time
import threading
from boltons.socketutils import BufferedSocket
from types import SimpleNamespace
import cv2
import json
from PIL import Image
import yolo

port = 4444
udp_port = 4445

serversocket = s.socket()
serversocket.bind((s.gethostname(), port))
serversocket.listen()

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

socket = BufferedSocket(socket)

last_frame = SimpleNamespace()
lock = threading.Lock()

def reverse_bytes(img):
    return img[:,:,::-1]

def communicate_thread():
    while True:
        request = socket.recv_until(b'\n',timeout=None).decode('utf-8')
        request = json.loads(request)
        if request['id'] == 1:
            lock.acquire()
            response = json.dumps(last_frame.__dict__) + '\n'
            print(response)
            lock.release()
            socket.send(response.encode())
            socket.flush()
        elif request['id'] == -1:
            global running
            running = False
            break;

cam = cv2.VideoCapture(0)

img_thread = threading.Thread(target=communicate_thread, args=(), daemon=True)
img_thread.start()

running = True

while True:
    if not running:
        break
    global img
    r, img = cam.read()
    if r:
        rgb = reverse_bytes(img)
        rgb = Image.fromarray(rgb)
        bbox = yolo.get_pred(rgb,'person')
        lock.acquire()
        global last_frame
        last_frame = SimpleNamespace()
        if bbox == (-1,-1,-1,-1):
            last_frame.isTargetPresent = False
            x_deg = 0
            y_deg = 0
        else:
            last_frame.isTargetPresent = True
            x = (bbox[0]+bbox[2])/2.0 - img.shape[1]
            y = (bbox[1]+bbox[3])/2.0 - img.shape[0]
            X_FOV = 60
            Y_FOV = 60
            x_deg = round((x / (img.shape[1]/2.0)) * X_FOV/2.0)
            y_deg = round((y / (img.shape[0]/2.0)) * Y_FOV/2.0)
        last_frame.targetPitch = y_deg
        last_frame.targetYaw = x_deg
        last_frame.timestamp = millis()
        lock.release()
        cv2.imshow('Image',img)
        cv2.waitKey(1)

cam.release()
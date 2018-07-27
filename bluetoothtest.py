import bluetooth
from boltons.socketutils import BufferedSocket

EV3_MAC = '00:16:53:41:9A:21'

s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
s.connect((EV3_MAC,1))

socket = BufferedSocket(s)

while True:
    print(socket.recv_until('\n'))
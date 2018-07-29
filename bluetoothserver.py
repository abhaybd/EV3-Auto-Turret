from __future__ import print_function
import bluetooth
from boltons.socketutils import BufferedSocket

server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
server_sock.bind(("", bluetooth.PORT_ANY))

bluetooth.advertise_service(server_sock, "Server", service_classes=[bluetooth.SERIAL_PORT_CLASS], profiles=[bluetooth.SERIAL_PORT_PROFILE])

print('Waiting for connection...')
socket, client_info = server_sock.accept()
socket = BufferedSocket(socket)

print('Connection from ' + client_info)

while True:
    print(socket.recv_until('\n'))
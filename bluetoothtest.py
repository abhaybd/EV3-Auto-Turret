from __future__ import print_function

import serial

conn = serial.Serial('/dev/rfcomm0')
print('Connected to bluetooth')

while True:
    print(conn.readline())
from __future__ import print_function
import sys
import serial

if len(sys.argv) > 1:
    timeout=sys.argv[1]
else:
    timeout=100

conn = serial.Serial('/dev/rfcomm0',timeout=timeout)
print('Connected to bluetooth')

while True:
    print(conn.readline())
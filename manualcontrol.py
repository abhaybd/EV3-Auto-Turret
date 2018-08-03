from __future__ import division
import time
from gamepad import GamePad

class Frame(object):
    pass

def millis():
    return round(time.time()*1000)

class ManualController(object):
    def __init__(self, deadzone=0.08):
        self.deadzone = deadzone
        try:
            self.controller = GamePad()
        except:
            self.controller = None
    
    def connected(self):
        return self.controller is not None
    
    def manual_control(self):
        if not self.connected():
            return False
        return self.controller.left_trigger > self.deadzone
    
    def get_yaw_power(self):
        if not self.connected():
            return 0
        x = self.controller.left_stick_x
        return x if self.manual_control() and abs(x) > self.deadzone else 0
    
    def get_pitch_power(self):
        if not self.connected():
            return 0
        y = self.controller.right_stick_y
        return y if self.manual_control() and abs(y) > self.deadzone else 0
    
    def get_fire(self):
        if not self.connected():
            return False
        return self.manual_control() and self.controller.right_trigger > self.deadzone
    
    def get_frame(self):
        frame = Frame()
        frame.timestamp = millis()
        frame.manualOverride = self.manual_control()
        frame.pitchPower = self.get_pitch_power()
        frame.yawPower = self.get_yaw_power()
        frame.manualFire = self.get_fire()
        return frame
    
    def release(self):
        if self.connected():
            self.controller.close()
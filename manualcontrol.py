import xbox
import time

class Frame(object):
    pass

def millis():
    return round(time.time()*1000)

class ManualController(object):
    def __init__(self, deadzone=0.1):
        self.deadzone = deadzone
        try:
            self.controller = xbox.Joystick()
        except:
            self.controller = None
    
    def connected(self):
        return self.controller is not None
    
    def manual_control(self):
        if not self.connected():
            return False
        return self.controller.leftTrigger() > self.deadzone
    
    def get_yaw_power(self):
        if not self.connected():
            return 0
        return self.controller.leftX() if self.manual_control() else 0
    
    def get_pitch_power(self):
        if not self.connected():
            return 0
        return self.controller.rightY() if self.manual_control() else 0
    
    def get_fire(self):
        if not self.connected():
            return False
        return self.controller.rightTrigger() > self.deadzone if self.manual_control() else False
    
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
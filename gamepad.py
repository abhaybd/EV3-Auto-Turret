from __future__ import division
import inputs
import threading

mappings = {
        'ABS_Z' :'left_trigger',
        'ABS_RZ' :'right_trigger',
        'BTN_TL' :'left_bumper',
        'BTN_TR' :'right_bumper',
        'ABS_X' :'left_stick_x',
        'ABS_Y' :'left_stick_y',
        'ABS_RX' :'right_stick_x',
        'ABS_RY' :'right_stick_y',
        'BTN_SOUTH' :'a_button',
        'BTN_EAST' :'b_button',
        'BTN_WEST' :'x_button',
        'BTN_NORTH' :'y_button',
        'ABS_HAT0X' :'d_pad_x',
        'ABS_HAT0Y' :'d_pad_y'}

processors = {
        'ABS_Z' : lambda x: x/255.0,
        'ABS_RZ' : lambda x: x/255.0,
        'BTN_TL' : bool,
        'BTN_TR' : bool,
        'ABS_X' : lambda x: x/32767.0,
        'ABS_Y' : lambda x: x/32767.0,
        'ABS_RX' : lambda x: x/32767.0,
        'ABS_RY' : lambda x: x/32767.0,
        'BTN_SOUTH' : bool,
        'BTN_EAST' : bool,
        'BTN_WEST' : bool,
        'BTN_NORTH' : bool,
        'ABS_HAT0X' : lambda x: x,
        'ABS_HAT0Y' : lambda x: x}

class GamePad(object):
    def __init__(self, device=None):
        if device is None:
            get_events = inputs.get_gamepad
        else:
            get_events = device.read
        for attr in mappings.values():
            setattr(self, attr, 0)
        self.running = True
        thread = threading.Thread(target=self._event_thread, args=(get_events,))
        thread.daemon = True
        thread.start()

    def _event_thread(self, get_events):
        while self.running:
            events = get_events()
            for event in events:
                if event.code in mappings:
                    setattr(self, mappings[event.code], processors[event.code](event.state))
    
    def close(self):
        self.running = False
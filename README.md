# EV3-Auto-Turret

This was built using [leJOS EV3](http://www.lejos.org/ev3.php) to run Java on an EV3 robot. Additionally, a Raspberry Pi Model B+ was used for vision processing.

The robot (client) runs Java 1.7, and the Raspberry Pi (server) runs Python 2.7.

~I'll make a requirements.txt later~ (nevermind I made one) but here are the requirements for now:

### Java:
* leJOS Eclipse plugin
* Gson (for data serialization)

### Python 2.7:
* opencv (for vision)
* numpy (for data manipulation)
* boltons (for easier tcp management)

A turret that automatically shoots faces. Unlike my [other project](https://github.com/coolioasjulio/EV3-Auto-Vision-Drive), hopefully I'll finish this one. This time, I'm strapping a Raspberry Pi to the robot with the webcam attached to it. It will be running a haar cascade for face detection. The EV3 turret will query the Raspberry Pi for vision data, (where are the faces) move to point at that location, and then shoot.

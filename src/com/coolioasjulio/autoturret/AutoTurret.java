package com.coolioasjulio.autoturret;

import com.coolioasjulio.autoturret.RemoteVisionProcessor.VisionFrame;

import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.RegulatedMotor;

public class AutoTurret {
    private static int FIRE_SPEED = 90;
    private static int YAW_ERR_THRESHOLD = 2;
    private static int PITCH_ERR_THRESHOLD = 2;

    private static double YAW_POS_FACTOR = 3.0; // tach units per degree turned
						// (gear ratio)
    private static double PITCH_POS_FACTOR = 1.0; // tach units per degree
						  // turned (gear ratio)
    private static int YAW_MID_TO_LIMIT_ANGLE = 270; // tach units from midpoint
						     // (origin) to forward
						     // limit switch
    private static int PITCH_MID_TO_LIMIT_ANGLE = 52; // tach units from
						      // midpoint (origin) to
						      // the motor thing

    public static void main(String[] args) {
	AutoTurret turret = new AutoTurret();
	turret.start();

	Button.ESCAPE.waitForPress();
    }

    private RemoteVisionProcessor vision;
    private volatile VisionFrame visionFrame;
    private Thread visionThread;
    private Thread trackingThread;
    private final Object lock = new Object();

    private RegulatedMotor pitchMotor, yawMotor, fireMotor;
    private EV3TouchSensor yawForwardLimitSwitch;

    public AutoTurret() {
	yawMotor = new EV3LargeRegulatedMotor(MotorPort.A);
	pitchMotor = new EV3LargeRegulatedMotor(MotorPort.D);
	fireMotor = new EV3MediumRegulatedMotor(MotorPort.B);

	yawForwardLimitSwitch = new EV3TouchSensor(SensorPort.S1);
    }

    public void start() {
	startVisionProcessing();
	startTrackingPosition();
    }

    public void stop() {
	visionThread.interrupt();
	trackingThread.interrupt();
	visionThread = null;
	trackingThread = null;
	vision.stop();
    }

    private void startVisionProcessing() {
	if (visionThread != null) {
	    visionThread.interrupt();
	    try {
		visionThread.join();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	visionThread = new Thread(new Runnable() {
	    public void run() {
		System.out.println("Connecting to server...");
		vision = RemoteVisionProcessor.getInstance();
		System.out.println("Connected!");
		while (!Thread.interrupted()) {
		    try {
			VisionFrame vf = vision.process();
			synchronized (lock) {
			    visionFrame = vf;
			}
		    } catch (Exception e) {
			e.printStackTrace();
			return;
		    }
		}
	    }
	});
	visionThread.start();
    }

    private void zeroCalibrate() {
	// Zero calibrate the yaw motor by running it into the limit switch
	float[] sample = new float[yawForwardLimitSwitch.sampleSize()];
	yawMotor.forward();
	do {
	    yawForwardLimitSwitch.fetchSample(sample, 0);
	} while (sample[0] == 0);

	yawMotor.stop();
	yawMotor.rotate(-YAW_MID_TO_LIMIT_ANGLE);
	yawMotor.resetTachoCount(); // This is the new zero

	// Zero calibrate the pitch motor by stalling it (i'm not made of touch
	// sensors)
	pitchMotor.setSpeed(20);
	// Wait for the motor to stall
	while (!pitchMotor.isStalled()) {
	    // Intentionally empty
	}
	pitchMotor.stop();
	pitchMotor.rotate(-PITCH_MID_TO_LIMIT_ANGLE);
	pitchMotor.resetTachoCount(); // this is the new zero
    }

    private void startTrackingPosition() {
	if (trackingThread != null) {
	    trackingThread.interrupt();
	    try {
		trackingThread.join();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	trackingThread = new Thread(new Runnable() {
	    public void run() {
		zeroCalibrate();
		while (!Thread.interrupted()) {
		    trackTargetPeriodic();
		}
	    }
	});
	trackingThread.start();
    }

    private void trackTargetPeriodic() {
	VisionFrame vf;
	synchronized (lock) {
	    if (visionFrame == null)
		return;
	    vf = visionFrame.copy();
	}

	if (System.currentTimeMillis() - vf.getTimestamp() > 1000)
	    return;

	if (vf.isManualOverride()) {
	    pitchMotor.setSpeed(round(vf.getPitchPower() * pitchMotor.getMaxSpeed()));
	    yawMotor.setSpeed(round(vf.getYawPower() * yawMotor.getMaxSpeed()));
	    fireMotor.setSpeed(vf.isManualFire() ? FIRE_SPEED : 0);
	} else {
	    int targetPitch = round(vf.getTargetPitch() * PITCH_POS_FACTOR);
	    int targetYaw = round(vf.getTargetYaw() * YAW_POS_FACTOR);
	    pitchMotor.rotateTo(targetPitch, true);
	    yawMotor.rotateTo(targetYaw, true);
	    if (vf.isTargetPresent() && onTarget(targetYaw, targetPitch)) {
		fireMotor.setSpeed(FIRE_SPEED);
	    }
	}
    }

    private boolean onTarget(int targetYaw, int targetPitch) {
	return Math.abs(yawMotor.getLimitAngle() - targetYaw) <= YAW_ERR_THRESHOLD
		&& Math.abs(pitchMotor.getLimitAngle() - targetPitch) <= PITCH_ERR_THRESHOLD;
    }

    private int round(double d) {
	return (int) Math.floor(d + 0.5);
    }
}

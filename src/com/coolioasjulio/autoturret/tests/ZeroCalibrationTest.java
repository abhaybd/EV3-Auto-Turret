package com.coolioasjulio.autoturret.tests;

import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.RegulatedMotor;

public class ZeroCalibrationTest {
    private static int YAW_MID_TO_LIMIT_ANGLE = 270; // tach units from midpoint
    // (origin) to forward
    // limit switch
    private static int PITCH_MID_TO_LIMIT_ANGLE = 52; // tach units from
    // midpoint (origin) to
    // the motor thing

    public static void main(String[] args) {
	Thread t = new Thread(new Runnable() {
	    public void run() {
		while(!Thread.interrupted()) {
		    if(Button.ESCAPE.isDown()) System.exit(1);
		}
	    }
	});
	t.setDaemon(true);
	t.start();
	
	ZeroCalibrationTest test = new ZeroCalibrationTest();
	System.out.println("Starting...");
	test.zeroCalibrate();
	System.out.println("Done!");
	t.interrupt();
    }

    private RegulatedMotor yawMotor, pitchMotor;
    private EV3TouchSensor yawForwardLimitSwitch;

    public ZeroCalibrationTest() {
	yawMotor = new EV3LargeRegulatedMotor(MotorPort.A);
	pitchMotor = new EV3LargeRegulatedMotor(MotorPort.D);

	yawForwardLimitSwitch = new EV3TouchSensor(SensorPort.S1);
    }

    public void zeroCalibrate() {
	System.out.print("Calibrating yaw...");
	// Zero calibrate the yaw motor by running it into the limit switch
	float[] sample = new float[yawForwardLimitSwitch.sampleSize()];
	yawMotor.forward();
	do {
	    yawForwardLimitSwitch.fetchSample(sample, 0);
	} while (sample[0] == 0);

	yawMotor.stop();
	yawMotor.rotate(-YAW_MID_TO_LIMIT_ANGLE);
	yawMotor.resetTachoCount(); // This is the new zero
	
	System.out.println("Done!");
	System.out.print("Calibrating pitch...");

	// Zero calibrate the pitch motor by stalling it (bitch i ain't made of touch
	// sensors)
	pitchMotor.forward();
	// Wait for the motor to stall
	while (!pitchMotor.isStalled()) {
	    // Intentionally empty
	}
	pitchMotor.stop();
	pitchMotor.rotate(-PITCH_MID_TO_LIMIT_ANGLE);
	pitchMotor.resetTachoCount(); // this is the new zero
	System.out.println("Done!");
    }
}

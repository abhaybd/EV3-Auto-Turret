package com.coolioasjulio.autoturret;

import com.coolioasjulio.autoturret.RemoteVisionProcessor.VisionFrame;

import lejos.hardware.Button;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

public class AutoTurret {
    private static int FIRE_SPEED = 90;
    private static int YAW_ERR_THRESHOLD = 2;
    private static int PITCH_ERR_THRESHOLD = 2;
    
    private static double YAW_POS_FACTOR = 2.5; // tach units per degree turned (gear ratio)
    private static double PITCH_POS_FACTOR = 1.0; // tach units per degree turned (gear ratio)
    
    public static void main(String[] args) {
	AutoTurret turret = new AutoTurret();
	turret.start();
	
	Button.ESCAPE.waitForPress();
    }
    
    private RemoteVisionProcessor vision;
    private VisionFrame visionFrame;
    private Thread visionThread;
    private Thread trackingThread;
    private final Object lock = new Object();
    
    private RegulatedMotor pitchMotor, yawMotor, fireMotor;
    public AutoTurret() {
	yawMotor = new EV3LargeRegulatedMotor(MotorPort.A);
	pitchMotor = new EV3LargeRegulatedMotor(MotorPort.D);
	fireMotor = new EV3MediumRegulatedMotor(MotorPort.B);
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
	if(visionThread != null) {
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
		while(!Thread.interrupted()) {
		    try {
			VisionFrame vf = vision.process();			
			synchronized(lock) {
			    visionFrame = vf;
			}
		    } catch(Exception e) {
			e.printStackTrace();
			return;
		    }
		}
	    }
	});
    }
    
    private void zeroCalibrate() {
	// yeah, i hope it's centered. cuz i ain't doing it
	yawMotor.resetTachoCount();
	pitchMotor.resetTachoCount();
    }
    
    private void startTrackingPosition() {
	if(trackingThread != null) {
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
		while(!Thread.interrupted()) {
		    trackTargetPeriodic();
		}
	    }
	});
	trackingThread.start();
    }
    
    private void trackTargetPeriodic() {
	VisionFrame vf;
	synchronized(lock) {
	    if(visionFrame == null) return;
	    vf = visionFrame.copy();
	}
	
	if(vf.isManualOverride()) {
	    pitchMotor.setSpeed(round(vf.getPitchPower() * pitchMotor.getMaxSpeed()));
	    yawMotor.setSpeed(round(vf.getYawPower() * yawMotor.getMaxSpeed()));
	    fireMotor.setSpeed(vf.isManualFire() ? FIRE_SPEED : 0);
	} else {
	    int targetPitch = round(vf.getTargetPitch() * PITCH_POS_FACTOR);
	    int targetYaw = round(vf.getTargetYaw() * YAW_POS_FACTOR);
	    pitchMotor.rotateTo(targetPitch, true);
	    yawMotor.rotateTo(targetYaw, true);
	    if(vf.isTargetPresent() && onTarget(targetYaw, targetPitch)) {
		fireMotor.setSpeed(FIRE_SPEED);
	    }
	}
    }
    
    private boolean onTarget(int targetYaw, int targetPitch) {
	return Math.abs(yawMotor.getLimitAngle() - targetYaw) <= YAW_ERR_THRESHOLD
		&& Math.abs(pitchMotor.getLimitAngle() - targetPitch) <= PITCH_ERR_THRESHOLD;
    }
    
    private int round(double d) {
	return (int)Math.floor(d+0.5);
    }
}

package com.coolioasjulio.autoturret;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

public class Test {
    public static void main(String[] args) {
	final RegulatedMotor yawMotor = new EV3LargeRegulatedMotor(MotorPort.A);
	final RegulatedMotor pitchMotor = new EV3LargeRegulatedMotor(MotorPort.D);
	Thread t = new Thread(new Runnable() {
	    public void run() {
		LCD.clearDisplay();
		while(!Thread.interrupted()) {
		    LCD.drawString(String.format("%d yaw - %d pitch", 
			    yawMotor.getTachoCount(),  pitchMotor.getTachoCount()), 0, 0);
		    try {
			Thread.sleep(20);
		    } catch (InterruptedException e) {
			return;
		    }
		}		
	    }
	});
	t.setDaemon(true);
	t.start();
	
	Button.ESCAPE.waitForPress();
	t.interrupt();
	yawMotor.close();
	pitchMotor.close();
    }
}

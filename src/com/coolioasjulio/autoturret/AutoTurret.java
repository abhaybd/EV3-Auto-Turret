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
    private static final int FIRE_SPEED = 90;
    private static final int YAW_ERR_THRESHOLD = 5;
    private static final int PITCH_ERR_THRESHOLD = 5;
    
    private static final double MANUAL_YAW_POWER_LIMIT = 0.3;
    private static final double MANUAL_PITCH_POWER_LIMIT = 0.2;
    
    private static final double AUTO_YAW_POWER_LIMIT = 1.0;
    private static final double AUTO_PITCH_POWER_LIMIT = 0.8;
    
    private static final int LED_OFF_PATTERN = 0;
    private static final int LED_AUTO_PATTERN = 4;
    private static final int LED_MANUAL_PATTERN = 7;

    private static final double YAW_POS_FACTOR = 3.0; // tach units per degree turned
                                                // (gear ratio)
    private static final double PITCH_POS_FACTOR = 1.0; // tach units per degree
                                                  // turned (gear ratio)
    private static final int YAW_MID_TO_LIMIT_ANGLE = 270; // tach units from midpoint
                                                     // (origin) to forward
                                                     // limit switch
    private static final int PITCH_MID_TO_LIMIT_ANGLE = 52; // tach units from
                                                      // midpoint (origin) to
                                                      // the motor thing

    private static final int VISION_REFRESH_PER_SECOND = 20; // How many times per
                                                       // second to grab
                                                       // information from
                                                       // vision server

    public static void main(String[] args) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (!Thread.interrupted()) {
                    if (Button.ESCAPE.isDown()) {
                        Thread.currentThread().interrupt();
                        Button.LEDPattern(LED_OFF_PATTERN); // Stop LEDS
                        System.exit(1);
                    }
                }
            }
        });
        t.setDaemon(false);
        t.start();

        AutoTurret turret = new AutoTurret();
        turret.start();
    }

    private RemoteVisionProcessor vision;
    private volatile VisionFrame visionFrame;
    private Thread visionThread;
    private Thread trackingThread;
    private final Object lock = new Object();
    private boolean wasManualLast = true;

    private RegulatedMotor pitchMotor, yawMotor, fireMotor;
    private EV3TouchSensor yawForwardLimitSwitch;

    public AutoTurret() {
        yawMotor = new EV3LargeRegulatedMotor(MotorPort.A);
        pitchMotor = new EV3LargeRegulatedMotor(MotorPort.D);
        fireMotor = new EV3MediumRegulatedMotor(MotorPort.B);
        fireMotor.setSpeed(FIRE_SPEED);

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
                try {
                    while (!Thread.interrupted()) {
                        long startTime = System.currentTimeMillis();
                        VisionFrame vf = vision.process();
                        synchronized (lock) {
                            visionFrame = vf;
                        }
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        long interval = Math.round(1000.0 / (double) VISION_REFRESH_PER_SECOND);
                        long waitTimeMillis = interval - elapsedTime;
                        if (waitTimeMillis > 0) {
                            Thread.sleep(waitTimeMillis);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
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
        pitchMotor.forward();
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

        // If the last vision frame is stale, don't do nothin'
        if (System.currentTimeMillis() - vf.getTimestamp() > 1000) {
            return;
        }

        if (vf.isManualOverride()) {
            if(!wasManualLast) {
                Button.LEDPattern(LED_MANUAL_PATTERN);
            }
            
            int pitchSpeed = round(MANUAL_PITCH_POWER_LIMIT * vf.getPitchPower() * pitchMotor.getMaxSpeed());
            int yawSpeed = round(MANUAL_YAW_POWER_LIMIT * vf.getYawPower() * yawMotor.getMaxSpeed());
            
            setSpeed(pitchMotor, pitchSpeed);
            setSpeed(yawMotor, yawSpeed);
            setSpeed(fireMotor, vf.isManualFire() ? FIRE_SPEED : 0);
            
            wasManualLast = true;
        } else {
            if(wasManualLast) {
                Button.LEDPattern(LED_AUTO_PATTERN);
            }
            pitchMotor.setSpeed(round(AUTO_PITCH_POWER_LIMIT * pitchMotor.getMaxSpeed()));
            yawMotor.setSpeed(round(AUTO_YAW_POWER_LIMIT * yawMotor.getMaxSpeed()));
            int targetPitch = round(vf.getTargetPitch() * PITCH_POS_FACTOR);
            int targetYaw = round(vf.getTargetYaw() * YAW_POS_FACTOR);
            pitchMotor.rotateTo(targetPitch, true);
            yawMotor.rotateTo(targetYaw, true);
            int fireSpeed = vf.isTargetPresent() && onTarget(targetYaw, targetPitch) ? FIRE_SPEED : 0;
            setSpeed(fireMotor, fireSpeed);
        }
    }
    
    private void setSpeed(RegulatedMotor motor, int speed) {
        if(speed != 0){
            motor.setSpeed(speed);
            if(speed > 0){
                motor.forward();
            } else {
                motor.backward();
            }
        } else {
            motor.stop(true);
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

package com.coolioasjulio.autoturret;

import com.coolioasjulio.autoturret.RemoteVisionProcessor.VisionFrame;

public class Test {
    
    public static void main(String[] args) {
	Test turret = new Test();
	turret.start();
	
	try {
	    synchronized(turret) {
		turret.wait();		
	    }
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
    
    private RemoteVisionProcessor vision;
    private volatile VisionFrame visionFrame;
    private Thread visionThread;
    private Thread trackingThread;
    private final Object lock = new Object();
    
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
		try {
		    while(!Thread.interrupted()) {
			VisionFrame vf = vision.process();			
			synchronized(lock) {
			    visionFrame = vf;
			}
		    }
		} catch(Exception e) {
		    e.printStackTrace();
		    return;
		}
	    }
	});
	visionThread.start();
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
		while(!Thread.interrupted()) {
		    trackTargetPeriodic();
//		    try {
//			Thread.sleep(1000);
//		    } catch (InterruptedException e) {
//			e.printStackTrace();
//			return;
//		    }
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
	
	System.out.println(vf.toString());
    }
}

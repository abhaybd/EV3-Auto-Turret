package com.coolioasjulio.autoturret;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class RemoteVisionProcessor {
    private static RemoteVisionProcessor instance;
    
    public static RemoteVisionProcessor getInstance() {
	if(instance == null) {
	    try {
		instance = new RemoteVisionProcessor();
	    } catch (IOException e) {
		throw new RuntimeException(e);
	    }
	}
	return instance;
    }
    
    private Socket socket;
    private PrintStream out;
    private BufferedReader in;
    private Gson gson;
    private final Object lock = new Object();
    private RemoteVisionProcessor() throws IOException {
	InetAddress server = NetworkUtils.pingAll(4445);
	socket = new Socket(server, 4444);
	out = new PrintStream(socket.getOutputStream());
	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	gson = new Gson();
    }
    
    public VisionFrame process() {
	try {
	    synchronized(lock) {
		Request r = new Request(Request.FRAME_REQ_ID);
		String jsonRequest = gson.toJson(r);
		out.println(jsonRequest);
		out.flush();
		String jsonResponse = in.readLine();
		VisionFrame response = gson.fromJson(jsonResponse, new TypeToken<VisionFrame>() {}.getType());
		return response;		
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    return null;
	}
    }
    
    public void processAsync(final VisionFrameCallback callback) {
	new Thread(new Runnable() {
	    public void run() {
		callback.onProcess(process());
	    }
	}).start();
    }
    
    public void stop() {
	Request r = new Request(Request.STOP_ID);
	out.println(gson.toJson(r));
	out.flush();
	try {
	    socket.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	instance = null;
    }
    
    private static interface VisionFrameCallback {
	void onProcess(VisionFrame frame);
    }
    
    private static class Request {
	private static int FRAME_REQ_ID = 1;
	private static int STOP_ID = -1;
	
	@SuppressWarnings("unused")
	private int id;
	
	public Request(int id) {
	    this.id = id;
	}
    }
    
    public static class VisionFrame {
	private boolean isTargetPresent;
	private int targetPitch;
	private int targetYaw;
	private boolean manualOverride;
	private double pitchPower;
	private double yawPower;
	private boolean manualFire;
	
	public boolean isTargetPresent() {
	    return isTargetPresent;
	}
	public int getTargetPitch() {
	    return targetPitch;
	}
	public int getTargetYaw() {
	    return targetYaw;
	}
	public boolean isManualOverride() {
	    return manualOverride;
	}
	public double getPitchPower() {
	    return pitchPower;
	}
	public double getYawPower() {
	    return yawPower;
	}
	public boolean isManualFire() {
	    return manualFire;
	}
	
	public VisionFrame copy() {
	    VisionFrame vs = new VisionFrame();
	    vs.isTargetPresent = isTargetPresent;
	    vs.targetPitch = targetPitch;
	    vs.targetYaw = targetYaw;
	    vs.manualOverride = manualOverride;
	    vs.pitchPower = pitchPower;
	    vs.yawPower = yawPower;
	    vs.manualFire = manualFire;
	    return vs;
	}
    }
}

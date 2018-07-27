package com.coolioasjulio.autoturret.tests;

import java.io.PrintStream;

import lejos.hardware.Button;
import lejos.remote.nxt.BTConnection;
import lejos.remote.nxt.BTConnector;

public class BluetoothTest {
    public static void main(String[] args){
	Thread t = new Thread(new Runnable(){
	    public void run() {
		while(!Thread.interrupted()){
		    if(Button.ESCAPE.isDown()) System.exit(1);
		}
	    }
	});
	t.setDaemon(true);
	t.start();
	
	BTConnector connector = new BTConnector();
	BTConnection connection = connector.waitForConnection(100, BTConnection.RAW);
	
	PrintStream out = new PrintStream(connection.openOutputStream());
	while(true) {
	    out.println(System.currentTimeMillis());
	    out.flush();
	    try {
		Thread.sleep(500);
	    } catch (InterruptedException e) {
		e.printStackTrace();
		t.interrupt();
		return;
	    }
	}
    }
}

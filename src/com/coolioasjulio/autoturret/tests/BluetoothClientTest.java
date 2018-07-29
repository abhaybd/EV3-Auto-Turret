package com.coolioasjulio.autoturret.tests;

import java.io.PrintStream;

import lejos.hardware.Button;
import lejos.remote.nxt.BTConnection;
import lejos.remote.nxt.BTConnector;

public class BluetoothClientTest {
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
	BTConnection connection = connector.connect("00:1a:7d:da:71:13",BTConnection.RAW);
	
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

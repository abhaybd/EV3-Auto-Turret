package com.coolioasjulio.autoturret.tests;

import java.io.PrintStream;

import lejos.hardware.Button;
import lejos.remote.nxt.BTConnection;
import lejos.remote.nxt.BTConnector;

public class BluetoothTest {
    public static void main(String[] args){
	BTConnector connector = new BTConnector();
	BTConnection connection = connector.connect("00:1A:7D:DA:71:13", BTConnection.RAW);
	
	PrintStream out = new PrintStream(connection.openOutputStream());
	while(!Button.ESCAPE.isDown()) {
	    out.println(System.currentTimeMillis());
	    out.flush();
	}
    }
}

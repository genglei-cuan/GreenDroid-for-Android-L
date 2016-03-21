package edu.nju.Alex.GreenDroid;

import java.util.ArrayList;

import android.app.Service;

public class ServiceState {
	//the real service
	public Service service;
	//the length of the three lists (e.g., conns, ibinders, and intents) should be the same (for handling bindService() case)
	//the connections bound with this service
	public ArrayList<Object> conns = new ArrayList<Object>();
	
	//the ibinders returned by the onBind()
	public ArrayList<Object> ibinders = new ArrayList<Object>();
	
	//the intents used to bind this service
	public ArrayList<Object> intents = new ArrayList<Object>();
	
	public ServiceState(Service service){
		this.service=service;
	}
	public static boolean containConn(ServiceState ss, Object conn){
		if(ss.conns.indexOf(conn)!=-1){
			return true;
		}else{
			return false;
		}
	}
}

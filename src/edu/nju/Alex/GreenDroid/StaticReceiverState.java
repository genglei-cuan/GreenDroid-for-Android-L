//all the state of a satic receiver, using old version, needs to update?
package edu.nju.Alex.GreenDroid;

import java.util.ArrayList;

public class StaticReceiverState {
	public String rcvName;
	public String permission;
	public ArrayList<String> actions;
	public StaticReceiverState(String rcvName, String permission, ArrayList<String> actions){
		this.rcvName=rcvName;
		this.permission=permission;
		this.actions=actions;
	}
}

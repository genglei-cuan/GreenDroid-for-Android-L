package edu.nju.Alex.GreenDroid;

import java.util.ArrayList;

import android.app.Fragment;

public class FragmentState {
	//the fragment state
	public Fragment fragment;
	
	//the corresponding classpath
	public String fragmentClass;
	
	public int lastExecutedLCHandler;
	//the parent Activity
	public ActivityState actParent;
	
	//the layout id
	public int layoutId;
	
	//the parent Activity's layout id the its own view id
	public int viewId;
	public int actLayoutId;
	
	/**
	 * registeredGUIComponentList tracks a list of GUI components that has registered listeners.
	 * There is index correspondence between registeredGUIComponentList and eventsList
	 */
	public ArrayList<Object> registeredGUIComponentList;
	public ArrayList<String> registeredGUIComponentText;
	
	/**
	 * eventsList[i] tracks the events that the registeredGUIComponentList[i] can respond to.
	 */
	public ArrayList<ArrayList<Integer>> eventsList;
	
	public boolean destroyed;
	
	public FragmentState(){
		registeredGUIComponentList=new ArrayList<Object>();
		registeredGUIComponentText=new ArrayList<String>();
		eventsList=new ArrayList<ArrayList<Integer>>();
		
		destroyed=false;
		fragment=null;
		actParent=null;
		fragmentClass=null;
		lastExecutedLCHandler=-1;
		layoutId=-1;
		viewId=-1;
		actLayoutId=-1;
	}
}

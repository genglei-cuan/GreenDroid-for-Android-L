package edu.nju.Alex.GreenDroid;

public interface ActivityLifeCycleHandler {
	public static final int ON_CREATE = 0;
	public static final int ON_START = 1;
	public static final int ON_RESUME = 2;
	public static final int ON_PAUSE = 3;
	public static final int ON_STOP = 4;
	public static final int ON_RESTART = 5;
	public static final int ON_DESTROY = 6;
	public static final int ON_BACKPRESSED = 7;
	
	public static final String ON_CREATE_STRING = "onCreate";
	public static final String ON_START_STRING = "onStart";
	public static final String ON_RESUME_STRING = "onResume";
	public static final String ON_PAUSE_STRING = "onPause";
	public static final String ON_STOP_STRING = "onStop";
	public static final String ON_RESTART_STRING = "onRestart";
	public static final String ON_DESTROY_STRING = "onDestroy";
	public static final String ON_BACKPRESSED_STRING = "onBackPressed";
}

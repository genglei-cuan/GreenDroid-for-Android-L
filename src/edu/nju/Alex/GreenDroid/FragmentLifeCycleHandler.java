package edu.nju.Alex.GreenDroid;

public interface FragmentLifeCycleHandler {
	public static final int ON_ATTACH=0;
	public static final int ON_CREATE=1;
	public static final int ON_CREATEVIEW=2;
	public static final int ON_ACTIVITYCREATED=3;
	public static final int ON_VIEWSTATERESTORED=4;
	public static final int ON_START=5;
	public static final int ON_RESUME=6;
	public static final int ON_PAUSE=7;
	public static final int ON_STOP=8;
	public static final int ON_DESTORYVIEW=9;
	public static final int ON_DESTORY=10;
	public static final int ON_DETACH=11;
	
	public static final String ON_ATTACH_STRING="onAttach";
	public static final String ON_CREATE_STRING="onCreate";
	public static final String ON_CREATEVIEW_STRING="onCreateView";
	public static final String ON_ACTIVITYCREATED_STRING="onActivityCreated";
	public static final String ON_VIEWSTATERESTORED_STRING="onViewStateRestored";
	public static final String ON_START_STRING="onStart";
	public static final String ON_RESUME_STRING="onResume";
	public static final String ON_PAUSE_STRING="onPause";
	public static final String ON_STOP_STRING="onStop";
	public static final String ON_DESTORY_STRING="onDestory";
	public static final String ON_DETACH_STRING="onDetach";
}

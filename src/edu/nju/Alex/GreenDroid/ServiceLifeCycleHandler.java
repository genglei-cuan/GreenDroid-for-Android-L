/**
 * @author andrew
 * File last modified on Dec. 1, 2012
 */
package edu.nju.Alex.GreenDroid;

public interface ServiceLifeCycleHandler {
	public static final String ON_CREATE_STRING = "onCreate";
	public static final String ON_START_STRING = "onStart";
	public static final String ON_STARTCOMMAND_STRING = "onStartCommand";
	public static final String ON_HANDLE_INTENT = "onHandleIntent";
	public static final String ON_BIND_STRING = "onBind";
	public static final String ON_UNBIND_STRING = "onUnbind";
	public static final String ON_DESTROY_STRING = "onDestroy";
}

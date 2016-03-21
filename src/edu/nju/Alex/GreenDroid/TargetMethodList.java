package edu.nju.Alex.GreenDroid;

public interface TargetMethodList {
	
	//this is a list of method signatures of those API calls we want to monitor using JPF listeners
	//they are not suitable for being modeled as native peers
	public static final String CLICK = "setOnClickListener(Landroid/view/View$OnClickListener;)V";
	public static final String LONG_CLICK = "setOnLongClickListener(Landroid/view/View$OnLongClickListener;)V";
	public static final String TOUCH = "setOnTouchListener(Landroid/view/View$OnTouchListener;)V";
	public static final String HOVER = "setOnHoverListener(Landroid/view/View$OnHoverListener;)V";
	public static final String CREATE_CONTEXT_MENU = "setOnCreateContextMenuListener(Landroid/view/View$OnCreateContextMenuListener;)V";
	public static final String DRAG = "setOnDragListener(Landroid/view/View$OnDragListener;)V";
	public static final String KEY = "setOnKeyListener(Landroid/view/View$OnKeyListener;)V";
	public static final String START_ACT = "startActivity(Landroid/content/Intent;)V";
	public static final String START_ACT_FOR_RESULT = "startActivityForResult(Landroid/content/Intent;I)V";
	public static final String SET_RESULTCODE = "setResult(I)V";
	public static final String SET_RESULTCODE_DATA = "setResult(ILandroid/content/Intent;)V";
	//in current implementation we only consider that activity GUI layout is set using setContentView(int) and inflate(int,ViewGroup,boolean)
	public static final String SET_CONTENT_VIEW = "setContentView(I)V";
	public static final String ON_CREATE_VIEW = "onCreateView(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;)Landroid/view/View;";
	public static final String INFLATE = "inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;";
	public static final String START_SER = "startService(Landroid/content/Intent;)Landroid/content/ComponentName;";
	public static final String BIND_SER = "bindService(Landroid/content/Intent;Landroid/content/ServiceConnection;I)Z";
	public static final String UNBIND_SER = "unbindService(Landroid/content/ServiceConnection;)V";
	public static final String ACT_FINISH = "finish()V";
	public static final String FINISH_ACT = "finishActivity(I)V";
	public static final String STOP_SELF = "stopSelf()V";
	public static final String STOP_SELF_INT = "stopSelf(I)V";
	public static final String STOP_SELF_RESULT = "stopSelfResult(I)Z";
	public static final String STOP_SER = "stopService(Landroid/content/Intent;)Z";
	public static final String ON_BIND = "onBind(Landroid/content/Intent;)Landroid/os/IBinder;";
	public static final String REG_RCV = "registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;";
	public static final String REG_RCV_WITH_PER = "registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;Ljava/lang/String;Landroid/os/Handler;)Landroid/content/Intent;";
	public static final String UNREG_RCV = "unregisterReceiver(Landroid/content/BroadcastReceiver;)V";
	public static final String SEND_BROADCAST = "sendBroadcast(Landroid/content/Intent;)V";
	public static final String SEND_BROADCAST_WITH_PER = "sendBroadcast(Landroid/content/Intent;Ljava/lang/String;)V";
	//we did not find the use of the sendOrderedBroadcast in our subjects. Currently we do not model them.
	public static final String SEND_ORDERED_BROADCAST = "sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;)V";
	public static final String SEND_ORDERED_BROADCAST_LONG = "sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;Landroid/content/BroadcastReceiver;Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V";
	public static final String SEND_STICKY_BROADCAST = "sendStickyBroadcast(Landroid/content/Intent;)V";
	public static final String SEND_STICKY_BROADCAST_LONG = "sendStickyBroadcast(Landroid/content/Intent;Landroid/content/BroadcastReceiver;Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V";
	//there are other requestLocationUpdates method. Currently, we do not model them.
	public static final String REG_LOC_LISTENER = "requestLocationUpdates(Ljava/lang/String;JFLandroid/location/LocationListener;)V";
	//currently, we only model removeUpdates(LocationListener)
	public static final String UNREG_LOC_LISTENER = "removeUpdates(Landroid/location/LocationListener;)V";
	public static final String LOC_CHANGE_HANDLER = "onLocationChanged(Landroid/location/Location;)V";
	
	//for modeling AsyncTask
	public static final String ASYNCTASK_EXECUTE = "execute([Ljava/lang/Object;)Landroid/os/AsyncTask;";
	
	//for modeling TimerTask
	public static final String TIMER_SCHEDULE_1 = "schedule(Ljava/util/TimerTask;Ljava/util/Date;J)V";
	public static final String TIMER_SCHEDULE_2 = "schedule(Ljava/util/TimerTask;JJ)V";
	public static final String TIMER_SCHEDULE_3 = "schedule(Ljava/util/TimerTask;Ljava/util/Date;)V";
	public static final String TIMER_SCHEDULE_4 = "schedule(Ljava/util/TimerTask;J)V";
	
	//for enabling/disabling concurrency
	public static final String THREAD_START = "start()V";
	
	//wake lock acquisition and releasing
	public static final String WAKELOCK_ACQUISITION = "acquire()V";
	public static final String WAKELOCK_RELEASING = "release()V";
	
	//set keep screen on 
	public static final String SET_KEEP_SCREEN_ON = "setKeepScreenOn(Z)V";
	
	//MyLocationOverlay.enableMyLocation & disableMyLocation
	public static final String ENABLE_MY_LOCATION = "enableMyLocation()Z";
	public static final String DISABLE_MY_LOCATION = "disableMyLocation()V";
	
	//dialog show method
//	void android.app.Dialog.show()
	public static final String SHOW_DIALOG = "show()V";
	
}

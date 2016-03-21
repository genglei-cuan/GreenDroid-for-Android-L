package edu.nju.Alex.GreenDroid;

import android.R.bool;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class ReceiverState {
	BroadcastReceiver receiver;
	Context context;
	IntentFilter intentFilter;
	String receiverPermission;
	boolean unregistered = false;
	
	public ReceiverState(BroadcastReceiver receiver, Context context, IntentFilter filter, String per) {
		this.receiver=receiver;
		this.context=context;
		this.intentFilter=filter;
		this.receiverPermission=per;
	}
}

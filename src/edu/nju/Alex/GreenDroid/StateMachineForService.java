package edu.nju.Alex.GreenDroid;

import java.lang.reflect.Method;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class StateMachineForService {
	public static void decide(ServiceState ss, EventsForService es,Intent intent) {
		switch (es) {
		case Create:
			invokeOnCreateHandler(ss);
			break;
		case StartCommand:
			invokeOnStartCommandHandler(ss,intent);
			break;
		case Bind:
			invokeOnBindHandler(ss,intent);
			break;
		case UnBind:
			invokeOnUnbindHandler(ss,intent);
			break;
		case Destory:
			invokeOnDestroyHandler(ss);
			break;
		case IntentHandle:
			invokeOnHandleIntent(ss,intent);
		default:
			System.out.println("[severe] StateMachineForService has no such event");
			break;
		}
	}
	
	public static void  invokeOnCreateHandler(ServiceState ss) {
		if(ss.service instanceof Service){
			ApplicationStateLog.w(ss.service.toString()+"@onCreate()");
			if(Main.DEBUG){
				System.out.println("[Service LC Handler] "+ss.service.toString()+"@"+ServiceLifeCycleHandler.ON_CREATE_STRING);
			}
			ss.service.onCreate();
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a Service!");
		}
	}
	
	public static void invokeOnStartCommandHandler(ServiceState ss, Intent intent) {
		//some developers may choose to override the onStart instead of onStartCommand
		//so we check whether the service overrides onStart, if yes, invoke it; else, try to trace the onStartCommand to the base class, and invoke it
		if(ss.service instanceof Service){
			Class cls = ss.service.getClass();
			Method method=null;
			try {
				//first check if onStart is overridden
				try {
					method = cls.getDeclaredMethod(ServiceLifeCycleHandler.ON_START_STRING, Intent.class,int.class);
					if(method!=null){
						//if onStart is overridden, invoke onStart, otherwise a NoSuchMethodException will be thrown
						method.setAccessible(true);
						ApplicationStateLog.w(ss.service.toString()+"@onStart()");
						if(Main.DEBUG){
							System.out.println("[Service LC Handler] "+ ss.service.toString() +"@onStart()");
						}
						method.invoke(ss.service, intent,0);
					}
				} catch (NoSuchMethodException e) {
					//indicate that onStart is not overridden, then trace to the base class to invoke onStartCommand
					while(cls!=null){
						try {
							method=cls.getDeclaredMethod(ServiceLifeCycleHandler.ON_STARTCOMMAND_STRING, Intent.class,int.class,int.class);
							if(method!=null){
								method.setAccessible(true);
								ApplicationStateLog.w(ss.service.toString()+"@onStartCommand()");
								if(Main.DEBUG){
									System.out.println("[Service LC Handler] "+ cls.getName()+"@onStartCommand()");
								}
								method.invoke(ss.service, intent, 0, 0);
								return;
							}
						} catch (NoSuchMethodException e2) {
							cls=cls.getSuperclass();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a Service!");
		}	
	}
	
	
	public static void invokeOnBindHandler(ServiceState ss, Intent intent) {
		if(ss.service instanceof Service){
			ApplicationStateLog.w(ss.service.toString()+"@onBind()");
			if(Main.DEBUG){
				System.out.println("[Service LC Handler] "+ ss.service.toString()+"@"+ServiceLifeCycleHandler.ON_BIND_STRING);
			}
			IBinder binder = ss.service.onBind(intent);
			ss.ibinders.add(binder);
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a Service!");
		}
	}
	
	public static void invokeOnUnbindHandler(ServiceState ss, Intent intent) {
		if(ss.service instanceof Service){
			ApplicationStateLog.w(ss.service.toString()+"@onUnBind()");
			if(Main.DEBUG){
				System.out.println("[Service LC Handler] "+ ss.service.toString()+"@"+ServiceLifeCycleHandler.ON_UNBIND_STRING);
			}
			ss.service.onUnbind(intent);
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a Service!");
		}
	}
	
	public static void invokeOnDestroyHandler(ServiceState ss) {
		if(ss.service instanceof Service){
			ApplicationStateLog.w(ss.service.toString()+"@onDestory()");
			if(Main.DEBUG){
				System.out.println("[Service LC Handler] "+ ss.service.toString()+"@"+ServiceLifeCycleHandler.ON_DESTROY_STRING);
			}
			ss.service.onDestroy();
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a Service!");
		}
	}
	
	public static void invokeOnHandleIntent(ServiceState ss, Intent intent) {
		//invoke onHandlerIntent() to extend IntentService
		//supposed to have a new thread, just ignore concurrentcy for now
		if(ss.service instanceof IntentService){
			Class cls=ss.service.getClass();
			Method method=null;
			try {
				while(cls!=null){
					try {
						method = cls.getDeclaredMethod(ServiceLifeCycleHandler.ON_HANDLE_INTENT, Intent.class);
						if(method!=null){
							method.setAccessible(true);
							ApplicationStateLog.w(ss.service.toString()+"@onHandleIntent()");
							if(Main.DEBUG){
								System.out.println("[Service LC Handler] "+ ss.service.toString()+"@"+ServiceLifeCycleHandler.ON_HANDLE_INTENT);
							}
							method.invoke(ss.service, intent);
							return;
						}
					} catch (NoSuchMethodException e) {
						cls=cls.getSuperclass();
					}
				}
			} catch (Exception e) {
				System.out.println("[severe]" + e.getMessage());
			}
		}else{
			System.out.println("[severe] "+ss.service.toString()+"is not a IntentService!");
		}
	}
}

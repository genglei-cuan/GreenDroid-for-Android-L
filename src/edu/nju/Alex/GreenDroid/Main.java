/**
 * @author alex
 * File last modified on Oct. 21, 2015
 */
package edu.nju.Alex.GreenDroid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import android.app.Activity;
import android.app.Fragment;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import gov.nasa.jpf.vm.ElementInfo;

public class Main {
	//stack of Activity,service and receiver
	public static LinkedList<ActivityState> activityStack= new LinkedList<ActivityState>();
	public static ArrayList<ServiceState> serviceList = new ArrayList<ServiceState>(); 
	public static ArrayList<ReceiverState> receiverList = new ArrayList<ReceiverState>();
	public static ArrayList<StaticReceiverState> staticReceiverList = new ArrayList<StaticReceiverState>();
	
	//specifically handle setKeepScreenOn
	public static boolean setKeepScreenOn = false;
	
	public static String test="test";
	
	
	//my little humble decision machines for event handler
	public static StateMachine stateMachine = new StateMachine();
	//public static StateMachineForService stateMachineForService= new StateMachineForService();
			
	//top of the stack,ma
	public static ActivityState currentActicity;
	
	//what can we do about this activity
	public static HashMap<String, String> actionToActivityMap = new HashMap<String, String>();
	
	//the hashmap stores maps between the activities to their fragments
	public static HashMap<Integer, ArrayList<FragmentState>> activityToFragment=new HashMap<Integer, ArrayList<FragmentState>>();
	
	//pick an action, plz
	public static Random randGen = new Random();
	
	//some scaling stuff
	public static double latitude = 0.0;
	public static double altitude = 0.0;
	public static double longitude = 0.0;
	public static int accurateSwitch = 0;
	public static boolean setAltitude = true;
	public static long lastFixTime = 0;
	
	//So can we see the app?
	public static boolean appVisible = true;
	
	//This one's for debugging
	public final static boolean DEBUG = false;
	
	//really not fan of concurrency..may cause endless run
	public final static boolean ENABLE_CONCURRENCY = false;
	
	//where the show begins
	public static void main(String[] args) {
		randGen.setSeed((int)System.currentTimeMillis());
		//for now, still need to do the old-school stuff. try to make it better latter.
		String actName = Config.ENTRY_ACTIVITY;
		
		//a new activity's instance
		Activity mainAct = null;
		try{
			mainAct = (Activity) Class.forName(actName).newInstance();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		//push onto stack
		ActivityState actState = new ActivityState(mainAct);
		activityStack.push(actState);
		
		//times record the number of times the user has been interacting with the application
		int times=0;
		
		while(!activityStack.isEmpty() && times<=Config.SEQ_LEN){
			boolean allDestroyed = true;
			for(ActivityState as: activityStack){
				if(!as.destroyed){
					allDestroyed=false;
					break;
				}
			}
			
			if(allDestroyed){
				break;
			}
			
			//no more touch than Config.SEQ_LEN
		//	System.out.println(Config.SEQ_LEN+" "+times);
			
			if(currentActicity == activityStack.peek()){
				//System.out.println("LAST HANDLER! "+currentActicity.lastExecutedLCHandler);
				if(currentActicity.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_RESUME){
					
					//getting the choice;
					int size = currentActicity.GUIComponentNumber;
					//System.out.println("Size:"+size);
					int choice =randGen.nextInt(size);
					//System.out.println("choice: "+choice);
					//we reserve the first three elements in GUIComponentList to represent physical buttons: back, menu, and home
					if(choice==0){
						//click back
						ApplicationStateLog.w(currentActicity.activity.toString()+"  Action: @backBtn");
						if(activityStack.size()>1){
							//change to an other activity and set current
							stateMachine.decide(activityStack.get(1),Events.SWITCHBACK_EVENT,0);
							currentActicity=activityStack.get(1);
							stateMachine.decide(activityStack.get(0),Events.FINISHED_EVENT,0);
							activityStack.pop();
						}else{
							stateMachine.decide(currentActicity,Events.FINISHED_EVENT,0);
							activityStack.pop();
						}
						times++;
						continue;
					}
					if(choice==1){
						//menu button is clicked, we create menu now, call onCreateOptionsMenu --> onPrepareOptionsMenu --> onOptionsItemSelected (or onMenuItemSelected)
						stateMachine.decide(currentActicity,Events.MENU_CLICK_EVENT,0);
						//now let's monkey test the menu~
						int noOfItems = currentActicity.menuItemIDs.size();
						if(noOfItems!=0){
							Random rand = new Random();
							int itemChoice = rand.nextInt(noOfItems);
							int itemID = currentActicity.menuItemIDs.get(itemChoice);
							stateMachine.decide(currentActicity,Events.MENU_ITEM_CLICK_EVENT,itemID);
						}
						ApplicationStateLog.w(currentActicity.activity.toString()+"  Action: @menuBtn");
						times++;
					}
					if(choice ==2 || choice ==3){
						//home button is clicked and we just assume user switch right back to it, 'cause other apps matter not
						ApplicationStateLog.w(currentActicity.activity.toString()+"  Action: @homeBtn activity switch");
						appVisible=false;
						int isVisible=0;
						//System.out.println("Switch away");
						stateMachine.decide(currentActicity,Events.SWITCH_EVENT,isVisible);
						//now we bring it back
						appVisible=true;
						isVisible=1;
						//System.out.println("switch back");
						stateMachine.decide(currentActicity,Events.SWITCHBACK_EVENT,isVisible);
					    //appVisible=true;
						times++;
					}
					if(choice==4){
						//simulating external events, just in case
						//fix it up later
					}
					if(choice>4){
						//real GUI event
						//Object view = currentActicity.registeredGUIComponentList.get(choice);
						//ArrayList<Integer> eventList = currentActicity.eventsList.get(choice);
						
						//int eventChoice = randGen.nextInt(eventList.size());
						
						//for mutiple events
						//System.out.println("GUI 4");
						stateMachine.decide(currentActicity,Events.GUI_EVENT,choice);
						times++;
					}
				}else{
					//System.out.println("needs to switch back!");
					stateMachine.decide(currentActicity,Events.SWITCHBACK_EVENT,0);
				}
				//make adjust in time
				checkCurrentActivity();
			}else{
				checkCurrentActivity();
			}
		}
		/**
		 * an execution is finished, now we perform some cleanup tasks
		 * (1) finish all unfinished activities on the "activity stack"
		 * (2) finish all services
		 * (3) check whether all sensors are unregistered after all activities and services are destroyed
		 */
		
		//if in some activity's onStop() handler, it starts a new activity, we cannot handler such extreme cases
		ApplicationStateLog.w("finish all activities");
		//we first execute to onStophandler and check sensor and wakelock misusage
		try{
			for(int i=0;i<activityStack.size();i++){
				ActivityState as = activityStack.get(i);
				stateMachine.decide(as,Events.FINISHED_EVENT,0);
			}
		}catch(Exception e){
			
		}
		
		activityStack.clear();
		
		//finish all services in the list and check
		
		ApplicationStateLog.print();
	}
	/**
	 * This method checks if the currentActivity is actually currentActicity, and adjust
	 */
	public static void checkCurrentActivity() {
		if(currentActicity==activityStack.peek()){
			return;
		}
		if(currentActicity==null){
			currentActicity = activityStack.peek();
		}else{
			//setting currentActivity
			ActivityState previousCurrent=currentActicity;
			stateMachine.decide(previousCurrent,Events.STOP_EVENT,0);
			currentActicity = activityStack.peek();
			stateMachine.decide(currentActicity,Events.SWITCHBACK_EVENT,0);
		}
	}
	/**
	 * This method binds the registered gui component with the current activity.
	 * It is possible that the gui component should not be bound to the current activity.
	 * For example, the onClick handler is registered when receiving some broadcast, the activity is not necessarily in foreground. 
	 * So this binding could introduce imprecision in extreme cases. 
	 * If we want precise binding, we may need to track an activity's gui elements at the findViewById time.(to be done)
	 * used by registerGUIEventListener in MethodInvocationListener
	 */
	public static void bindGUIComponentWithActivity(ElementInfo ei, String GUIElementType, int GUIEventType, String text,int layoutId){
		//If the GUI component has been registered, check if the event has been recorded.
		//Later when manipulate the GUI components, we still need to check whether corresponding listener is null
		//Because once we see the invocation of setOnXXXListener(), we consider the GUI component is registered even when invoking setOnXXXListener(null)
		//In fact this method runs in JPF's VM, so the ei here actually is regarded as android.widget.button etc... (not element info)
		//System.out.println("Main: "+layoutId);
		
		//first we have to decide if the GUI component is from the activity or the fragment
		if(currentActicity.layoutId!=layoutId){
			for(FragmentState fs:currentActicity.fragmentList){
				if(fs.layoutId==layoutId){
					int index=fs.registeredGUIComponentList.indexOf(ei);
					Object o=null;
					if (index == -1) {
						try {
							o=Class.forName(GUIElementType).cast(ei);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (o!=null) {
							fs.registeredGUIComponentList.add(o);
							fs.registeredGUIComponentText.add(text);
							ArrayList<Integer> events = new ArrayList<>();
							events.add(GUIEventType);
							fs.eventsList.add(events);
						}
					}else{
						ArrayList<Integer> events = fs.eventsList.get(index);
						if (events!=null) {
							if(events.indexOf(GUIEventType)==-1){
								events.add(GUIEventType);
							}
						}
					}
					if (fs.equals(currentActicity.currentFragment)){
						currentActicity.GUIComponentNumber=currentActicity.registeredGUIComponentList.size()+fs.registeredGUIComponentList.size();
					}
					return;
				}
			}
		}
		
		if(currentActicity!=null){
			int index = currentActicity.registeredGUIComponentList.indexOf(ei);
			Object o =null;
			if(index== -1){
				//the gui is not registered
				try {
					//cast ei
					o = Class.forName(GUIElementType).cast(ei);
					//System.out.println(ei.getClass().getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(o!=null){
					currentActicity.registeredGUIComponentList.add(o);
					currentActicity.registeredGUIComponentText.add(text);
					//System.out.println("Main: "+ei.getObjectRef());
					ArrayList<Integer> events = new ArrayList<>();
					//System.out.println("Main: "+GUIEventType);
					events.add(GUIEventType);
					
					currentActicity.eventsList.add(events);
					if(DEBUG){
						System.out.println("[UI Element Registration] " + GUIElementType + " @" +currentActicity.activity.toString());
					}
				}
			}else{
				//the gui element is already there, so what about the event?
				ArrayList<Integer> events = currentActicity.eventsList.get(index);
				if(events!=null){
					//check if the event is in
					if(events.indexOf(GUIEventType)==-1){
						events.add(GUIEventType);
					}
				}
			}
		}else{
			if(DEBUG)
				System.out.println("[severe] binding GUI component with a null activity");
		}
	//	System.out.println("done.?");
	}
	
	/**
	 * This method pushes an activity instance onto the activity stack
	 */
	//<2do> What if the activity is already on stack, how to check? via class name.
			//Experiments in emulator show that start activity A in A's handler will cause infinite loop
			//This shows that an activity can have multiple instances, but normally developers will not do this. This task is not urgent
	//about to do it
	public static void pushActivityOntoStack(Object compName, Object action){
		//System.out.println("using this righrt?");
		if(compName == null){
			return;
		}
		
		//at the implementation time of this method, we didn't properly model the constructor of Intent, the passed argument here is instance of ComponentName
		String actName =null;
		try {
			//we can also cast the compName to ComponentName type and call getClassName() method
			//we have modeled getClassName() in native peer
			//compName.getClass() returns null, so we first get the ComponentName class and then get mClass field
			Class cls = Class.forName("android.content.ComponentName");
			Field field = cls.getDeclaredField("mClass");
			field.setAccessible(true);
			//System.out.println(field.get(compName));
			try {
				//System.out.println("using this righrt?");
				actName = field.get(compName).toString();
				//System.out.println("using this righrt?");
				//System.out.println(actName);
			} catch (Exception e) {
				//the native peer of reflection may generate exceptions when mComponent of intent is not set
				//then we may need to check the mAction field (activities can be started using actions)
				if(action!=null){
					actName = actionToActivityMap.get(action);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(actName!=null){
			//we construct a new activity and wrap it up, and push to activity stack
			try {
				Class actCls = Class.forName(actName);
				//System.out.println(actName);
				Activity act = (Activity) actCls.newInstance();
				//act.onCreate
				//System.out.println(actName);
				ActivityState activityState = new ActivityState(act);
				activityStack.push(activityState);
				if(DEBUG){
					System.out.println("[New Activity] "+actName+" pushed onto stack");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This methods pushes an activity instance onto the activity stack for result. 
	 */
	public static void pushActivityOntoStack(Object parentAct, Object compName, Object action, int requestCode){
		//handle the startActivityForResult case
		//(1) get the activity class name, create a new instance
		//(2) add the request code and new activity instance pair to the codeActMap in the activityState of the parent activity
		//(3) push the newly created activity onto stack
		if(compName == null){
			return;
		}
		//at the implementation time of this method, we didn't properly model the constructor of Intent, the passed argument here is instance of ComponentName
		String actName =null;
		try {
			//we can also cast the compName to ComponentName type and call getClassName() method
			//we have modeled getClassName() in native peer
			//compName.getClass() returns null, so we first get the ComponentName class and then get mClass field
			Class cls = Class.forName("android.content.ComponentName");
			Field field = cls.getDeclaredField("mClass");
			try {
				actName = field.get(compName).toString();
			} catch (Exception e) {
				//the native peer of reflection may generate exceptions when mComponent of intent is not set
				//then we may need to check the mAction field (activities can be started using actions)
				if(action!=null){
					actName = actionToActivityMap.get(action);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(actName!=null){
			//we construct a new activity and wrap it up, and push to activity stack
			try {
				Class actCls = Class.forName(actName);
				Activity act = (Activity) actCls.newInstance();
				ActivityState activityState = new ActivityState(act);
				activityState.requestCode=requestCode;
				for(ActivityState as: activityStack){
					if(as.activity.equals(parentAct)){
						//fing the parent
						activityState.parentActState=as;
						as.codeActMap.put(requestCode, activityState);
						break;
					}
				}
				activityStack.push(activityState);
				if(DEBUG){
					System.out.println("[New Activity] "+actName+" pushed onto stack and its parentActivity is "+activityState.parentActState.activity.getLocalClassName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * This method finishes an activity (handles activity.finish())
	 */
	public static void finishActivity(Object activity){
		//(1) find the activity state
		//(2) invoke the activity's handler to onDestroy
		//(3) remove the activityState from stack
		for(ActivityState as : activityStack){
			if(as.activity.equals(activity)){
				stateMachine.decide(as, Events.FINISHED_EVENT, 0);
				activityStack.remove(as);
			}
		}
	}
	
	
	/**
	 * This method finishes an activity (handles finishActivity(requestCode))
	 */
	public static void finishActivityByRequestCode(Object activity, int requestCode){
		//(1) find the activityState enclosing the activity to search for the requestCode
		//(2) finish the activity assoiciated with the requestcode
		Object activityToFinish = null;
		for(ActivityState as : activityStack){
			if(as.activity.equals(activity)){
				activityToFinish = as.codeActMap.get(requestCode);
			}
		}
		if(activityToFinish!=null){
			for(ActivityState as : activityStack){
				if(as.activity.equals(activityToFinish)){
					stateMachine.decide(as, Events.FINISHED_EVENT, 0);
					activityStack.remove(as);
				}
			}
		}else{
			if(DEBUG){
				System.out.println("[Activity finished] finished earlier or not started");
			}
		}
		
	}
	
	/**
	 * This method registers the fragments of each activity
	 * @param viewId
	 * @param classText
	 * @param layoutId
	 */
	public static void setFragmentMap(int viewId, String classText, int layoutId) {
		 FragmentState fragmentState=new FragmentState();
		 fragmentState.actLayoutId=layoutId;
		 fragmentState.fragmentClass=classText;
		 fragmentState.viewId=viewId;
		 ArrayList<FragmentState> list;
		 if(activityToFragment.containsKey(layoutId)){
			 list=activityToFragment.get(layoutId);
			 list.add(fragmentState);
		 }else{
			 list=new ArrayList<FragmentState>();
			 list.add(fragmentState);
			 activityToFragment.put(layoutId, list);
		 }
		// System.out.println(activityToFragment.size());
	}
	/**
	 * This method handles Activity.setContentView(int) API call 
	 * also, we attach fragment to the activity
	 */
	public static void handleSetContentView(Object activity, int layoutId){
		//(1) find the activityState enclosing the activity
		//(2) set the layout id field
		//(3) find if there is any fragment attached to the activity layout
		//System.out.println("handleSetContentView");
		for(ActivityState as : activityStack){
			if(as.activity.equals(activity)){
				as.layoutId=layoutId;
				//System.out.println(test+" "+activityToFragment.size()+" "+activityToFragment.containsKey(layoutId)+" "+layoutId);
				as.fragmentList.clear();
				as.currentFragment=null;
				if(activityToFragment.containsKey(layoutId)){
					
					ArrayList<FragmentState> myFras = activityToFragment.get(layoutId);
					for (int i = 0; i < myFras.size(); i++) {
						FragmentState fragmentState=myFras.get(i);
						try {
							//System.out.println(fragmentState.fragmentClass);
							//Activity testt=(Activity)Class.forName("com.example.alexwang.testforgreendroidverisiontwo.MainActivity").newInstance();
							fragmentState.fragment=(Fragment)Class.forName(fragmentState.fragmentClass).newInstance();
							//System.out.println("all good?");
							if(fragmentState.fragment!=null){
								as.fragmentList.push(fragmentState);
								//for now test with one fragment,so we simply create view here
								as.currentFragment=as.fragmentList.peek();
								
							}
						} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				return;
			}
		}
	}
	
	public static void handleInflate(Fragment fragment, int layoutId) {
		for (FragmentState fs : currentActicity.fragmentList) {
			if (fs.fragment.equals(fragment)) {
				fs.layoutId=layoutId;
				return;
			}
		}
	}
	

	/**
	 * This method handles activity.setResult(int) or setResult(int, intent)
	 */
	public static void handleSetResult(Object activity, int returnResult, Object returnData){
		for(ActivityState as : activityStack){
			if(as.activity.equals(activity)){
				as.returnResult =returnResult;
				as.returnIntent = (Intent)returnData;
			}
		}
	}
	
	/**
	 * This methods starts a service component
	 * Service can be started by calling Context.startService() or Context.bindService(). 
	 * startedByBinding == 1 means the service is started in the latter way.
	 */
 	public static void handleStartService (Object compName, int startedByBinding, Object intent, Object conn) throws Exception{
 		String serviceName = null;
 		try {
 			//we can also cast the compName to ComponentName type and call getClassName() method
			//we have modeled getClassName() in native peer
			//compName.getClass() returns null, so we first get the ComponentName class and then get mClass field
 			Class cls = Class.forName("android.content.ComponentName");
 			Field field = cls.getDeclaredField("mClass");
 			serviceName = field.get(compName).toString();
		} catch (Exception e) {
			System.out.println("[severe] cannot resolve service class");
			e.printStackTrace();
		}
 		
 		if (startedByBinding == 0) {
 			//the service is started by Context.startService() call
			//Context.startService() does not nest, they do result in multiple corresponding calls to onStartCommand(), no matter how many times it is started
			//a service will be stopped once Context.stopService() is called, or stopSelf() is called
 			if(serviceName !=null){
 				//check if the service has been started
				//if yes, simply call onStartCommand() URL: http://developer.android.com/reference/android/content/Context.html#startService(android.content.Intent)
				//if no, construct the service object and call onCreate and then onStartCommand
 				// TODO create ServiceState
 				ServiceState serState = getStartedService(serviceName);
 				if(serState == null){
 					//service not started yet
 					try {
						Class serCls = Class.forName(serviceName);
						Service service = (Service) serCls.newInstance();
						serState = new ServiceState(service);
						serviceList.add(serState);
						if(DEBUG){
							System.out.println("[New service]"+serviceName+" added to service list");
						}
						
						//invoke onCreate
						StateMachineForService.decide(serState, EventsForService.Create, null);
						//ServiceState.invokeOnCreateHandlerForService(serState);
						
						//invoke onStartCommand or OnStart
						//ServiceState.invokeOnStartCommandHandlerForService(serState,((Intent) intent));
						StateMachineForService.decide(serState, EventsForService.StartCommand, ((Intent) intent));
						
						//if the service extends IntentService, we run the onHandleIntent in a new thread (in current implementation, we ignore concurrency)
						if(service instanceof IntentService){
							//we should have run onHandleIntent in a new thread
							StateMachineForService.decide(serState, EventsForService.IntentHandle, ((Intent) intent));
							//ServiceState.invokeOnHandleIntentForIntentService(serState, ((Intent) intent));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
 				}else{
 					//service is started, simply call onStartCommand
					if(DEBUG){
						System.out.println("[Service Running]"+serviceName+" is already running!");
					}
					
					//invoke onStartCommand or onStart
					//ServiceState.invokeOnStartCommandHandlerForService(serState, ((Intent) intent));
					StateMachineForService.decide(serState, EventsForService.StartCommand, ((Intent) intent));
					
					//if the service extends IntentService, we run the onHandleIntent in a new thread (in current implementation, we ignore concurrency)
					if(serState.service instanceof IntentService){
						//we should have run onHandleIntent in a new thread
						//ServiceState.invokeOnHandleIntentForIntentService(serState, ((Intent) intent));
						StateMachineForService.decide(serState, EventsForService.IntentHandle, ((Intent) intent));
					}
 				}
 			}
		} else {
			// the service is started by bindService()
			// (1) check if the service has been started, if not construct the service, and call onCreate(); if yes nothing
			// (2) call onBind(), it will return an IBinder, make IBinder and connection a pair and add to the service state's connection map
			// (3) after invoking onBind(), call connection.onServiceConnected(); We don't handle connection.onServiceDisconnected() at this stage
			// then we need to listen to the unBindService(), if unBindService(conn) is called, then we search all serviceState, remove the conn and its paired IBinder
			// if after removing the service has no bound connections, then call onUnbind(), onDestroy() and remove the service from list
			if(serviceName !=null){
				ServiceState serState = getStartedService(serviceName);
				if (serState == null) {
					//not started yet
					try {
						Class serCls = Class.forName(serviceName);
						Service service = (Service) serCls.newInstance();
						serState = new ServiceState(service);
						serviceList.add(serState);
						if(DEBUG){
							System.out.println("[New service]"+serviceName+" added to service list");
						}
						
						//invoke onCreate()
						//ServiceState.invokeOnCreateHandleForService(serState);
						StateMachineForService.decide(serState, EventsForService.Create, null);
						
						//for services started with bindService(), we call onBind() instead of onStartCommand()
						//then we add the returned ibinder to the service state object's ibinders list
						//so after invokeOnBindHanlderForService, the service state object's ibinders's length should be larger than that of conns by 1
						//if not throw an exception
						StateMachineForService.decide(serState, EventsForService.Bind, ((Intent) intent));
						//ServiceState.invokeOnBindHandlerForService(serState, ((Intent)intent));
						
						//now we add the connection to the conns of the service state object to make the length of conns (and intents) and ibinders equal
						if(serState.ibinders.size() != serState.conns.size()+1){
							throw new Exception("there must be one more binder than connections");
						}else{
							serState.conns.add(conn);
							serState.intents.add(intent);
							IBinder binder = (IBinder) serState.ibinders.get(serState.conns.indexOf(conn));
							((ServiceConnection) conn).onServiceConnected(((ComponentName) compName), binder);
						}
						//if the service extends IntentService, we run the onHandleIntent in a new thread (in current implementation, we ignore concurrency)
						if(service instanceof IntentService){
							//we should have run onHandleIntent in a new thread
							//ServiceState.invokeOnHandleIntentForIntentService(serState, ((Intent) intent));
							StateMachineForService.decide(serState, EventsForService.IntentHandle, ((Intent) intent));
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					//service is started, simply call onBind
					if(DEBUG){
						System.out.println("[Service Running]"+serviceName+" is already running!");
					}
					
					//ServiceState.invokeOnBindHandlerForService(serState, ((Intent) intent));
					StateMachineForService.decide(serState, EventsForService.Bind, ((Intent) intent));
					//now we add the connection to the conns of the service state object to make the length of conns and ibinders equal 
					if(serState.ibinders.size() != (serState.conns.size() + 1)){
						throw new Exception("there must be one more binder than connections");
					} else{
						serState.conns.add(conn);
						IBinder binder = (IBinder) serState.ibinders.get(serState.conns.indexOf(conn));
						((ServiceConnection) conn).onServiceConnected(((ComponentName) compName), binder);
					}
					
					//if the service extends IntentService, we run the onHandleIntent in a new thread (in current implementation, we ignore concurrency)
					if(serState.service instanceof IntentService){
						//we should have run onHandleIntent in a new thread
						//ServiceState.invokeOnHandleIntentForIntentService(serState, ((Intent) intent));
						StateMachineForService.decide(serState, EventsForService.IntentHandle, ((Intent) intent));
					}			
				}
			}
		}
 	}
 	
 	/**
 	 *	This method finishes a service component. 
 	 */
	public static void handleStopService(Object service){
		//when stopSelf(I/V),stopSelfResult(I), or stopService is executed, 
		//this method executes the corresponding service's onDestroy and remove it from list
		if(service instanceof Service){
			//find the corresponding serviceState
			ServiceState ss = getEnclosingServiceState(service);
			if(ss!=null){
				//ServiceState.invokeOnDestroyHandlerForService(ss);
				StateMachineForService.decide(ss, EventsForService.Destory, null);
				serviceList.remove(ss);
				if(DEBUG){
					System.out.println("[Remove service] "+ss.service.toString()+" removed from service list");
				}
			}else{
				if(DEBUG){
					System.out.println("service has been removed earlier");
				}
			}
		}
	}
	
	/**
	 * This method finishes a service component.
	 */
	public static void handleStopServiceWithName(Object compName){
		//when stopService(intent) is called, this method will be executed to stop the corresponding service
		//(1) get the service name from the ComponentName object
		//(2) get started service using the service name
		//(3) invoke the onDestroy() handler and remove the service state from serviceList
		String serviceName = null;
		try {
			//we can also cast the compName to ComponentName type and call getClassName() method
			//we have modeled getClassName() in native peer
			//compName.getClass() returns null, so we first get the ComponentName class and then get mClass field
			Class cls = Class.forName("android.content.ComponentName");
			Field field = cls.getDeclaredField("mClass");
			serviceName = field.get(compName).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(serviceName != null){
			ServiceState ss = getStartedService(serviceName);
			if(ss != null){
				//ServiceState.invokeOnDestroyHandlerForService(ss);
				StateMachineForService.decide(ss, EventsForService.Destory, null);
				serviceList.remove(ss);
				if(DEBUG){
					System.out.println("[Remove service] "+ss.service.toString()+" removed from service list");
				}
			} else{
				if(DEBUG){
					System.out.println("service has been removed earlier");
				}
			}
		}
	}
	
	
	/**
	 * This method unbinds a service component
	 */
	public static void handleUnBindService(Object conn){
		//(0) find the service state
		//(1) invoke service's onUnbind()
		//(2) remove conn and the corresponding ibinder in serviceState
		//(3) if the serviceState's conns and ibinders' length becomes 0, invoke onDestroy() and remove service from list
		for(ServiceState ss : serviceList){
			int index = ss.conns.indexOf(conn);
			if(index !=-1){
				//ServiceState.invokeOnUnbindHandlerForService(ss,((Intent) ss.intents.get(index)));
				StateMachineForService.decide(ss, EventsForService.UnBind, ((Intent)ss.intents.get(index)));
				ss.conns.remove(index);
				ss.intents.remove(index);
				ss.ibinders.remove(index);
				if(ss.conns.size()==0){
					//destroy the service
					//ServiceState.invokeOnDestoryForService(ss);
					StateMachineForService.decide(ss, EventsForService.Destory, null);
					serviceList.remove(ss);
					if(DEBUG){
						System.out.println("[Remove service] "+ss.service.toString()+" removed from service list");
					}
				}
				return;
			}
		}
	}
	/**
	 * This method returns the a service component "service"'s state object
	 */
	private static ServiceState getEnclosingServiceState(Object service) {
		for(ServiceState ss:serviceList){
			if(ss.service.equals(service)){
				return ss;
			}
		}
		return null;
	}
	/**
	 * This method check whether a service component with name "serviceName" is started or not.
	 * If started, the method returns the service's state object; otherwise, return null
	 */
	private static ServiceState getStartedService(String serviceName) {
		for(ServiceState ss:serviceList){
			if(ss.service.getClass().getName().equals(serviceName)){
				return ss;
			}
		}
		return null;
	}
	
	
	/**
	 * This method registers a broadcast receiver
	 */
	public static void regBroadcastRcv(Object context, Object rcv, Object filter, Object permission){
		if(context!=null && rcv !=null && filter!=null){
			boolean receiverExist = false;
			for(ReceiverState rs : receiverList){
				if(!rs.unregistered && rs.context.equals(context) && rs.receiver.equals(rcv) && rs.intentFilter.equals(filter) && rs.receiverPermission.equals(permission)){
					receiverExist=true;
					break;
				}
			}
			if(!receiverExist){
				if(DEBUG){
					System.out.println("[Reg Broadcast Receiver]");
				}
				ApplicationStateLog.w(((Context) context).toString()+" register broadcast receiver "+((BroadcastReceiver) rcv).toString());
				receiverList.add(new ReceiverState(((BroadcastReceiver) rcv), ((Context) context), ((IntentFilter) filter), ((String) permission)));
			}
		}
	}
	/**
	 * This method unregisters a broadcast receiver
	 */
	public static void unRegBroadcastRcv(Object context, Object rcv){
		for(ReceiverState rs : receiverList){
			if(rs.context.equals(context) && rs.receiver.equals(rcv)){
				if(DEBUG){
					System.out.println("[Unreg Broadcast Receiver]");
				}
				ApplicationStateLog.w(((Context) context).toString()+" unregister broadcast receiver "+((BroadcastReceiver) rcv).toString());
				//to avoid concurrent modification exception, we do not remove the broadcast receiver from list; instead, we disable the receiver
				rs.unregistered = true;
			}
		}
		return;
	}
	
	/**
	 * This method dispatches a broadcast message.
	 */
	public static void dispatchBroadcastMsg(Object context, Object intent, Object permission){
		String action = ((Intent) intent).getAction();
		if(permission == null){
			//check whether any static receiver is qulified to handle the broadcast
			for(StaticReceiverState srs:staticReceiverList){
				if(srs.actions.contains(action)){
					try {
						Class cls = Class.forName(srs.rcvName);
						if(cls!=null){
							Object rcv = cls.newInstance();
							ApplicationStateLog.w("static "+((BroadcastReceiver) rcv).toString()+"@onReceive");
							((BroadcastReceiver) rcv).onReceive(((Context) context), ((Intent)intent));
							rcv=null;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			//check whether any dynamic receiver is qualified to handle the broadcast
			for(ReceiverState rs : receiverList){
				if(!rs.unregistered && rs.intentFilter.hasAction(action)){
					if(rs.receiver != null){
						ApplicationStateLog.w(((BroadcastReceiver) rs.receiver).toString()+"@onReceive");
						rs.receiver.onReceive(rs.context, (Intent) intent);
					}
				}
			}
		}else{
			//check whether any static receiver is qualified to handle the broadcast
			for(StaticReceiverState srs : staticReceiverList){
				if((srs.permission != null && srs.permission.equals(permission)) || srs.permission == null){
					if(srs.actions.contains(action)){
						try{
							Class cls = Class.forName(srs.rcvName);
							if(cls != null){
								Object rcv = cls.newInstance();
								ApplicationStateLog.w("static "+((BroadcastReceiver) rcv).toString()+"@onReceive");
								((BroadcastReceiver) rcv).onReceive((Context) context, (Intent) intent);
								rcv = null;
							}
						} catch(Exception e){
							continue;
						} 
					}
				}
			}
			
			//check whether any dynamic receiver is qualified to handle the broadcast
			for(ReceiverState rs : receiverList){
				if(!rs.unregistered && rs.intentFilter.hasAction(action) && rs.receiverPermission.equals(permission)){
					if(rs.receiver != null){
						ApplicationStateLog.w(((BroadcastReceiver) rs.receiver).toString()+"@onReceive");
						rs.receiver.onReceive(rs.context, (Intent) intent);
					}
				}
			}
		}
	}
	
	//do the old version do
	/**
	 * This method handles static broadcast receiver registration.
	 * I guess this method does not work. For safety, we keep it here. 
	 */
	public static void addStaticReciever(ArrayList<StaticReceiverState> receivers) {
		staticReceiverList=receivers;
		
	}
	//TODO
	/**
	 * This method registers a location listener
	 */
	public static void registerLocListener(Object provider, long minTime, double minDist, Object listener){
		if(DEBUG){
			System.out.println("[Reg location listener]");
		}
		ApplicationStateLog.w("register location listener "+listener.toString());
		//finished later with LocationListenerState
	}
	
	//TODO
	/**
	 * This method handles wake lock acquisition
	 */
	public static void handleWakeLockAcquisition(Object wl){
		if(DEBUG){
			System.out.println("[wake lock] acquiring wake lock");
		}
		ApplicationStateLog.w("acquire wake lock");
		//finished later with WakeLockStateState
	}
	
	//TODO
	/**
	 * This method unregisters a location listener
	 */
	public static void unregisterLocListener(Object listener){}
	
	/**
	 * This method handles setKeepScreenOn
	 */
	public static void handleSetKeepScreenOn(int status){
		if(status == 1){
			setKeepScreenOn=true;
		}else{
			if(status == 0){
				setKeepScreenOn=false;
			}
		}
	}
	
	//TODO
	/**
	 * This method handles enableMyLocation()
	 */
	public static void handleEnableMyLocation(Object overlay){
		if(DEBUG){
			System.out.println("[location overlay] enable location");
		}
		ApplicationStateLog.w("enable my location");
		// finish later with LocationOverlayState
	}
	
	//TODO
	/**
	 * This method handles disableMyLocation()
	 */
	public static void handleDisableMyLocation(Object overlay){
	}
	
	//TODO
	/**
	 * This method handles wake lock releasing
	 */
	public static void handleWakeLockReleasing(Object wl){}
	
	/**
	 * the important part, leave here first, considering make an independent class
	 *TODO: different accuracy
	 * This method feeds mock GPS data to the running app, and triggers the onLocationChanged() handler
	 */
	public static void locDataSimulation(){}
}

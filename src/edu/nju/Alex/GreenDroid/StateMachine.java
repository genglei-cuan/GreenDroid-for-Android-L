package edu.nju.Alex.GreenDroid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import android.R.bool;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;

public class StateMachine {
	
	//choicenum for gui_event,for Activity
	public void decide(ActivityState as, Events event, int choicenum){
		switch (event) {
		case SWITCHBACK_EVENT:
				if(as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_RESUME){
					return;
				}
				if(as.lastExecutedLCHandler == -1 || as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_START || as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_STOP){
					exeLCHandlerToOnResume(as);
				}else{
					System.out.println("woo, not the right time to resume"+as.activity.toString());
				}
			break;
		case FINISHED_EVENT:
			exeLCHandlerToOnStop(as);
			exeLCHandlerToOnDestroy(as);
			break;
		case STOP_EVENT:
			exeLCHandlerToOnStop(as);
			break;
		case GUI_EVENT:
			if(as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_RESUME){
				//we need to see if the component is on the activity of fragment
				
				Object view;	
				ArrayList<Integer> eventList ;
				String Text;
				boolean act=true;
				if (choicenum<as.registeredGUIComponentList.size()) {
					view = as.registeredGUIComponentList.get(choicenum);
					eventList = as.eventsList.get(choicenum);
					Text=as.registeredGUIComponentText.get(choicenum);
				}else{
					act=false;
					int realchoice=choicenum-as.registeredGUIComponentList.size();
					view=as.currentFragment.registeredGUIComponentList.get(realchoice);
					eventList=as.currentFragment.eventsList.get(realchoice);
					Text=as.currentFragment.registeredGUIComponentText.get(realchoice);
				}
					
				
				Random randGen= new Random();
				//System.out.println(eventList.get(0));
				int eventChoice = eventList.get(randGen.nextInt(eventList.size()));
				
				//System.out.println(eventChoice);
				switch (eventChoice) {
				case HandlerType.CLICK:
					//System.out.println("CLICK");
					//perform click
					try {
						//System.out.println("CLICK");
						Method m =(Method)view.getClass().getMethod("performClick", null);
						//System.out.println("CLICK");
						if(m==null){
							System.out.println("[severe] no performClick() method found for "+ view.getClass().getName());
						} else{
							m.invoke(view, null);
							//Method getText = view.getClass().getMethod("getId", null);
							//System.out.println(getText.isAccessible());
							if(Text!=null && act){
								ApplicationStateLog.w(as.activity.toString()+"  Action: @Click :"+view.getClass().getName()+" "+Text);
							}
							if (Text!=null && !act) {
								ApplicationStateLog.w(as.activity.toString()+" 's currently active fragment: "+as.currentFragment.fragmentClass+"  Action: @Click :"+view.getClass().getName()+" "+Text);
							}
						}
					} catch (Exception e) {
						if(!(e instanceof java.lang.NoSuchMethodException)){
							e.printStackTrace();
						}
					}
					break;
				case HandlerType.LONG_CLICK:
					//System.out.println("Long Click!");
					try {
						//perform long click
						Method m =(Method) view.getClass().getMethod("performLongClick", null);
						//System.out.println("Long Click!");
						if(m==null){
							System.out.println("[severe] no performLongClick() method found for "+ view.toString());
						} else{
							m.invoke(view, null);
							if(Text!=null && !act){
								ApplicationStateLog.w(as.activity.toString()+"  Action: @long Click :"+view.getClass().getName()+" "+Text);
							}
							if (Text!=null && !act) {
								ApplicationStateLog.w(as.activity.toString()+" 's currently active fragment: "+as.currentFragment.fragmentClass+"  Action: @long Click :"+view.getClass().getName()+" "+Text);
							}
						}
					} catch (Exception e) {
						if(!(e instanceof java.lang.NoSuchMethodException)){
							e.printStackTrace();
						}
					}
					break;
				case HandlerType.TOUCH:
					//<2do> not urgent as on touch listener is not common
//					
					break;
				case HandlerType.HOVER:
					//<2do>
//					times++;
					break;
				case HandlerType.CREATE_CONTEXT_MENU:
					//<2do>
//					times++;
					break;
				case HandlerType.DRAG:
					//<2do>
//					times++;
					break;
				case HandlerType.KEY:
					//<2do>
//					times++;
					break;
				default:
					break;
				}
			}
			break;
		case SWITCH_EVENT:
			if(choicenum==0){
				exeLCHandlerToOnStop(as);
			}
			break;
		case MENU_CLICK_EVENT:
			if(as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_RESUME){
				executeOnCreateOptionsMenu(as);
				executeOnPrepareOptionsMenu(as);
			}
			break;
		case MENU_ITEM_CLICK_EVENT:
			if(as.lastExecutedLCHandler == ActivityLifeCycleHandler.ON_RESUME){
				executeOnOptionsItemSelected(as, choicenum);
			}
			break;
		default:
			break;
		}
	}
	/**
	 * Deal with the corresponding handler logic
	 * at the same time,we apply the Fragment life cycle handler, if any
	 * @param actState
	 */
	public static void exeLCHandlerToOnResume(ActivityState actState){
		switch (actState.lastExecutedLCHandler) {
		case -1:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_CREATE_STRING,actState.activity,actState.destroyed,Bundle.class);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_CREATE;
			if(actState.currentFragment!=null){
			//	ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_ATTACH_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,Activity.class);
				try {
					System.out.print("I'm trying");
					Field act=Class.forName(actState.currentFragment.fragment.getClass().getSuperclass().getName()).getDeclaredField("mActivity");
					System.out.print("really?");
					act.setAccessible(true);
					act.set(actState.currentFragment.fragment, actState.activity);
				} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_CREATE_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity, Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_CREATEVIEW_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity,LayoutInflater.class,ViewGroup.class,Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_ACTIVITYCREATED_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity,Bundle.class);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_ACTIVITYCREATED;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_START_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_START;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_VIEWSTATERESTORED_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_START_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity, (Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_START;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_RESUME_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_RESUME;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_RESUME_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_RESUME;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size()+actState.currentFragment.registeredGUIComponentList.size();
			}
			
			break;
		case ActivityLifeCycleHandler.ON_PAUSE:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_RESUME_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_RESUME;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_RESUME_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_RESUME;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size()+actState.currentFragment.registeredGUIComponentList.size();
			}
			
			break;
		case ActivityLifeCycleHandler.ON_STOP:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_RESTART_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_RESTART;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_VIEWSTATERESTORED_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,Bundle.class);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_VIEWSTATERESTORED;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_START_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_START;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_START_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed, actState.activity, (Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_START;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_RESUME_STRING,actState.activity,actState.destroyed,(Class[])null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_RESUME;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_RESUME_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_RESUME;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size()+actState.currentFragment.registeredGUIComponentList.size();
			}
		}
		
	}
	public static void exeLCHandlerToOnStop(ActivityState actState){
		switch (actState.lastExecutedLCHandler) {
		case -1:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_CREATE_STRING, actState.activity, actState.destroyed, Bundle.class);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_CREATE;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_ATTACH_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,Activity.class);
				try {
					Field act=Class.forName(actState.currentFragment.fragmentClass).getDeclaredField("mActivity");
					act.setAccessible(true);
					act.set(actState.currentFragment.fragment, actState.activity);
				} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_CREATE_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity,  Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_CREATEVIEW_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity, LayoutInflater.class,ViewGroup.class,Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_ACTIVITYCREATED_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity, Bundle.class);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_ACTIVITYCREATED;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_START_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_START;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_VIEWSTATERESTORED_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,Bundle.class);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_START_STRING, actState.currentFragment.fragment, actState.currentFragment.destroyed,actState.activity, (Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_START;
			}
			
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_RESUME_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_RESUME;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_RESUME_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_RESUME;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size()+actState.currentFragment.registeredGUIComponentList.size();
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_PAUSE_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_PAUSE;
			if(actState.currentFragment!=null  && actState.currentFragment.lastExecutedLCHandler==FragmentLifeCycleHandler.ON_RESUME){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_PAUSE_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_PAUSE;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size();
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_STOP_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_STOP;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_STOP_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_STOP;
			}
			
			break;
		case ActivityLifeCycleHandler.ON_RESUME:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_PAUSE_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_PAUSE;
			if(actState.currentFragment!=null  && actState.currentFragment.lastExecutedLCHandler==FragmentLifeCycleHandler.ON_RESUME){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_PAUSE_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_PAUSE;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size();
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_STOP_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_STOP;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_STOP_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_STOP;
			}
			
			break;
		case ActivityLifeCycleHandler.ON_PAUSE:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_STOP_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_STOP;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_STOP_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_STOP;
			}
			
			break;
		default:
			break;
		}
	}
	public static void exeLCHandlerToOnDestroy(ActivityState actState){
		switch(actState.lastExecutedLCHandler){
		//no handler has been executed to finish yet, we will not execute any handler
		case -1: 
			break;
		case ActivityLifeCycleHandler.ON_RESUME:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_PAUSE_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_PAUSE;
			if(actState.currentFragment!=null && actState.currentFragment.lastExecutedLCHandler==FragmentLifeCycleHandler.ON_RESUME){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_PAUSE_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_PAUSE;
				actState.GUIComponentNumber=actState.registeredGUIComponentList.size();
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_STOP_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_STOP;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_STOP_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_STOP;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_DESTROY_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_DESTROY;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DESTORY_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DETACH_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_DETACH;
				actState.currentFragment.destroyed=true;
			}
			break;
		case ActivityLifeCycleHandler.ON_PAUSE:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_STOP_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_STOP;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_STOP_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_STOP;
			}
			
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_DESTROY_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_DESTROY;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DESTORY_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DETACH_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_DETACH;
				actState.currentFragment.destroyed=true;
			}
			
			break;
		case ActivityLifeCycleHandler.ON_STOP:
			invokeLCHandlerForActivity(ActivityLifeCycleHandler.ON_DESTROY_STRING, actState.activity, actState.destroyed, (Class[]) null);
			actState.lastExecutedLCHandler = ActivityLifeCycleHandler.ON_DESTROY;
			if(actState.currentFragment!=null){
				//ApplicationStateLog.w("Current Active Fragment: "+actState.currentFragment.fragmentClass);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DESTORY_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				invokeLCHandlerForFragment(FragmentLifeCycleHandler.ON_DETACH_STRING,actState.currentFragment.fragment,actState.currentFragment.destroyed,actState.activity,(Class[])null);
				actState.currentFragment.lastExecutedLCHandler=FragmentLifeCycleHandler.ON_DETACH;
				actState.currentFragment.destroyed=true;
			}
			
			break;
		}
		//may have parent and may returnCode , returnIntent or requestCode be set,then call onActivity's onActivityResult()
		if(actState.parentActState !=null && actState.requestCode!=null){
			if(actState.requestCode!=null){
				Class cls=actState.parentActState.activity.getClass();
				Method m = null;
				try{
					while(cls !=null){
						try{
							m=cls.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
							if(m!=null){
								m.setAccessible(true);
								ApplicationStateLog.w(actState.parentActState.activity.toString()+"@onActivityResult");
								m.invoke(actState.parentActState.activity, actState.requestCode.intValue(),actState.returnResult);
								break;
							}
						}catch(NoSuchMethodException e){
							cls=cls.getSuperclass();
						}
					}
				}catch(Exception oe){
					oe.printStackTrace(System.err);
				}
			}
		}
		actState.destroyed=true;
	}
	public static boolean executeOnCreateOptionsMenu(ActivityState actState){
		try{
			Method m = actState.activity.getClass().getDeclaredMethod("onCreateOptionsMenu", Menu.class);
			if(m != null){
				m.setAccessible(true);
				if(actState.menu !=null){
					m.invoke(actState.activity, actState.menu);
					return true;
				}
			}
		}catch(NoSuchMethodException e){
			}catch (Exception e) {
				e.printStackTrace(System.err);
			}
		return false;
	}
	public static boolean executeOnPrepareOptionsMenu(ActivityState actState){
		try {
			Method m = actState.activity.getClass().getDeclaredMethod("onPrepareOptionsMenu", Menu.class);
			if(m !=null){
				m.setAccessible(true);
				if(actState.menu != null){
					m.invoke(actState.activity, actState.menu);
					return true;
				}
			}
		}catch(NoSuchMethodException e){} 
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return false;
	}
	public static boolean executeOnOptionsItemSelected(ActivityState actState, int itemId){
		try {
			Method m = actState.activity.getClass().getDeclaredMethod("onOptionsItemSelected", MenuItem.class);
			if(m!=null){
				m.setAccessible(true);
				if(actState.menu!=null){
					MenuItem item = new MenuItem() {
						int id;
						@Override
						public MenuItem setVisible(boolean arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setTitleCondensed(CharSequence arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setTitle(int arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setTitle(CharSequence arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setShowAsActionFlags(int arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public void setShowAsAction(int arg0) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public MenuItem setShortcut(char arg0, char arg1) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setOnActionExpandListener(OnActionExpandListener arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setNumericShortcut(char arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setIntent(Intent arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setIcon(int arg0) {
							id=arg0;
							return this;
						}
						
						@Override
						public MenuItem setIcon(Drawable arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setEnabled(boolean arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setChecked(boolean arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setCheckable(boolean arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setAlphabeticShortcut(char arg0) {
							// TODO Auto-generated method stub
							return this;
						}
						
						@Override
						public MenuItem setActionView(int arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setActionView(android.view.View arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MenuItem setActionProvider(ActionProvider arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public boolean isVisible() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean isEnabled() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean isChecked() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean isCheckable() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean isActionViewExpanded() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean hasSubMenu() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public CharSequence getTitleCondensed() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public CharSequence getTitle() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public SubMenu getSubMenu() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public int getOrder() {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public char getNumericShortcut() {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public ContextMenuInfo getMenuInfo() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public int getItemId() {
							// TODO Auto-generated method stub
							return id;
						}
						//necessary?
						public void setItemId(int id) {
							this.id=id;
						}
						
						@Override
						public Intent getIntent() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Drawable getIcon() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public int getGroupId() {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public char getAlphabeticShortcut() {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public android.view.View getActionView() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public ActionProvider getActionProvider() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public boolean expandActionView() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public boolean collapseActionView() {
							// TODO Auto-generated method stub
							return false;
						}
					};
					item.setIcon(itemId);
					m.invoke(actState.activity, item);
					return true;
				}
			}
		}catch(NoSuchMethodException e){}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return false;
	}
	public static boolean invokeLCHandlerForActivity(String hanlerName, Object o, boolean destroyed, Class... arguments) {
		if(o instanceof Activity && !destroyed){
			Class cls =o.getClass();
			Method m=null;
			//some activity may use base class's handler, need to trace
			try {
				while(cls!=null){
					try {
						m=cls.getDeclaredMethod(hanlerName, arguments);
						//System.out.println(hanlerName+" "+cls.getName());
						if(m!=null){
							m.setAccessible(true);
							ApplicationStateLog.w(o.toString()+"@"+hanlerName);
							if(arguments!=null){
								//the onCreate will use argument savedBundleInstance, may cause exception£¬possiblly because no instance?
								//System.out.println("onCreate!");
								m.invoke(o, new Object[arguments.length]);
								//System.out.println("onCreate!");
							}else{
								m.invoke(o,null);
							}
							return true;
						}
					} catch (NoSuchMethodException oe) {
						cls=cls.getSuperclass();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public static boolean invokeLCHandlerForFragment(String handlerName, Object o, boolean destoryed,Activity activity,Class... arguments){
		if(o instanceof Fragment && !destoryed){
			Class cls=o.getClass();
			Method m=null;
			try {
				while (cls!=null) {
					try {
						//System.out.println(handlerName+" "+cls.getName());
						m=cls.getDeclaredMethod(handlerName, arguments);
						if(m!=null){
							m.setAccessible(true);
							//System.out.println(o.toString()+"@"+handlerName);
							ApplicationStateLog.w("   "+o.getClass().getPackage().getName()+"."+o.toString()+"@"+handlerName);
							if (handlerName.equals(FragmentLifeCycleHandler.ON_ATTACH_STRING)) {
								m.invoke(o, activity);
								return true;
							}
							if(arguments!=null){
								m.invoke(o, new Object[arguments.length]);
							}else {
								m.invoke(o, null);
							}
							return true;
						}
					} catch (NoSuchMethodException e) {
						//System.out.println("NoSuchMethod");
						cls=cls.getSuperclass();
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}

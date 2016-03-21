package edu.nju.Alex.GreenDroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnActionExpandListener;
import android.view.MenuItem.OnMenuItemClickListener;

public class ActivityState {
	//the Activity instance
	public Activity activity;
	
	/**
	 * registeredGUIComponentList tracks a list of GUI components that has registered listeners.
	 * There is index correspondence between registeredGUIComponentList and eventsList
	 */
	public ArrayList<Object> registeredGUIComponentList;
	public ArrayList<String> registeredGUIComponentText;
	//because of the fragment, the total number of component is different
	public int GUIComponentNumber=0;
	/**
	 * eventsList[i] tracks the events that the registeredGUIComponentList[i] can respond to.
	 */
	public ArrayList<ArrayList<Integer>> eventsList;
	
	/**
	 * fragmentList stores all the fragments possible for the activity, currentFragment refers to the current Fragment
	 */
	public LinkedList<FragmentState> fragmentList;
	public FragmentState currentFragment;
	
	
	//populate menu here
	public Menu menu;
	
	public ArrayList<Integer> menuItemIDs = new ArrayList<Integer>();
	
	public int lastExecutedLCHandler;
	
	public boolean destroyed;
	
	//who dares start this activity
	public ActivityState parentActState;
	
	//the request code that is used to start this activity, we use a Integer object so that the object being null means no request code
	public Integer requestCode;
	
	//the return result set by calling setResult(I) or setResult(I, Intent)
	//if we do not model setResult(I) and setResult(I, Intent) as native peer, these two can also be get from the mResultCode and mResultData fields of the activity object
	public Integer returnResult;
	
	//the return intent
	public Intent returnIntent;
	
	//the layoutId
	public int layoutId;
	
	//this one's for handling startActivityForResult(),each request code for a started activity
	public HashMap<Integer, Object> codeActMap= new HashMap<Integer, Object>();
	
	//initialization
	public ActivityState(Activity act){
		this.activity=act;
		this.registeredGUIComponentList=new ArrayList<Object>();
		this.registeredGUIComponentText=new ArrayList<String>();
		this.eventsList=new ArrayList<ArrayList<Integer>>();
		this.fragmentList=new LinkedList<FragmentState>();
		currentFragment=null;
		
		//the first three buttons for default, added to gui component list
		//1:back 2:menu(actually along with a random select item) 3:home 
		//the old version add for, 2 and 3 are both for home button, why? 
		ArrayList<Integer> clickEvent = new ArrayList<Integer>();
		clickEvent.add(HandlerType.CLICK);
		//the fifth one contain external events
		for(int i=0;i<5;i++){
			this.registeredGUIComponentList.add(null);
			this.registeredGUIComponentText.add(null);
			this.eventsList.add(clickEvent);
		}
		
		this.GUIComponentNumber=this.registeredGUIComponentList.size();
		this.lastExecutedLCHandler=-1;
		this.destroyed=false;
		this.layoutId=-1;
		this.parentActState=null;
		this.requestCode=null;
		this.returnResult=null;
		this.returnIntent=null;
		
		//populate menu here
		try {
			this.menu = new Menu() {
				
				@Override
				public int size() {
					// TODO Auto-generated method stub
					return 0;
				}
				
				@Override
				public void setQwertyMode(boolean arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void setGroupVisible(int arg0, boolean arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void setGroupEnabled(int arg0, boolean arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void setGroupCheckable(int arg0, boolean arg1, boolean arg2) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void removeItem(int arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void removeGroup(int arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public boolean performShortcut(int arg0, KeyEvent arg1, int arg2) {
					// TODO Auto-generated method stub
					return false;
				}
				
				@Override
				public boolean performIdentifierAction(int arg0, int arg1) {
					// TODO Auto-generated method stub
					return false;
				}
				
				@Override
				public boolean isShortcutKey(int arg0, KeyEvent arg1) {
					// TODO Auto-generated method stub
					return false;
				}
				
				@Override
				public boolean hasVisibleItems() {
					// TODO Auto-generated method stub
					return false;
				}
				
				@Override
				public MenuItem getItem(int arg0) {
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
						public MenuItem setActionView(View arg0) {
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
						public View getActionView() {
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
					item.setIcon(arg0);
					return item;
				}
				
				@Override
				public MenuItem findItem(int arg0) {
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
						public MenuItem setActionView(View arg0) {
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
						public View getActionView() {
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
					item.setIcon(arg0);
					return item;
				}
				
				@Override
				public void close() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void clear() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public SubMenu addSubMenu(int arg0, int arg1, int arg2, int arg3) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public SubMenu addSubMenu(int arg0, int arg1, int arg2, CharSequence arg3) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public SubMenu addSubMenu(int arg0) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public SubMenu addSubMenu(CharSequence arg0) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public int addIntentOptions(int arg0, int arg1, int arg2, ComponentName arg3, Intent[] arg4, Intent arg5, int arg6,
						MenuItem[] arg7) {
					// TODO Auto-generated method stub
					return 0;
				}
				
				@Override
				public MenuItem add(int arg0, int arg1, int arg2, int arg3) {
					if(!menuItemIDs.contains(new Integer(arg1))){
						menuItemIDs.add(arg1);
					}
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
						public MenuItem setActionView(View arg0) {
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
						public View getActionView() {
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
					item.setIcon(arg1);
					return item;
				}
				
				@Override
				public MenuItem add(int arg0, int arg1, int arg2, CharSequence arg3) {
					if(!menuItemIDs.contains(new Integer(arg1))){
						menuItemIDs.add(arg1);
					}
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
						public MenuItem setActionView(View arg0) {
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
						public View getActionView() {
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
					item.setIcon(arg1);
					return item;
				}
				
				@Override
				public MenuItem add(int arg0) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public MenuItem add(CharSequence arg0) {
					// TODO Auto-generated method stub
					return null;
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

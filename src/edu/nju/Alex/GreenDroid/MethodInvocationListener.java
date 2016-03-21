package edu.nju.Alex.GreenDroid;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL;
import gov.nasa.jpf.jvm.bytecode.InstanceInvocation;
import gov.nasa.jpf.jvm.bytecode.InstructionFactory;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.DirectCallStackFrame;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

/**
 * This listener monitors a set of critical Android API calls. 
 * Whenever such calls occur, we perform certain tasks in JPF's JVM for modeling the semantics of such calls.
 * The most important thing we should know is that this listener runs in local JVM
 * @see {@link edu.hkust.cse.android.analyzer.TargetMethodList the set of APIs we would monitor}
 */
public class MethodInvocationListener extends ListenerAdapter{
	//the layout xml being processed
	public static String layoutFileName;
	
	//the class containing info about one viewd'le
	public static class MyMap{
		//each layoutId and viewId pair match to a unique view
		int layoutId;
		int viewId;
		//in order to mark the instance ref
		int objRef;
		String viewIdS;
		String viewType;
		String viewText;
	}
	//the list containing info about each view
	public static ArrayList<MyMap> viewInfoList = new ArrayList<MethodInvocationListener.MyMap>();
	
	//the activityRefToLayoutIdMap stores maps created activities to its layout id
	public static HashMap<Integer, Integer> activityRefToLayoutIdMap = new HashMap<Integer,Integer>();
	//the FragmentRefToLayoutIdMap stores maps created Fragments to their layout ids
	public static HashMap<Integer, Integer> fragmentRefToLayoutIdMap = new HashMap<>();
	
	//broadcast receiver class name and its intent filter's actions (for handling static broadcast receivers)
	public static ArrayList<StaticReceiverState> receivers = new ArrayList<StaticReceiverState>();
	
	//translating the String.xml to a map
	public static HashMap<String, String> textIdToViewTextMap = new HashMap<String, String>();
	
	//the hashmap stores maps between the activities to their fragments, and whether the activities has been sent to Main
	public static HashMap<Integer, ArrayList<FragmentState>> activityToFragment=new HashMap<Integer, ArrayList<FragmentState>>();
	public static HashMap<Integer, Integer> activitySent=new HashMap<>();
	//the latest fragment's objref that called onCreateView
	public static int latestFragmentObj=-1;
		
	
	//mapping between action string and activities
	public static ArrayList<String> actionToActivityMap = new ArrayList<String>();
	
	public static boolean staticRcvsAdded = false;
	
	public static boolean actionToActivityMapAdded = false;
	
	//static initialization code for finding all UI Elements
	static{
		try{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			//extracts String constatns from String.xml file
			if(!edu.nju.Alex.GreenDroid.Config.STR_PATH.equals("null")){
				DefaultHandler handler1 = new DefaultHandler(){
					public boolean hit=false;
					public String textId = null;
					public String text = null;
					@Override
					public void startElement(String nameSpaceURI, String localName,String qName, Attributes attrs){
						//System.out.println("nameSpaceURI:"+ nameSpaceURI+" localName:"+localName+" qName:"+qName);
						if(qName.equals("string")){
							for (int i = 0; i < attrs.getLength(); i++) {
								if(attrs.getQName(i).equals("name")){
									textId=attrs.getValue(i);
									//System.out.println("textId: "+textId);
									hit=true;
									break;
								}
							}
						}
					}
					@Override
					public void characters(char[] ch, int start, int length){
					//	System.out.println("characters! "+hit+" "+new String(ch,start,length));
						if(hit){
							text=new String(ch,start,length);
							//System.out.println("text:"+text);
							if(textId!=null && text!=null){
								textIdToViewTextMap.put(textId, text);
							}
							hit=false;
						}
					}
				};
				
				//first load all text constants to the textIdToViewTextMap by parsing the strings.xml file
				File stringXML = new File(edu.nju.Alex.GreenDroid.Config.STR_PATH);
				System.out.println("[Preprocessing] parsing " + edu.nju.Alex.GreenDroid.Config.STR_PATH);
				saxParser.parse(stringXML, handler1);
			}
			
			//analyzes the layout configuration file and get (1) view id and view type mapping, and (2) view id view text mapping, and (3) the view id and the GUI layout mapping
			if(!edu.nju.Alex.GreenDroid.Config.LAYOUT_PATH.equals("null")){
				
				DefaultHandler handler2 =new DefaultHandler(){
					//the R$did inner class
					@SuppressWarnings("rawtypes")
					public Class id_cls = null;
					public Class layout_cls = null;
					public int viewId = -1;
					public int layoutId = -1;
					public String type;
					public String vId;
					public String text;
					
					//initialization, not static
					{
						//System.out.println("handler2");
						if(!edu.nju.Alex.GreenDroid.Config.R_ID_Class.equals("null")){
							//System.out.println("edu.nju.Alex.GreenDroid.Config.R_ID_Class"+edu.nju.Alex.GreenDroid.Config.R_ID_Class);
							id_cls = Class.forName(edu.nju.Alex.GreenDroid.Config.R_ID_Class);
						}
						if(!edu.nju.Alex.GreenDroid.Config.R_LAYOUT_Class.equals("null")){
							//System.out.println("edu.nju.Alex.GreenDroid.Config.LAYOUT_ID_Class"+edu.nju.Alex.GreenDroid.Config.R_LAYOUT_Class);
							layout_cls=Class.forName(edu.nju.Alex.GreenDroid.Config.R_LAYOUT_Class);
						}
					}
					@Override
					public void startDocument () throws SAXException {
						//get the layout id
						try {
							if(this.layout_cls !=null){
								Field f = this.layout_cls.getField(layoutFileName);
								layoutId = f.getInt(null);
								//System.out.println(f.getName()+" "+layoutId);
							}else{
								System.out.println("[Sever] No R.layout Class Found!");
							}
						} catch (Exception e) {
							System.out.println("[Severe] "+e.toString());
						}
					}
					
					@Override
					public void startElement(String nameSpaceURI, String localName, String qName, Attributes attrs){
						//System.out.println("nameSpaceURI:"+ nameSpaceURI+" localName:"+localName+" qName:"+qName);
						//System.out.println("gone?");
						if(!"view".equals(qName)){
							viewId = -1;
							type=null;
							text=null;
							//process if the view is a fragment
							if ("fragment".equals(qName)) {
								for(int i=0;i<attrs.getLength();i++){
									if(attrs.getQName(i).equals("android:id")){
										String value=attrs.getValue(i);
										String id=value.substring(value.indexOf('/')+1);
										try {
											if(this.id_cls!=null){
												Field f=this.id_cls.getField(id);
												viewId=f.getInt(null);	
											}
										} catch (Exception e) {
											System.out.println("[Severe] No R.id Class Found!");
										}
									}
									if (attrs.getQName(i).equals("android:name")) {
										text=attrs.getValue(i);
										//System.out.println(text);
									}
								}
								if(layoutId>0 && viewId>0){
									//System.out.println(activityToFragment.containsKey(layoutId)+" "+layoutId);
									FragmentState myFra=new FragmentState();
									myFra.viewId=viewId;
									myFra.fragmentClass=text;
									myFra.actLayoutId=layoutId;
									ArrayList<FragmentState> fra;
									if(activityToFragment.containsKey(layoutId)){
										fra=activityToFragment.get(layoutId);
										fra.add(myFra);
									}else {
										fra=new ArrayList<FragmentState>();
										fra.add(myFra);
										activityToFragment.put(layoutId, fra);
										//System.out.println(activityToFragment.containsKey(layoutId));
									}
									viewId=-1;
									type=text=null;
								}
								return;
							}
							for (int i = 0; i < attrs.getLength(); i++) {
								//what about app:xxx? ¡ûno such thing
								//System.out.println(attrs.getQName(i));
								if(attrs.getQName(i).equals("android:id")){
									String value = attrs.getValue(i);
									String id =value.substring(value.indexOf('/')+1);
									//System.out.println(id+" "+value);
									try {
										if(this.id_cls!=null){
											Field f =this.id_cls.getField(id);
											viewId=f.getInt(null);
											type=qName;
											if(!type.startsWith("android") && !type.contains(".")){
												type = "android.widget."+type;
											}
											vId=id;
											//System.out.println(viewId+" "+type);
										}else{
											System.out.println("[Severe] No R.id Class Found!");
										}
									} catch (Exception e) {
										System.out.println("[Severe] "+e.toString());
									}
								} else{
									if(attrs.getQName(i).equals("android:text")){
										String textId = attrs.getValue(i);
										//System.out.println(textId);
										if(textId.indexOf('/') == -1){
											//the text is assigned directly
											text=textId;
										}else{
											//the text is assigned via string/id
											String id = textId.substring(textId.indexOf('/')+1);
											text = textIdToViewTextMap.get(id);
										}
									}
								}
							}
							//System.out.println(layoutId+" "+viewId);
							if(viewId>0 && layoutId>0){
								MyMap viewInfo = new MyMap();
								viewInfo.layoutId = layoutId;
								viewInfo.viewId = viewId;
								viewInfo.viewIdS=vId;
								if(type!=null){
									viewInfo.viewType=type;
								}else{
									String type = qName;
									if(!type.startsWith("android") && !type.contains(".")){
										type="android.widget."+type;
									}
									viewInfo.viewType=type;
								}
								
								viewInfo.viewText=text;
								viewInfoList.add(viewInfo);
								viewId=-1;
								type=text=null;
							}
						}else{
							//System.out.println("user defined");
							//handling user-defined widget type
							viewId = -1;
							type=null;
							text=null;
							
							
							for(int i=0; i<attrs.getLength();i++){
								//System.out.println(attrs.getQName(i));
								if(attrs.getQName(i).equals("android:id")){
									String value = attrs.getValue(i);
									String id = value.substring(value.indexOf('/')+1);
									try{
										if(this.id_cls != null){
											Field f = this.id_cls.getField(id);
											viewId = f.getInt(null);
											type = attrs.getValue("class");
											if(!type.startsWith("android") && !type.contains(".")){
												type = "android.widget."+type;
											} 
										} else{
											System.out.println("[Severe] No R.id Class Found!");
										}
									} catch(Exception e){
										System.out.println("[Severe] "+e.toString());
									}
								} else{
									if(attrs.getQName(i).equals("android:text")){
										String textId = attrs.getValue(i);
										if(textId.indexOf('/') == -1){
											//the view's text is assigned directly instead of via string/id
											text = textId;
										} else{
											//the view's text is assigned via string/id
											String id = textId.substring(textId.indexOf('/')+1);
											text = textIdToViewTextMap.get(id);
										}
									}
								}
							}
							
							if(viewId>0 && layoutId>0){
								MyMap viewInfo = new MyMap();
								viewInfo.layoutId = layoutId;
								viewInfo.viewId = viewId;
								if(type!=null){
									viewInfo.viewType=type;
								}else {
									String type=qName;
									if(!type.startsWith("android") && !type.contains(".")){
										type = "android.widget."+type;
									}
									viewInfo.viewType=type;
								}
								
								viewInfo.viewText=text;
								viewInfoList.add(viewInfo);
								//System.out.println(viewInfo.layoutId + "=" + viewInfo.viewId + "=" + viewInfo.viewType);
								viewId=-1;
								type=text=null;
							}
						}
					}
				};
				
				//there could be multiple layout directories whose paths are separated by @
				String[] layoutFolders = edu.nju.Alex.GreenDroid.Config.LAYOUT_PATH.split("@");
				for(String layoutDir: layoutFolders){
					File layoutFolder = new File(layoutDir);
					File[] xmlList = layoutFolder.listFiles();
					for(File f: xmlList){
						if(f.getName().endsWith("xml")){
							System.out.println("[Preprocessing] parsing " + layoutDir + "/" + f.getName());
							layoutFileName = f.getName();
							layoutFileName=layoutFileName.split(".xml")[0];
							saxParser.parse(f, handler2);
						}
					}
				}
			}
			
			//extracts static broadcast receivers from the AndroidManifest file
			DefaultHandler handler3 = new DefaultHandler(){
				public String pkgName;
				public boolean pkgNameFound = false;
				public boolean handlingOneReceiver = false;
				public String receiverClsName;
				public String receiverPermission;
				public ArrayList<String> actions;
				
				@Override
				public void endDocument() throws SAXException{
					//I guess the following line does not work, that is why we introduce addStaticReceiversToMain; for safety, we keep it here, why?
					Main.addStaticReciever(receivers);
				}
				
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
					//System.out.println("nameSpaceURI:"+ uri+" localName:"+localName+" qName:"+qName);
					if(!pkgNameFound){
						if(qName.equals("manifest")){
							
							//iterate all to get the pkgName
							pkgName = attributes.getValue("package");
							pkgNameFound = true;
						}
					}
					
					if(qName.equals("receiver")){
						handlingOneReceiver=true;
						actions=new ArrayList<String>();
						receiverClsName = attributes.getValue("android:name");
						receiverPermission = attributes.getValue("android:permission");
						if(receiverClsName !=null){
							if(!receiverClsName.contains(pkgName)){
								if(receiverClsName.startsWith(".")){
									receiverClsName = pkgName + receiverClsName;
								}else{
									receiverClsName = pkgName+"."+receiverClsName;
								}
							}
						}
						return;
					}
					
					if(handlingOneReceiver && qName.equals("action")){
						String temp = attributes.getValue("android:name");
						if(temp!=null){
							actions.add(temp);
						}
					}
				}
				
				@Override
				public void endElement(String uri, String localName, String qName)throws SAXException{
					if(qName.equals("recervier")){
						if(receiverClsName !=null && actions.size()>0){
							receivers.add(new StaticReceiverState(receiverClsName, receiverPermission, actions));
						}
						handlingOneReceiver=false;
					}
				}
			};
			
			File manifestFile = new File(edu.nju.Alex.GreenDroid.Config.MANIFEST_PATH);
			System.out.println("[Preprocessing] parsing "+edu.nju.Alex.GreenDroid.Config.MANIFEST_PATH);
			saxParser.parse(manifestFile, handler3);
			
			
			//extracts the association between action strings and activities
			DefaultHandler handler4 = new DefaultHandler(){
				public String pkgName;
				public boolean pkgNameFound = false;
				public String actClsName;
				public boolean handlingOneActivity = false;
				public boolean handlingOneIntentFilter = false;
				public String actionStr;
				
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
					//System.out.println("nameSpaceURI:"+ uri+" localName:"+localName+" qName:"+qName);
					if(!pkgNameFound){
						if(qName.equals("manifest")){
							//get pkaName
							pkgName=attributes.getValue("package");
							pkgNameFound=true;
						}
					}
					
					if("activity".equals(qName)){
						handlingOneActivity=true;
						actClsName = attributes.getValue("android:name");
						//System.out.println(actClsName);
						if(actClsName!=null){
							if(!actClsName.contains(pkgName)){
								if(actClsName.startsWith(".")){
									actClsName=pkgName+actClsName;
								}else{
									actClsName=pkgName+"."+actClsName;
								}
							}
						}
					}
					if("intent-filter".equals(qName)){
						handlingOneIntentFilter=true;
					}
					if("action".equals(qName) && handlingOneIntentFilter && handlingOneActivity){
						actionStr = attributes.getValue("android:name");
						//System.out.println(actionStr);
						//we found an action and activity pair (in current implementation, we ignore the "category")
						//if an intent contains some categories, then the activity needs to have all those categories set in manifest file in order to be started
						actionToActivityMap.add(actionStr+":"+actClsName);
					}
				}
				
				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException{
					if("activity".equals(qName)){
						handlingOneActivity=false;
					}
					if("intent-filter".equals(qName)){
						handlingOneIntentFilter=false;
					}
				}
			};
			
			System.out.println("[Preprocessing] parsing "+edu.nju.Alex.GreenDroid.Config.MANIFEST_PATH);
			saxParser.parse(manifestFile, handler4);
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//execute when jpf has executed a instruction
	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction){
		//System.out.println("before the Execution?");
		ClassInfo ciMain = ClassLoaderInfo.getCurrentResolvedClassInfo("edu.nju.Alex.GreenDroid.Main");
		
		if(executedInstruction instanceof InstanceInvocation){
			String className = ((InstanceInvocation) executedInstruction).getInvokedMethodClassName();
			String methodName = ((InstanceInvocation) executedInstruction).getInvokedMethodName();
			//System.out.println("                                                                after the execution? "+className+"."+methodName);
			ElementInfo ei = ((InstanceInvocation) executedInstruction).getThisElementInfo(currentThread);
			//System.out.println(((InstanceInvocation) executedInstruction).getInvokedMethodClassName()+"."+methodName)
			//test
			if(Main.DEBUG){
				System.out.println("calling" + ei.toString() +" "+ methodName);
			}
			
			//in the following we model some methods that are not suitable for being modeled as native peers
			
			//handle setOnClickListener() API call
			if(methodName.equals(TargetMethodList.CLICK)){
				if(ei!=null){
					registerGUIEventListener(ciMain,ei,vm,HandlerType.CLICK,currentThread);
				}
				return;
			}
			
			//handle setOnLongClickListener() API call
			if(methodName.equals(TargetMethodList.LONG_CLICK)){
				if(ei!=null){
					//System.out.println("long click?");
					registerGUIEventListener(ciMain,ei,vm,HandlerType.LONG_CLICK,currentThread);
				}
				return;
			}
			
			//handle setOnTouchListener() API call
			if(methodName.equals(TargetMethodList.TOUCH)){
				if(ei!=null){
					registerGUIEventListener(ciMain,ei,vm,HandlerType.TOUCH,currentThread);
				}
				return;
			}
			
			//handle startActivity() API call
			//with fragment, it might not just from Context, could be from Fragment
			if(methodName.equals(TargetMethodList.START_ACT)){
				if(ei !=null && (ei.instanceOf("Landroid.content.Context;") || ei.instanceOf("Landroid.app.Fragment;"))){//still ture?
					//System.out.println("using this right?");
					Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
					if(args !=null && args.length==1){
						ElementInfo intentEi = (ElementInfo) args[0];
						
						//directly manipulate JPF's call stack
						MethodInfo pushAct = ciMain.getMethod("pushActivityOntoStack(Ljava/lang/Object;Ljava/lang/Object;)V", false);
						if(pushAct == null){
							throw new JPFException("no pushActivityOntoStack method in edu.nju.Alex.greendroid.Main class");
						}
						
						//process intent
						int compRef=intentEi.getReferenceField("mComponent");
						int actionRef=intentEi.getReferenceField("mAction");
						
						//create the call stub,don't know if right
						int maxLocals = pushAct.getMaxLocals();
						int maxStack=pushAct.getNumberOfCallerStackSlots();
						MethodInfo stub = new MethodInfo(pushAct, maxLocals,maxStack);
						ClassInfo ci=pushAct.getClassInfo();
						ArrayList<Instruction> a=new ArrayList<Instruction>();
						InstructionFactory instructionFactory=new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), pushAct.getName(), pushAct.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						if(Main.DEBUG){
							System.out.println(stub.isDirectCallStub());
						}
						//right order?
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
							
							@Override
							public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setLongArgumentLocal(int idx, long value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setArgumentLocal(int idx, int value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public int setReferenceArgument(int argOffset, int ref, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setLongArgument(int argOffset, long value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setArgument(int argOffset, int value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
						};
						
						frame.pushRef(compRef);
						frame.pushRef(actionRef);
						currentThread.pushFrame(frame);					
						currentThread.executeInstruction();
				}else{
					throw new JPFException("startActivity() should have one Intent argument");
				}
				return;	
				}
			}
			
			
			//handle startActivityForResult() API call
			if(methodName.equals(TargetMethodList.START_ACT_FOR_RESULT)){
				if(ei!=null && ei.instanceOf("Landroid.app.Activity;")){
					Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
					if(args !=null && args.length==2){
						ElementInfo intentEi = (ElementInfo) args[0];
						int requestCode = ((Integer) args[1]).intValue();
						
						MethodInfo pushAct =ciMain.getMethod("pushActivityOntoStack(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)V", false);
						if(pushAct == null){
							throw new JPFException("no pushActivityOntoStack method in edu.nju.Alex.greendroid.Main class");
						}
						int compRef = intentEi.getReferenceField("mComponent");
						int actionRef = intentEi.getReferenceField("mAction");
						int maxLocals = pushAct.getMaxLocals();
						int maxStack=pushAct.getNumberOfCallerStackSlots();
						MethodInfo stub = new MethodInfo(pushAct, maxLocals, maxStack);
						if(Main.DEBUG){
							System.out.println(stub.isDirectCallStub());
						}
						ClassInfo ci=pushAct.getClassInfo();
						ArrayList<Instruction> a =new ArrayList<Instruction>();
						InstructionFactory instructionFactory=new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), pushAct.getName(), pushAct.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						//right Order
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
							
							@Override
							public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setLongArgumentLocal(int idx, long value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setArgumentLocal(int idx, int value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public int setReferenceArgument(int argOffset, int ref, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setLongArgument(int argOffset, long value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setArgument(int argOffset, int value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
						};
						
						frame.pushRef(ei.getObjectRef());
						frame.pushRef(compRef);
						frame.pushRef(actionRef);
						frame.push(requestCode);
						currentThread.pushFrame(frame);
						currentThread.executeInstruction();
					
					} else{
						throw new JPFException("startActivityForResult() should have two arguments (Intent and RequestCode)");
					}
					return;
				}
			}
			
			
			
			//handler setResult() API call
			if(methodName.equals(TargetMethodList.SET_RESULTCODE)){
				if(ei!=null && ei. instanceOf("Landroid.app.Activity")){
					Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
					if(args !=null && args.length==1){
						int returnResult = ((Integer) args[0]).intValue();
						
						MethodInfo handleSetResult =ciMain.getMethod("handleSetResult(Ljava/lang/Object;ILjava/lang/Object;)V", false);
						if(handleSetResult == null){
							throw new JPFException("no pushActivityOntoStack method in edu.nju.Alex.greendroid.Main class");
						}
						int maxLocals = handleSetResult.getMaxLocals();
						int maxStack=handleSetResult.getNumberOfCallerStackSlots();
						MethodInfo stub = new MethodInfo(handleSetResult, maxLocals,maxStack);
						ClassInfo ci=handleSetResult.getClassInfo();
						ArrayList<Instruction> a=new ArrayList<Instruction>();
						InstructionFactory instructionFactory=new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), handleSetResult.getName(), handleSetResult.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						if(Main.DEBUG){
							System.out.println(stub.isDirectCallStub());
						}
						//right Order?
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
							
							@Override
							public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setLongArgumentLocal(int idx, long value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setArgumentLocal(int idx, int value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public int setReferenceArgument(int argOffset, int ref, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setLongArgument(int argOffset, long value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setArgument(int argOffset, int value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
						};
						
						frame.pushRef(ei.getObjectRef());
						frame.push(returnResult);
						frame.pushRef(MJIEnv.NULL);
						currentThread.pushFrame(frame);
						currentThread.executeInstruction();
					
					} else{
						throw new JPFException("SetResult() should have one arguments (RequestCode)");
					}
					return;
				}
			}
			
			//handle setResult() API call (setResult is overloaded such that people can only set ResultCode or set both the ResultCode and the result Intent)
			if(methodName.equals(TargetMethodList.SET_RESULTCODE_DATA)){
				if(ei!=null && ei. instanceOf("Landroid.app.Activity")){
					Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
					if(args !=null && args.length==2){
						int returnResult = ((Integer) args[0]).intValue();
						ElementInfo intentEi = (ElementInfo)args[1];
						
						MethodInfo handleSetResult =ciMain.getMethod("handleSetResult(Ljava/lang/Object;ILjava/lang/Object;)V", false);
						if(handleSetResult == null){
							throw new JPFException("no pushActivityOntoStack method in edu.nju.Alex.greendroid.Main class");
						}
						int maxLocals = handleSetResult.getMaxLocals();
						int maxStack=handleSetResult.getNumberOfCallerStackSlots();
						MethodInfo stub = new MethodInfo(handleSetResult, maxLocals,maxStack);
						ClassInfo ci=handleSetResult.getClassInfo();
						ArrayList<Instruction> a=new ArrayList<Instruction>();
						InstructionFactory instructionFactory=new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), handleSetResult.getName(), handleSetResult.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						if(Main.DEBUG){
							System.out.println(stub.isDirectCallStub());
						}
						//right Order?
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
							
							@Override
							public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setLongArgumentLocal(int idx, long value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setArgumentLocal(int idx, int value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public int setReferenceArgument(int argOffset, int ref, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setLongArgument(int argOffset, long value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setArgument(int argOffset, int value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
						};
						
						frame.pushRef(ei.getObjectRef());
						frame.push(returnResult);
						frame.pushRef(intentEi.getObjectRef());
						currentThread.pushFrame(frame);
						currentThread.executeInstruction();
					
					} else{
						throw new JPFException("SetResult() should have two arguments (Intent and RequestCode)");
					}
					return;
				}
			}
			
		//handle finish() API call
		if(methodName.equals(TargetMethodList.FINISH_ACT)){
			////called to finish an activity started by startActivityForResult
			
			if(ei !=null && ei.instanceOf("Landroid.app.Activity;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args !=null && args.length == 1){
					int requestCode = ((Integer) args[0]).intValue();
					
					MethodInfo finishAct = ciMain.getMethod("finishActivityByRequestCode(Ljava/land/Object;I)V", false);
					if(finishAct == null){
						throw new JPFException("no finishActivityByRequestCode method in edu.nju.Alex.greendroid.Main class");
					}
					
					int maxLocals = finishAct.getMaxLocals();
					int maxStack=finishAct.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(finishAct, maxLocals,maxStack);
					ClassInfo ci=finishAct.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), finishAct.getName(), finishAct.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					//create direct call stub
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(ei.getObjectRef());
					frame.push(requestCode);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
				}else{
					throw new JPFException("startActivityForResult() should have 1 argument");
				}
				return;
			}
		}
		//handle Activity.finishe() API call
		if(methodName.equals(TargetMethodList.ACT_FINISH)){
			//an activity call this to finish itself
			if(ei !=null && ei.instanceOf("Landroid.app.Activity;")){					
				MethodInfo finishAct = ciMain.getMethod("finishActivity(Ljava/land/Object;)V", false);
				if(finishAct == null){
					throw new JPFException("no finishActivityByRequestCode method in edu.nju.Alex.greendroid.Main class");
				}
				
				int maxLocals = finishAct.getMaxLocals();
				int maxStack=finishAct.getNumberOfCallerStackSlots();
				MethodInfo stub = new MethodInfo(finishAct, maxLocals,maxStack);
				ClassInfo ci=finishAct.getClassInfo();
				ArrayList<Instruction> a=new ArrayList<Instruction>();
				InstructionFactory instructionFactory=new InstructionFactory();
				a.add(instructionFactory.invokevirtual(ci.getName(), finishAct.getName(), finishAct.getSignature()));
				a.add(instructionFactory.directcallreturn());
				stub.setCode(a.toArray(new Instruction[a.size()]));
				//create direct call stub					
				DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
				frame.pushRef(ei.getObjectRef());
				currentThread.pushFrame(frame);
				currentThread.executeInstruction();
				return;
			}
		}
		
		//handle Activity.setContentView API call,only for public void setContentView(@LayoutRes int layoutResID),still mainly used
		if(methodName.equals(TargetMethodList.SET_CONTENT_VIEW)){
			if(ei !=null && ei.instanceOf("Landroid.app.Activity;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!=null && args.length==1){
					if(args[0] instanceof Integer) {
						int layoutid = ((Integer) args[0]).intValue();
						MethodInfo handleSetContentView = ciMain.getMethod("handleSetContentView(Ljava/lang/Object;I)V", false);
						
						if(handleSetContentView == null){
							throw new JPFException("no handleSetContentView method in edu.nju.Alex.greendroid.Main class");
						}
						
						
						//record the activity to layout id mapping
						if(activityRefToLayoutIdMap.containsKey(ei.getObjectRef())){
							activityRefToLayoutIdMap.remove(ei.getObjectRef());
							activityRefToLayoutIdMap.put(ei.getObjectRef(), layoutid);
						}else{
							activityRefToLayoutIdMap.put(ei.getObjectRef(), layoutid);
						}
						
						
						//MethodInfo stub = handleSetContentView.create(ciMain,"[handleSetContentView]", "()V", 1);
						int maxLocals = handleSetContentView.getMaxLocals();
						int maxStack = handleSetContentView.getNumberOfCallerStackSlots();
						MethodInfo stub=new MethodInfo(handleSetContentView, maxLocals, maxStack);
						//stub.setCode(handleSetContentView.getInstructions());
						//System.out.println(stub.getMaxLocals()+" "+stub.getMaxStack()+" "+handleSetContentView.getMaxLocals()+" "+handleSetContentView.getMaxStack());
						ClassInfo ci=handleSetContentView.getClassInfo();
						ArrayList<Instruction> a=new ArrayList<Instruction>();
						InstructionFactory instructionFactory = new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), handleSetContentView.getName(), handleSetContentView.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
							
							@Override
							public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setLongArgumentLocal(int idx, long value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void setArgumentLocal(int idx, int value, Object attr) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public int setReferenceArgument(int argOffset, int ref, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setLongArgument(int argOffset, long value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
							
							@Override
							public int setArgument(int argOffset, int value, Object attr) {
								// TODO Auto-generated method stub
								return 0;
							}
						};
					//	System.out.println(frame.getTopPos());
						frame.pushRef(ei.getObjectRef());
						frame.push(layoutid);
						currentThread.pushFrame(frame);
						//return;
						//System.out.println("really?!");
						currentThread.executeInstruction();
						//System.out.println("come back?");
						
						//we record the actlayout to fragment map in this listener, now we know the map between activity and fragments, so we send this information to Main class to build a activituToFragment map
						if(activityToFragment.containsKey(layoutid) && !activitySent.containsKey(layoutid)){
							activitySent.put(layoutid, null);
							MethodInfo setFragmentMap = ciMain.getMethod("setFragmentMap(ILjava/lang/String;I)V", false);
							if(setFragmentMap == null){
								throw new JPFException("no setFragmentMap in edu.nju.Alex.greendroid.Main class");
							}
							int imaxLocals = setFragmentMap.getMaxLocals();
							int imaxStack = setFragmentMap.getNumberOfCallerStackSlots();
							MethodInfo istub=new MethodInfo(setFragmentMap,imaxLocals,imaxStack);
							ClassInfo ici=setFragmentMap.getClassInfo();
							ArrayList<Instruction> ia =new ArrayList<Instruction>();
							InstructionFactory iinstructionFactory=new InstructionFactory();
							ia.add(iinstructionFactory.invokevirtual(ici.getName(), setFragmentMap.getName(), setFragmentMap.getSignature()));
							ia.add(iinstructionFactory.directcallreturn());
							istub.setCode(ia.toArray(new Instruction[a.size()]));
							ArrayList<FragmentState> list=activityToFragment.get(layoutid);
							for(int i=0;i<list.size();i++){
								DirectCallStackFrame iframe=new DirectCallStackFrame(istub,null) {
									
									@Override
									public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
										// TODO Auto-generated method stub
										
									}
									
									@Override
									public void setLongArgumentLocal(int idx, long value, Object attr) {
										// TODO Auto-generated method stub
										
									}
									
									@Override
									public void setArgumentLocal(int idx, int value, Object attr) {
										// TODO Auto-generated method stub
										
									}
									
									@Override
									public int setReferenceArgument(int argOffset, int ref, Object attr) {
										// TODO Auto-generated method stub
										return 0;
									}
									
									@Override
									public int setLongArgument(int argOffset, long value, Object attr) {
										// TODO Auto-generated method stub
										return 0;
									}
									
									@Override
									public int setArgument(int argOffset, int value, Object attr) {
										// TODO Auto-generated method stub
										return 0;
									}
								};
								iframe.push(list.get(i).viewId);
								ElementInfo sInfo=vm.getHeap().newString(list.get(i).fragmentClass, currentThread);
								iframe.pushRef(sInfo.getObjectRef());
								iframe.push(list.get(i).actLayoutId);
								currentThread.pushFrame(iframe);
								currentThread.executeInstruction();
								//System.out.println("what?");
							}
						}
					}else{
						System.out.println("[Warning] In current implementation, we only handle setContentView(int) API call");
					}
				}else{
					System.out.println("[Warning] In current implementation, we only handle setContentView(int) API call");
				}
			}
			return;
		}
		
		//handle onCreateView API call
		//simply record the last fragment that call on create view
		if (methodName.equals(TargetMethodList.ON_CREATE_VIEW)) {
			if (ei!=null && ei.instanceOf("Landroid.app.Fragment;")) {
				latestFragmentObj=ei.getObjectRef();
			}
			
		}
		
		//handle inflate() API call
		if (methodName.equals(TargetMethodList.INFLATE)) {
			if (ei!=null && ei.instanceOf("Landroid.view.LayoutInflater;")) {

				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!=null && args.length==3){
					int layoutId=((Integer) args[0]).intValue();
					
					MethodInfo handleInflate=ciMain.getMethod("handleInflate(Landroid/app/Fragment;I)V", false);
					if(handleInflate == null){
						throw new JPFException("No handleInflate method in edu.nju.Alex.greendroid.Main class");
					}
					
					//update the fragmentRefToLayoutIdMap
					if(fragmentRefToLayoutIdMap.containsKey(latestFragmentObj)){
						fragmentRefToLayoutIdMap.remove(latestFragmentObj);
						fragmentRefToLayoutIdMap.put(latestFragmentObj, layoutId);
					}else{
						fragmentRefToLayoutIdMap.put(latestFragmentObj, layoutId);
					}
					
					int maxLocals = handleInflate.getMaxLocals();
					int maxStack = handleInflate.getNumberOfCallerStackSlots();
					
					MethodInfo stub = new MethodInfo(handleInflate,maxLocals,maxStack);
					ArrayList<Instruction> a = new ArrayList<>();
					InstructionFactory instructionFactory = new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ciMain.getName(), handleInflate.getName(), handleInflate.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};

					frame.pushRef(latestFragmentObj);
					frame.push(layoutId);
					
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
				}else {
					System.out.println("[Severe] Inflate should have three argument");
				}
			}
		}
		
		//handle startService() API call
		if(methodName.equals(TargetMethodList.START_SER)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args !=null && args.length == 1){
					ElementInfo intentEi = (ElementInfo) args[0];
					
					MethodInfo handleService = ciMain.getMethod("handleStartService(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;)V", false);
					if(handleService == null){
						throw new JPFException("no handleStartService method in edu.nju.Alex.greendroid.Main class");
					}
					
					int compRef = intentEi.getReferenceField("mComponent");
					
					int maxLocals = handleService.getMaxLocals();
					int maxStack=handleService.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleService, maxLocals,maxStack);
					ClassInfo ci=handleService.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleService.getName(), handleService.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					//create direct call stub
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(compRef);
					frame.push(0);
					frame.pushRef(intentEi.getObjectRef());
					frame.push(MJIEnv.NULL);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
				}else{
					throw new JPFException("startService() should have one argument (Intent)");
				}
				return;
			}
		}
		
		
		//handle BindService() API call
		if(methodName.equals(TargetMethodList.BIND_SER)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args !=null && args.length == 3){
					ElementInfo intentEi = (ElementInfo) args[0];
					ElementInfo connEi = (ElementInfo) args[1];
					
					MethodInfo handleService = ciMain.getMethod("handleStartService(Ljava/land/Object;ILjava/lang/Object;Ljava/land/Object;)V", false);
					if(handleService == null){
						throw new JPFException("no handleStartService method in edu.nju.Alex.greendroid.Main class");
					}
					
					int compRef = intentEi.getReferenceField("mComponent");
					
					
					//create direct call stub
					int maxLocals = handleService.getMaxLocals();
					int maxStack=handleService.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleService, maxLocals,maxStack);
					ClassInfo ci=handleService.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleService.getName(), handleService.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(compRef);
					frame.push(1);
					frame.pushRef(intentEi.getObjectRef());
					frame.push(connEi.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
				}else{
					throw new JPFException("bindService() should have three arguments (Intent, ServiceConnection, and Flags)");
				}
				return;
			}
		}
		
		//handle Service.stopSelf() and Service.stopSelfResult() API calls
		if(methodName.equals(TargetMethodList.STOP_SELF) || methodName.equals(TargetMethodList.STOP_SELF_INT) || methodName.equals(TargetMethodList.STOP_SELF_RESULT)){
			if(ei !=null && ei.instanceOf("Landroid.app.Service;")){
				//a service calls stopSelf(void/int) or stopSelfResult(int) to stop itself
				MethodInfo handleStopService = ciMain.getMethod("handleStopService(Ljava/land/Object;)V", false);
				if(handleStopService == null){
					throw new JPFException("no handleStopService method in edu.nju.Alex.greendroid.Main class");
				}
				
				//create direct call stub
				int maxLocals = handleStopService.getMaxLocals();
				int maxStack=handleStopService.getNumberOfCallerStackSlots();
				MethodInfo stub = new MethodInfo(handleStopService, maxLocals,maxStack);
				ClassInfo ci=handleStopService.getClassInfo();
				ArrayList<Instruction> a=new ArrayList<Instruction>();
				InstructionFactory instructionFactory=new InstructionFactory();
				a.add(instructionFactory.invokevirtual(ci.getName(), handleStopService.getName(), handleStopService.getSignature()));
				a.add(instructionFactory.directcallreturn());
				stub.setCode(a.toArray(new Instruction[a.size()]));
				DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
					
					@Override
					public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void setLongArgumentLocal(int idx, long value, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void setArgumentLocal(int idx, int value, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public int setReferenceArgument(int argOffset, int ref, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
					
					@Override
					public int setLongArgument(int argOffset, long value, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
					
					@Override
					public int setArgument(int argOffset, int value, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
				};
				frame.pushRef(ei.getObjectRef());
				currentThread.pushFrame(frame);
				currentThread.executeInstruction();
				return;
			}
		}
		
		//handle context.stopService() API call
		if(methodName.equals(TargetMethodList.STOP_SER)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 1){
					ElementInfo intentEi = (ElementInfo) args[0];
					
					MethodInfo handleService = ciMain.getMethod("handleStopServiceWithName(Ljava/lang/Object;)V", false);
					if(handleService == null){
						throw new JPFException("no handlerStopServiceWithName method in edu.nju.Alex.greendroid.Main class");
					}
					
					int compRef = intentEi.getReferenceField("mComponent");
					//create direct call stub
					int maxLocals = handleService.getMaxLocals();
					int maxStack=handleService.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleService, maxLocals,maxStack);
					ClassInfo ci=handleService.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleService.getName(), handleService.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,handleService) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(compRef);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("stopService() should have one argument (Intent)");
				}
				return;
			}
		}
		
		//handle unBindService() API call
		if(methodName.equals(TargetMethodList.UNBIND_SER)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 1){
					ElementInfo connEi = (ElementInfo) args[0];
					
					MethodInfo handleUnbindService = ciMain.getMethod("handleUnbindService(Ljava/lang/Object;)V", false);
					if(handleUnbindService == null){
						throw new JPFException("no handlerUnbindService method in edu.nju.Alex.greendroid.Main class");
					}
					
					int connRef = connEi.getReferenceField("mComponent");
					//create direct call stub
					int maxLocals = handleUnbindService.getMaxLocals();
					int maxStack=handleUnbindService.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleUnbindService, maxLocals,maxStack);
					ClassInfo ci=handleUnbindService.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleUnbindService.getName(), handleUnbindService.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(connRef);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("unbindService() should have one argument (Intent)");
				}
				return;
			}
		}
		
		
		//handle registerReciever() API call
		if(methodName.equals(TargetMethodList.REG_RCV)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 2){
					ElementInfo rcvEi = (ElementInfo) args[0];
					ElementInfo ifEi = (ElementInfo) args[1];
					
					MethodInfo regReceiver = ciMain.getMethod("RegBroadcastRcv(Ljava/lang/Object;Ljava/lang/Object;Ljava/land/Object;Ljava/lang/Object;)V", false);
					if(regReceiver == null){
						throw new JPFException("no regBroadcastRcv method in edu.nju.Alex.greendroid.Main class");
					}
					
				
					//create direct call stub
					int maxLocals = regReceiver.getMaxLocals();
					int maxStack=regReceiver.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(regReceiver, maxLocals,maxStack);
					ClassInfo ci=regReceiver.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), regReceiver.getName(), regReceiver.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(ei.getObjectRef());
					frame.pushRef(rcvEi.getObjectRef());
					frame.pushRef(ifEi.getObjectRef());
					frame.pushRef(MJIEnv.NULL);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("registerReceiver(BroadcastReceiver, IntentFilter) should have 2 arguments");
				}
				return;
			}
		}
		
		//handle unRegisterReceiver() API call
		if(methodName.equals(TargetMethodList.UNREG_RCV)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 1){
					ElementInfo rcvEi = (ElementInfo) args[0];
					if(rcvEi !=null){
						MethodInfo unregRcv = ciMain.getMethod("unRegBroadcastRcv(Ljava/lang/Object;Ljava/lang/Object;)V", false);
						if(unregRcv == null){
							throw new JPFException("no unRegBroadcastRcv method in edu.nju.Alex.greendroid.Main class");
						}
					
				
						//create direct call stub
						int maxLocals = unregRcv.getMaxLocals();
						int maxStack=unregRcv.getNumberOfCallerStackSlots();
						MethodInfo stub = new MethodInfo(unregRcv, maxLocals,maxStack);
						ClassInfo ci=unregRcv.getClassInfo();
						ArrayList<Instruction> a=new ArrayList<Instruction>();
						InstructionFactory instructionFactory=new InstructionFactory();
						a.add(instructionFactory.invokevirtual(ci.getName(), unregRcv.getName(),unregRcv.getSignature()));
						a.add(instructionFactory.directcallreturn());
						stub.setCode(a.toArray(new Instruction[a.size()]));
						DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
						frame.pushRef(ei.getObjectRef());
						frame.pushRef(rcvEi.getObjectRef());
						currentThread.pushFrame(frame);
						currentThread.executeInstruction();
					}
				}else{
					throw new JPFException("registerReceiver(BroadcastReceiver, IntentFilter) should have 2 arguments");
				}
				return;
			}
		}
		
		
		//handle sendBoardcast() API call
		if(methodName.equals(TargetMethodList.SEND_BROADCAST)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 1){
					ElementInfo intentEi = (ElementInfo) args[0];
					
					MethodInfo dispatchBoardcasts = ciMain.getMethod("dispatchBoardcastMsg(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false);
					if(dispatchBoardcasts == null){
						throw new JPFException("no dispatchBoardcastMsg method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = dispatchBoardcasts.getMaxLocals();
					int maxStack=dispatchBoardcasts.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(dispatchBoardcasts, maxLocals,maxStack);
					ClassInfo ci=dispatchBoardcasts.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), dispatchBoardcasts.getName(), dispatchBoardcasts.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(ei.getObjectRef());
					frame.pushRef(intentEi.getObjectRef());
					frame.pushRef(MJIEnv.NULL);
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("sendBroadcast(Intent) should have 1 argument");
				}
				return;
			}
		}
		
		
		//handle overloaded sendBoardcast() API call
		if(methodName.equals(TargetMethodList.SEND_BROADCAST_WITH_PER)){
			if(ei !=null && ei.instanceOf("Landroid.content.Context;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 2){
					ElementInfo intentEi = (ElementInfo) args[0];
					ElementInfo permissionEi = (ElementInfo) args[1];
					
					MethodInfo dispatchBoardcasts = ciMain.getMethod("dispatchBoardcastMsg(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false);
					if(dispatchBoardcasts == null){
						throw new JPFException("no dispatchBoardcastMsg method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = dispatchBoardcasts.getMaxLocals();
					int maxStack=dispatchBoardcasts.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(dispatchBoardcasts, maxLocals,maxStack);
					ClassInfo ci=dispatchBoardcasts.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), dispatchBoardcasts.getName(), dispatchBoardcasts.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					frame.pushRef(ei.getObjectRef());
					frame.pushRef(intentEi.getObjectRef());
					frame.pushRef(permissionEi.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("sendBroadcast(Intent) should have 1 argument");
				}
				return;
			}
		}
		
		//handle requestLocationUpdates() API call
		if(methodName.equals(TargetMethodList.REG_LOC_LISTENER)){
			if(ei !=null && ei.instanceOf("Landroid.location.LocationManager;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 4){
					ElementInfo providerEi = (ElementInfo) args[0];
					Long minTimeEi = (Long) args[1];
					Float minDistEi = (Float) args[2];
					ElementInfo listenerEi = (ElementInfo) args[3];
					
					MethodInfo regListener = ciMain.getMethod("registerLocListener(Ljava/lang/Object;JDLjava/lang/Object;)V", false);
					if(regListener == null){
						throw new JPFException("no registerLocListener method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = regListener.getMaxLocals();
					int maxStack=regListener.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(regListener, maxLocals,maxStack);
					ClassInfo ci=regListener.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), regListener.getName(), regListener.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					//don't know still the same, check later
					if(providerEi == null){
						frame.pushRef(MJIEnv.NULL);
					}else{
						frame.pushRef(providerEi.getObjectRef());
					}
					frame.pushLong(minTimeEi.longValue());
					frame.pushDouble(minDistEi.doubleValue());
					frame.pushRef(listenerEi.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("requestLocationUpdates should have 4 arguments");
				}
				return;
			}
		}
		
		//handle removeUpdate() API call
		if(methodName.equals(TargetMethodList.UNREG_LOC_LISTENER)){
			if(ei !=null && ei.instanceOf("Landroid.location.LocationManager;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 4){
					ElementInfo providerEi = (ElementInfo) args[0];
					Long minTimeEi = (Long) args[1];
					Float minDistEi = (Float) args[2];
					ElementInfo listenerEi = (ElementInfo) args[3];
					
					MethodInfo regListener = ciMain.getMethod("registerLocListener(Ljava/lang/Object;JDLjava/lang/Object;)V", false);
					if(regListener == null){
						throw new JPFException("no registerLocListener method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = regListener.getMaxLocals();
					int maxStack=regListener.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(regListener, maxLocals,maxStack);
					ClassInfo ci=regListener.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), regListener.getName(), regListener.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					//don't know still the same, check later
					if(providerEi == null){
						frame.pushRef(MJIEnv.NULL);
					}else{
						frame.pushRef(providerEi.getObjectRef());
					}
					frame.pushLong(minTimeEi.longValue());
					frame.pushDouble(minDistEi.doubleValue());
					frame.pushRef(listenerEi.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("requestLocationUpdates should have 4 arguments");
				}
				return;
			}
		}
		
		
		//handle wakelock.acquire() API call
		if(methodName.equals(TargetMethodList.WAKELOCK_ACQUISITION)){
			if(ei !=null && ei.instanceOf("Landroid.os.PowerManager$WakeLock;")){//$:nested class
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 0){
					
					MethodInfo handleWakeLockAcquisition = ciMain.getMethod("handleWakeLockAcquistion(Ljava/lang/Object;)V", false);
					if(handleWakeLockAcquisition == null){
						throw new JPFException("no handleWakeLockAcquistion method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = handleWakeLockAcquisition.getMaxLocals();
					int maxStack=handleWakeLockAcquisition.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleWakeLockAcquisition, maxLocals,maxStack);
					ClassInfo ci=handleWakeLockAcquisition.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleWakeLockAcquisition.getName(), handleWakeLockAcquisition.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};

					frame.pushRef(ei.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("acquire() has no arguments");
				}
				return;
			}
		}
		
		
		//handle wakelock.release() API call
		if(methodName.equals(TargetMethodList.WAKELOCK_RELEASING)){
			if(ei !=null && ei.instanceOf("Landroid.os.PowerManager$WakeLock;")){//$:nested class
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 0){
					
					MethodInfo handleWakeLockReleasing = ciMain.getMethod("handleWakeLockReleasing(Ljava/lang/Object;)V", false);
					if(handleWakeLockReleasing == null){
						throw new JPFException("no handleWakeLockReleasing method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = handleWakeLockReleasing.getMaxLocals();
					int maxStack=handleWakeLockReleasing.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleWakeLockReleasing, maxLocals,maxStack);
					ClassInfo ci=handleWakeLockReleasing.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(),handleWakeLockReleasing.getName(), handleWakeLockReleasing.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};

					frame.pushRef(ei.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("release() has no arguments");
				}
				return;
			}
		}
		
		//handle setKeepScreenOn API
		if(methodName.equals(TargetMethodList.SET_KEEP_SCREEN_ON)){
			if(ei !=null && ei.instanceOf("Landroid.view.View;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 1){
					Boolean status = (Boolean) args[0];
					
					MethodInfo handleSetKeepScreenOn = ciMain.getMethod("handleSetKeepScreenOn(I)V", false);
					if(handleSetKeepScreenOn == null){
						throw new JPFException("no handleSetKeepScreenOn method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = handleSetKeepScreenOn.getMaxLocals();
					int maxStack=handleSetKeepScreenOn.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleSetKeepScreenOn, maxLocals,maxStack);
					ClassInfo ci=handleSetKeepScreenOn.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleSetKeepScreenOn.getName(), handleSetKeepScreenOn.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					if(status){
						frame.push(1);
					}else{
						frame.push(0);
					}
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("setKeepSvreenOn() has one arguments");
				}
				return;
			}
		}
		
		//handle MyLocaitionOverlay.enableMyLocation()
		if(methodName.equals(TargetMethodList.ENABLE_MY_LOCATION)){
			if(ei !=null && ei.instanceOf("Lcom.android.maps.MyLocationOverLay;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 0){
					
					MethodInfo handleEnableMyLocation = ciMain.getMethod("handleEnableMyLocation(Ljava/lang/Object;)V", false);
					if(handleEnableMyLocation == null){
						throw new JPFException("no handleEnableMyLocation method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = handleEnableMyLocation.getMaxLocals();
					int maxStack=handleEnableMyLocation.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleEnableMyLocation, maxLocals,maxStack);
					ClassInfo ci=handleEnableMyLocation.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleEnableMyLocation.getName(), handleEnableMyLocation.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					
					frame.pushRef(ei.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("enableMyLocation() should have no arguments");
				}
				return;
			}
		}
		
		//handle MyLocationOverlay.disableMyLocation()
		if(methodName.equals(TargetMethodList.DISABLE_MY_LOCATION)){
			if(ei !=null && ei.instanceOf("Lcom.android.maps.MyLocationOverLay;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length == 0){
					
					MethodInfo handleDisableMyLocation = ciMain.getMethod("handleDisableMyLocation(Ljava/lang/Object;)V", false);
					if(handleDisableMyLocation == null){
						throw new JPFException("no handleDisableMyLocation method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = handleDisableMyLocation.getMaxLocals();
					int maxStack=handleDisableMyLocation.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(handleDisableMyLocation, maxLocals,maxStack);
					ClassInfo ci=handleDisableMyLocation.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), handleDisableMyLocation.getName(), handleDisableMyLocation.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					
					frame.pushRef(ei.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("disableMyLocation() should have no arguments");
				}
				return;
			}
		}
		
		
		//handle AsyncTask.execute()
		if(methodName.equals(TargetMethodList.ASYNCTASK_EXECUTE)){
			//model execution of asyncTask ignoring concurrency
			//step 1: call the onPreExecute in the main thread
			//Step 2: call doInBackGround in the main thread
			//Step 3: call onPostExecute in the main thread
			//ps,we model execute()in the peers as a empty method
			
			
			if(ei !=null && ei.instanceOf("Landroid.os.AsyncTask;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null){
					
					MethodInfo exeAsyncTask = ciMain.getMethod("executeAsyncTask(ILjava/lang/Object;[Ljava/lang/Object;)V", false);
					if(exeAsyncTask == null){
						throw new JPFException("no executeAsyncTask method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = exeAsyncTask.getMaxLocals();
					int maxStack=exeAsyncTask.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(exeAsyncTask, maxLocals,maxStack);
					ClassInfo ci=exeAsyncTask.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), exeAsyncTask.getName(), exeAsyncTask.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,exeAsyncTask) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					
					frame.push(args.length);
					frame.pushRef(ei.getObjectRef());
					for (Object obj:args) {
						ElementInfo objEi = (ElementInfo) obj;
						frame.pushRef(objEi.getObjectRef());
					}
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("execute should have at least 1 argument, even null object");
				}
				return;
			}
		}
		
		
		//handle Timer.schedule()
		if(methodName.equals(TargetMethodList.TIMER_SCHEDULE_1) || methodName.equals(TargetMethodList.TIMER_SCHEDULE_2) || methodName.equals(TargetMethodList.TIMER_SCHEDULE_3) || methodName.equals(TargetMethodList.TIMER_SCHEDULE_4)){
			if(ei !=null && ei.instanceOf("Ljava.util.Timer;")){
				Object[] args = ((INVOKEVIRTUAL) executedInstruction).getArgumentValues(currentThread);
				if(args!= null && args.length > 0){
					ElementInfo task = (ElementInfo) args[0];
					
					MethodInfo exeTimerTask = ciMain.getMethod("executeTimerTask(Ljava/lang/Object;)V", false);
					if(exeTimerTask == null){
						throw new JPFException("no executeTimerTask method in edu.nju.Alex.greendroid.Main class");
					}
					
					//create direct call stub
					int maxLocals = exeTimerTask.getMaxLocals();
					int maxStack=exeTimerTask.getNumberOfCallerStackSlots();
					MethodInfo stub = new MethodInfo(exeTimerTask, maxLocals,maxStack);
					ClassInfo ci=exeTimerTask.getClassInfo();
					ArrayList<Instruction> a=new ArrayList<Instruction>();
					InstructionFactory instructionFactory=new InstructionFactory();
					a.add(instructionFactory.invokevirtual(ci.getName(), exeTimerTask.getName(), exeTimerTask.getSignature()));
					a.add(instructionFactory.directcallreturn());
					stub.setCode(a.toArray(new Instruction[a.size()]));
					DirectCallStackFrame frame = new DirectCallStackFrame(stub,exeTimerTask) {
						
						@Override
						public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setLongArgumentLocal(int idx, long value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setArgumentLocal(int idx, int value, Object attr) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public int setReferenceArgument(int argOffset, int ref, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setLongArgument(int argOffset, long value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public int setArgument(int argOffset, int value, Object attr) {
							// TODO Auto-generated method stub
							return 0;
						}
					};
					
					frame.pushRef(task.getObjectRef());
					currentThread.pushFrame(frame);
					currentThread.executeInstruction();
					//return;
				}else{
					throw new JPFException("schedule should have at least 1 argument (TimerTask)");
				}
				return;
			}
		}
		
		//handle android app.Dialog.show()
		if(methodName.equals(TargetMethodList.SHOW_DIALOG)){	
			if(ei !=null && ei.instanceOf("Landroid/app/Dialog;")){
				MethodInfo showDialog = ciMain.getMethod("showDialog()V", false);
				if(showDialog==null){
					throw new JPFException("no showDialog method in edu.nju.Alex.greendroid.Main class");
				}
				//create direct call stub
				int maxLocals = showDialog.getMaxLocals();
				int maxStack=showDialog.getNumberOfCallerStackSlots();
				MethodInfo stub = new MethodInfo(showDialog, maxLocals,maxStack);
				ClassInfo ci=showDialog.getClassInfo();
				ArrayList<Instruction> a=new ArrayList<Instruction>();
				InstructionFactory instructionFactory=new InstructionFactory();
				a.add(instructionFactory.invokevirtual(ci.getName(), showDialog.getName(), showDialog.getSignature()));
				a.add(instructionFactory.directcallreturn());
				stub.setCode(a.toArray(new Instruction[a.size()]));
				DirectCallStackFrame frame = new DirectCallStackFrame(stub,showDialog) {
					
					@Override
					public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void setLongArgumentLocal(int idx, long value, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void setArgumentLocal(int idx, int value, Object attr) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public int setReferenceArgument(int argOffset, int ref, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
					
					@Override
					public int setLongArgument(int argOffset, long value, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
					
					@Override
					public int setArgument(int argOffset, int value, Object attr) {
						// TODO Auto-generated method stub
						return 0;
					}
				};
				
				currentThread.pushFrame(frame);
				currentThread.executeInstruction();
				return;
			}
		}
		//for now, we don't deal with sofia or concurrency yet
	}
	
		
	if(executedInstruction instanceof INVOKESTATIC){
		StaticElementInfo sei = ((INVOKESTATIC) executedInstruction).getStaticElementInfo();
		String methodName = ((INVOKESTATIC) executedInstruction).getInvokedClassName();
		
		//make sure static broadcast receivers area recorded in the main class
		if(methodName.equals("mian([Ljava/lang/String;)V") && sei.getClassInfo().getName().equals("edu.nju.Alex.greendroid.Main")){
			if(!staticRcvsAdded){
				addStaticReceiversToMain(ciMain,vm,currentThread);
				staticRcvsAdded =true;
			}
			
			if(!actionToActivityMapAdded){
				addActionToActivityMapToMain(ciMain,vm,currentThread);
				actionToActivityMapAdded=true;
			}
		}
	}
	}
	
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {
		/**
		 * This following code is very important for avoiding null pointer exception!!!
		 */
		//System.out.println(instructionToExecute.toString()+" "+instructionToExecute.getFileLocation());
		//when invoke setOnClickListener, at one point Handler will be initialized and would require mLooper.Queue, which somehow not existed. so we manually create one. don't know if it would work yet.
		if (instructionToExecute instanceof GETFIELD) {
			String ins=instructionToExecute.toString();
		    if("getfield android.os.Looper.mQueue".equals(ins)){
		    	ElementInfo elementInfo=((GETFIELD) instructionToExecute).peekElementInfo(currentThread);
		    	if (elementInfo==null) {
					ClassInfo ci=ClassLoaderInfo.getCurrentResolvedClassInfo("android.os.MessageQueue");
					try {
						int ref = currentThread.getHeap().newObject(ci, currentThread).getIndex();
						int thisOffset = ((GETFIELD) instructionToExecute).getFieldSize()-1;
						currentThread.getTopFrame().setOperand(thisOffset, ref, true);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
		    }
		}
		if(instructionToExecute instanceof InstanceInvocation){
			//System.out.println(((InstanceInvocation) instructionToExecute).getInvokedMethodName().chars());
			ElementInfo ei = ((InstanceInvocation) instructionToExecute).getThisElementInfo(currentThread);
			String className = ((InstanceInvocation) instructionToExecute).getInvokedMethodClassName();
			String MethodName = ((InstanceInvocation) instructionToExecute).getInvokedMethodName();
			//System.out.println("                                                                before the execution? "+className+"."+((InstanceInvocation) instructionToExecute).getInvokedMethodName());
			ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(className);
			
			
			//redirect some methods to child classes
			if(RedirectTargets.CCOS.equals(MethodName)){
				//System.out.println(className+" "+MethodName);
				((InstanceInvocation) instructionToExecute).setInvokedMethod("android.content.ContextWrapper", "checkCallingOrSelfPermission", "(Ljava/lang/String;)I");
				ci = ClassLoaderInfo.getCurrentResolvedClassInfo("android.content.ContextWrapper");
				if(ei!=null && !ei.instanceOf("Landroid.content.ContextWrapper;")){
					ei=null;
				}
			}
			if (RedirectTargets.GML.equals(MethodName)) {
				//System.out.println(className+" "+MethodName);
				((InstanceInvocation) instructionToExecute).setInvokedMethod("android.content.ContextWrapper", "getMainLooper", "()Landroid/os/Looper;");
				ci = ClassLoaderInfo.getCurrentResolvedClassInfo("android.content.ContextWrapper");
				//ClassInfo classInfo=ClassLoaderInfo.getCurrentResolvedClassInfo("android.os.Looper");
				//currentThread.getHeap().newObject(classInfo, currentThread);
				if(ei!=null && !ei.instanceOf("Landroid.content.ContextWrapper;")){
					//System.out.println("heher!");
					ei=null;
				}
			}
			if (RedirectTargets.GBPN.equals(MethodName)) {
			//	System.out.println(className+" "+MethodName);
				((InstanceInvocation) instructionToExecute).setInvokedMethod("android.content.ContextWrapper", "getBasePackageName", "()Ljava/lang/String;");
				ci = ClassLoaderInfo.getCurrentResolvedClassInfo("android.content.ContextWrapper");
				if(ei!=null && !ei.instanceOf("Landroid.content.ContextWrapper;")){
					ei=null;
				}
			}
			
			
			//sometimes some variables are declared using interface
			//e.g., List<Type> list = new ArrayList<Type>();
			if (!ci.isInterface() && !ci.isEnum()) {
				if(Main.DEBUG){
					System.out.println("creating object @@@@ " + className);
				}
				
				if(ei==null){
					try {
						int ref = currentThread.getHeap().newObject(ci, currentThread).getIndex();
						//directly manipulate the operand stack
						int thisOffset = ((InstanceInvocation) instructionToExecute).getArgSize()-1;
						//System.out.println(currentThread.getTopFrameMethodInfo().getName()+" "+ref+" "+thisOffset);
						currentThread.getTopFrame().setOperand(thisOffset, ref, true);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		}
	}
	

	/**
	 * This method will make JPF run bindGUIComponentWithAcitivity 
	 * @param ciMain: the ClassInfo of our analyzer's Main class
	 * @param ei: the ElementInfo object of the GUI element (for example, when btn.setOnClickListener is called, this registerGUIEventListener will be called and ei is the ElementInfo object for btn )
	 */
	public void registerGUIEventListener(ClassInfo ciMain, ElementInfo ei, VM vm, int guiEventType, ThreadInfo currentThread){
		MethodInfo bind = ciMain.getMethod("bindGUIComponentWithActivity(Lgov/nasa/jpf/vm/ElementInfo;Ljava/lang/String;ILjava/lang/String;I)V", false);
		
		//check whether the binding method exists
		if(bind==null){
			throw new JPFException("no bindGUIComponentWithActivity method in edu.nju.Alex.greendroid.Main class");
		}
		
		//get the layoutId of the obj
		int objref=ei.getObjectRef();
		int layoutId=-1;
		for (MyMap m:viewInfoList) {
			if(m.objRef==objref){
				layoutId=m.layoutId;
				break;
			}
		}
		//create the call stub
		int maxLocals = bind.getMaxLocals();
		int maxStack=bind.getNumberOfCallerStackSlots();
		MethodInfo stub = new MethodInfo(bind, maxLocals,maxStack);
		ClassInfo ci=bind.getClassInfo();
		ArrayList<Instruction> a=new ArrayList<Instruction>();
		InstructionFactory instructionFactory=new InstructionFactory();
		a.add(instructionFactory.invokevirtual(ci.getName(), bind.getName(), bind.getSignature()));
		a.add(instructionFactory.directcallreturn());
		stub.setCode(a.toArray(new Instruction[a.size()]));
		DirectCallStackFrame frame = new DirectCallStackFrame(stub,null) {
			
			@Override
			public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setLongArgumentLocal(int idx, long value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setArgumentLocal(int idx, int value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public int setReferenceArgument(int argOffset, int ref, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setLongArgument(int argOffset, long value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setArgument(int argOffset, int value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		//System.out.println(ei.getObjectRef());
		frame.pushRef(ei.getObjectRef());
		//System.out.println(ei.getObjectRef());
		String s =ei.getType();
		s=s.substring(1,s.length()-1);
		//intentionally changes all / with . . is this okay? also the previous ones
		String sTrans = s.replace('/', '.');
	//	System.out.println("sTrans "+sTrans);
		ElementInfo type = vm.getHeap().newString(sTrans, currentThread);
		frame.pushRef(type.getObjectRef());
		frame.push(guiEventType);
		String tString=getGUIElementText(ei.getObjectRef());
		//System.out.println(tString);
		ElementInfo text=vm.getHeap().newString(tString, currentThread);
		frame.push(text.getObjectRef());
		frame.push(layoutId);
		
		currentThread.pushFrame(frame);
		currentThread.executeInstruction();
		
	}
	
	/**
	 * Get the GUI element type of the view (whose id is id) 
	 */
	public static String getGUIElementType(int activityRef, int id){
		//first get layout id
		//System.out.println(id);
		int layoutId = -1;
		if(activityRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = activityRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId == -1 && fragmentRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = fragmentRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId>0 && id >0){
			for(MyMap m : viewInfoList){
				if(m.layoutId == layoutId && m.viewId == id){
					//System.out.println("viewType "+m.viewType);
					return m.viewType;
				}
			}
		}
		return null;
	}
	/**
	 * Get the GUI element text of the view (whose id is id)
	 */
	public static String getUIElementText(int activityRef, int id){
		//first get layout id
		int layoutId = -1;
		if(activityRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = activityRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId == -1 && fragmentRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = fragmentRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId > 0 && id > 0){
			for(MyMap m : viewInfoList){
				if(m.layoutId == layoutId && m.viewId == id){
					if(m.viewText!=null){
						return m.viewText;
					}else{
						return m.viewIdS;
					}
				}
			}
		}
		return null;
	}
	/**
	 * set the objRef to the GUI element
	 */
	public static void setObjRef(int objRef, int activityRef, int id) {
		//System.out.println("MethodInvocationListener: "+objRef);
		int layoutId = -1;
		if(activityRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = activityRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId == -1 && fragmentRefToLayoutIdMap.containsKey(activityRef)){
			layoutId = fragmentRefToLayoutIdMap.get(activityRef);
		}
		if(layoutId > 0 && id > 0){
			for(MyMap m : viewInfoList){
				if(m.layoutId == layoutId && m.viewId == id){
					m.objRef=objRef;
				}
			}
		}
	}
	/**
	 * get the id string for certain objRef
	 * @param objRef
	 * @return
	 */
	public static String getGUIElementId(int objRef) {
		for(MyMap m :viewInfoList){
			if (m.objRef==objRef) {
				return m.viewIdS;
			}
		}
		return null;
	}
	/**
	 * get the text for certain objRef
	 * @param objRef
	 * @return
	 */
	public static String getGUIElementText(int objRef) {
		for(MyMap m :viewInfoList){
			if (m.objRef==objRef) {
				if(m.viewText!=null){
					return m.viewText;
				}else{
					return m.viewIdS;
				}
			}
		}
		System.out.println("[Severe] the Object "+objRef+" has no record int viewInfoList");
		return null;
	}
	public static int getLastFragment() {
		return latestFragmentObj;
	}
	public static void addStaticReceiversToMain(ClassInfo ciMain, VM vm, ThreadInfo currentThread){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < receivers.size(); i++) {
			StaticReceiverState srs = receivers.get(i);
			sb.append("clsName"+srs.rcvName+"\n");
			if(srs.permission !=null && !srs.permission.equals("null")){
				sb.append("permission:"+srs.permission+"\n");
			}
			for(String action: srs.actions){
				sb.append("action:"+action+"\n");
			}
		}
		
		ElementInfo strArray = vm.getHeap().newString(sb.toString(), currentThread);
		MethodInfo addStaticReceiver = ciMain.getMethod("addStaticReceiverToMain(Ljava/lang/String;)V", false);
		if(addStaticReceiver == null){
			throw new JPFException("no addStaticReceiverToMain method in edu.nju.Alex.greendroid.Main class");
		}
		
		//create direct call stub
		int maxLocals = addStaticReceiver.getMaxLocals();
		int maxStack=addStaticReceiver.getNumberOfCallerStackSlots();
		MethodInfo stub = new MethodInfo(addStaticReceiver, maxLocals,maxStack);
		ClassInfo ci=addStaticReceiver.getClassInfo();
		ArrayList<Instruction> a=new ArrayList<Instruction>();
		InstructionFactory instructionFactory=new InstructionFactory();
		a.add(instructionFactory.invokevirtual(ci.getName(), addStaticReceiver.getName(), addStaticReceiver.getSignature()));
		a.add(instructionFactory.directcallreturn());
		stub.setCode(a.toArray(new Instruction[a.size()]));
		DirectCallStackFrame frameTemp = new DirectCallStackFrame(stub,null) {
			
			@Override
			public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setLongArgumentLocal(int idx, long value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setArgumentLocal(int idx, int value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public int setReferenceArgument(int argOffset, int ref, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setLongArgument(int argOffset, long value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setArgument(int argOffset, int value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		
		frameTemp.pushRef(strArray.getObjectRef());
		currentThread.pushFrame(frameTemp);
		currentThread.executeInstruction(); 
	}
	
	
	public static void addActionToActivityMapToMain(ClassInfo ciMain, VM vm, ThreadInfo currentThread){
		StringBuffer sb = new StringBuffer();
		for(String mapEntry: actionToActivityMap){
			sb.append(mapEntry);
			sb.append("\n");
		}
		
		ElementInfo str = vm.getHeap().newString(sb.toString(), currentThread);
		MethodInfo addActionToActivityMap = ciMain.getMethod("addActionToActivityMapToMain(Ljava/lang/String;)V", false);
		if(addActionToActivityMap == null){
			throw new JPFException("no addActionToActivityMapToMain method in edu.nju.Alex.greendroid.Main class");
		}
		
		//create direct call stub
		int maxLocals = addActionToActivityMap.getMaxLocals();
		int maxStack=addActionToActivityMap.getNumberOfCallerStackSlots();
		MethodInfo stub = new MethodInfo(addActionToActivityMap, maxLocals,maxStack);
		ClassInfo ci=addActionToActivityMap.getClassInfo();
		ArrayList<Instruction> a=new ArrayList<Instruction>();
		InstructionFactory instructionFactory=new InstructionFactory();
		a.add(instructionFactory.invokevirtual(ci.getName(), addActionToActivityMap.getName(), addActionToActivityMap.getSignature()));
		a.add(instructionFactory.directcallreturn());
		stub.setCode(a.toArray(new Instruction[a.size()]));
		DirectCallStackFrame frameTemp = new DirectCallStackFrame(stub,addActionToActivityMap) {
			
			@Override
			public void setReferenceArgumentLocal(int idx, int ref, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setLongArgumentLocal(int idx, long value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setArgumentLocal(int idx, int value, Object attr) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public int setReferenceArgument(int argOffset, int ref, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setLongArgument(int argOffset, long value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int setArgument(int argOffset, int value, Object attr) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
		
		frameTemp.pushRef(str.getObjectRef());
		currentThread.pushFrame(frameTemp);
		currentThread.executeInstruction(); 
	}
}

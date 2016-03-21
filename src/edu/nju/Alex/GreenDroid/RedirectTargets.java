package edu.nju.Alex.GreenDroid;


public interface RedirectTargets {
	//This class marks those methods that should be handled by child classes but jpf cannot regconize(i.e. android.content.Context.checkCallingOrSelfPermission)
	public static final String CCOS="checkCallingOrSelfPermission(Ljava/lang/String;)I";
	public static final String GML="getMainLooper()Landroid/os/Looper;";
	public static final String GBPN="getBasePackageName()Ljava/lang/String;";
}

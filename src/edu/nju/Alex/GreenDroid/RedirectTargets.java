package edu.nju.Alex.GreenDroid;


public interface RedirectTargets {
	//This class marks those methods that should be handled by child classes but jpf cannot regconize(i.e. android.content.Context.checkCallingOrSelfPermission)
	public static final String CCOS="checkCallingOrSelfPermission(Ljava/lang/String;)I";
	public static final String GML="getMainLooper()Landroid/os/Looper;";
	public static final String GBPN="getBasePackageName()Ljava/lang/String;";
	public static final String GPN="getPackageName()Ljava/lang/String;";
	public static final String GAI="getApplicationInfo()Landroid/content/pm/ApplicationInfo;";
	public static final String SPGS="getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
	public static final String SPGB="getBoolean(Ljava/lang/String;Z)Z";
	public static final String SPC="contains(Ljava/lang/String;)Z";
	public static final String WSF="setFeatureInt(II)V";
	public static final String GCIOT="getColumnIndexOrThrow(Ljava/lang/String;)I";
	public static final String RCO="registerContentObserver(Landroid/database/ContentObserver;)V";
	public static final String RDSO="registerDataSetObserver(Landroid/database/DataSetObserver;)V";
	public static final String CIC="isClosed()Z";
	public static final String CC="close()V";
}

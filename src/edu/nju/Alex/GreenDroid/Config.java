package edu.nju.Alex.GreenDroid;

public class Config {
	//the maximum execution of app
	public static final int SEQ_LEN = 3;
	//we can also scan the manifest xml file to get the entry activity
		public static String ENTRY_ACTIVITY;//="com.example.alexwang.testforgreendroidverisiontwo.MainActivity";
		public static String R_ID_Class;//="com.example.alexwang.testforgreendroidverisiontwo.R$id";
		public static String R_LAYOUT_Class;//="com.example.alexwang.testforgreendroidverisiontwo.R$layout";
		public static String PKGNAME;// = null;
		//there could be multiple layout folders; we use the character @ to separate the paths
		public static String LAYOUT_PATH;//="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/res/layout";
		public static String STR_PATH;//="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/res/values/strings.xml"; 
		public static String MANIFEST_PATH;//="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/AndroidManifest.xml";
		
		public static int SUB_ID=2;
		public static final int TESTVERSION = 0;
		public static final int ANDTWEET_BAD = 1;
		public static final int GPSLOGGER=2;
		
		static{
			switch (SUB_ID) {
			case TESTVERSION:
				ENTRY_ACTIVITY="com.example.alexwang.testforgreendroidverisiontwo.MainActivity";
				R_ID_Class="com.example.alexwang.testforgreendroidverisiontwo.R$id";
				R_LAYOUT_Class="com.example.alexwang.testforgreendroidverisiontwo.R$layout";
				PKGNAME= null;
				LAYOUT_PATH="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/res/layout";
				STR_PATH="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/res/values/strings.xml"; 
				MANIFEST_PATH="F:/androidstuff/TestForGreendroidVerisionTwo/app/src/main/AndroidManifest.xml";
				break;
			case ANDTWEET_BAD:
				ENTRY_ACTIVITY = "com.xorcode.andtweet.TweetListActivity";
				R_ID_Class = "com.xorcode.andtweet.R$id";
				R_LAYOUT_Class = "com.xorcode.andtweet.R$layout";
				PKGNAME = "com.xorcode";
				LAYOUT_PATH = "F:/greeendroid/workspace2/AndTweet-bad/res/layout";
				STR_PATH = "F:/greeendroid/workspace2/AndTweet-bad/res/values/strings.xml";
				MANIFEST_PATH = "F:/greeendroid/workspace2/AndTweet-bad/AndroidManifest.xml";
				
				break;
			case GPSLOGGER:
				ENTRY_ACTIVITY = "com.prom2m.android.gpslogger.GPSLoggerActivity";
				R_ID_Class = "com.prom2m.android.gpslogger.R$id";
				R_LAYOUT_Class = "com.prom2m.android.gpslogger.R$layout";
				PKGNAME = "com.prom2m.android.gpslogger";
				LAYOUT_PATH = "F:/greeendroid/workspace2/GPSLogger-r15/res/layout";
				STR_PATH = "F:/greeendroid/workspace2/GPSLogger-r15/res/values/strings.xml";
				MANIFEST_PATH = "F:/greeendroid/workspace2/GPSLogger-r15/AndroidManifest.xml";
				break;
			default:
				break;
			}
		}
}

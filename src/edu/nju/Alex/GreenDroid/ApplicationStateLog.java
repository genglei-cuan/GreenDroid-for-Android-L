package edu.nju.Alex.GreenDroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ApplicationStateLog {
	public static ArrayList<String> exeContext = new ArrayList<String>();
/*	public static File file;
	public static FileOutputStream out;
	//write log into file
	static{
		String path="F:\\greeendroid\\output\\GPSLogger.txt";
		file=new File(path);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			out=new FileOutputStream(file,true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	*/
	public static void w(String s) {
		exeContext.add(s);
	}
	
	//public static void ToFile(String text) {
	//	try {
	//		out.write(text.getBytes());
	//	} catch (IOException e) {
	//		// TODO Auto-generated catch block
	//		e.printStackTrace();
	//	}
	//}
	public static void print() {
		System.out.println("@@tracelen="+exeContext.size());
		//ToFile("@@tracelen="+Integer.toString(exeContext.size())+"\n\r");
		
		for(int i=0;i<exeContext.size();i++){
			if(exeContext.get(i) == null){
				System.out.println("nothing here????????");
			//	ToFile("nothing here????????\n\r");
			}
			System.out.print(exeContext.get(i));
			//ToFile(exeContext.get(i));
			if(i!=exeContext.size()-1){
				System.out.println(" -->");
				//ToFile(" -->\n\r");
			}
		}
		System.out.println();
		//ToFile("\n\r");
		System.out.println("-----------------------------");
		//ToFile("-----------------------------\n\r");
	}
}

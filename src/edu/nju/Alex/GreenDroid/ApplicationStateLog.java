package edu.nju.Alex.GreenDroid;

import java.util.ArrayList;

public class ApplicationStateLog {
	public static ArrayList<String> exeContext = new ArrayList<String>();
	
	public static void w(String s) {
		exeContext.add(s);
	}
	
	public static void print() {
		System.out.println("@@tracelen="+exeContext.size());
		
		for(int i=0;i<exeContext.size();i++){
			if(exeContext.get(i) == null){
				System.out.println("nothing here????????");
			}
			System.out.print(exeContext.get(i));
			if(i!=exeContext.size()-1){
				System.out.println(" -->");
			}
		}
		System.out.println();
		System.out.println("-----------------------------");
	}
}

/***
 * this class is used to test the choice generator of JPF
 */

package test;

import java.util.LinkedList;
import java.util.Random;

import edu.nju.Alex.GreenDroid.Main;
import junit.framework.Test;

public class TestCG {
	public static LinkedList<Integer> exeContext = new LinkedList<Integer>();
	public static Random rand = new Random();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=0;
		while(i<5){
			i++;
			int choice1 = rand.nextInt(3);
			log(-1);
			System.out.println(choice1);
			for(int j = 0; j<4; j++){
				int choice2 = rand.nextInt(4);
				System.out.println(choice2);
			}
			System.out.println("finished");
		}
		//TestCG.printLog();
	}
	
	public static void log(int i){
		exeContext.add(i);
	}
	
	public static void printLog(){
		for(Integer i : exeContext){
			System.out.print(i+" ");
		}
		System.out.println();
	}
}

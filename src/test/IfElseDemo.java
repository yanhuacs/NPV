package test;

import java.util.Random;

public class IfElseDemo {
	public static void main(String[] args) {
		int testscore = 76;
		IfElseDemo obj = null;
		Random rand = new Random();
		if (rand.nextInt(10) > 5)
			obj = new IfElseDemo();
		doStuff(testscore, obj);
	}
	
	public static int doStuff(int score, IfElseDemo obj) {

        int grade = 3;

        if (score < 60)
        	grade = 5;
        else
        	grade = 6;

        
        if (score > 90)
        	grade--;
        else
        	grade++;
        
        System.out.println("Grade = " + grade);
        return grade;
    }
}
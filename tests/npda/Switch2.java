package npda;

import java.util.Random;

public class Switch2 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Switch2 o = new Switch2();
		//Random rand = new Random();
		//int r1 = rand.nextInt(100);
		switch ((new Double(Math.random() * 10)).intValue()) {
		case 1:
			o.x++; //safe
			break;
			
		case 2:
			o.x++; //safe
			break;
			
		case 3:
			o.x++; //safe
			break;
			
		case 4:
			o.x++; //safe
			//int r2 = rand.nextInt(100);
			switch ((new Double(Math.random() * 10)).intValue()) {
			case 1:
				o.x++; //safe
				break;
				
			case 2:
				o.x++; //safe
				break;
				
			case 3:
				o.x++; //safe
				break;
				
			case 4:
				o.x++; //safe
				o = null; //causing subsequent null-pointer dereference
				break;
				
			default:
				break;
			}
			break;
			
		case 5:
			o.x++; //safe
			break;
			
		default:
			break;
		}
		
		if (Math.random() < 0.5) {
			System.console().printf("%d", o.x); //bug
		}
	}
	
	int x;
}

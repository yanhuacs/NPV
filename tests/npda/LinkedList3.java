package npda;

public class LinkedList3 {
	public static void main(String[] args) {
		doStuff();
	}
	
	public static void doStuff() {
		Node_3 cur = new Node_3();
		for (int k = 0; k < 5; ++k) { 
			System.console().printf("%d", cur.val);
			cur.nxt = new Node_3();
			cur = cur.nxt; //safe
			cur.nxt = null; //safe
			System.console().printf("%d", cur.nxt.val); //bug
		}
	}
}

class Node_3 //Linked list data structure.
{
	int val;
	Node_3 nxt;
}
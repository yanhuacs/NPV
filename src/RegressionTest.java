import static org.junit.Assert.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class RegressionTest {

	@Test
	public void test() {
		//fail("Not yet implemented");
		
		ArrayList<Integer> expectedBugLineNums = new ArrayList<>(Arrays.asList(22));
		
		PrintStream original = System.out;
	    System.setOut(new PrintStream(new OutputStream() { public void write(int b) { } }));
		
	    String class_name = "npda.Intra";
	    RunTest.testInterNullPtrAnalysis(class_name);
		
		System.setOut(original);
		
		boolean isSuccessful = true;
		for (Integer i : expectedBugLineNums) { 
			if (RunTest.getBugLineNums().contains(i) == false) {
				isSuccessful = false;
				break;
			}
		}
		for (Integer i : RunTest.getBugLineNums()) { 
			if (expectedBugLineNums.contains(i) == false) {
				isSuccessful = false;
				break;
			}
		}

		if (isSuccessful) {
			System.out.println("Successful : " + class_name);
			System.out.println();
		}
		else {
			System.out.println("Failed : " + class_name);
			System.out.println("-----------------------------");
			System.out.println("  Expected : " + expectedBugLineNums);
			System.out.println("  Actual   : " + RunTest.getBugLineNums());
			System.out.println("-----------------------------");
			System.out.println();
		}
		
		
        assertEquals(isSuccessful, true);
	}

}

package npda;

//Mike Scott
//examples of array manipulations

public class Array3
{	public static void main(String[] args)
	{	int[] list = {1,2};
		System.console().printf(isAscending( list )? "successful":"failed");
		System.console().printf("%d", list[1]); //safe
	}

	public static boolean isAscending( int[] list )
	{	boolean ascending = true;
		int index = 1;
		while( ascending && index < list.length )
		{	

			ascending = !ascending;
			index++;
		}

		return ascending;
	}
}
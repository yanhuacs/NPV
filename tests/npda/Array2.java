package npda;

//Mike Scott
//examples of array manipulations

public class Array2
{	public static void main(String[] args)
	{	int[] list = {1, 2, 3, 4, 1, 2, 3};
		findMin(list);				
		list = null;
		findMin(list);
	}


	// pre: list != null, list.length > 0
	// post: return index of minimum element of array
	public static int findMin(int[] list)
	{	

		int indexOfMin = 0;
		for(int i = 1; i < list.length; i++)
		{	if(list[i] < list[indexOfMin]) //bugs (assertion might be disabled)
			{	indexOfMin = i;
			}
		}

		return indexOfMin;
	}

}
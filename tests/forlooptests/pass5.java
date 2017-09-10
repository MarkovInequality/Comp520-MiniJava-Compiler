//Pass5
//nested for loops

class MainClass{

	public static void main( String[] args ){
		for (int i = 0; i < 10; i = i + 1)
			for (int j = 0; j < 10; j = j + 1) 
				if (j == 9 && i == 9) System.out.println((i*2-j+1)/2);
	}

}
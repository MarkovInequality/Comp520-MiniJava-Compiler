//Pass3
//declaration of loop var outside of for loop and inside of for loop

class MainClass{

	public static void main( String[] args ) {
		int i = 0;
		int j = 0;
		for (i = 0; i != 7; i = i + 1) {
			j = i;
		}
		for (int k = 0; k < 3; k = k + 1) {
			j = j - 1;
		}
		System.out.println(j);
	}
}
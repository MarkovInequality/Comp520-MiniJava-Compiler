//Pass4
//Esoteric for loops with loop condition being the result of a method call and multiple updating conditions

class MainClass{

	public static void main( String[] args ){
		for (int i = 0; test(i); i = i + 2, i = i - 1) {
			if (i == 4) {
				System.out.println(i);
			}
		}
	}

	public static boolean test(int i) {
		return (i < 5);
	}
}
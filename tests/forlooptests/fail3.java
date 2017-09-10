//Fail3
//empty stmt after comma in var increment statements
class MainClass {

	public static void main( String[] args ){
		for (int i = 0; i < 2; i = i + 1,) {
			System.out.println(i);
		}
	}
}
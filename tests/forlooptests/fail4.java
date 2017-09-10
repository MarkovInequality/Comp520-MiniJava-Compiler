//Fail4
//variable declared in for loop can't be used outside of it
class MainClass {

	public static void main( String[] args ){
		for (int i = 0; i < 2; i = i + 1) {
			System.out.println(i);
		}
		i = 4;
		System.out.println(i);
	}
}
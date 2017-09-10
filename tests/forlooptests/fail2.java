//Fail2
//loop condition expression does not have type boolean
class MainClass {

	public static void main( String[] args ){
		for (int i = 0; i + 2; i = i + 1) {
			System.out.println(i);
		}
	}
}
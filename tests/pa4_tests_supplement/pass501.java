//Pass1
//tests for var init statement as call stmt
class MainClass {

	public static void main( String[] args ){
		SubClass test = new SubClass();
		test.runTest();
	}
}

class SubClass {
	
	int i;

	public void runTest() {
		for (init(0); i < 10; i = i + 1 ){
			int j = i;
		}
		System.out.println(i/10);
	}
	
	public void init(int val) {
		i = val;
	}
}
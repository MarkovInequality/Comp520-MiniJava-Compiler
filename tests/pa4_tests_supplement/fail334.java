/*** can't assign to method name
 * COMP 520
 * Identification
 */
class fail34 { 	
    public static void main(String[] args) {
        B b = new B();
        b.test();
    }
}

class B {
	
	public int f() {return 3;}
	
	public void test() {
		f = 5;
	}
}

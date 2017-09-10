/*** can't invoke static methods in PA3
 * COMP 520
 * Identification
 */
class fail50 { 	
    public static void main(String[] args) {
    	int x = foo(20);
    }
    
    public static int foo(int parm) {
        return 50;
    }
}

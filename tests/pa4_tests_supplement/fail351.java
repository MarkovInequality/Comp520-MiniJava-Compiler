/*** can't access static fields in PA3
 * COMP 520
 * Identification
 */
class fail51 { 	
    public static void main(String[] args) {
    	int z = x;
    }
    
    public static int x;
}
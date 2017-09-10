/*** No field out in class System (a new class overrides predefined System)
 * COMP 520
 * Identification
 */
class Fail09 {
    public static void main(String[] args) {
	System.out.println(5);
    }
}

class System {
    public int x;
}
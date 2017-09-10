/** can not assign to reference on LHS
 * COMP 520
 *   fail:  length attribute of array can not be assigned
 */
class MainClass {
   public static void main (String [] args) {
       int [] b = new int[10];
       b.length = -1;
   }
}


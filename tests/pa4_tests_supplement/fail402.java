/** field "length" is undeclared
 * COMP 520
 *   fail:  length attribute only applies to arrays
 */
class A {
   public static void main (String [] args) {
       A a = new A();
       int x = a.length;
   }
}


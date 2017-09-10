//Fail1
//for loops can't have varDeclStmt as its only stmt
class MainClass {

	public static void main( String[] args ){
		for (;;) int i = 0;
	}
}
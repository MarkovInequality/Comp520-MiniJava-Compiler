//Fail test for loop
//can only have assign, vardecl, or call stmt in initializing statement

class MainClass{

	public static void main( String[] args ){

		int i = 0;
		for( if( true ){i = 0;} i < 9; i = i + 2){
			System.out.println( i );
		}

	}

}
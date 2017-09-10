//Fail test for loop
//i not defined outside loop

class MainClass{

	public static void main( String[] args ){

		for( int i = 0; i < 9; i = i + 2){
			System.out.println( i );
		}

		if( i == 0 ){
			i = 9;
		}

	}

}
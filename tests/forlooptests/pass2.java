//Pass2
//test for handling of missing parameters in forloop

class MainClass{

	public static void main( String[] args ){
		int counter = 0;
		for(;counter < 10;){
			counter = counter + 1;
			if( counter == 2 ){
				System.out.println(counter);
			}
		}
	}

}
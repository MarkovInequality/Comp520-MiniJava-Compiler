//Test for null
//should compile but cannot call method of null item.
//mJAM does not see this as an issue so I have implemented it in the Interpreter (lines 567-568).

class MainClass{

	public static void main( String[] args ){

		MainClass b = null;
		b.print();

	}

	public void print(){
		System.out.println(4);
	}

}
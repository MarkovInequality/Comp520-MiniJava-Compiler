package miniJava;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ErrorReporter {
	
	public static void reportError(String message, SourcePosition posn) {
		System.out.println(message + " at source position " + posn.toString());
	}
	
	public static void reportError(String message) {
		System.out.println(message);
	}
	
	public static void reportUnresolvableError(String message, SourcePosition posn) {
		System.out.println(message + " at source position " + posn.toString());
		System.exit(4);
	}
	
	public static void reportUnresolvableError(String message) {
		System.out.println(message);
		System.exit(4);
	}
	
}
package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int lineNum;
	public int linePos;
	
	public SourcePosition(int lineNum, int linePos) {
		this.lineNum = lineNum;
		this.linePos = linePos;
	}
	
	public String toString() {
		return "[" + Integer.toString(lineNum) + ":" + Integer.toString(linePos) + "]";
	}
}

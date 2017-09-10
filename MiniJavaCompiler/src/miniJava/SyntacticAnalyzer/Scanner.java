package miniJava.SyntacticAnalyzer;

import java.io.*;
import miniJava.ErrorReporter;

public class Scanner {

	private PushbackInputStream inputStream;
	private boolean eot;
	
	private char bufferedChar;
	private char currentChar;
	
	private SourcePosition pos;
	private SourcePosition prevPos;
	
	private String currentWord;
	private TokenType currentWordType;
	
	public Scanner(PushbackInputStream inputStream) {
		this.inputStream = inputStream;
		eot = false;
		pos = new SourcePosition(1, 1);
		prevPos = pos;
		
		readChar();
	}
	
	public Token next() {
		currentWordType = null;
		currentWord = "";
		SourcePosition p = pos;
		boolean hasParsedToken = false;
		
		while (true) {
			boolean skippedComment = skipComment(); //skip to first char after comments
			boolean skippedWhiteSpace = skipWhiteSpace(); //skip to first char after whitespace
			
			if (!skippedComment && !skippedWhiteSpace) { //has encountered the first char of a token
				hasParsedToken = true;
				if (currentWordType == null) {
					p = pos;
					currentWordType = detectType();
					readChar();
				}
				else if (currentWordType == TokenType.ID ||
						 currentWordType == TokenType.NUM) {
					if (detectType() != TokenType.ERROR) {
						readChar();
					}
					else {
						break;
					}
				}
				else {
					break;
				}
			}
			else {
				if (hasParsedToken) {
					break;
				}
			}
		}
		
		return new Token(currentWordType, currentWord, p);
	}
	
	private TokenType detectType() {
		if (eot) {
			currentWord = "END";
			return TokenType.EOT;
		}
		
		if (currentWordType == TokenType.ID) {
			if ((currentChar >= 'A' && currentChar <= 'Z') ||
				(currentChar >= 'a' && currentChar <= 'z') ||
				(currentChar >= '0' && currentChar <= '9') ||
				(currentChar == '_')) {
				currentWord += Character.toString(currentChar);
				return TokenType.ID;
			}
			else {
				return TokenType.ERROR;
			}
		}
		else if (currentWordType == TokenType.NUM) {
			if (currentChar >= '0' && currentChar <= '9') {
				currentWord += Character.toString(currentChar);
				return TokenType.NUM;
			}
			else {
				return TokenType.ERROR;
			}
		}
		else {
			if ((currentChar >= 'A' && currentChar <= 'Z') ||
				(currentChar >= 'a' && currentChar <= 'z')) {
				currentWord += Character.toString(currentChar);
				return TokenType.ID;
			}
			
			if (currentChar >= '0' && currentChar <= '9') {
				currentWord += Character.toString(currentChar);
				return TokenType.NUM;
			}
			
			switch (currentChar) {
			case '>':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '=') {
					currentWord += Character.toString(currentChar);
					return TokenType.GTE;
				}
				else {
					unreadChar();
					return TokenType.GT;
				}
			case '<':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '=') {
					currentWord += Character.toString(currentChar);
					return TokenType.LTE;
				}
				else {
					unreadChar();
					return TokenType.LT;
				}
			case '=':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '=') {
					currentWord += Character.toString(currentChar);
					return TokenType.ISEQ;
				}
				else {
					unreadChar();
					return TokenType.EQ;
				}
			case '!':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '=') {
					currentWord += Character.toString(currentChar);
					return TokenType.NE;
				}
				else {
					unreadChar();
					return TokenType.NOT;
				}
			case '&':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '&') {
					currentWord += Character.toString(currentChar);
					return TokenType.AND;
				}
				break;
			case '|':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '|') {
					currentWord += Character.toString(currentChar);
					return TokenType.OR;
				}
				break;
			case '+':
				currentWord += Character.toString(currentChar);
				return TokenType.PLUS;
			case '-':
				currentWord += Character.toString(currentChar);
				readChar();
				if (currentChar == '-') {
					currentWord += Character.toString(currentChar);
					return TokenType.DECR;
				}
				else {
					unreadChar();
					return TokenType.MINUS;
				}
			case '*':
				currentWord += Character.toString(currentChar);
				return TokenType.MUL;
			case '/':
				currentWord += Character.toString(currentChar);
				return TokenType.DIV;
			case '(':
				currentWord += Character.toString(currentChar);
				return TokenType.LPAREN;
			case ')':
				currentWord += Character.toString(currentChar);
				return TokenType.RPAREN;
			case '{':
				currentWord += Character.toString(currentChar);
				return TokenType.LBRACE;
			case '}':
				currentWord += Character.toString(currentChar);
				return TokenType.RBRACE;
			case '[':
				currentWord += Character.toString(currentChar);
				return TokenType.LBRACK;
			case ']':
				currentWord += Character.toString(currentChar);
				return TokenType.RBRACK;
			case '\'':
				currentWord += Character.toString(currentChar);
				return TokenType.QUOTE;
			case '"':
				currentWord += Character.toString(currentChar);
				return TokenType.DQUOTE;
			case '.':
				currentWord += Character.toString(currentChar);
				return TokenType.DOT;
			case ',':
				currentWord += Character.toString(currentChar);
				return TokenType.COMMA;
			case ';':
				currentWord += Character.toString(currentChar);
				return TokenType.SEMICOL;
			}
		}
		
		ErrorReporter.reportError("Scan Error: Unrecognized character '" + currentChar + "' in input", pos);
		currentWord += Character.toString(currentChar);
		//readChar();
		return TokenType.ERROR;
	}

	private boolean skipComment() {
		if (currentChar == '/') {
			readChar();
			if (eot) {
				unreadChar();
				eot = false;
				return false;
			}
			if (currentChar == '/') {
				readChar();
				while (!eot && currentChar != '\n' && currentChar != '\r') {
					readChar();
				}
			}
			else if (currentChar == '*') {
				readChar();
				readChar();
				while (bufferedChar != '*' || currentChar != '/') {
					if (eot) {
						ErrorReporter.reportUnresolvableError("Scan Error: unterminated comment");
					}
					else {
						readChar();
					}
				}
			}
			else {
				unreadChar();
				return false;
			}
			readChar();
			return true;
		}
		return false;
	}
	
	private boolean skipWhiteSpace() {
		boolean skippedSpace = false;
		wloop: 	while (!eot) {
					switch (currentChar) {
						case ' ':
						case '\t':
						case '\n':
						case '\r':
							readChar();
							skippedSpace = true;
							break;
						default:
							break wloop;
					}
				}
		return skippedSpace;
	}
	
	private void unreadChar() {
		try {
			movebackSourcePos();
			inputStream.unread(currentChar);
			currentChar = bufferedChar;
		} catch (IOException e) {
			ErrorReporter.reportUnresolvableError("Scan Error: insufficient space in the pushback buffer to peak ahead");
		}
	}
	
	private void readChar() {
		try {
			advanceSourcePos();
			int c = inputStream.read();
			bufferedChar = currentChar;
			if (c == -1) {
				eot = true;
			}
			else {
				currentChar = (char) c;
			}
		}
		catch (IOException e) {
			ErrorReporter.reportUnresolvableError("Scan Error: I/O Exception!");
		}
	}
	
	private void movebackSourcePos() {
		pos = prevPos;
		prevPos = null;
	}
	
	private void advanceSourcePos() {
		if (!eot) {
			prevPos = pos;
			if (currentChar == '\n') {
				if (bufferedChar != '\r') {
					pos = new SourcePosition(pos.lineNum + 1, 1);
				}
			}
			else if (currentChar == '\r') {
				pos = new SourcePosition(pos.lineNum + 1, 1);
			}
			else if (currentChar == '\t') {
				pos = new SourcePosition(pos.lineNum, pos.linePos + 4);
			}
			else {
				pos = new SourcePosition(pos.lineNum, pos.linePos + 1);
			}
		}
	}
}

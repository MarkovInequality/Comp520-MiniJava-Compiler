package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

import java.util.*;

public class Parser {

	private Scanner scanner;
	private List<Token> tokenBuffer;
	private int bufferPos;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
		tokenBuffer = new LinkedList<Token>();
		bufferPos = 0;
	}
	
	public Package parse() throws SyntaxError {
		ClassDeclList classes = null;
		tokenBuffer.add(scanner.next());
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		try {
			classes = parseProgram();
		}
		catch (SyntaxError e) {
			System.exit(4);
		}
		return new Package(classes, sp);  //implement sourcePosition later
	}
	
	private ClassDeclList parseProgram() throws SyntaxError {
		ClassDeclList classes = new ClassDeclList();
		while (tokenBuffer.get(bufferPos).type != TokenType.EOT) {
			classes.add(parseClassDeclaration());
		}
		accept(TokenType.EOT);
		return classes;
	}
	
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		String className;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		if (tokenBuffer.get(bufferPos).spelling.equals("class")) {
			acceptIt();
		}
		else {
			parseError("expecting 'class' but found '" + tokenBuffer.get(0).spelling + "'");
		}
		className = tokenBuffer.get(bufferPos).spelling;
		acceptIDNoKeyword();
		accept(TokenType.LBRACE);
		while (tokenBuffer.get(bufferPos).type != TokenType.RBRACE) {
			MemberDecl membr = parseMemberDeclaration();
			
			if (membr.type != null && membr.type.typeKind == TypeKind.VOID) {
				mdl.add(parseMethodDeclaration(membr));
			}
			else if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
				mdl.add(parseMethodDeclaration(membr));
			}
			else {
				fdl.add(parseFieldDeclaration(membr));
			}
		}
		accept(TokenType.RBRACE);
		return new ClassDecl(className, fdl, mdl, sp); //implement sourcePosition later
	}
	
	private MemberDecl parseMemberDeclaration() throws SyntaxError {
		boolean isPrivate = false;
		boolean isPublic = false;
		boolean isStatic = false;
		TypeDenoter type;
		String name;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		
		if (tokenBuffer.get(bufferPos).spelling.equals("private")) {
			isPrivate = true;
			acceptIt();
		}
		else if (tokenBuffer.get(bufferPos).spelling.equals("public")) {
			isPublic = true;
			acceptIt();
		}
		
		if (tokenBuffer.get(bufferPos).spelling.equals("static")) {
			isStatic = true;
			acceptIt();
		}
		
		if (tokenBuffer.get(bufferPos).spelling.equals("void")) {
			type = new BaseType(TypeKind.VOID, tokenBuffer.get(bufferPos).posn); //implement sourcePos later
			acceptIt();
		}
		else {
			type = parseType();
		}
		
		name = tokenBuffer.get(bufferPos).spelling;
		acceptIDNoKeyword();
		return new FieldDecl(isPrivate, isPublic, isStatic, type, name, sp); //implement sourcePos later
	}
	
	private FieldDecl parseFieldDeclaration(MemberDecl md) throws SyntaxError {
		accept(TokenType.SEMICOL);
		return new FieldDecl(md, md.posn); //implement sourcePos later
	}
	
	private MethodDecl parseMethodDeclaration(MemberDecl md) throws SyntaxError {
		ParameterDeclList pl;
		StatementList sl = new StatementList();
		
		accept(TokenType.LPAREN);
		pl = parseParameterList();
		accept(TokenType.LBRACE);
		while (tokenBuffer.get(bufferPos).type != TokenType.RBRACE) {
			sl.add(parseStatement());
		}
		accept(TokenType.RBRACE);
		return new MethodDecl(md, pl, sl, md.posn);
	}
	
	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList pl = new ParameterDeclList();
		if (tokenBuffer.get(bufferPos).type == TokenType.RPAREN) {
			accept(TokenType.RPAREN);
			return pl;
		}
		while (tokenBuffer.get(bufferPos).type != TokenType.RPAREN) {
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			pl.add(new ParameterDecl(parseType(), tokenBuffer.get(bufferPos).spelling, sp)); //implement sourcePos later
			acceptIDNoKeyword();
			if (tokenBuffer.get(bufferPos).type != TokenType.COMMA) {
				accept(TokenType.RPAREN);
				break;
			}
			else {
				acceptIt();
			}
		}
		return pl;
	}
	
	private ExprList parseArgumentList() throws SyntaxError {
		ExprList eList = new ExprList();
		if (tokenBuffer.get(bufferPos).type == TokenType.RPAREN) {
			accept(TokenType.RPAREN);
			return eList;
		}
		while (tokenBuffer.get(bufferPos).type != TokenType.RPAREN) {
			eList.add(parseExpression());
			if (tokenBuffer.get(bufferPos).type != TokenType.COMMA) {
				accept(TokenType.RPAREN);
				break;
			}
			else {
				acceptIt();
			}
		}
		return eList;
	}
	
	private Statement parseStatement() {
		
		if (tokenBuffer.get(bufferPos).type == TokenType.LBRACE) {
			return parseBlockStmt();
		}

		if (tokenBuffer.get(bufferPos).spelling.equals("return")) {
			return parseReturnStmt();
		}

		if (tokenBuffer.get(bufferPos).spelling.equals("if")) {
			return parseIfStmt();
		}

		if (tokenBuffer.get(bufferPos).spelling.equals("while")) {
			return parseWhileStmt();
		}
		if (tokenBuffer.get(bufferPos).spelling.equals("for")) {
			return parseForStmt();
		}
		
		if (tokenBuffer.get(bufferPos).type == TokenType.ID) {
			switch (tokenBuffer.get(bufferPos).spelling) {
				case "int":
				case "boolean": //varDecl
					return parseVarDeclStmt();
				case "this": //call or assignment
					return parseCallAssignStmt();
				default:
					keepIt();
					if (tokenBuffer.get(bufferPos).type == TokenType.DOT) { //call or assignment
						bufferPos = 0;
						return parseCallAssignStmt();
					}
					else if (tokenBuffer.get(bufferPos).type == TokenType.LBRACK) { //varDecl
						keepIt();
						if (tokenBuffer.get(bufferPos).type == TokenType.RBRACK) {
							bufferPos = 0;
							return parseVarDeclStmt();
						}
						else { //call or assignment
							bufferPos = 0;
							return parseCallAssignStmt();
						}
					}
					else {
						if (tokenBuffer.get(bufferPos).type == TokenType.ID) { //varDecl
							bufferPos = 0;
							return parseVarDeclStmt();
						}
						else { //call or assignment
							bufferPos = 0;
							return parseCallAssignStmt();
						}
					}
			}
		}
		
		parseError("expecting a statement but found '" + tokenBuffer.get(0).spelling + "'");
		return null;
	}
	
	private Statement parseBlockStmt() {
		StatementList sl = new StatementList();
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		accept(TokenType.LBRACE);
		while (tokenBuffer.get(bufferPos).type != TokenType.RBRACE) {
			Statement s = parseStatement();
			if (s != null) {
				sl.add(s);
			}
		}
		accept(TokenType.RBRACE);
		return new BlockStmt(sl, sp); //implement sourcePos later
	}
	
	private Statement parseReturnStmt() {
		Expression e = null;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		acceptIt();
		if (tokenBuffer.get(bufferPos).type == TokenType.SEMICOL) {
			acceptIt();
		}
		else {
			e = parseExpression();
			accept(TokenType.SEMICOL);
		}
		return new ReturnStmt(e, sp); //implement sourcePos later
	}
	
	private Statement parseIfStmt() {
		Expression cond;
		Statement thenStmt;
		Statement elseStmt;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		acceptIt();
		accept(TokenType.LPAREN);
		cond = parseExpression();
		accept(TokenType.RPAREN);
		thenStmt = parseStatement();
		if (tokenBuffer.get(bufferPos).spelling.equals("else")) {
			acceptIt();
			elseStmt = parseStatement();
			return new IfStmt(cond, thenStmt, elseStmt, sp); //implement sourcePos later
		}
		else {
			return new IfStmt(cond, thenStmt, sp); //implement sourcePos later
		}
	}
	
	private Statement parseWhileStmt() {
		Expression cond;
		Statement body;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		acceptIt();
		accept(TokenType.LPAREN);
		cond = parseExpression();
		accept(TokenType.RPAREN);
		body = parseStatement();
		return new WhileStmt(cond, body, sp); //implement sourcePos later
	}
	
	private Statement parseForStmt() {
		//#TODO
		Statement varInitStmt;
		Expression loopCondExpr;
		StatementList varIncrStmts = new StatementList();
		Statement body;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		acceptIt();
		accept(TokenType.LPAREN);
		if (tokenBuffer.get(bufferPos).type == TokenType.SEMICOL) {
			acceptIt();
			varInitStmt = null;
		}
		else if (tokenBuffer.get(bufferPos).spelling.equals("int") || tokenBuffer.get(bufferPos).spelling.equals("boolean")) {
			varInitStmt = parseVarDeclStmt();
		}
		else {
			keepIt();
			if (tokenBuffer.get(bufferPos).type == TokenType.DOT) { //assignment
				bufferPos = 0;
				varInitStmt = parseCallAssignStmt();
			}
			else if (tokenBuffer.get(bufferPos).type == TokenType.LBRACK) { //varDecl
				keepIt();
				if (tokenBuffer.get(bufferPos).type == TokenType.RBRACK) {
					bufferPos = 0;
					varInitStmt = parseVarDeclStmt();
				}
				else { //assignment
					bufferPos = 0;
					varInitStmt = parseCallAssignStmt();
				}
			}
			else {
				if (tokenBuffer.get(bufferPos).type == TokenType.ID) { //varDecl
					bufferPos = 0;
					varInitStmt = parseVarDeclStmt();
				}
				else { //assignment
					bufferPos = 0;
					varInitStmt = parseCallAssignStmt();
				}
			}
		}
		if (tokenBuffer.get(bufferPos).type == TokenType.SEMICOL) {
			acceptIt();
			loopCondExpr = null;
		}
		else {
			loopCondExpr = parseExpression();
			accept(TokenType.SEMICOL);
		}
		while (tokenBuffer.get(bufferPos).type != TokenType.RPAREN) {
			SourcePosition varIncrSp = tokenBuffer.get(bufferPos).posn;
			Reference varIncrRef = parseReference();
			if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
				ExprList eList;
				acceptIt();
				eList = parseArgumentList();
				varIncrStmts.add(new CallStmt(varIncrRef, eList, sp)); //implement sourcePos later
			}
			else if (tokenBuffer.get(bufferPos).type == TokenType.EQ) {
				acceptIt();
				Expression varIncrExpr = parseExpression();
				varIncrStmts.add(new AssignStmt(varIncrRef, varIncrExpr, varIncrSp));
			}
			if (tokenBuffer.get(bufferPos).type == TokenType.COMMA) {
				acceptIt();
			}
			else {
				accept(TokenType.RPAREN);
				break;
			}
		}
		body = parseStatement();
		return new ForStmt(varInitStmt, loopCondExpr, varIncrStmts, body, sp); //implement sourcePos later
	}
	
	private Statement parseCallAssignStmt() {
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Reference ref = parseReference();
		
		if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
			ExprList eList;
			acceptIt();
			eList = parseArgumentList();
			accept(TokenType.SEMICOL);
			return new CallStmt(ref, eList, sp); //implement sourcePos later
		}
		else if (tokenBuffer.get(bufferPos).type == TokenType.EQ) {
			Expression e;
			acceptIt();
			e = parseExpression();
			accept(TokenType.SEMICOL);
			return new AssignStmt(ref, e, sp); //implement sourcePos later
		}
		else {
			parseError("expecting '(' or '=' but found '" + tokenBuffer.get(0).spelling + "'");
			return null;
		}
	}
	
	private Statement parseVarDeclStmt() {
		VarDecl vd;
		Expression e;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		vd = new VarDecl(parseType(), tokenBuffer.get(bufferPos).spelling, sp); //implement sourcePos later
		acceptIDNoKeyword();
		accept(TokenType.EQ);
		e = parseExpression();
		accept(TokenType.SEMICOL);
		return new VarDeclStmt(vd, e, sp); //implement sourcePos later
	}

	private Expression parseExpression() throws SyntaxError { //parses OR
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression2();
		while (tokenBuffer.get(bufferPos).type == TokenType.OR) {
			op = tokenBuffer.get(bufferPos);
			acceptIt();
			ex = new BinaryExpr(new Operator(op), ex, parseExpression2(), sp); //implement sourcePos later
		}
		return ex;
	}
	
	private Expression parseExpression2() throws SyntaxError {
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression3();
		while (tokenBuffer.get(bufferPos).type == TokenType.AND) {
			op = tokenBuffer.get(bufferPos);
			acceptIt();
			ex = new BinaryExpr(new Operator(op), ex, parseExpression3(), sp); //implement sourcePos later
		}
		return ex;
	}
	
	private Expression parseExpression3() throws SyntaxError {
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression4();
		while (tokenBuffer.get(bufferPos).type == TokenType.ISEQ ||
				tokenBuffer.get(bufferPos).type == TokenType.NE) {
			op = tokenBuffer.get(bufferPos);
			acceptIt();
			ex = new BinaryExpr(new Operator(op), ex, parseExpression4(), sp); //implement sourcePos later
		}
		return ex;
	}
	
	private Expression parseExpression4() throws SyntaxError {
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression5();
		expression4Loop:
		while (true) {
			switch (tokenBuffer.get(bufferPos).type) {
				case LT:
				case LTE:
				case GT:
				case GTE:
					op = tokenBuffer.get(bufferPos);
					acceptIt();
					ex = new BinaryExpr(new Operator(op), ex, parseExpression5(), sp); //implement sourcePos later
					break;
				default:
					break expression4Loop;
			}
		}
		return ex;
	}
	
	private Expression parseExpression5() throws SyntaxError {
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression6();
		while (tokenBuffer.get(bufferPos).type == TokenType.PLUS ||
				tokenBuffer.get(bufferPos).type == TokenType.MINUS) {
			op = tokenBuffer.get(bufferPos);
			acceptIt();
			ex = new BinaryExpr(new Operator(op), ex, parseExpression6(), sp); //implement sourcePos later
		}
		return ex;
	}
	
	private Expression parseExpression6() throws SyntaxError {
		Token op;
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Expression ex = parseExpression7();
		while (tokenBuffer.get(bufferPos).type == TokenType.MUL ||
				tokenBuffer.get(bufferPos).type == TokenType.DIV) {
			op = tokenBuffer.get(bufferPos);
			acceptIt();
			ex = new BinaryExpr(new Operator(op), ex, parseExpression7(), sp); //implement sourcePos later
		}
		return ex;
	}
	
	private Expression parseExpression7() throws SyntaxError {
		if (tokenBuffer.get(bufferPos).type == TokenType.NOT ||
			tokenBuffer.get(bufferPos).type == TokenType.MINUS) {
			Operator o = new Operator(tokenBuffer.get(bufferPos));
			Expression e;
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			acceptIt();
			e = parseExpression7();
			return new UnaryExpr(o, e, sp); //implement sourcePos later
		}
		else {
			return parseExpressionEnd();
		}
	}
	
	private Expression parseExpressionEnd() {
		//expression in paren
		if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
			Expression e;
			acceptIt();
			e = parseExpression();
			accept(TokenType.RPAREN);
			return e;
		}
		
		//intLiteral expression
		if (tokenBuffer.get(bufferPos).type == TokenType.NUM) {
			Terminal t = new IntLiteral(tokenBuffer.get(bufferPos));
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			acceptIt();
			return new LiteralExpr(t, sp); //implement sourcePos later
		}
		
		//booleanLiteral expression
		if (tokenBuffer.get(bufferPos).spelling.equals("true") ||
			tokenBuffer.get(bufferPos).spelling.equals("false")) {
			Terminal t = new BooleanLiteral(tokenBuffer.get(bufferPos));
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			acceptIt();
			return new LiteralExpr(t, sp);
		}
		
		//nullLiteral expression
		if (tokenBuffer.get(bufferPos).spelling.equals("null")) {
			Terminal t = new NullLiteral(tokenBuffer.get(bufferPos));
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			acceptIt();
			return new LiteralExpr(t, sp);
		}
		
		//new expression
		if (tokenBuffer.get(bufferPos).spelling.equals("new")) {
			return parseNewExpr();
		}
		
		//call or ref expression
		if (tokenBuffer.get(bufferPos).type == TokenType.ID) {
			return parseCallRefExpr();
		}
		
		parseError("expecting an expression but found '" + tokenBuffer.get(0).spelling + "'");
		return null;
	}
	
	private Expression parseNewExpr() {
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		acceptIt();
		if (!tokenBuffer.get(bufferPos).spelling.equals("boolean")) {
			if (tokenBuffer.get(bufferPos).spelling.equals("int")) {
				TypeDenoter type = new BaseType(TypeKind.INT, tokenBuffer.get(bufferPos).posn); //implement sourcePos later
				acceptIt();
				return parseNewAryExpr(type, sp);
			}
			else {
				ClassType type = new ClassType(new Identifier(tokenBuffer.get(bufferPos)), tokenBuffer.get(bufferPos).posn); //implement sourcePos later
				acceptIDNoKeyword();
				if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
					return parseNewObjExpr(type, sp);
				}
				else {
					return parseNewAryExpr(type, sp);
				}
			}
		}
		else {
			parseError("expecting an array type but found '" + tokenBuffer.get(0).spelling + "'");
			return null;
		}
	}
	
	private Expression parseCallRefExpr() {
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		Reference ref = parseReference();
		ExprList el = new ExprList();
		if (tokenBuffer.get(bufferPos).type == TokenType.LPAREN) {
			acceptIt();
			if (tokenBuffer.get(bufferPos).type == TokenType.RPAREN) {
				acceptIt();
			}
			else {
				el = parseArgumentList();
			}
			return new CallExpr(ref, el, sp); //implement sourcePos later
		}
		else {
			return new RefExpr(ref, sp); //implement sourcePos later
		}
	}
	
	private Expression parseNewObjExpr(ClassType type, SourcePosition sp) {
		acceptIt();
		accept(TokenType.RPAREN);
		return new NewObjectExpr(type, sp); //implement sourcePos later
	}
	
	private Expression parseNewAryExpr(TypeDenoter type, SourcePosition sp) {
		Expression e;
		accept(TokenType.LBRACK);
		e = parseExpression();
		accept(TokenType.RBRACK);
		return new NewArrayExpr(type, e, sp); //implement sourcePos later
	}
	
	private Reference parseReference() throws SyntaxError {
		Reference ref;
		ref = parseBaseRef();
		while (tokenBuffer.get(bufferPos).type == TokenType.DOT) {
			acceptIt();
			ref = parseQualifiedRef(ref);
		}
		return ref;
	}
	
	private BaseRef parseBaseRef() throws SyntaxError {
		BaseRef ref;
		if (tokenBuffer.get(bufferPos).spelling.equals("this")) {
			ref = new ThisRef(tokenBuffer.get(bufferPos).posn); //implement sourcePos later
			acceptIt();
		}
		else {
			Identifier Id = new Identifier(tokenBuffer.get(bufferPos));
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			acceptIDNoKeyword();
			if (tokenBuffer.get(bufferPos).type == TokenType.LBRACK) {
				Expression e;
				acceptIt();
				e = parseExpression();
				accept(TokenType.RBRACK);
				ref = new IxIdRef(Id, e, sp); //implement sourcePos later
			}
			else {
				ref = new IdRef(Id, sp); //implement sourcePos later
			}
		}
		return ref;
	}
	
	private QualifiedRef parseQualifiedRef(Reference inRef) throws SyntaxError {
		QualifiedRef ref;
		Identifier Id = new Identifier(tokenBuffer.get(bufferPos));
		SourcePosition sp = tokenBuffer.get(bufferPos).posn;
		if (!tokenBuffer.get(bufferPos).spelling.equals("this")) {
			acceptIDNoKeyword();
		}
		else {
			parseError("expecting a qualifiedRef but found '" + tokenBuffer.get(0).spelling + "'");
		}
		
		if (tokenBuffer.get(bufferPos).type == TokenType.LBRACK) {
			Expression e;
			acceptIt();
			e = parseExpression();
			accept(TokenType.RBRACK);
			ref = new IxQRef(inRef, Id, e, sp); //implement sourcePos later
		}
		else {
			ref = new QRef(inRef, Id, sp); //implement sourcePos later
		}
		return ref;
	}
	
	private TypeDenoter parseType() throws SyntaxError {
		TypeDenoter type = null;
		if (tokenBuffer.get(bufferPos).spelling.equals("boolean")) {
			type = new BaseType(TypeKind.BOOLEAN, tokenBuffer.get(bufferPos).posn); //implement sourcePos later
			acceptIt();
		}
		else if (tokenBuffer.get(bufferPos).type == TokenType.ID) { //checks for keyword int as well
			SourcePosition sp = tokenBuffer.get(bufferPos).posn;
			if (tokenBuffer.get(bufferPos).spelling.equals("int")) {
				type = new BaseType(TypeKind.INT, sp); //implement sourcePos later
				accept(TokenType.ID);
			}
			else {
				type = new ClassType(new Identifier(tokenBuffer.get(bufferPos)), sp); //implement sourcePos later
				acceptIDNoKeyword();
			}
			if (tokenBuffer.get(bufferPos).type == TokenType.LBRACK) {
				acceptIt();
				accept(TokenType.RBRACK);
				type = new ArrayType(type, sp); //implement sourcePos later
			}
		}
		else {
			parseError("expecting Type but found '" + tokenBuffer.get(0).spelling + "'");
		}
		return type;
	}
	
	private void keepIt() {
		keep(tokenBuffer.get(bufferPos).type);
	}
	
	private void keep(TokenType type) {
		if (type == tokenBuffer.get(bufferPos).type) {
			tokenBuffer.add(scanner.next());
			bufferPos++;
		}
		else {
			parseError("expecting '" + type + "' but found '" + tokenBuffer.get(0).type + "'");
		}
	}
	
	private void acceptIt() {
		accept(tokenBuffer.get(bufferPos).type);
	}
	
	private void accept(TokenType type) {
		if (type == tokenBuffer.get(bufferPos).type) {
			tokenBuffer.remove(0);
			if (tokenBuffer.size() == 0) {
				tokenBuffer.add(scanner.next());
			}
		}
		else {
			parseError("expecting '" + type + "' but found '" + tokenBuffer.get(0).type + "'");
		}
	}
	
	private void acceptIDNoKeyword() {
		switch (tokenBuffer.get(bufferPos).spelling) {
		case "this":
		case "boolean":
		case "int":
		case "class":
		case "void":
		case "public":
		case "private":
		case "static":
		case "return":
		case "if":
		case "else":
		case "while":
		case "for":
		case "true":
		case "false":
		case "new":
		case "null":
			parseError("reserved keyword cannot be used as an ID");
			break;
		default:
			accept(TokenType.ID);
			break;
		}
	}
	
	private void parseError(String e) throws SyntaxError {
		ErrorReporter.reportError("Parse error: " + e, tokenBuffer.get(bufferPos).posn);
		throw new SyntaxError();
	}
}
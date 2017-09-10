package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeCheckTraversal implements Visitor<Object, TypeDenoter>{

	public boolean hasError;
	
	public TypeCheckTraversal() {
		hasError = false;
	}
	
	public void runTCPass(AST ast) {
		ast.visit(this, null);
	}
	
	public TypeDenoter visitPackage(Package prog, Object arg) {
        for (ClassDecl c: prog.classDeclList){
            c.visit(this, null);
        }
        return null;
	}

	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		for (MethodDecl m: cd.methodDeclList) {
			m.visit(this, null);
		}
		return null;
	}

	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		//not called
		return null;
	}

	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		TypeDenoter returnType = null;
		for (Statement s: md.statementList) {
			TypeDenoter td = s.visit(this, md.type);
			if (returnType == null) {
				returnType = td;
			}
		}
		if (returnType == null) {
			if (md.type.typeKind == TypeKind.VOID) {
				md.statementList.add(new ReturnStmt(null, new SourcePosition(0, 0)));
			}
			else {
				this.reportReturnTypeError(md.posn);
			}
		}
		return null;
	}

	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		//not called
		return null;
	}

	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		//not called
		return null;
	}

	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		return type;
	}

	public TypeDenoter visitClassType(ClassType type, Object arg) {
		return type;
	}

	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		return type.eltType;
	}

	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		TypeDenoter out = null;
		for (Statement s : stmt.sl) {
			TypeDenoter td = s.visit(this, arg);
			if (td == null) {
				out = td;
			}
		}
		return out;
	}

	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeDenoter expected = stmt.varDecl.type;
		TypeDenoter found = stmt.initExp.visit(this, null);
		if (!expected.equals(found) || expected.typeKind == TypeKind.UNSUPPORTED || expected.typeKind == TypeKind.VOID) {
			if (expected.typeKind != TypeKind.ERROR && found.typeKind != TypeKind.ERROR) {
				reportUnexpectedTypeError(getStringPrintOut(expected), getStringPrintOut(found), stmt.initExp.posn);
			}
		}
		return null;
	}

	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeDenoter expected = stmt.ref.visit(this, null);
		TypeDenoter found = stmt.val.visit(this, null);
		if (!expected.equals(found) || expected.typeKind == TypeKind.UNSUPPORTED || expected.typeKind == TypeKind.VOID) {
			if (expected.typeKind != TypeKind.ERROR && found.typeKind != TypeKind.ERROR) {
				reportUnexpectedTypeError(getStringPrintOut(expected), getStringPrintOut(found), stmt.val.posn);
			}
		}
		return null;
	}

	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		MethodDecl md = (MethodDecl)stmt.methodRef.decl;
		for (int i = 0; i < md.parameterDeclList.size(); i++) {
			TypeDenoter expected = md.parameterDeclList.get(i).type;
			TypeDenoter found = stmt.argList.get(i).visit(this, null);
			if (!expected.equals(found) || expected.typeKind == TypeKind.UNSUPPORTED || expected.typeKind == TypeKind.VOID) {
				if (expected.typeKind != TypeKind.ERROR && found.typeKind != TypeKind.ERROR) {
					this.reportIncorrectArgumentList(stmt.posn);
					break;
				}
			}

		}
		return null;
	}

	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr == null) {
			if (((TypeDenoter)arg).typeKind == TypeKind.VOID) {
				return new BaseType(TypeKind.VOID, stmt.posn);
			}
			else {
				reportReturnTypeError(stmt.posn);
				return (TypeDenoter)arg;
			}
		}
		else {
			TypeDenoter returnType =  stmt.returnExpr.visit(this, null);
			if (((TypeDenoter)arg).equals(returnType) && returnType.typeKind != TypeKind.ERROR) {
				return returnType;
			}
			else {
				reportReturnTypeError(stmt.posn);
				return (TypeDenoter)arg;
			}
		}
	}

	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		TypeDenoter returnType = null;
		TypeDenoter returnType2 = null;
		TypeKind tk = stmt.cond.visit(this, null).typeKind;
		if (tk != TypeKind.ERROR && tk != TypeKind.BOOLEAN) {
			reportUnexpectedTypeError("BOOLEAN", tk.toString(), stmt.cond.posn);
		}
		returnType = stmt.thenStmt.visit(this, arg);
		if (stmt.elseStmt != null) {
			returnType2 = stmt.elseStmt.visit(this, arg);
		}
		if (returnType != null && returnType2 != null) {
			return returnType;
		}
		else {
			return null;
		}
	}

	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeKind tk = stmt.cond.visit(this, null).typeKind;
		if (tk != TypeKind.ERROR && tk != TypeKind.BOOLEAN) {
			reportUnexpectedTypeError("BOOLEAN", tk.toString(), stmt.cond.posn);
		}
		stmt.body.visit(this, arg);
		return null;
	}
	
	public TypeDenoter visitForStmt(ForStmt stmt, Object arg) {
		if (stmt.varInitStmt != null) {
			stmt.varInitStmt.visit(this, null);
		}
		if (stmt.loopCondExpr != null) {
			TypeKind tk = stmt.loopCondExpr.visit(this, null).typeKind;
			if (tk != TypeKind.ERROR && tk != TypeKind.BOOLEAN) {
				reportUnexpectedTypeError("BOOLEAN", tk.toString(), stmt.loopCondExpr.posn);
			}
		}
		for (int i = 0; i < stmt.varIncrStmts.size(); i++) {
			stmt.varIncrStmts.get(i).visit(this, null);
		}
		stmt.body.visit(this, arg);
		return null;	
	}

	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		TypeDenoter t = expr.expr.visit(this, null);
		if (expr.operator.type == TokenType.MINUS) {		//MINUS operator
			return uOpTypeChecker(TypeKind.INT, t, TypeKind.INT, expr.operator);
		}
		else if (expr.operator.type == TokenType.NOT) {		//NOT operator
			return uOpTypeChecker(TypeKind.BOOLEAN, t, TypeKind.BOOLEAN, expr.operator);
		}
		else {
			ErrorReporter.reportError(expr.operator + " is not an unary operator", expr.posn);
			hasError = true;
			return new BaseType(TypeKind.ERROR, expr.posn);
			//should not ever be reached
		}
	}

	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeDenoter left = expr.left.visit(this, null);
		TypeDenoter right = expr.right.visit(this, null);
		switch (expr.operator.type) {
		case GT:
		case LT:
		case LTE:
		case GTE:
			return binOPTypeChecker(TypeKind.INT, left, right, TypeKind.BOOLEAN, expr.operator);
		case ISEQ:
		case NE:
			return binOPTypeChecker(left.typeKind, left, right, TypeKind.BOOLEAN, expr.operator);
		case AND:
		case OR:
			return binOPTypeChecker(TypeKind.BOOLEAN, left, right, TypeKind.BOOLEAN, expr.operator);
		case PLUS:
		case MINUS:
		case MUL:
		case DIV:
			return binOPTypeChecker(TypeKind.INT, left, right, TypeKind.INT, expr.operator);
		default:
			ErrorReporter.reportError(expr.operator + " is not an unary operator", expr.posn);
			return new BaseType(TypeKind.ERROR, expr.posn);
			//should not ever be reached
		}
	}

	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		MethodDecl md = (MethodDecl)expr.functionRef.decl;
		for (int i = 0; i < md.parameterDeclList.size(); i++) {
			TypeDenoter expected = md.parameterDeclList.get(i).type;
			TypeDenoter found = expr.argList.get(i).visit(this, null);
			if (!expected.equals(found) || expected.typeKind == TypeKind.UNSUPPORTED || expected.typeKind == TypeKind.VOID) {
				if (expected.typeKind != TypeKind.ERROR && found.typeKind != TypeKind.ERROR) {
					this.reportIncorrectArgumentList(expr.posn);
					break;
				}
			}
		}
		return md.type;
	}

	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, null);
	}

	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return expr.classtype;
	}

	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		TypeKind tk = expr.sizeExpr.visit(this, null).typeKind;
		if (tk != TypeKind.ERROR && tk != TypeKind.INT) {
			this.reportUnexpectedTypeError("INT", tk.toString(), expr.sizeExpr.posn);
		}
		return new ArrayType(expr.eltType, expr.posn);
	}

	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		Identifier id = new Identifier(new Token(TokenType.ID, ref.decl.name, ref.posn));
		id.decl = ref.decl;
		return new ClassType(id, ref.posn);
	}

	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		if (ref.decl.type == null) {
			return new ClassType(ref.id, ref.posn);
		}
		else {
			return ref.decl.type;
		}
	}

	public TypeDenoter visitIxIdRef(IxIdRef ref, Object arg) {
		TypeKind tk = ref.indexExpr.visit(this, null).typeKind;
		if (tk != TypeKind.INT && tk != TypeKind.ERROR) {
			reportUnexpectedTypeError("INT", tk.toString(), ref.indexExpr.posn);
			return new BaseType(TypeKind.ERROR, ref.indexExpr.posn);
		}
		else {
			return ref.decl.type.visit(this, null);
		}
	}

	public TypeDenoter visitQRef(QRef ref, Object arg) {
		if (ref.ref.decl.type instanceof ArrayType && ref.id.spelling.equals("length")) {
			return new BaseType(TypeKind.INT, new SourcePosition(0, 0));
		}
		if (ref.decl.type == null) {
			return new ClassType(ref.id, ref.posn);
		}
		else {
			return ref.decl.type;
		}
	}

	public TypeDenoter visitIxQRef(IxQRef ref, Object arg) {
		TypeKind tk = ref.ixExpr.visit(this, null).typeKind;
		if (tk != TypeKind.INT && tk != TypeKind.ERROR) {
			reportUnexpectedTypeError("INT", tk.toString(), ref.ixExpr.posn);
			return new BaseType(TypeKind.ERROR, ref.ixExpr.posn);
		}
		else {
			return ref.decl.type.visit(this, null);
		}
	}

	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		if (id.decl.type == null) {
			return new ClassType(id, id.posn);
		}
		else {
			return id.decl.type.visit(this, null);
		}
	}

	public TypeDenoter visitOperator(Operator op, Object arg) {
		//not called
		return null;
	}

	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		return new BaseType(TypeKind.INT, num.posn);
	}

	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

	public TypeDenoter visitNullLiteral(NullLiteral n, Object arg) {
		return new BaseType(TypeKind.NULL, n.posn);
	}
	
	//r cannot be classtype or arraytype
	private TypeDenoter binOPTypeChecker(TypeKind ex, TypeDenoter f1, TypeDenoter f2, TypeKind r, Operator o) {
		if ((f1.typeKind == TypeKind.ERROR && f2.typeKind == ex) || (f1.typeKind == ex && f2.typeKind == TypeKind.ERROR)) {
			return new BaseType(r, f1.posn);
		}
		else if (f1.equals(f2)) {
			if (f1.typeKind == ex && f1.typeKind != TypeKind.UNSUPPORTED && f1.typeKind != TypeKind.VOID) {
				return new BaseType(r, f1.posn);
			}
			else {
				reportBinaryOperationNotSupported(getStringPrintOut(f1), getStringPrintOut(f2), o, o.posn);
				return new BaseType(TypeKind.ERROR, o.posn);
			}
		}
		else {
			reportBinaryOperationNotSupported(getStringPrintOut(f1), getStringPrintOut(f2), o, o.posn);
			return new BaseType(TypeKind.ERROR, o.posn);
		}
	}
	
	//ex or r cannot be classtype or arraytype
	private TypeDenoter uOpTypeChecker(TypeKind ex, TypeDenoter f, TypeKind r, Operator o) {
		if (ex == TypeKind.ERROR) {
			return new BaseType(r, o.posn);
		}
		else if (ex == f.typeKind) {
			return new BaseType(r, o.posn);
		}
		else {
			reportUnaryOperationNotSupported(f.typeKind, o, o.posn);
			return new BaseType(TypeKind.ERROR, o.posn);
		}
	}
	
	private String getStringPrintOut(TypeDenoter t) {
		TypeDenoter type = t;
		String out = "";
		while (type.typeKind == TypeKind.ARRAY) {
			type = ((ArrayType)type).eltType;
			out += "[]";
		}
		if (type.typeKind == TypeKind.CLASS) {
			out = ((ClassType)type).className.spelling + out;
		}
		else {
			out = type.typeKind + out;
		}
		return out;
	}
	
	private void reportIncorrectArgumentList(SourcePosition posn) {
		hasError = true;
		ErrorReporter.reportError("***TypeError: The parameters provided does not match the form provided by the method declaration", posn);
	}
	
	private void reportReturnTypeError(SourcePosition posn) {
		hasError = true;
		ErrorReporter.reportError("***TypeError: The return type does not match the type of the method", posn);
	}
	
	private void reportUnexpectedTypeError(String expected, String found, SourcePosition posn) {
		hasError = true;
		ErrorReporter.reportError("***TypeError: Expecting Type '" + expected + "' but found '" + found + "'", posn);
	}

	private void reportBinaryOperationNotSupported(String t1, String t2, Operator o, SourcePosition posn) {
		hasError = true;
		ErrorReporter.reportError("***TypeError: The binary operator '" + o.spelling + "' is not defined for types '" + t1 + "' and '" + t2 + "'", posn);
	}
	
	private void reportUnaryOperationNotSupported(TypeKind type, Operator o, SourcePosition posn) {
		hasError = true;
		ErrorReporter.reportError("***TypeError: The unary operator '" + o.spelling + "' is not defined on type '" + type + "'", posn);
	}
}
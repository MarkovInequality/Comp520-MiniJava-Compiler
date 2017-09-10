package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class IDTraversal implements Visitor<ScopedIDTable, Object> {
	
	public void runIDPass(AST ast) {
		ast.visit(this, null);
	}
	
	public Object visitPackage(Package prog, ScopedIDTable st) {
		ScopedIDTable declRefTable = new ScopedIDTable();
		declRefTable.enterScope();
		int mainMthds = 0;
		for (ClassDecl c: prog.classDeclList) {
			declRefTable.addVarToScope(c.name, c);
			for (FieldDecl f: c.fieldDeclList) {
				c.membersTable.addMember(f.name, f);
			}
			for (MethodDecl m: c.methodDeclList) {
				if (isMain(m)) {
					m.isMain = true;
					mainMthds++;
				}
				c.membersTable.addMember(m.name, m);
			}
		}
		if (mainMthds == 0) {
			ErrorReporter.reportUnresolvableError("***IdentificationError: main method is not found");
		}
		else if (mainMthds > 1) {
			ErrorReporter.reportUnresolvableError("***IdentificationError: multiple main methods");
		}
		
		for (ClassDecl c: prog.classDeclList) {
			for (FieldDecl f: c.fieldDeclList) {
				f.type.visit(this, declRefTable);
			}
			for (MethodDecl m: c.methodDeclList) {
				m.type.visit(this, declRefTable);
			}
		}
		for (ClassDecl c: prog.classDeclList) {
			c.visit(this, declRefTable);
		}
		declRefTable.exitScope();
        return null;
	}

	public Object visitClassDecl(ClassDecl cd, ScopedIDTable st) {
		st.thisClass = cd;
		st.enterScope();
		for (FieldDecl f: cd.fieldDeclList) {
			st.addVarToScope(f.name, f);
		}
		for (MethodDecl m: cd.methodDeclList) {
			st.addVarToScope(m.name, m);
		}
		
		for (MethodDecl m: cd.methodDeclList) {
			m.visit(this, st);
		}
		st.exitScope();
		st.thisClass = null;
		return null;
	}

	public Object visitFieldDecl(FieldDecl fd, ScopedIDTable st) {
		//not called
		return null;
	}

	public Object visitMethodDecl(MethodDecl md, ScopedIDTable st) {
		st.currentMethod = md;
		st.enterScope();
		for (ParameterDecl p : md.parameterDeclList) {
			p.visit(this, st);
		}
		for (Statement s: md.statementList) {
			s.visit(this, st);
		}
		st.exitScope();
		st.currentMethod = null;
		return null;
	}

	public Object visitParameterDecl(ParameterDecl pd, ScopedIDTable st) {
		pd.type.visit(this, st);
		st.addVarToScope(pd.name, pd);
		return null;
	}

	public Object visitVarDecl(VarDecl decl, ScopedIDTable st) {
		//not called
		return null;
	}

	public Object visitBaseType(BaseType type, ScopedIDTable st) {
		//nothing needs to be done here
		return null;
	}

	public Object visitClassType(ClassType type, ScopedIDTable st) {
		Declaration decl = st.findClassDecl(type.className);
		if (decl == null) {
			reportNoDeclarationError(type.className);
		}
		type.className.decl = decl;
		return type.className.decl;
	}

	public Object visitArrayType(ArrayType type, ScopedIDTable st) {
		return type.eltType.visit(this, st);
	}

	public Object visitBlockStmt(BlockStmt stmt, ScopedIDTable st) {
		st.enterScope();
		for (Statement s: stmt.sl) {
			s.visit(this, st);
		}
		st.exitScope();
		return null;
	}

	public Object visitVardeclStmt(VarDeclStmt stmt, ScopedIDTable st) {
		st.startDeclaringVar(stmt.varDecl);
        st.addVarToScope(stmt.varDecl.name, stmt.varDecl);
        stmt.initExp.visit(this, st);
        st.varFinishedBeingDeclared(stmt.varDecl);
		return null;
	}

	public Object visitAssignStmt(AssignStmt stmt, ScopedIDTable st) {
		if (stmt.ref instanceof ThisRef) {
			ErrorReporter.reportError("***IdentificationError: The left-hand side of an assignment must be a variable", stmt.posn);
		}
		if (stmt.ref.visit(this, st) != null) {
			ErrorReporter.reportUnresolvableError("***IdentificationError: the length field of arrays cannot be assigned", stmt.ref.posn);
		}
		if (stmt.ref.decl instanceof MethodDecl) {
			reportUnresolvableRefsError(stmt.ref.decl.name, stmt.ref.posn);
		}
		stmt.val.visit(this, st);
		return null;
	}

	public Object visitCallStmt(CallStmt stmt, ScopedIDTable st) {
		stmt.methodRef.visit(this, st);
		if (stmt.methodRef instanceof ThisRef) {
			this.reportUnresolvableMethodError("this", stmt.posn);
		}
		for (Expression e: stmt.argList) {
			e.visit(this, st);
		}
		return null;
	}

	public Object visitReturnStmt(ReturnStmt stmt, ScopedIDTable st) {
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, st);
		}
		return null;
	}

	public Object visitIfStmt(IfStmt stmt, ScopedIDTable st) {
		stmt.cond.visit(this, st);
		if (stmt.thenStmt instanceof VarDeclStmt) {
			reportDeclStmtInBranch(stmt.thenStmt.posn);
		}
		stmt.thenStmt.visit(this, st);
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				reportDeclStmtInBranch(stmt.elseStmt.posn);
			}
			stmt.elseStmt.visit(this, st);
		}
		return null;
	}

	public Object visitWhileStmt(WhileStmt stmt, ScopedIDTable st) {
		stmt.cond.visit(this, st);
		if (stmt.body instanceof VarDeclStmt) {
			reportDeclStmtInBranch(stmt.body.posn);
		}
		stmt.body.visit(this, st);
		return null;
	}
	
	public Object visitForStmt(ForStmt stmt, ScopedIDTable st) {
		st.enterScope();
		if (stmt.varInitStmt != null) {
			stmt.varInitStmt.visit(this, st);
		}
		if (stmt.loopCondExpr != null) {
			stmt.loopCondExpr.visit(this, st);
		}
		for (int i = 0; i < stmt.varIncrStmts.size(); i++) {
			stmt.varIncrStmts.get(i).visit(this, st);
		}
		if (stmt.body instanceof VarDeclStmt) {
			reportDeclStmtInBranch(stmt.body.posn);
		}
		stmt.body.visit(this, st);
		st.exitScope();
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr expr, ScopedIDTable st) {
		expr.expr.visit(this, st);
		return null;
	}

	public Object visitBinaryExpr(BinaryExpr expr, ScopedIDTable st) {
		expr.left.visit(this, st);
		expr.right.visit(this, st);
		return null;
	}

	public Object visitRefExpr(RefExpr expr, ScopedIDTable st) {
		expr.ref.visit(this, st);
		if (expr.ref.decl instanceof MethodDecl) {
			reportUnresolvableRefsError(expr.ref.decl.name, expr.ref.posn);
		}
		if (expr.ref instanceof BaseRef && expr.ref.decl instanceof ClassDecl && !(expr.ref instanceof ThisRef)) {
			reportUnresolvableRefsError(expr.ref.decl.name, expr.ref.posn);
		}
		return null;
	}

	public Object visitCallExpr(CallExpr expr, ScopedIDTable st) {
		expr.functionRef.visit(this, st);
		if (expr.functionRef instanceof ThisRef) {
			reportUnresolvableMethodError("this", expr.posn);	
		}
		for (Expression e: expr.argList) {
			e.visit(this, st);
		}
		return null;
	}

	public Object visitLiteralExpr(LiteralExpr expr, ScopedIDTable st) {
		//nothing needed to be done here
		return null;
	}

	public Object visitNewObjectExpr(NewObjectExpr expr, ScopedIDTable st) {
		expr.classtype.visit(this, st);
		return null;
	}

	public Object visitNewArrayExpr(NewArrayExpr expr, ScopedIDTable st) {
		expr.sizeExpr.visit(this, st);
		return null;
	}

	public Object visitThisRef(ThisRef ref, ScopedIDTable st) {
		if (st.currentMethod.isStatic) {
			ErrorReporter.reportUnresolvableError("***Identification Error: cannot reference current instance from static method", ref.posn);
		}
		else {
			ref.decl = st.thisClass;
		}
		return null;
	}

	public Object visitIdRef(IdRef ref, ScopedIDTable st) {
		Declaration decl = st.findVarDecl(ref.id);
		if (decl == null) {
			reportNoDeclarationError(ref.id);
		}
		if (st.varBeingDeclared(decl)) {
			reportVarRefedWhileDeclaring(ref.id);
		}
		if (decl instanceof MemberDecl) {
			if (st.currentMethod.isStatic && !((MemberDecl)decl).isStatic) {
				reportStaticAccessError(ref.id);
			}
		}
		ref.decl = decl;
		ref.id.decl = decl;
		return null;
	}

	public Object visitIxIdRef(IxIdRef ref, ScopedIDTable st) {
		Declaration decl = st.findVarDecl(ref.id);
		if (decl == null) {
			reportNoDeclarationError(ref.id);
		}
		if (st.varBeingDeclared(decl)) {
			reportVarRefedWhileDeclaring(ref.id);
		}
		if (decl instanceof MemberDecl) {
			if (st.currentMethod.isStatic != ((MemberDecl)decl).isStatic) {
				reportStaticAccessError(ref.id);
			}
		}
		ref.decl = decl;
		ref.id.decl = decl;
		if (!(ref.decl.type instanceof ArrayType)) {
			reportNotArrayError(ref.id, ref.posn);
		}
		ref.indexExpr.visit(this, st);
		return null;
	}

	public Object visitQRef(QRef ref, ScopedIDTable st) {
		ref.ref.visit(this, st);
		ClassDecl cd;
		MemberDecl decl;
		if (ref.ref.decl instanceof ClassDecl) {
			cd = (ClassDecl)ref.ref.decl;
			decl = (MemberDecl)cd.membersTable.getMember(ref.id);
		}
		else {
			cd = (ClassDecl)ref.ref.decl.type.visit(this, st);
			if (ref.ref.decl.type instanceof BaseType) {
				this.reportNoSuchPropertyError(ref.ref.decl.name, ref.id);
			}
			if (ref.ref.decl.type instanceof ArrayType) {
				if (ref.id.spelling.equals("length")) {
					return new BaseType(TypeKind.INT, new SourcePosition(0, 0));
				}
				else if (!(ref.ref instanceof IxIdRef) && !(ref.ref instanceof IxQRef)) {
					this.reportNoSuchPropertyError(ref.ref.decl.name, ref.id);
				}
			}
			decl = (MemberDecl)cd.membersTable.getMember(ref.id);
		}
		if (decl == null) {
			reportNoDeclarationError(ref.id);
		}
		else if (decl.isPrivate && (cd != st.thisClass)) {
			reportPrivacyError(ref.id);
		}
		boolean isStaticAccess = (ref.ref.decl instanceof ClassDecl) 
				&& (ref.ref instanceof BaseRef) 
				&& !(ref.ref instanceof ThisRef);
		if (!decl.isStatic && isStaticAccess) {
			reportStaticAccessError(ref.id);
		}
		if (ref.ref.decl instanceof MethodDecl) {
			reportUnresolvableRefsError(ref.ref.decl.name, ref.ref.posn);
		}
		ref.decl = decl;
		ref.id.decl = decl;
		return null;
	}

	public Object visitIxQRef(IxQRef ref, ScopedIDTable st) {
		ref.ref.visit(this, st);
		ClassDecl cd;
		MemberDecl decl;
		if (ref.ref.decl instanceof ClassDecl) {
			cd = (ClassDecl)ref.ref.decl;
			decl = (MemberDecl)cd.membersTable.getMember(ref.id);
		}
		else {
			cd = (ClassDecl)ref.ref.decl.type.visit(this, st);
			if (cd == null) {
				this.reportNoSuchPropertyError(ref.ref.decl.name, ref.id);
			}
			decl = (MemberDecl)cd.membersTable.getMember(ref.id);
		}
		if (decl == null) {
			reportNoDeclarationError(ref.id);
		}
		else if (decl.isPrivate && (cd != st.thisClass)) {
			reportPrivacyError(ref.id);
		}
		boolean isStaticAccess = (ref.ref.decl instanceof ClassDecl) 
				&& (ref.ref instanceof BaseRef) 
				&& !(ref.ref instanceof ThisRef);
		if (!decl.isStatic && isStaticAccess) {
			reportStaticAccessError(ref.id);
		}
		if (ref.ref.decl instanceof MethodDecl) {
			reportUnresolvableRefsError(ref.ref.decl.name, ref.ref.posn);
		}
		ref.decl = decl;
		ref.id.decl = decl;
		if (!(ref.decl.type instanceof ArrayType)) {
			reportNotArrayError(ref.id, ref.posn);
		}
		ref.ixExpr.visit(this, st);
		return null;
	}

	public Object visitIdentifier(Identifier id, ScopedIDTable st) {
		return null;
	}

	public Object visitOperator(Operator op, ScopedIDTable st) {
		return null;
	}

	public Object visitIntLiteral(IntLiteral num, ScopedIDTable st) {
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral bool, ScopedIDTable st) {
		return null;
	}
	
	public Object visitNullLiteral(NullLiteral n, ScopedIDTable st) {
		return null;
	}
	
	private boolean isMain(MethodDecl md) {
		if (md.isPublic && md.isStatic && md.type.typeKind == TypeKind.VOID) {
			if (md.parameterDeclList.size() == 1) {
				ParameterDecl pd = md.parameterDeclList.get(0);
				if (pd.type instanceof ArrayType) {
					ArrayType type = (ArrayType)pd.type;
					if (type.eltType instanceof ClassType
					    && ((ClassType)type.eltType).className.spelling.equals("String")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	//ERRORS
	
	private void reportNotArrayError(Identifier id, SourcePosition posn) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: '" + id.spelling + "' cannot be indexed over as it is not an array");
	}
	
	private void reportUnresolvableMethodError(String name, SourcePosition posn) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: '" + name + "' cannot be resolved to a method", posn);
	}
	
	private void reportUnresolvableRefsError(String name, SourcePosition posn) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: '" + name + "' cannot be resolved to a variable or is not a field", posn);
	}
	
	private void reportNoSuchPropertyError(String s, Identifier id) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: The variable '" + s + "' does not have property '" + id.spelling + "'", id.posn);
	}
	
	private void reportDeclStmtInBranch(SourcePosition sp) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: Variable declarations cannot be the only statement in a conditional branch", sp);
	}
	
	private void reportVarRefedWhileDeclaring(Identifier id) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: The variable '" + id.spelling + "' is currently being declared", id.posn);
	}
	
	private void reportNoDeclarationError(Identifier id) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: The reference '" + id.spelling + "' is undeclared", id.posn);
	}
	
	private void reportPrivacyError(Identifier id) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: Cannot access '" + id.spelling + "' due to scope restraints", id.posn);
	}
	
	private void reportStaticAccessError(Identifier id) {
		ErrorReporter.reportUnresolvableError("***IdentificationError: Reference '" + id.spelling + "' must follow static access rules", id.posn);
	}
}

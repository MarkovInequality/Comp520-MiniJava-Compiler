package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForStmt extends Statement {

	public Statement varInitStmt;
	public Expression loopCondExpr;
	public StatementList varIncrStmts;
	public Statement body;
	
	public ForStmt(Statement varInitStmt, Expression loopCondExpr, StatementList varIncrStmts, Statement body, SourcePosition sp) {
		super(sp);
		this.varInitStmt = varInitStmt;
		this.loopCondExpr = loopCondExpr;
		this.varIncrStmts = varIncrStmts;
		this.body = body;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitForStmt(this, o);
	}

}

package miniJava.CodeGenerator;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenType;

import java.util.HashMap;

import mJAM.*;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;

public class mJAMGenerator implements Visitor<Integer, Integer>{

	private HashMap<Integer, MethodDecl> unpatchedCalls;
	private int localVarOffset;
	private int methodArgs;
	
	public mJAMGenerator() {
		unpatchedCalls = new HashMap<Integer, MethodDecl>();
		methodArgs = 0;
		localVarOffset = 3;
	}
	
	public void runGenerationPass(AST ast) {
		unpatchedCalls = new HashMap<Integer, MethodDecl>();
		methodArgs = 0;
		localVarOffset = 3;
		Machine.initCodeGen();
		ast.visit(this, null);
		for (Integer i : unpatchedCalls.keySet()) {
			Machine.patch(i, unpatchedCalls.get(i).entity.value);
		}
	}

	public Integer visitPackage(Package prog, Integer arg) {
		// TODO Auto-generated method stub
		//setup
		
		int staticFieldOffset = 0;
		for(ClassDecl c : prog.classDeclList) {
			int fieldOffset = 0;
			for (FieldDecl fd : c.fieldDeclList) {
				if (fd.isStatic) {
					Machine.emit(Op.LOADL, 0);
					fd.entity = new RunTimeEntity(staticFieldOffset++);
				}
				else {
					fd.entity = new RunTimeEntity(fieldOffset++);
				}
			}
			c.entity = new RunTimeEntity(fieldOffset);
		}
		
		Machine.emit(Op.LOADL, 0);
		Machine.emit(Prim.newarr);
		int patchAddr_Call_main = Machine.nextInstrAddr();
		Machine.emit(Op.CALL,Reg.CB,-1);
		Machine.emit(Op.HALT,0,0,0);
		
		int mainCBOffset = -1;
		for(ClassDecl c : prog.classDeclList) {
			Integer mainLoc = c.visit(this, null);
			if (mainLoc != null) {
				mainCBOffset = mainLoc.intValue();
			}
		}
		Machine.patch(patchAddr_Call_main, mainCBOffset);
		
		return null;
	}

	public Integer visitClassDecl(ClassDecl cd, Integer arg) {
		// TODO Auto-generated method stub
		Integer mainPos = null;
		for (MethodDecl md : cd.methodDeclList) {
			if (md.isMain) {
				mainPos = Machine.nextInstrAddr();
			}
			md.visit(this, null);
		}
		return mainPos;
	}

	public Integer visitFieldDecl(FieldDecl fd, Integer arg) {
		// TODO Auto-generated method stub
		//not visited
		return null;
	}

	public Integer visitMethodDecl(MethodDecl md, Integer arg) {
		// TODO Auto-generated method stub
		localVarOffset = 3;
		methodArgs = md.parameterDeclList.size();
		int i = 0;
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.entity = new RunTimeEntity(-methodArgs + i++);
		}
		md.entity = new RunTimeEntity(Machine.nextInstrAddr());
		for (Statement s : md.statementList) {
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Integer visitParameterDecl(ParameterDecl pd, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitVarDecl(VarDecl decl, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitBaseType(BaseType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitClassType(ClassType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer visitArrayType(ArrayType type, Integer arg) {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		int counter = 0;
		for (Statement s : stmt.sl) {
			if (s.visit(this, null) != null) {
				counter++;
			}
		}
		localVarOffset -= counter;
		Machine.emit(Op.POP, counter);
		return null;
	}

	@Override
	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		stmt.varDecl.entity = new RunTimeEntity(localVarOffset++);
		stmt.initExp.visit(this, null);
		return 1;
	}

	@Override
	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		if (stmt.ref.decl instanceof FieldDecl) {
			FieldDecl md = (FieldDecl)stmt.ref.decl;
			if (md.isStatic) {
				stmt.val.visit(this, null);
				stmt.ref.visit(this, 1);
			}
			else {
				stmt.ref.visit(this, 1);
				stmt.val.visit(this, null);
				Machine.emit(Prim.fieldupd);
			}
		}
		else if (stmt.ref.decl.type instanceof ArrayType) {
			stmt.ref.visit(this, 1);
			stmt.val.visit(this, null);
			Machine.emit(Prim.arrayupd);
		}
		else {
			stmt.val.visit(this, null);
			stmt.ref.visit(this, 1);
		}
		return null;
	}

	@Override
	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		for (Expression e : stmt.argList) {
			e.visit(this, null);
		}
		stmt.methodRef.visit(this, null);
		if (((MethodDecl)stmt.methodRef.decl).type.typeKind != TypeKind.VOID) {
			Machine.emit(Op.POP, 1);
		}
		return null;
	}

	@Override
	public Integer visitReturnStmt(ReturnStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
			Machine.emit(Op.RETURN, 1, 0, methodArgs);
		}
		else {
			Machine.emit(Op.RETURN, 0, 0, methodArgs);
		}
		return null;
	}

	@Override
	public Integer visitIfStmt(IfStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		stmt.cond.visit(this, null);
		int patchLoc = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);
		stmt.thenStmt.visit(this, null);
		int patchLoc2 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, -1);
		int displacement = Machine.nextInstrAddr();
		Machine.patch(patchLoc, displacement);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		int displacement2 = Machine.nextInstrAddr();
		Machine.patch(patchLoc2, displacement2);
		return null;
	}

	@Override
	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {
		// TODO Auto-generated method stub
		int patchLoc = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, -1);
		int body = Machine.nextInstrAddr();
		stmt.body.visit(this, null);
		int displacement = Machine.nextInstrAddr();
		Machine.patch(patchLoc, displacement);
		stmt.cond.visit(this, null);
		Machine.emit(Op.JUMPIF, 1, Reg.CB, body);
		return null;
	}
	
	@Override
	public Integer visitForStmt(ForStmt stmt, Integer arg) {
		//TODO Auto-generated method stub
		if (stmt.varInitStmt != null) {
			stmt.varInitStmt.visit(this, null);
		}
		int patchLoc = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, -1);
		int body = Machine.nextInstrAddr();
		stmt.body.visit(this, null);
		for (int i = 0; i < stmt.varIncrStmts.size(); i++) {
			stmt.varIncrStmts.get(i).visit(this, null);
		}
		int displacement = Machine.nextInstrAddr();
		Machine.patch(patchLoc, displacement);
		if (stmt.loopCondExpr != null) {
			stmt.loopCondExpr.visit(this, null);
		}
		else {
			Machine.emit(Op.LOADL, 1);
		}
		Machine.emit(Op.JUMPIF, 1, Reg.CB, body);
		
		if (stmt.varInitStmt instanceof VarDeclStmt) {
			localVarOffset--;
			Machine.emit(Op.POP, 1);
		}
		return null;
	}
	
	@Override
	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		expr.expr.visit(this, null);
		if (expr.operator.type == TokenType.MINUS) {
			Machine.emit(Prim.neg);
		}
		else if (expr.operator.type == TokenType.NOT) {
			Machine.emit(Prim.not);
		}
		return null;
	}

	@Override
	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		expr.left.visit(this, null);
		int patchLoc = -1;
		int patchLoc2 = -1;
		switch (expr.operator.type) {
		case GT: 
			expr.right.visit(this, null);
			Machine.emit(Prim.gt); break;
		case LT:
			expr.right.visit(this, null);
			Machine.emit(Prim.lt); break;
		case LTE: 
			expr.right.visit(this, null);
			Machine.emit(Prim.le); break;
		case GTE: 
			expr.right.visit(this, null);
			Machine.emit(Prim.ge); break;
		case ISEQ: 
			expr.right.visit(this, null);
			Machine.emit(Prim.eq); break;
		case NE: 
			expr.right.visit(this, null);
			Machine.emit(Prim.ne); break;
		case AND: 
			patchLoc = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);
			Machine.emit(Op.LOADL, 1);
			expr.right.visit(this, null);
			Machine.emit(Prim.and);
			patchLoc2 = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, -1);
			Machine.patch(patchLoc, Machine.nextInstrAddr());
			Machine.emit(Op.LOADL, 0);
			Machine.patch(patchLoc2, Machine.nextInstrAddr());
			break;
		case OR: 
			patchLoc = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 1, Reg.CB, -1);
			Machine.emit(Op.LOADL, 0);
			expr.right.visit(this, null);
			Machine.emit(Prim.or);
			patchLoc2 = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, -1);
			Machine.patch(patchLoc, Machine.nextInstrAddr());
			Machine.emit(Op.LOADL, 1);
			Machine.patch(patchLoc2, Machine.nextInstrAddr());
			break;
		case PLUS: 
			expr.right.visit(this, null);
			Machine.emit(Prim.add); break;
		case MINUS: 
			expr.right.visit(this, null);
			Machine.emit(Prim.sub); break;
		case MUL: 
			expr.right.visit(this, null);
			Machine.emit(Prim.mult); break;
		case DIV: 
			expr.right.visit(this, null);
			Machine.emit(Prim.div); break;
		default:
			//should never be reached
			System.out.println(expr.operator.type);
		}
		return null;
	}

	@Override
	public Integer visitRefExpr(RefExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		for (Expression e : expr.argList) {
			e.visit(this, null);
		}
		expr.functionRef.visit(this, null);
		return null;
	}

	@Override
	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.entity.value);
		Machine.emit(Prim.newobj);
		return null;
	}

	@Override
	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		// TODO Auto-generated method stub
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	@Override
	public Integer visitThisRef(ThisRef ref, Integer arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}

	@Override
	public Integer visitIdRef(IdRef ref, Integer arg) {
		// TODO Auto-generated method stub
		if (ref.decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl)ref.decl;
			if (fd.isStatic) {
				if (arg == null) {
					Machine.emit(Op.LOAD, Reg.SB, fd.entity.value);
				}
				else {
					Machine.emit(Op.STORE, Reg.SB, fd.entity.value);
				}
			}
			else {
				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, fd.entity.value);
				if (arg == null) {
					Machine.emit(Prim.fieldref);
				}
			}
		}
		else if (ref.decl instanceof MethodDecl) {
			int pos;
			MethodDecl md = (MethodDecl)ref.decl;
			if (md.isStatic) {
				pos = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Reg.OB, -1);
			}
			else {
				Machine.emit(Op.LOADA, Reg.OB, 0);
				pos = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Reg.CB, -1);
			}
			unpatchedCalls.put(pos, (MethodDecl)ref.decl);
		}
		else {
			if (arg == null) {
				Machine.emit(Op.LOAD, Reg.LB, ref.decl.entity.value);
			}
			else {
				Machine.emit(Op.STORE, Reg.LB, ref.decl.entity.value);
			}
		}
		return null;
	}

	@Override
	public Integer visitIxIdRef(IxIdRef ref, Integer arg) {
		// TODO Auto-generated method stub
		if (ref.decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl)ref.decl;
			if (fd.isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, fd.entity.value);
			}
			else {
				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, fd.entity.value);
				Machine.emit(Prim.fieldref);
			}
		}
		else {
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.entity.value);
		}
		ref.indexExpr.visit(this, null);
		if (arg == null) {
			Machine.emit(Prim.arrayref);
		}
		return null;
	}

	@Override
	public Integer visitQRef(QRef ref, Integer arg) {
		// TODO Auto-generated method stub
		if (ref.decl instanceof MemberDecl) {
			MemberDecl md = (MemberDecl)ref.decl;
			if (md.isStatic) {
				if (ref.decl instanceof FieldDecl) {
					if (arg == null) {
						Machine.emit(Op.LOAD, Reg.SB, md.entity.value);
					}
					else {
						Machine.emit(Op.STORE, Reg.SB, md.entity.value);
					}
				}
				else {
					int pos = Machine.nextInstrAddr();
					Machine.emit(Op.CALL, Reg.CB, -1);
					unpatchedCalls.put(pos, (MethodDecl)ref.decl);
				}
			}
			else {
				ref.ref.visit(this, null);
				if (ref.decl instanceof FieldDecl) {
					Machine.emit(Op.LOADL, md.entity.value);
					if (arg == null) {
						Machine.emit(Prim.fieldref);
					}
				}
				else {
					if (ref.ref.decl.type instanceof ClassType &&
					   ((ClassType)ref.ref.decl.type).className.spelling.equals("_PrintStream")) {
						Machine.emit(Op.POP, 1);
						Machine.emit(Prim.putintnl);
					}
					else {
						int pos = Machine.nextInstrAddr();
						Machine.emit(Op.CALLI, Reg.CB, -1);
						unpatchedCalls.put(pos, (MethodDecl)ref.decl);
					}
				}
			}
		}
		else {
			if (ref.ref.decl.type instanceof ArrayType && ref.id.spelling.equals("length")) {
				ref.ref.visit(this, null);
				Machine.emit(Prim.arraylen);
				return new Integer(-1);
			}
		}
		return null;
	}

	@Override
	public Integer visitIxQRef(IxQRef ref, Integer arg) {
		// TODO Auto-generated method stub
		if (ref.decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl)ref.decl;
			if (fd.isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, fd.entity.value);
			}
			else {
				ref.ref.visit(this, null);
				Machine.emit(Op.LOADL, fd.entity.value);
				Machine.emit(Prim.fieldref);
			}
		}
		ref.ixExpr.visit(this, null);
		if (arg == null) {
			Machine.emit(Prim.arrayref);
		}
		return null;
	}

	@Override
	public Integer visitIdentifier(Identifier id, Integer arg) {
		// TODO Auto-generated method stub
		//not visited
		return null;
	}

	@Override
	public Integer visitOperator(Operator op, Integer arg) {
		// TODO Auto-generated method stub
		//not visited
		return null;
	}

	@Override
	public Integer visitIntLiteral(IntLiteral num, Integer arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
		return null;
	}

	@Override
	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		// TODO Auto-generated method stub
		if (bool.spelling.equals("true")) {
			Machine.emit(Op.LOADL, 1);
		}
		else {
			Machine.emit(Op.LOADL, 0);
		}
		return null;
	}

	@Override
	public Integer visitNullLiteral(NullLiteral n, Integer arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, 0);
		return null;
	}
	
}

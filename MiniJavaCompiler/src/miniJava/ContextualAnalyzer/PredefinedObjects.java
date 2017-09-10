package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class PredefinedObjects {

	public void addDefinitionsToTree(AST ast) {
		Package p = ((Package)ast);
		
		////System
		
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		SourcePosition spoofPos = new SourcePosition(0, 0);
		fdl.add(
			new FieldDecl(
				false,
				true,
				true, 
				new ClassType(
					new Identifier(
						new Token(TokenType.ID, "_PrintStream", spoofPos)
					),
					spoofPos
				),
				"out",
				spoofPos
			)
		);
		
		////_PrintStream
		
		FieldDeclList fdl2 = new FieldDeclList();
		MethodDeclList mdl2 = new MethodDeclList();
		ParameterDeclList pl = new ParameterDeclList();
		pl.add(
			new ParameterDecl(
				new BaseType(TypeKind.INT, spoofPos),
				"n",
				spoofPos
			)
		);
		mdl2.add(
			new MethodDecl(
				new FieldDecl(
					false,
					true,
					false,
					new BaseType(TypeKind.VOID, spoofPos),
					"println",
					spoofPos
				),
				pl,
				new StatementList(),
				spoofPos
			)
		);
		
		////String
		
		FieldDeclList fdl3 = new FieldDeclList();
		MethodDeclList mdl3 = new MethodDeclList();
		
		boolean hasSystem = false;
		boolean hasPrintStream = false;
		boolean hasString = false;
		for (ClassDecl c : p.classDeclList) {
			if (c.name.equals("System")) {
				hasSystem = true;
			}	
			if (c.name.equals("_PrintStream")) {
				hasPrintStream = true;
			}
			if (c.name.equals("String")) {
				hasString = true;
			}
		}
		
		if (!hasSystem) p.classDeclList.add(new ClassDecl("System", fdl, mdl, spoofPos));
		if (!hasPrintStream) p.classDeclList.add(new ClassDecl("_PrintStream", fdl2, mdl2, spoofPos));
		if (!hasString) p.classDeclList.add(new ClassDecl("String", fdl3, mdl3, spoofPos));
	}
}

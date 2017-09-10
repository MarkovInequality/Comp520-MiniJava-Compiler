package miniJava;

import java.io.File;
import java.io.FileInputStream;
import java.io.PushbackInputStream;

import mJAM.ObjectFile;

import java.io.FileNotFoundException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.mJAMGenerator;
import miniJava.ContextualAnalyzer.IDTraversal;
import miniJava.ContextualAnalyzer.PredefinedObjects;
import miniJava.ContextualAnalyzer.TypeCheckTraversal;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {

	public static void main(String[] args) {
		
		PushbackInputStream inputStream = null;
		
		if (args.length == 0) {
			ErrorReporter.reportUnresolvableError("Error: No arguments");
		}
		else if (args.length > 1) {
			ErrorReporter.reportUnresolvableError("Error: Too many arguments");
		}
		else {
			try {
				inputStream = new PushbackInputStream(new FileInputStream(args[0]), 1);
			}
			catch (FileNotFoundException e) {
				ErrorReporter.reportUnresolvableError("Error: Input file " + args[0] + " not found");
			}
		}

		Scanner scanner = new Scanner(inputStream);
		Parser parser = new Parser(scanner);
		ASTDisplay treeDisplay = new ASTDisplay();
		IDTraversal idTraverse = new IDTraversal();
		TypeCheckTraversal tcTraverse = new TypeCheckTraversal();
		PredefinedObjects po = new PredefinedObjects();
		mJAMGenerator gen = new mJAMGenerator();
		
		AST ast = parser.parse();
		po.addDefinitionsToTree(ast);
		idTraverse.runIDPass(ast);
		tcTraverse.runTCPass(ast);
		
		if (tcTraverse.hasError) {
			System.exit(4);
		}
		
		//treeDisplay.showTree(ast);
		
		gen.runGenerationPass(ast);
		
		File javaFile = new File(args[0]);
		String objectCodeFileName = javaFile.getName();
		objectCodeFileName = objectCodeFileName.substring(0, objectCodeFileName.lastIndexOf('.')) + ".mJAM";
		//objectCodeFileName = "program.mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			System.exit(4);
		}
		else
			System.out.println("SUCCEEDED");
		
		System.exit(0);
	}
}
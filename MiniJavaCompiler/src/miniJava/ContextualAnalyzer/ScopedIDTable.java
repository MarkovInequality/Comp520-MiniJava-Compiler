package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.LinkedList;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;

public class ScopedIDTable {

	private LinkedList<HashMap<String, Declaration>> varDeclStack;
	public MethodDecl currentMethod;
	public ClassDecl thisClass;
	private Declaration varBeingDeclared;
	public boolean isInBranch;
	
	public ScopedIDTable() {
		varDeclStack = new LinkedList<HashMap<String, Declaration>>();
		currentMethod = null;
		thisClass = null;
		varBeingDeclared = null;
		isInBranch = false;
	}
	
	public void enterScope() {
		varDeclStack.add(new HashMap<String, Declaration>());
	}
	
	public void exitScope() {
		varDeclStack.removeLast();
	}
	
	public void startDeclaringVar(Declaration decl) {
		varBeingDeclared = decl;
	}
	
	public void addVarToScope(String name, Declaration decl) {
		if (varDeclStack.size() < 2) {
			if (varDeclStack.getLast().containsKey(name)) {
				reportNameConflictError(decl);
			}
			else {
				varDeclStack.getLast().put(name, decl);
			}
		}
		else {
			for (int i = varDeclStack.size() - 1; i >= 2; i--) {
				if (varDeclStack.get(i).containsKey(name)) {
					reportNameConflictError(decl);
				}
			}
			varDeclStack.getLast().put(name, decl);
		}
	}
	
	public Declaration findVarDecl(Identifier id) {
		for (int i = varDeclStack.size() - 1; i >= 0; i--) {
			if (varDeclStack.get(i).containsKey(id.spelling)) {
				return varDeclStack.get(i).get(id.spelling);
			}
		}
		return null;
	}
	
	public boolean varBeingDeclared(Declaration decl) {
		return varBeingDeclared == decl;
	}
	
	public Declaration findClassDecl(Identifier id) {
		return varDeclStack.getFirst().get(id.spelling);
	}
	
	public boolean hasClassDecl(Declaration decl) {
		return varDeclStack.getFirst().containsKey(decl.name);
	}
	
	public void varFinishedBeingDeclared(Declaration decl) {
		if (varBeingDeclared(decl)) {
			varBeingDeclared = null;
		}
	}
	
	public void removeVarFromScope(String name) {
		varDeclStack.getLast().remove(name);
	}
	
	private void reportNameConflictError(Declaration decl) {
		ErrorReporter.reportUnresolvableError("***Identification Error: The name '" + decl.name + "'is already in use", decl.posn);
	}
}

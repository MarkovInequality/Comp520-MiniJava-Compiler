package miniJava.ContextualAnalyzer;

import java.util.HashMap;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;

public class MembersTable {
	public HashMap<String, Declaration> membersTable;
	
	public MembersTable() {
		membersTable = new HashMap<String, Declaration>();
	}
	
	public void addMember(String name, Declaration decl) {
		if (membersTable.containsKey(name)) {
			reportNameConflictError(decl);
		}
		else {
			membersTable.put(name, decl);
		}
	}
	
	public Declaration getMember(Identifier id) {
		return membersTable.get(id.spelling);
	}
	
	private void reportNameConflictError(Declaration decl) {
		ErrorReporter.reportUnresolvableError("***Identification Error: The name '" + decl.name + "'is already in use", decl.posn);
	}
}

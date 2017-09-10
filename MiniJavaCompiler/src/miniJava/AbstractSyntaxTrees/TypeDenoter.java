/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }
    
    public boolean equals(TypeDenoter t) {
    	if (this.typeKind == TypeKind.CLASS && t.typeKind == TypeKind.CLASS) {
			return ((ClassType)this).equals((ClassType)t);
    	}
    	else if (this.typeKind == TypeKind.ARRAY && t.typeKind == TypeKind.ARRAY) {
    		return ((ArrayType)this).equals((ArrayType)t);
    	}
    	else if (this.typeKind == t.typeKind) {
    		return true;
    	}
    	else {
    		if (this.typeKind == TypeKind.NULL && t.typeKind == TypeKind.CLASS) {
        		return true;
        	}
    		else if (this.typeKind == TypeKind.CLASS && t.typeKind == TypeKind.NULL) {
    			return true;
    		}
    		else if (this.typeKind == TypeKind.ARRAY && t.typeKind == TypeKind.NULL) {
    			return true;
    		}
    		else if (this.typeKind == TypeKind.NULL && t.typeKind == TypeKind.ARRAY) {
    			return true;
    		}
    		return false;
    	}
    }
    
    public TypeKind typeKind;
    
}

        
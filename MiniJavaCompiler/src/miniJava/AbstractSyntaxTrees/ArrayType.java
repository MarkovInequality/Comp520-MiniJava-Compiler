/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }
	    
	    public boolean equals(ArrayType t) {
	    	if (this.eltType.typeKind == TypeKind.ARRAY && t.eltType.typeKind == TypeKind.ARRAY) {
	    		return this.eltType.equals(t.eltType);
	    	}
	    	else if (this.eltType.typeKind == TypeKind.CLASS && t.eltType.typeKind == TypeKind.CLASS) {
	    		return ((ClassType)this.eltType).equals(((ClassType)t.eltType));
	    	}
	    	else if (this.eltType.typeKind == t.eltType.typeKind) {
	    		return !(this.eltType.typeKind == TypeKind.UNSUPPORTED || this.eltType.typeKind == TypeKind.VOID);
	    	}
	    	else {
	    		return false;
	    	}
	    }

	    public TypeDenoter eltType;
	}


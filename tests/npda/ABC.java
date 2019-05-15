package npda;

abstract class AST1 { 
	AST1 parent;
}

abstract class Expr extends AST1 {
	Expr() { }
}


class BinaryExpr extends AST1 {
	BinaryExpr() {
	}
}

abstract class Stmt extends AST1 {
	Stmt() { }
}

class ExprStmt extends Stmt { 
	int f;
	ExprStmt(BinaryExpr E) {
		E.parent = this;
	}
}

class ABC { 

	public static void main(String[] args) {
		ABC a = new ABC();
		a.test();
	}
	void test() {
		BinaryExpr e = new BinaryExpr();
		ExprStmt s = new ExprStmt(e);
		System.console().printf("%d", ((ExprStmt)e.parent).f); //safe
	}
}

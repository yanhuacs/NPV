package npda;

public class vctest {
	public static void main(String[] args) {
		ErrorReporter rep = new ErrorReporter();
		CheckerAssign ca = new CheckerAssign(rep);
		AssignExpr asgn = new AssignExpr();
		ca.check(asgn);
		//check(asgn);
	}
	
	/*public static void check(AST ast) {
		AssignExpr e = ((AssignExpr) ast);
		//System.out.println(e.type.isErrorType());
		System.console().printf("%d", e.type.isErrorType());
		System.console().printf("%d", e.type.x);
	}*/
}

class CheckerAssign {
	private ErrorReporter reporter;

	public CheckerAssign(ErrorReporter reporter) {
		this.reporter = reporter;
	}

	public void check(AST ast) {
		AssignExpr e = ((AssignExpr) ast);
		//System.out.println(e.type.isErrorType());
		System.console().printf("%d", e.type.isErrorType()); //bug
		System.console().printf("%d", e.type.x); //bug
	}
}

class ErrorReporter {
	
}

class Type {
	boolean isErrorType() {
		return false;
	}
	int x;
}

class AST {
	Type type;
	AST() {
		//type = null;
	}
}

class AssignExpr extends AST {
	public AssignExpr() {
	}
}
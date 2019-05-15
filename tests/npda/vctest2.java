package npda;

public class vctest2 {
	public static void main(String[] args) {
		ErrorReporter_2 rep = new ErrorReporter_2();
		CheckerAssign_2 ca = new CheckerAssign_2(rep);
		asgn = new AssignExpr_2();
		ca.check(asgn);
	}
	
	static AssignExpr_2 asgn;
}

class CheckerAssign_2 {
	private ErrorReporter_2 reporter;

	public CheckerAssign_2(ErrorReporter_2 reporter) {
		this.reporter = reporter;
	}

	public void check(AST_2 ast) {
		AssignExpr_2 e = ((AssignExpr_2) ast);
		//System.out.println(e.type.isErrorType());
		System.console().printf("%d", e.type.isErrorType()); //bug
		System.console().printf("%d", e.type.x); //bug
	}
}

class ErrorReporter_2 {
	
}

class Type_2 {
	boolean isErrorType() {
		return false;
	}
	int x;
}

class AST_2 {
	Type_2 type;
	AST_2() {
		//type = null;
	}
}

class AssignExpr_2 extends AST_2 {
	public AssignExpr_2() {
	}
}
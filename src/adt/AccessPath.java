package adt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.StaticFieldRef;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Access path is as standard in Java pointer analysis,
 *  i.e., in the form of "x.y.z".
 */
public class AccessPath {
	private LinkedList<SootField> flds;
	private Value base;
	
	/**
	 * Deep clone by cloning the flds LinkedList.
	 */
	public AccessPath clone() {
		@SuppressWarnings("unchecked")
		LinkedList<SootField> flds_clone = (LinkedList<SootField>) flds.clone();
		AccessPath clone = new AccessPath(base, flds_clone);
		return clone;
	}
	
	public AccessPath() {
		flds = new LinkedList<>();
	}
	
	public AccessPath(Value v) {
		this();
		if (v instanceof InstanceFieldRef) {
			this.base = ((InstanceFieldRef) v).getBase();
			this.flds.push(((InstanceFieldRef) v).getField());
		}
		else if (v instanceof StaticFieldRef) {
			this.base = null;
			this.flds.push(((StaticFieldRef) v).getField());
		}
		else if (v instanceof CastExpr) {
			this.base = ((CastExpr) v).getOp();
		}
		else {
			this.base = v;
		}
	}
	
	public AccessPath(Value base, LinkedList<SootField> flds) {
		this.base = base;
		this.flds = flds;
	}
	
	public void addFieldFront(SootField fld) {
		flds.addFirst(fld);
	}
	
	public LinkedList<SootField> getFields() {
		return flds;
	}
	
	public void setBase(Value v) {
		base = v;
	}
	
	public Value getBase() {
		return base;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AccessPath)) {
			return false;
		}
		AccessPath ap = (AccessPath) obj;
		return sameValue(this.base, ap.base)
				&& Objects.equals(this.flds, ap.flds);
	}

	/**
	 * e.g., if this = a.b.c.d , other = a.b.c, then "this" subsumes other.
	 */
	public boolean subsume(AccessPath other) {
		if (this.getBase() != other.getBase())
			return false; 
		
		LinkedList<SootField> flds_this = this.getFields();
		LinkedList<SootField> flds_other = other.getFields();
		if (flds_this.size() < flds_other.size())
			return false;
		
		Iterator<SootField> I = flds_this.iterator();
		Iterator<SootField> J = flds_other.iterator();
		//Note that "other" is shorter than "this".
		while (J.hasNext()) {
			if (I.next() != J.next())
				return false;
		}
		
		return true;
	}
	
	
	public boolean subsume_dbg(AccessPath other) {
		if (this.getBase() != other.getBase()) {
			System.err.println("** 1. " + this.getBase() + " != " + other.getBase() + " Not OK!");
			return false; 
		}
		System.err.println("** 1. " + this.getBase() + " == " + other.getBase() + " OK!");
		
		LinkedList<SootField> flds_this = this.getFields();
		LinkedList<SootField> flds_other = other.getFields();
		if (flds_this.size() < flds_other.size()) {
			System.err.println("** 2. " + flds_this.size() + " <  " + flds_other.size() + " Not OK!");
			return false;
		}
		System.err.println("** 2. " + flds_this.size() + " >=  " + flds_other.size() + " OK!");
		
		Iterator<SootField> I = flds_this.iterator();
		Iterator<SootField> J = flds_other.iterator();
		//Note that "other" is shorter than "this".
		while (J.hasNext()) {
			SootField f1 = I.next();
			SootField f2 = J.next();
			if (f1 != f2) {
				System.err.println("** 3. " + f1 + " != " + f2 + " Not OK!");
				return false;
			}
			System.err.println("** 3. " + f1 + " == " + f2 + " OK!");
		}
		
		return true;
	}
	
	
	//Soot does not implement equal function for ArrayRef, so we have to handle it here.
	private boolean sameValue(Value v1, Value v2) {
		if (v1 == null || v2 == null)
			return v1 == v2;
		if (v1 instanceof ArrayRef && v2 instanceof ArrayRef) {
			ArrayRef a1 = (ArrayRef) v1;
			ArrayRef a2 = (ArrayRef) v2;
			return a1.getBase().equals(a2.getBase())
					&& a1.getIndex().equals(a2.getIndex());
		}
		return v1.equals(v2);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(base, flds);
	}

	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		res.append(base);
		for (SootField fld : flds) {
			res.append(".").append(fld.getName());
		}
		return res.toString();
	}
	
	/**
	 * @deprecated
	 */
	public static AccessPath getAccessPath(Value v, Unit u, SimpleLocalDefs du) {
		AccessPath ap = new AccessPath();
		System.out.println(v.getClass());
		
		if (v instanceof Local) {
			Local local = (Local) v;
			List<Unit> defs = du.getDefsOfAt(local, u);
			for (Unit def : defs) {
				System.out.println("DEF: " + def);
			}
		}
		
		if (v instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) v;
			Value base = ifr.getBase();
			SootField field = ifr.getField();
			System.out.println("BASE:  " + base);
			if (base instanceof Local) {
				Local local = (Local) base;
				List<Unit> defs = du.getDefsOfAt(local, u);
				for (Unit def : defs) {
					System.out.println("DEF: " + def);
				}
			}
			System.out.println("FIELD: " + field);
			System.out.println(field.getClass());
			System.out.println("CLASS: " + field.getDeclaringClass());
			System.out.println("NAME : " + field.getName());
			System.out.println("sign : " + field.getSignature());
		}
		
		return ap;
	}
	
	public static void buildAccessPath(Value v, Unit u, SimpleLocalDefs du, AccessPath res) {
		assert res != null;
		
		if (v instanceof Local) {
			res.setBase(v);
		}
		else if (v instanceof NewExpr) {
			res.setBase(v);
		}
		else if (v instanceof NewArrayExpr) {
			res.setBase(v);
		}
		else if (v instanceof ArrayRef) {
			res.setBase(v);
		}
		else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) v;
			Value base = ifr.getBase();
			
			SootField field = ifr.getField();
			res.addFieldFront(field);
			
			if (! (base instanceof Local)) {
				System.out.println("Base is Not LOCAL!!!");
				System.exit(0);
			}
			assert base instanceof Local; //TODO: what about non-local, is it possible?
			
			Local baseLoc = (Local) base;
			List<Unit> defs = du.getDefsOfAt(baseLoc, u);
			//Access path stops, when reaching Phi node.
			if (defs.size() == 1) { //TODO: what about multiple reaching definitions, i.e. phi node
				Unit def = defs.get(0);
				if (def instanceof AssignStmt) {
					Value lVal = ((AssignStmt) def).getLeftOp();
					Value rVal = ((AssignStmt) def).getRightOp();
					if (rVal instanceof InstanceFieldRef) {
						buildAccessPath(rVal, def, du, res);
					}
					else {
						res.setBase(lVal);
					}
				}
				else if (def instanceof IdentityStmt) { //In case def is a method parameter
					Value lVal = ((IdentityStmt) def).getLeftOp();
					res.setBase(lVal);
				}
			}
		}
	}	
}

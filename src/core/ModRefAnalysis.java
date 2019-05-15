package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import solver.Helper;
import pag.node.GNode;
import pta.pts.PTSetInternal;
import pta.pts.PTSetVisitor;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.jimple.toolkits.callgraph.Targets;
import soot.jimple.toolkits.pointer.SideEffectAnalysis;
import soot.util.Chain;

public class ModRefAnalysis {
	
	private static ModRefAnalysis instance = null;
	
	private ModRefAnalysis() { 
		bkReachable = new HashMap<>();
		store2Method = new HashMap<>();
		storeArray2Method = new HashMap<>();
		storeStatic2Method = new HashMap<>();
		buffer_forSoot = new HashMap<>();
		buffer_forDruid = new HashMap<>();
		init();
	}
	
	public static ModRefAnalysis getInstance() {
		if (instance == null)
			instance = new ModRefAnalysis();
		return instance;
	}
	
	private void init() {
		for (SootClass cls : Scene.v().getClasses()) {
			for (SootMethod m : cls.getMethods()) {
				if (!m.hasActiveBody()) continue;
				for (Unit u : m.getActiveBody().getUnits()) {
					if (Helper.getInstance().isPutField(u))
						store2Method.put(u, m);
					else if (Helper.getInstance().isArrayPutField(u))
						storeArray2Method.put(u, m);
					else if (Helper.getInstance().isStaticPutField(u)) {
						storeStatic2Method.put(u, m);
					}
				}
			}
		}
	}
	
	private HashMap<Unit, SootMethod> store2Method;
	private HashMap<Unit, SootMethod> storeArray2Method;
	private HashMap<Unit, SootMethod> storeStatic2Method;
	private HashMap<Node, HashSet<SootMethod>> buffer_forSoot;
	private HashMap<GNode, HashSet<SootMethod>> buffer_forDruid;
	
	private boolean res_hasModRef;
	private SootMethod m_hasModRef;
	
	public boolean hasModRef(SootMethod m, PointsToSet pts) {
		if (true) return true;
		if (pts == null) return true;
		
		res_hasModRef = false;
		m_hasModRef = m;
		if (pts instanceof PTSetInternal) {
			PTSetInternal pts_in = (PTSetInternal) pts;
			pts_in.forall( 
					new PTSetVisitor() {
						public final void visit( GNode n ) {
							if (!buffer_forDruid.containsKey(n)) {
								HashSet<SootMethod> modSet = new HashSet<>();
								computeModSet(n, modSet);
								buffer_forDruid.put(n, modSet);
							}
							if (buffer_forDruid.get(n).contains(m_hasModRef))
								res_hasModRef = true;
						}
					} );
		}
		else if (pts instanceof PointsToSetInternal) {
			PointsToSetInternal pts_in = (PointsToSetInternal) pts;
			pts_in.forall( 
					new P2SetVisitor() {
						public final void visit( Node n ) {
							if (!buffer_forSoot.containsKey(n)) {
								HashSet<SootMethod> modSet = new HashSet<>();
								computeModSet(n, modSet);
								buffer_forSoot.put(n, modSet);
							}
							if (buffer_forSoot.get(n).contains(m_hasModRef))
								res_hasModRef = true;
						}
					} );
		}
		return res_hasModRef;
	}
	
	private void computeModSet(Node n, HashSet<SootMethod> res) {
		PointsToAnalysis pta = Helper.getInstance().getPTA();
		HashSet<SootMethod> seed = new HashSet<>();
		for (Unit u : store2Method.keySet()) {
			Value base = Helper.getInstance().getPutFieldBase(u);
			SootField fld = Helper.getInstance().getPutFieldFld(u);
			if (base == null || fld == null) continue;
			if (base instanceof Local) {
				Local base_local = (Local) base;
				PointsToSetInternal pts = (PointsToSetInternal) pta.reachingObjects(base_local, fld);
				if (pts.contains(n))
					seed.add(store2Method.get(u));
			}
		}
		
		for (Unit u : storeArray2Method.keySet()) {
			Value base = Helper.getInstance().getArrayPutFieldBase(u);
			if (base == null) continue;
			if (base instanceof Local) {
				Local base_local = (Local) base;
				PointsToSet pts_base = pta.reachingObjects(base_local); 
				PointsToSetInternal pts = (PointsToSetInternal) pta.reachingObjectsOfArrayElement(pts_base);
				if (pts.contains(n))
					seed.add(storeArray2Method.get(u));
			}
		}
		
		for (Unit u : storeStatic2Method.keySet()) {
			SootField fld = Helper.getInstance().getStaticPutFieldFld(u);
			if (fld == null || !fld.isStatic()) continue;
			PointsToSetInternal pts = (PointsToSetInternal) pta.reachingObjects(fld);
			if (pts.contains(n))
				seed.add(storeStatic2Method.get(u));
		}
		
		res.addAll(seed);
		for (SootMethod m : seed) {
			HashSet<SootMethod> propagted = computeBkReachable(m);
			res.addAll(propagted);
		}
	}
	
	private void computeModSet(GNode n, HashSet<SootMethod> res) {
		PointsToAnalysis pta = Helper.getInstance().getPTA();
		HashSet<SootMethod> seed = new HashSet<>();
		for (Unit u : store2Method.keySet()) {
			Value base = Helper.getInstance().getPutFieldBase(u);
			SootField fld = Helper.getInstance().getPutFieldFld(u);
			if (base == null || fld == null) continue;
			if (base instanceof Local) {
				Local base_local = (Local) base;
				PTSetInternal pts = (PTSetInternal) pta.reachingObjects(base_local, fld);
				if (pts.contains(n))
					seed.add(store2Method.get(u));
			}
		}
		
		for (Unit u : storeArray2Method.keySet()) {
			Value base = Helper.getInstance().getArrayPutFieldBase(u);
			if (base == null) continue;
			if (base instanceof Local) {
				Local base_local = (Local) base;
				PointsToSet pts_base = pta.reachingObjects(base_local); 
				PTSetInternal pts = (PTSetInternal) pta.reachingObjectsOfArrayElement(pts_base);
				if (pts.contains(n))
					seed.add(storeArray2Method.get(u));
			}
		}
		
		for (Unit u : storeStatic2Method.keySet()) {
			SootField fld = Helper.getInstance().getStaticPutFieldFld(u);
			if (fld == null || !fld.isStatic()) continue;
			PTSetInternal pts = (PTSetInternal) pta.reachingObjects(fld);
			if (pts.contains(n))
				seed.add(storeStatic2Method.get(u));
		}
		
		res.addAll(seed);
		for (SootMethod m : seed) {
			HashSet<SootMethod> propagted = computeBkReachable(m);
			res.addAll(propagted);
		}
	}
	
	private HashSet<SootMethod> computeBkReachable(SootMethod m) {
		if (bkReachable.containsKey(m)) {
			return bkReachable.get(m); //cache hits
		}
		
		//cache misses
		CallGraph cg = Helper.getInstance().getCallGraph();
		HashSet<SootMethod> reachable = new HashSet<>();
		ArrayList<SootMethod> nxtRound = new ArrayList<>();
		ArrayList<SootMethod> curRound = new ArrayList<>();
		curRound.add(m);
		reachable.add(m);
		while(curRound.isEmpty() == false) {
			nxtRound.clear();
			for (SootMethod tgt : curRound) {
				Iterator<MethodOrMethodContext> Isrc = new Sources(cg.edgesInto(tgt));
				while (Isrc.hasNext()) {
					SootMethod src = (SootMethod)Isrc.next();
					if (reachable.contains(src) == false) {
						reachable.add(src);
						nxtRound.add(src);
					}
				}
			}
			ArrayList<SootMethod> tmp = curRound;
			curRound = nxtRound;
			nxtRound = tmp;
		}
		bkReachable.put(m, reachable);
		return reachable;
	}
	
	private HashMap<SootMethod, HashSet<SootMethod>> bkReachable;
}

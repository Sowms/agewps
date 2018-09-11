package agewps;


import java.util.ArrayList;
//import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
//import org.ejml.interfaces.linsol.LinearSolver;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.factory.LinearSolverFactory;

public class PredicateController {
	public static HashMap<String, Person> actors = new HashMap<String, Person>();
	public static HashMap<String, String> varMap = new HashMap<String, String>();
	public static boolean solveFlag = true;
	public static void reset() {
		actors = new HashMap<String, Person>();
		varMap = new HashMap<String, String>();
		solveFlag = true;
	}
	public static String getYear(String predicate) {
		if (predicate.startsWith("shift")) {
			 if (predicate.startsWith("shiftPlus")) {
				 String num = predicate.replaceAll("shiftPlus", "");
				 num = num.replaceAll("\\(", "");
				 num = num.replaceAll("\\)", "");
				 if (num.matches("[0-9]+"))
					 return Age.shiftPlus(Integer.parseInt(num))+"";
				 return Age.currentYear + num;
			 }
			 if (predicate.startsWith("shiftMinus")) {
				 String num = predicate.replaceAll("shiftMinus", "");
				 num = num.replaceAll("\\(", "");
				 num = num.replaceAll("\\)", "");
				 if (num.matches("[0-9]+"))
						return Age.shiftMinus(Integer.parseInt(num))+"";
				 return Age.currentYear + " - " + num;
			 }
		}
		if (predicate.startsWith("current"))
			 return Age.currentYear+"";
		return predicate;
	}
	public static String equationCreator (ArrayList<ArrayList<String>> predicates) {
		String eqn = "";
		Iterator<ArrayList<String>> it = predicates.iterator();
		while (it.hasNext()) {
			ArrayList<String> e = it.next();
			String pName = e.get(0);
			List<String> arg = e.subList(1, e.size());
			if (pName.equals("thing")) {
				actors.put(arg.get(0),new Person(arg.get(0)));
			}
			if (pName.equals("difference")) {
				 String p1 = arg.get(0);
				 String p2 = arg.get(2);
				 String y1 = arg.get(1);
				 String y2 = arg.get(3);
				 String diff = arg.get(4);
				 String year1 = getYear(y1), year2 = getYear(y2);
				 eqn = eqn + Age.diff(actors.get(p1), year1, actors.get(p2), year2) + " = " + diff + "\n";
				 
			}
			else if (pName.equals("ratio")) {
				 String p1 = arg.get(0);
				 String p2 = arg.get(2);
				 String y1 = arg.get(1);
				 String y2 = arg.get(3);
				 String diff = arg.get(4);
				 String year1 = getYear(y1), year2 = getYear(y2);
				 eqn = eqn + Age.ratio(actors.get(p1), year1, actors.get(p2), year2) + " = " + diff + "\n";
			}
			else if (pName.startsWith("sum")) {
				String sum = arg.get(arg.size() - 1);
				Person[] p = new Person[(arg.size()-1)/2]; String[] y = new String[(arg.size()-1)/2];
				for (int i = 0, j = 0; j < arg.size()-1; j += 2, i++) {
					String p1 = arg.get(j);
					y[i] = getYear(arg.get(j+1));
					p[i] = actors.get(p1);
				}
				eqn = eqn + Age.sum(p, y) + " = " + sum + "\n";
			}
			else if (pName.startsWith("product")) {
				String sum = arg.get(arg.size() - 1);
				Person[] p = new Person[(arg.size()-1)/2]; String[] y = new String[(arg.size()-1)/2];
				for (int i = 0, j = 0; j < arg.size()-1; j += 2, i++) {
					String p1 = arg.get(j);
					y[i] = getYear(arg.get(j+1));
					p[i] = actors.get(p1);
				}
				eqn = eqn + Age.prod(p, y) + " = " + sum + "\n";
			}
			else if (pName.startsWith("age")) {
				String p = arg.get(0);
				String y = getYear(arg.get(1));
				String a = arg.get(2);
				int age = 0;
				if (a.matches("[0-9]+")) {
						age = Integer.parseInt(a);
						if (y.matches("[0-9]+"))
							actors.get(p).birthYear(age, Integer.parseInt(y));
				}
				eqn = eqn + p+".age("+y+")" + " = " + a + "\n";
			}
		}
			return eqn;
	}
	public static DenseMatrix64F solveBareEqn(double[][] a, double[][] b) {
		DenseMatrix64F A = new DenseMatrix64F(a);
		DenseMatrix64F B = new DenseMatrix64F(b);
		DenseMatrix64F X = new DenseMatrix64F(new double[A.numRows][1]);
		
		LinearSolver<DenseMatrix64F> solver;
		solver = LinearSolverFactory.linear(A.numRows);
		if( !solver.setA(A) )
            throw new RuntimeException("Solver failed");
		solver.solve(B, X);
		//System.out.println(X);
		//System.out.println(A);
		//System.out.println(B);
		return X;
	}
	public static void solveEqn(String eqn) {
		//Collect all age predicates
		Pattern pattern = Pattern.compile("[a-z]+\\.age\\([a-z0-9]+\\)");
        Matcher matcher = pattern.matcher(eqn);
        String varBase = "x";
        int varCount = 1;
        //pre-processing
        while (matcher.find()) {
        	String var = matcher.group();
        	String name = var.replaceAll("\\.age\\([a-z0-9]+\\)", "");
        	String year = var.replaceAll("[a-z]+\\.age\\(", "");
        	year = year.replaceAll("\\)", "");
        	//System.out.println(name+"|"+year);
        	String varName = varBase + varCount;
        	if (!varMap.containsKey(var)) {
        		varMap.put(var, varName);
        		varCount++;
        	}
        	varName = varBase + varCount;
        	if (!varMap.containsKey(name+".birthYear")) { 
        		varMap.put(name+".birthYear", varName);
        		varCount++;
        	}
        }
        
        
        Iterator<Entry<String, String>> it = varMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        eqn = eqn.replace(pair.getKey(), pair.getValue());
	        String key = pair.getKey();
	        if (!pair.getKey().contains("birthYear")) {
	        	String name = key.replaceAll("\\.age\\([a-z0-9]+\\)", "");
	        	String year = key.replaceAll("[a-z]+\\.age\\(", "");
	        	year = year.replaceAll("\\)", "");
	        	String var1	= varMap.get(name+".birthYear");
	        	eqn = eqn + var1 + " = " + year + " - " + pair.getValue() + "\n";
	        }
	    }
	    //System.out.println(eqn+"\n"+varCount);
	    pattern = Pattern.compile("[a-wy-z]+");
	    matcher = pattern.matcher(eqn);
	    while (matcher.find()) {
	    	String varName = varBase + varCount;
	    	String name = matcher.group();
	    	if (!varMap.containsKey(name)) {
	    		varMap.put(name, varName);
	    		eqn = eqn.replace(name, varName);
	    		varCount++;
	    	}
	    }
	    
	    //System.out.println(eqn);
	    //System.out.println(varMap);
	    int dim = varMap.size();
    	//double[][] a = {{1,0,0,0,0,0,0}, {0,0,1,0,0,0,0}, {0,0,0,0,1,-2,0}, {0,0,0,1,0,1,-1}, {1,1,0,0,0,0,0}, {0,0,1,1,0,0,0},{0,1,0,0,1,0,-1}};
    	//double[][] b = {{26},{4},{0},{0},{2017},{2017},{0}};
	    double[][] a = new double[dim][dim];
	    double[][] b = new double[dim][1];
    	String[] equations = eqn.split("\\n");
    	int i = 0;
    	for (String equation : equations) {
    		//String eq = EquationSimplifier.simplify(equation);
    		////System.out.println(equation);
    		pattern = Pattern.compile("x[0-9]+");
    	    matcher = pattern.matcher(equation);
    	    int index = 0;
    	    if (equation.contains("/")) {
    	    	while (matcher.find()) {
    	    		String var = matcher.group();
    	    		index = Integer.parseInt(var.substring(1)) - 1;
    	    			if (equation.indexOf(var) < equation.indexOf("/"))
    	    				a[i][index] = 1;
    	    			else {
    	    				String expr = equation.split(" = ")[1];
    	    				if (!expr.contains("x"))
    	    					a[i][index] = -(Double.parseDouble(expr));
    	    				else {
    	    					solveFlag = false; //quad
    	    					return;
    	    				}
    	    			}
    	    			//System.out.println(i + "|" + index + "|" + a[i][index]);
    	    	}
    	    }
    	    else {
    	    	boolean plusFlag = true;
    	    	//System.out.println(equation);
    	    	ArrayList<String> comp = new ArrayList<String>();
    	    	for (String token : equation.split(" ")) {
    	    		comp.add(token);
    	    	}
    	    	for (String token : equation.split(" ")) {
    	    		if (token.startsWith("x")) {
    	    			index = Integer.parseInt(token.substring(1)) - 1;
    	    			if (comp.indexOf(token) < comp.indexOf("=") && plusFlag)
    	    				a[i][index] = 1;
    	    			else if (comp.indexOf(token) < comp.indexOf("=") && !plusFlag)
    	    				a[i][index] = -1;
    	    			else if (comp.indexOf(token) > comp.indexOf("=") && plusFlag)
    	    				a[i][index] = -1;
    	    			else if (comp.indexOf(token) > comp.indexOf("=") && !plusFlag)
    	    				a[i][index] = 1;
    	    		}
    	    		if (token.startsWith("+") || token.startsWith("="))
    	    			plusFlag = true;
    	    		else if (token.equals("-"))
    	    			plusFlag = false;
    	    		else if (token.matches("[0-9]+(\\.[0-9]+)*")) {
    	    			double num = Double.parseDouble(token);
    	    			if (comp.indexOf(token) < comp.indexOf("=") && plusFlag)
    	    				b[i][0] -= num;
    	    			else if (comp.indexOf(token) < comp.indexOf("=") && !plusFlag)
    	    				b[i][0] += num;
    	    			else if (comp.indexOf(token) > comp.indexOf("=") && plusFlag)
    	    				b[i][0] += num;
    	    			else if (comp.indexOf(token) > comp.indexOf("=") && !plusFlag)
    	    				b[i][0] -= num;
    	    		}
    	    		
    		    	
    	    	}
    	    	
    	    }
    	    
    	    i++;
    	}

    	DenseMatrix64F X = solveBareEqn(a, b);
    	//System.out.println(X);
    	if (X.toString().contains("NaN")) {
    		solveFlag = false;
    	}
    	for (double val : X.data) {
    		if (val <= 0.0)
    			solveFlag = false;
    	}
    	it = varMap.entrySet().iterator();
    	HashMap<String, String> temp = new HashMap<String, String>(); 
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        String val = pair.getKey();
	        String var = pair.getValue();
	        int x = Integer.parseInt(var.substring(1));
	        double ans = (double) X.get(x-1, 0);
	        if (val.contains("birthYear")) {
	        	String name = val.replace(".birthYear", "");
	        	actors.get(name).birthyear(ans);
	        }
	        if (!val.contains("age") && !val.contains("birthYear")) {
	        	//System.out.println(val + " | " + ans);
	        	temp.put(val, ans+"");
	        }
	    }
	    it = temp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
	        varMap.put(pair.getKey(), pair.getValue());
	    }
	    //System.out.println(varMap);
	}
	public static String query(ArrayList<String> ques) throws Exception{
		String pred = ques.get(0);
		List<String> arg = ques.subList(1, ques.size());
		if (pred.startsWith("age")) {
			String per = arg.get(0);
			String year = getYear(arg.get(1));
			if (year.matches("[0-9]+"))
				return actors.get(per).age(Integer.parseInt(year)) + "";
			if (year.matches("currentYear")) 
				return actors.get(per).age(Age.currentYear) + "";
			Pattern pattern = Pattern.compile("[a-z]+");
    	    Matcher matcher = pattern.matcher(year);
    	    while (matcher.find()) {
    	    	String phrase = matcher.group();
    	    	//System.out.println(phrase+"|"+varMap);
    	    	year.replace(phrase, varMap.get(phrase));
    	    }
    	    ScriptEngineManager mgr = new ScriptEngineManager();
			ScriptEngine engine = mgr.getEngineByName("JavaScript");
			int y = (int) engine.eval(year); 
			return actors.get(per).age(y) + "";
		} else if (pred.startsWith("birthYear")) {
			String per = arg.get(0);
				return actors.get(per).birthYear+"";
		} else if (pred.startsWith("offset")) {
			String year = arg.get(0);
			if (year.matches("[0-9]+")) {
				int y = Integer.parseInt(year);
				return Math.abs(y - Age.currentYear)+"";
			}
			else {
				double y = Double.parseDouble(varMap.get(year));
				return Math.abs(y - Age.currentYear)+"";
			}
		} else if (pred.startsWith("difference")) {
			String p1 = arg.get(0);
			String p2 = arg.get(2);
			String y1 = arg.get(1);
			String y2 = arg.get(3);
			String year1 = getYear(y1), year2 = getYear(y2);
			return (actors.get(p1).age(Integer.parseInt(year1)) - actors.get(p2).age(Integer.parseInt(year2))) + ""; 
		} else if (pred.startsWith("ratio")) {
			String p1 = arg.get(0);
			String p2 = arg.get(2);
			String y1 = arg.get(1);
			String y2 = arg.get(3);
			String year1 = getYear(y1), year2 = getYear(y2);
			return (actors.get(p1).age(Integer.parseInt(year1)) / (double)(actors.get(p2).age(Integer.parseInt(year2)))) + ""; 
		} else if (pred.startsWith("sum")) {
			String p1 = arg.get(0);
			String p2 = arg.get(2);
			String y1 = arg.get(1);
			String y2 = arg.get(3);
			String year1 = getYear(y1), year2 = getYear(y2);
			////System.out.println(actors.get(p1).age(Integer.parseInt(year1)));
			return (actors.get(p1).age(Integer.parseInt(year1)) + actors.get(p2).age(Integer.parseInt(year2))) + ""; 
		}
		return varMap.get(pred);
		
	}
	public static ArrayList<String> convStat(String statement) {
		ArrayList<String> pred = new ArrayList<String>();
		int l = statement.length();
		String arg = "";
		int i = 0;
		for (i = 0; i < l; i++) {
			if (statement.charAt(i) == '(') {
				break;
			}
			arg = arg + statement.charAt(i);
		}
		pred.add(arg);
		arg = "";
		i++;
		for (int j = i; j < l; j++) {
			if (statement.charAt(j) == ',' || (statement.charAt(j) == ')' && j == l-1)) {
				pred.add(arg);
				arg = "";
				continue;
			}
			arg = arg + statement.charAt(j);
		}
		////System.out.println(pred);
		return pred;
	}
	public static ArrayList<ArrayList<String>> getPredicates(String program) {
		ArrayList<ArrayList<String>> pred = new ArrayList<ArrayList<String>>();
		for (String stmt : program.split("\\n")) {
			pred.add(convStat(stmt));
		}
		return pred;
	}
	public static void main(String[] args) throws Exception {
		ArrayList<ArrayList<String>> pred = new ArrayList<ArrayList<String>>();
		/*String program = "person(john)\n" +
						 "person(mary)\n" +
						 "age(john,currentYear,26)\n" + 
						 "age(mary,currentYear,4)\n" +
						 "ratio(john,year,mary,year,2)\n";*/
		/*String program = "person(adam)\n" +
				 		 "person(brian)\n" +
				 		 "diff(brian,currentYear,adam,currentYear,20)\n" + 
				 		 "ratio(brian,shiftPlus(2),adam,shiftPlus(2),2)\n" +
				 		 "age(adam,currentYear,agea)\n" + 
				 		"age(brian,currentYear,ageb)\n";*/
		String program = "thing(father)\nthing(ronit)\nratio(father,currentYear,ronit,currentYear,3)\nratio(father,shiftPlus(8),ronit,shiftPlus(8),2.5)\n";

		pred = getPredicates(program);
		/*ArrayList<String> p1 = convStat("person(john)");
		pred.add(p1);
		ArrayList<String> p2 = convStat("age(john,currentYear,26)");
		ArrayList<String> p3 = new ArrayList<String>(
			    Arrays.asList("person", "mary"));
		pred.add(p3);
		pred.add(p2);
		ArrayList<String> p6 = new ArrayList<String>(
			    Arrays.asList("age", "mary", "currentYear", "4"));
		pred.add(p6);
		//ArrayList<String> p4 = new ArrayList<String>(
			//    Arrays.asList("diff", "mary", "currentYear", "john", "currentYear", "20"));
		//pred.add(p4);
		ArrayList<String> p5 = new ArrayList<String>(
			    Arrays.asList("ratio", "john", "year", "mary", "year", "2"));
		pred.add(p5);*/
		String eqn = equationCreator(pred);
		System.out.println(eqn);
		solveEqn(eqn);
		//queries
		/*ArrayList<String> q1 = convStat("age(john,currentYear)");
		//System.out.println(query(q1));
		ArrayList<String> q2 = convStat("age(mary,shiftPlus(5))");
		//System.out.println(query(q2));
		ArrayList<String> q3 = convStat("year");
		//System.out.println(query(q3));
		ArrayList<String> q4 = convStat("offset(year)");
		//System.out.println(query(q4));*/
		ArrayList<String> q5 = convStat("ratio(father,shiftPlus(16),ronit,shiftPlus(16))");
		System.out.println(query(q5));
		
		/*//System.out.println(query(convStat("age(adam,currentYear)")));
		//System.out.println(query(convStat("age(brian,currentYear)")));
		//System.out.println(query(convStat("age(brian,shiftPlus(2))")));*/
	}
}

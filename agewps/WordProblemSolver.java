package agewps;

import java.util.ArrayList;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class WordProblemSolver {

	
	public static String solveRepr(String program) throws Exception {
		String ans = "";
		String assertions = "";
	    ArrayList<String> queries = new ArrayList<String>();
	    for (String line : program.split("\n")) {
	    	if (!line.endsWith("?"))
	    		assertions = assertions + line.replaceAll(" ", "") + "\n";
	    	else
	    		break;
	    }
	    for (String line : program.split("\n")) {
	    	if (!line.endsWith("?"))
	    		continue;
	    	queries.add(line.substring(0, line.indexOf("?")).replaceAll(" ", ""));
	    }
		String input = assertions;
		System.out.println(input);
    	ArrayList<ArrayList<String>> pred = PredicateController.getPredicates(input);
    	String eqn = PredicateController.equationCreator(pred);
    	PredicateController.solveEqn(eqn);
	    for (String query : queries) {
	    	System.out.println(query);
	    	ArrayList<String> q = PredicateController.convStat(query);
	    	String qAns = PredicateController.query(q);
	    	if (!qAns.equals("-1"))
	    		ans = ans + qAns + ", ";
	    }
		return ans;
	}
	public static String solve(String wordProblem, StanfordCoreNLP pipeline, float[] param) throws Exception {
		String ans = "";
		wordProblem = CorefResolver.coref(wordProblem, pipeline);
		wordProblem = Preprocessor.convert(wordProblem, pipeline);
		System.out.println(wordProblem);
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,diffcount,0})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,ratiocount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,prodcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,sumcount})\n";
		String program = "";
		if (param == null)
			program = SymbolicPredicateGenerator.genProgram(wordProblem, allowed);
		ans = solveRepr(program);
		return ans;
	}
	public static void main(String[] args) throws Exception {
		String wordProblem = "In 56 years, Kevin will be 9 times as old as he is right now. How old is he right now?";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,parse,mention,coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    System.out.println(solve(wordProblem, pipeline, null));
		

	}

}

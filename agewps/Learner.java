package agewps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

class Derivation {
	String candidate;
	float score;
}
public class Learner {
	
	public static ArrayList<String> truePrograms = new ArrayList<String>();
	public static HashMap<String,ArrayList<String>> pos = new HashMap<String,ArrayList<String>>(); 
	public static HashMap<String,ArrayList<String>> ner = new HashMap<String,ArrayList<String>>();
	
	public static boolean goalTest(String candidate, String allQueries, String answer) throws Exception {
		boolean isAns = false;
		PredicateController.reset();
		ArrayList<ArrayList<String>> pred = PredicateController.getPredicates(candidate);
		String eqn = PredicateController.equationCreator(pred);
		PredicateController.solveEqn(eqn);
		if (!PredicateController.solveFlag)
			return false;
		String[] queries = allQueries.split("\n");
		String ans = "";
		for (String query : queries) {
	    	ArrayList<String> q = PredicateController.convStat(query);
	    	String qAns = PredicateController.query(q);
	    	if (!(qAns == null) && !qAns.equals("-1"))
	    		ans = ans + qAns + ", ";
	    }
		isAns = (TextDatasetTester.checkAns(ans, answer));
		return isAns;
	}
	public static boolean isConsistent(String candidate, String allQueries) {
		String[] queries = allQueries.split("\n");
		String[] candidates = candidate.split("\n");
		ArrayList<String> progActors = new ArrayList<String>();
		ArrayList<String> queryActors = new ArrayList<String>();
		for (String query : queries) {
			if (!query.contains("("))
				continue;
			String pred = query.substring(0, query.indexOf("("));
			String args = query.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			if (pred.equals("age"))
				queryActors.add(arguments.get(0));
			else if (!pred.equals("offset")) {
				queryActors.add(arguments.get(0));
				queryActors.add(arguments.get(2));
			}
	    }
		for (String c : candidates) {
			if (c.isEmpty())
				continue;
			String pred = c.substring(0, c.indexOf("("));
			String args = c.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			//System.out.println(arguments);
			if (pred.equals("age"))
				progActors.add(arguments.get(0));
			else {
				progActors.add(arguments.get(0));
				progActors.add(arguments.get(2));
			}
		}
		for (String actor : queryActors) {
			//System.out.println(actor+"|"+progActors);
			if (!progActors.contains(actor))
				return false;
		}
		//year check
		progActors = new ArrayList<String>();
		queryActors = new ArrayList<String>();
		for (String query : queries) {
			if (!query.contains("(")) {
				queryActors.add(query);
				continue;
			}
			String pred = query.substring(0, query.indexOf("("));
			String args = query.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			if (pred.equals("offset"))
				queryActors.add(arguments.get(0));
			else if (pred.equals("age")) {
				String year = arguments.get(1);
				if (!year.equals("currentYear") && !year.matches("\\d+"))
					queryActors.add(year);
			}
			else {
				String year1 = arguments.get(1), year2 = arguments.get(3);
				if (!year1.equals("currentYear") && !year1.matches("\\d+"))
					queryActors.add(year1);
				if (!year2.equals("currentYear") && !year2.matches("\\d+"))
					queryActors.add(year2);
			}
	    }
		for (String c : candidates) {
			if (c.isEmpty())
				continue;
			String pred = c.substring(0, c.indexOf("("));
			String args = c.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			if (pred.equals("age")) {
				String year = arguments.get(1);
				if (!year.equals("currentYear") && !year.matches("\\d+"))
					progActors.add(year);
			}
			else {
				String year1 = arguments.get(1), year2 = arguments.get(3);
				if (!year1.equals("currentYear") && !year1.matches("\\d+"))
					progActors.add(year1);
				if (!year2.equals("currentYear") && !year2.matches("\\d+"))
					progActors.add(year2);
			}
		}
		for (String actor : queryActors) {
			//System.out.println("cons"+actor+"|"+progActors);
			if (!progActors.contains(actor) || progActors.isEmpty())
				return false;
		}
		return true;
	}
	//preprocessed word problem as input
	public static ArrayList<String> getBeam(String wordProblem, float[] param, String allowed, String qAllowed, String ans) throws Exception {
		ArrayList<String> beam = new ArrayList<String>();
		truePrograms = new ArrayList<String>();
		CandidateProgramGenerator.pos = new HashMap<String, ArrayList<String>>();
		CandidateProgramGenerator.ner = new HashMap<String, ArrayList<String>>();
		ArrayList<String> candidates = CandidateProgramGenerator.genCandidates(wordProblem, allowed, "", "year");
		pos = CandidateProgramGenerator.pos; 
		ner = CandidateProgramGenerator.ner;
		ArrayList<String> answers = new ArrayList<>();
		for (String a : ans.split(","))
			answers.add(a);
		//System.out.println(answers);
		//first level
		boolean isSolutionFound = false;
		Comparator<Derivation> comparator = new Comparator<Derivation>() {
            @Override
            public int compare(Derivation a, Derivation b) {
                return a.score < b.score ? -1 : a.score == b.score ? 0 : 1;
            }
        };
        ArrayList<Derivation> derivations = new ArrayList<Derivation>();
        int counter =  0;
		while (!isSolutionFound && counter < 1) {
			//first time
			System.out.println(beam);
			if (beam.isEmpty()) {
				System.out.println(candidates.size());
				for (String candidate : candidates) {
					String[] check = new String[1];
					check[0] = candidate.replace(", ", ",");
					Derivation candDerivation = new Derivation();
					List<String> cand = Arrays.asList(check);
					float score = 0;
					//try {
					score = Model.score(param, cand, wordProblem, pos, ner);
					candDerivation.candidate = candidate;
					candDerivation.score = score;
					if (checkIn(candDerivation, derivations))
						continue;
					if (derivations.size() < 1000)
						derivations.add(candDerivation);
					else {
						Collections.sort(derivations, comparator);
						Derivation min = derivations.get(0);
						if (min.score < score) {
							derivations.remove(min);
							derivations.add(candDerivation);
						}
					} //}
					//catch (Exception e) {
						//System.out.println(e.getMessage());
					//}
					System.out.println(candidate + "|" + derivations.size() + "|" + score);
					//Thread.sleep(1000);
				}
			}
			else {
				ArrayList<Derivation> copy = new ArrayList<>();
				for (Derivation d : derivations) {
					copy.add(d);
				}
				for (Derivation derivation : copy) {
					String base = derivation.candidate;
					String[] init = base.split("\n");
					for (String candidate : beam) {
						if (base.replaceAll("\n", "").contains(candidate.replaceAll("\n", "")))
							continue;
						//candidate = candidate.replaceAll(", ", ", ");
						//candidate = candidate.replaceAll("\n", "");
						String[] check = new String[init.length+1];
						for (int i = 0; i < init.length; i++)
							check[i] = init[i];
						check[init.length] = candidate;
						Derivation candDerivation = new Derivation();
						List<String> cand = Arrays.asList(check);
						float score = Model.score(param, cand, wordProblem, pos, ner);
						candDerivation.candidate = base+"\n"+candidate;
						candDerivation.score = score;
						if (derivations.contains(candDerivation))
							continue;
						if (derivations.size() < 1000)
							derivations.add(candDerivation);
						else {
							Collections.sort(derivations, comparator);
							Derivation min = derivations.get(0);
							//System.out.println(min.score+"|"+score);
							if (min.score < score) {
								derivations.remove(min);
								derivations.add(candDerivation);
							}
						}
					}
					//System.exit(0);
				}
			}
			Iterator<Derivation> it = derivations.iterator();
			beam = new ArrayList<String>();
			while (it.hasNext()) {
				beam.add(it.next().candidate.replaceAll(" ", "") + "\n");
			}
			if (ans.endsWith("-1")) {
				counter++;
				continue;
			}
			if (counter < 1) {
				System.out.println(derivations.get(0).score);
				System.out.println(beam+"\n"+beam.size());
				counter++;
				continue;
			}
				
			//queries
			ArrayList<String> queries = CandidateProgramGenerator.genCandidates(wordProblem, "", qAllowed, "year");
			for (String beamMember : beam) {
				String program = beamMember;
				program = refine(program);
				//System.out.println(program);
				String[] candQueries = new String[answers.size()];
				int size = 0;
				ArrayList<String> ansCopy = new ArrayList<>();
				for (String a : answers) {
					ansCopy.add(a);
				}
				for (String query : queries) {
					/*if (query.contains("age(kevin, currentYear)") && program.contains("ratio(kevin,shiftPlus(56),kevin,currentYear,9)")) {
						System.out.println("YES");
						System.out.println(program + "|" + query);
						query = query.replaceAll(" ", "") + "\n";
						System.out.println(program + "|" + query);
						System.out.println(goalTest(program, query, "7"));
						System.out.println(isConsistent(beamMember, query));
						System.exit(0);
					}*/
					
					query = query.replaceAll(" ", "") + "\n";
					//System.out.println("consOut"+beamMember+"|"+query+"|"+isConsistent(beamMember, query));
					if (isConsistent(beamMember, query) /*&& (goalTest(program, query, "7"))*/) {
						String toRem = "";
						for (String a : ansCopy) {
							try {
							if (goalTest(program, query, a)) {
								candQueries[size++] = query; 
							} } catch (Exception e) {
								//System.out.println("progerr"+program);
							}
							toRem = a;
							break;
						}
						ansCopy.remove(toRem);
						//return beam;
					}
					if (ansCopy.isEmpty())
						break;
				}
				//System.out.println(size + "|" + answers.size());
				if (size == answers.size()) {
					for (String query : candQueries) {
						truePrograms.add(program+query+"\n");
					}
					isSolutionFound = true;
					//break;
				}
			}
			counter++;
			//if (counter == 2)
				//break;
		}
		return beam;
	}
	private static boolean checkIn(Derivation candDerivation,
			ArrayList<Derivation> derivations) {
		for (Derivation d : derivations) {
			if (d.candidate.equals(candDerivation.candidate))
				return true;
		}
		return false;
	}
	//all word problems have been preprocessed
	public static float[] learnParam(float[] initParam, float eta, float n, ArrayList<String> wp, ArrayList<String> ans, String allowed, String qAllowed) throws Exception {
		float[] param = initParam;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < wp.size(); j++) {
				ArrayList<String> beam = getBeam(wp.get(j), param, allowed, qAllowed, ans.get(j));
				System.out.println(truePrograms);
				float[] gradient = Model.gradient(param, beam, truePrograms, wp.get(j), pos, ner);
				for (int k = 0; k < param.length; k++) {
					param[k] = param[k] + eta * gradient[k];
					//System.out.println("learning"+param[k]+"|"+gradient[k]);
				}
			}
		}
		System.out.println("TP"+truePrograms);
		return param;
	}
	public static ArrayList<String> loadWordProblems(String filename) {
		ArrayList<String> wordProblems = new ArrayList<String>();
		BufferedReader br = null;
		String allowed = "age(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,diffcount,0})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,ratiocount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,prodcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,sumcount})\n";
		String qAllowed = "age(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "offset({ner=DATE,yeara})\n"+
				  "difference(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "ratio(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "sum(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "product(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,parse,mention,coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
		try {
 			String sCurrentLine;
 			br = new BufferedReader(new FileReader(filename));
 			while ((sCurrentLine = br.readLine()) != null) {
 				String wordProblem = sCurrentLine;
 				wordProblem = CorefResolver.coref(wordProblem, pipeline);
 				wordProblem = Preprocessor.convert(wordProblem, pipeline);
 				wordProblems.add(wordProblem);
 			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return wordProblems;
	}
	public static Model train(String allowed, String qAllowed) throws Exception {
		Model m = new Model();
		float param[] = {(float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.2};
		ArrayList<String> wp = loadWordProblems("wp-train");
		ArrayList<String> ans = loadWordProblems("ans-train");
		Word2VecLoader.load();
		m.param = learnParam(param, (float)0.1, 1, wp, ans, allowed, qAllowed);
		for (int i = 0; i < param.length; i++) {
			System.out.println(m.param[i]);
		}
		return m;
	}
	public static ArrayList<String> loadAnswers(String filename) {
		ArrayList<String> answers = new ArrayList<String>();
		BufferedReader br = null;
		try {
 			String sCurrentLine;
 			br = new BufferedReader(new FileReader(filename));
 			while ((sCurrentLine = br.readLine()) != null) {
 				answers.add(sCurrentLine);
 			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return answers;
	}
	private static String refine(String program) {
		// TODO Auto-generated method stub
		String[] candidates = program.split("\n");
		LinkedHashSet<String> progActors = new LinkedHashSet<String>();
		String newProgram = "";
		for (String c : candidates) {
			if (c.isEmpty())
				continue;
			String pred = c.substring(0, c.indexOf("("));
			String args = c.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			if (pred.equals("age")) 
				progActors.add(arguments.get(0));
			else {
				progActors.add(arguments.get(0));
				progActors.add(arguments.get(2));
			}
		}
		for (String actor : progActors) {
			newProgram = newProgram + "thing(" + actor + ")\n";
		}
		newProgram = newProgram + program;
		return newProgram;
	}
	public static void main(String[] args) throws Exception {
		/*String program = "thing(kevin)\n" +
						 "ratio(kevin,shiftPlus(56),kevin,currentYear,9)\n";
		String query = "age(kevin,currentYear)";
		System.out.println(goalTest(program, query, "7"));
		System.out.println(isConsistent("ratio(kevin,shiftPlus(56),kevin,currentYear,9)\n","age(kevin,currentYear)\n"));*/
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,dcount})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,rcount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,pcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,scount})\n";
		String qAllowed = "age(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "offset({ner=DATE,yeara})\n"+
				  "difference(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "ratio(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "sum(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "product(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n";
		//float param[] = {(float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.2};
		Word2VecLoader.load();
		//String wordProblem = "In 56 years, Kevin will be 9 times as old as he is right now. How old is he right now?";
		/*String wordProblem = "Eric is ten years younger than Jason. In six years, Jason will be twice as old as Eric. What are their present ages?";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,parse,mention,coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    wordProblem = CorefResolver.coref(wordProblem, pipeline);
		wordProblem = Preprocessor.convert(wordProblem, pipeline);
		System.out.println(getBeam(wordProblem, param, allowed, qAllowed, "4,14"));*/
		train(allowed, qAllowed);
		
	}

}

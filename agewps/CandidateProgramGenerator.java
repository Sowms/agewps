package agewps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class CandidateProgramGenerator {

	//ArrayList<String> predTemplates = new ArrayList<String>();
	//ArrayList<String> queryTemplates = new ArrayList<String>();
	static HashMap<String,ArrayList<String>> pos = new HashMap<String, ArrayList<String>>();
	static HashMap<String,ArrayList<String>> ner = new HashMap<String, ArrayList<String>>();
	public static ArrayList<String> genCandidates(String wordProblem, String allowed, String qAllowed, String keyword) {
		//ArrayList<String> programs = new ArrayList<String>();
		Annotation document = new Annotation(wordProblem);
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		Pattern pattern = Pattern.compile("pos=[A-Z]+(\\+[A-Z]+)*");
        Matcher matcher = pattern.matcher(allowed+qAllowed);
        pos = new HashMap<String, ArrayList<String>>();
        ner = new HashMap<String, ArrayList<String>>();
        while (matcher.find()) {
        	String p = matcher.group();
        	p = p.replace("pos=", "");
        	pos.put(p, new ArrayList<String>());
        }
        pattern = Pattern.compile("ner=[A-Z]+");
        matcher = pattern.matcher(allowed+qAllowed);
        while (matcher.find()) {
        	String n = matcher.group();
        	n = n.replace("ner=", "");
        	ner.put(n, new ArrayList<String>());
        }
        System.out.println(pos);
        System.out.println(ner);
        String jj="", nn = "";
        for (CoreMap sentence : sentences) {
        	List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			String prev = "";
			for (CoreLabel token: tokens) {
				if (token.lemma().toLowerCase().equals(keyword) || token.lemma().toLowerCase().equals("age")) {
					jj = "";
					nn = "";
					continue;
				}
				String posToken = token.tag();
				String nerToken = token.ner();
				/*if (!posToken.contains("NN") && !nn.isEmpty() && nn.equals(prev)) {
					ArrayList<String> posList = pos.get("JJ+NN");
					String element = nn;
					posList.add(element);
					pos.put("JJ+NN", posList);
					nn="";
				}*/
				if (posToken.contains("NNP") || posToken.equals("NN")) {
					ArrayList<String> posList = pos.get("JJ+NN");
					String element = token.originalText().toLowerCase();
					//System.out.println(jj+"|"+prev);
					if (jj.equals(prev))
						element = (jj+" "+element).trim();
					if (nn.equals(prev)) {
						element = (nn+" "+element).trim();
						
					}
					if (posList.contains(nn) && nn.equals(prev))
						posList.remove(nn);
					//if (!posList.contains(element))
					posList.add(element);
					System.out.println(token.originalText()+"|"+posList);
					pos.put("JJ+NN", posList);
					jj = "";
					nn = token.originalText().toLowerCase();
				} 
				
				if (posToken.contains("JJ"))
					jj = token.lemma().toLowerCase();
				if (pos.containsKey(posToken)) {
					ArrayList<String> posList = pos.get(posToken);
					String element = token.originalText().toLowerCase(); 
					//if (!posList.contains(element))
						posList.add(element);
					pos.put(posToken, posList);
				}
				if (ner.containsKey(nerToken)) {
					ArrayList<String> nerList = ner.get(nerToken);
					String element = token.lemma().toLowerCase(); 
					//if (!nerList.contains(element))
						nerList.add(element);
					ner.put(nerToken, nerList);
				}
				prev = token.lemma().toLowerCase();
				
			}
        }
        System.out.println(pos);
        System.out.println(ner);
        //possible predicates and queries
        String all = allowed+qAllowed;
        ArrayList<String> candidates = new ArrayList<String>();
        HashMap<String,Character> counters = new HashMap<String, Character>();
        System.out.println(pos);
        System.out.println(ner);
        String[] possibilities = all.split("\n");
        for (int i = 0; i < possibilities.length; i++) {
        	String pred = possibilities[i].substring(0, possibilities[i].indexOf("("));
        	System.out.println(pred);
        	String args = possibilities[i].replace(pred+"(", "");
        	args = args.substring(0, args.length()-1);
        	List<String> arguments = Arrays.asList(args.split(", "));
        	//System.out.println(arguments);
        	ArrayList<ArrayList<String>> instArgs = new ArrayList<ArrayList<String>>();
        	int[] argSizes = new int[arguments.size()];
        	int j = 0;
        	int prod = 1;
        	for (String arg : arguments) {
        		ArrayList<String> newArgs = new ArrayList<String>();
        		arg = arg.replace("{", "").replace("}", "");
        		String[] options = arg.split(",");
        		for (String option: options) {
        			if (option.contains("pos=")) {
        				pattern = Pattern.compile("pos=[A-Z]+(\\+[A-Z]+)*");
        				matcher = pattern.matcher(option);        	
        				matcher.find();
        				String match = matcher.group();
        				for (String repl : pos.get(match.replace("pos=", ""))) {
        					newArgs.add(option.replace(match, repl));
        				}
        			}
        			else if (option.contains("ner=")) {
        				pattern = Pattern.compile("ner=[A-Z]+");
        				matcher = pattern.matcher(option);
        				matcher.find();
        				String match = matcher.group();
        				for (String repl : ner.get(match.replace("ner=", ""))) {
        					newArgs.add(option.replace(match, repl));
        				}
        			}
        			else
        				newArgs.add(option);
        		}
        		argSizes[j] = newArgs.size();
        		prod *= argSizes[j];
        		j++;
        		instArgs.add(newArgs);
        	}
        	//System.out.println(instArgs);
        	int[] ind = new int[arguments.size()];
        	ind[0] = 1;
        	for (j = 1; j < ind.length; j++) { 
        		ind[j] = argSizes[j-1] * ind[j-1];
        	}
        	int[] pointer = new int[arguments.size()];
        	for (j = 0; j < prod; j++) {
        		String candidate = pred+"(";
        		for (int k=0; k<arguments.size(); k++) {
        			if (j % ind[k] == 0) 
        				pointer[k] = (pointer[k] + 1) % argSizes[k];
        			String ans = instArgs.get(k).get(pointer[k]);
        			if (ans.contains("count")) {
        				if (!counters.containsKey(ans)) {
        					counters.put(ans, 'a');
        					ans = ans.replace("count", "a");
        				}
        				else {
        					char r = counters.get(ans);
        					ans = ans.replace("count", (char)(r+1)+"");
        				}
        			}
        			candidate = candidate+ans+", ";
        		}
        		candidate = candidate.substring(0,candidate.length()-2) + ")";
            	candidates.add(candidate);
            	//System.out.println(candidate);
        	}
        }
        System.out.println(candidates.size());
        return candidates;
	}
	public static double score(String candidate, String text, String completeText) {
		double score = 0, covScore = 0;
		int numArg = candidate.split(",").length;
		//numArg++;
		String pred = candidate.substring(0, candidate.indexOf("("));
		double max = 0, sim = 0;
		for (String word : text.split(" ")) {
			sim = WordNetInterface.compute(word, pred);
			if (sim > max)
				max = sim;
		}
		if (max > 1) {
			//System.err.println(":(");
			max = 1;
		}
		//score+=max;
		text = text.replace(" ", "").toLowerCase();
		completeText = completeText.replace(" ", "").toLowerCase();
		ArrayList<String> checkList = new ArrayList<String>();
		Iterator<Entry<String, ArrayList<String>>> it = pos.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<String>> en = it.next();
			ArrayList<String> checks = new ArrayList<String>(en.getValue());
			for (String check : checks) {
				if (text.contains(check) && !checkList.contains(check))
					checkList.add(check);
			}
		}
		it = ner.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<String>> en = it.next();
			ArrayList<String> checks = new ArrayList<String>(en.getValue());
			for (String check : checks) {
				if (text.contains(check) && !checkList.contains(check))
					checkList.add(check);
			}
		}
		covScore = checkList.size();
		String args = candidate.replace(pred+"(", "");
    	args = args.substring(0, args.length()-1);
    	List<String> arguments = Arrays.asList(args.split(", "));
    	ArrayList<String> uniqArguments = new ArrayList<String>();
    	for (String argument : arguments) {
    		if (uniqArguments.contains(argument))
    			continue;
    	//	uniqArguments.add(argument);
    		if (text.contains(argument)) {
    			score+=1;
    			checkList.remove(argument);
    		}
    		else if (completeText.contains(argument))
    			score+=0.5;
    		if (argument.contains("(")) {
    			String inPred = argument.substring(0, argument.indexOf("("));
    			String arg = argument.replace(inPred+"(", "");
    			arg = arg.substring(0, arg.length()-1);
    			if (!uniqArguments.contains(arg) && text.contains(arg)) {
    				score+=1;
    				uniqArguments.add(arg);
    				checkList.remove(argument);
    			} else if (!uniqArguments.contains(arg) && completeText.contains(arg)) {
    				score+=0.5;
    				uniqArguments.add(arg);
    			}
    		}
    		
    	}
    	covScore = 1 - checkList.size()/covScore;
    	score /= numArg;
    	
    	score = 0.1 * max + 0.45 * score + 0.45 * covScore;
		return score;
	}
	public static ArrayList<String> genPrograms(String wordProblem, String allowed, String qAllowed) {
		ArrayList<String> programs = new ArrayList<String>();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    Annotation document = new Annotation(wordProblem);
	    pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ArrayList<ArrayList<String>> miniPrograms = new ArrayList<ArrayList<String>>();
		ArrayList<String> candidates = genCandidates(wordProblem, allowed, "", "year");
		//System.out.println(candidates);
		ArrayList<String> uniqScores = new ArrayList<String>();
		for (CoreMap sentence : sentences) {
			ArrayList<String> miniProgram = new ArrayList<String>();
			HashMap<String, Double> scoreMap = new HashMap<>();
			double minScore = 2, maxScore = 0;
			for (String candidate: candidates) {
				double score = score(candidate, sentence.toString(), wordProblem);
				//if (candidate.equals("thing(bluerug)"))
					//System.err.println(score);
				scoreMap.put(candidate, score);
				if (miniProgram.size() <= 200) { 
					miniProgram.add(candidate);
					if (!uniqScores.contains(score+""))
						uniqScores.add(score+"");
					if (score > maxScore)
						maxScore = score;
					if (score < minScore)
						minScore = score;
				} else if (score > minScore && miniProgram.size() > 200) {
					if (!uniqScores.contains(score+""))
						uniqScores.add(score+"");
					String replace = "";
					for (String mini : miniProgram) {
						if (scoreMap.get(mini) == minScore) {
							replace = mini;
							break;
						}
					}
					miniProgram.remove(replace);
					miniProgram.add(candidate);
					minScore = 2;
					for (String mini : miniProgram) {
						if (scoreMap.get(mini) > maxScore) {
							maxScore = scoreMap.get(mini);
						}
						if (scoreMap.get(mini) < minScore) {
							minScore = scoreMap.get(mini);
						}
					}
				}
			}
			/*for (String mini : miniProgram) {
				System.out.println(scoreMap.get(mini));
			}*/
			//System.out.println(miniProgram);
			uniqScores.clear();
			miniPrograms.add(miniProgram);
		}
		if (!qAllowed.equals("")) {
			candidates = genCandidates(wordProblem, qAllowed, "", "year");	
			miniPrograms.add(candidates);
		}
		System.out.println(miniPrograms.size());
		int[] args = new int[miniPrograms.size()];
		int[] ind = new int[miniPrograms.size()];
		ind[0] = 1;
		int prod = 1;
		for (int i = 0; i < miniPrograms.size(); i++) {
			args[i] = miniPrograms.get(i).size();
			prod = prod * args[i];
			if (i != 0) {
				ind[i] = args[i-1] * ind[i-1];
			}
		}
		int[] pointer = new int[miniPrograms.size()];
		for (int j = 0; j < prod; j++) {
    		String candidate = "";
    		for (int k = 0; k < miniPrograms.size(); k++) {
    			if (j % ind[k] == 0) 
    				pointer[k] = (pointer[k] + 1) % args[k];
    			String ans = miniPrograms.get(k).get(pointer[k]);
    			candidate = candidate+ans+".\n";
    		}
    		programs.add(candidate);
    		System.out.println(candidate);
    	}
		return programs;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String keyword = "year";
		
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,dcount})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,rcount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,pcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,scount})\n";
		String qAllowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
						  "offset({ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
						  "difference(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
						  "ratio(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
						  "sum(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
						  "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n";
		//genCandidates("The blue rug was 26 years old in 2013. The black rug was 13 years old in the same year. How old are the rugs now?", allowed, "", keyword);
		//genPrograms("2 years from now, John will be 2 times as old as he was 8 years ago.", allowed);
		//genPrograms("The blue rug was 26 years old in 2013. The black rug was 13 years old in the same year.", allowed);
		//genPrograms("The blue rug was 26 years old in 2013. The black rug was 13 years old in the same year.", allowed);
		//genPrograms("A boy is 10 years older than his brother", allowed);
		//genPrograms("A pewter bowl is 8 years old. A silver bowl is 22 years old.", allowed, qAllowed);
		genPrograms("A man’s age is 36 years. His daughter's age is 3 years. In how many years will the man be 4 times as old as his daughter?", allowed, qAllowed);
	}

}

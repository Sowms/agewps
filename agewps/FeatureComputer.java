package agewps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class FeatureComputer {

	public static String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
			 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,diffcount,0})\n"+
			 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,ratiocount})\n"+
			 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,prodcount})\n"+
			 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,sumcount})\n";
	public static float[] computeFeatures(List<String> candidates, String text, HashMap<String,ArrayList<String>> pos, HashMap<String,ArrayList<String>> ner) {
		float[] features = new float[6];
		float sim = 0;
		for (String candidate : candidates) {
			if (candidate.isEmpty())
				continue;
			String pred = candidate.substring(0, candidate.indexOf("("));
			int count = 0;
			float localSim = 0;
			for (String word : text.split(" ")) {
				double value = WordNetInterface.compute(word, pred);
				if (word.endsWith("er") && value == -1) {
					word = word.replace("er", "");
					value = WordNetInterface.compute(word, pred);
				}
				if (value != -1 && value < Double.MAX_VALUE) {
					localSim += WordNetInterface.compute(word, pred);
					count++;
				}
			}
			if (count != 0)
				sim += localSim/count;
		}
		//feature 1 - wordnet similarity
		features[0] = sim/candidates.size();
		//System.out.println("ha");
		String orig = text;
		text = text.replace(" ", "").toLowerCase();
		ArrayList<String> checkList = new ArrayList<String>();
		Iterator<Entry<String, ArrayList<String>>> it = pos.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<String>> en = it.next();
			ArrayList<String> checks = new ArrayList<String>(en.getValue());
			for (String check : checks) {
				check = check.replace(" ", "");
				if (text.contains(check) && !checkList.contains(check))
					checkList.add(check);
			}
		}
		it = ner.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<String>> en = it.next();
			ArrayList<String> checks = new ArrayList<String>(en.getValue());
			for (String check : checks) {
				check = check.replace(" ", "");
				if (text.contains(check) && !checkList.contains(check))
					checkList.add(check);
			}
		}
		ArrayList<String> checkCopy = new ArrayList<String>();
		checkCopy = new ArrayList<String>(checkList);
		float covScore = checkList.size();
		//System.out.println("aaaaa"+covScore);
		features[2] = 0;
		features[1] = 0;
		for (String candidate : candidates) {
			if (candidate.isEmpty())
				continue;
			String pred = candidate.substring(0, candidate.indexOf("("));
			String args = candidate.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(","));
			ArrayList<String> uniqArguments = new ArrayList<String>();
			for (String argument : arguments) {
				if (!uniqArguments.contains(argument))
					uniqArguments.add(argument);
				if (text.contains(argument) && checkList.contains(argument)) {
					checkList.remove(argument);
				}
				if (argument.contains("(")) {
					String inPred = argument.substring(0, argument.indexOf("("));
					String arg = argument.replace(inPred+"(", "");
					arg = arg.substring(0, arg.length()-1);
					//if (text.contains(arg) && !uniqArguments.contains(arg))
						//uniqArguments.add(arg);
					if (text.contains(arg) && checkList.contains(arg)) {
						checkList.remove(arg);
					}
				}
			}
			//feature 3 - no of unique arguments
			features[2] += uniqArguments.size()/arguments.size();
		}
		//features[2] /= candidates.size();
		features[2] = 1;
    	//feature 2 - how much is covered in the predicate
		//System.out.println("cov score"+covScore+"|"+checkList.size());
		if (covScore != 0)
			features[1] = 1 - checkList.size()/covScore;
		else
			features[1] = 0;
		//System.out.println(features[1]+"cov"+candidates.get(0));
		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    	features[3] = 0;
    	//feature 4 - per sentence argument coverage
    	String[] sentences = text.split("\\.");
    	//System.out.println(Arrays.asList(sentences));
    	float score = 0;
    	for (String candidate : candidates) {
    		if (candidate.isEmpty())
    			continue;
    		float step = 0;
    		for (String sentence : sentences) {
    			String pred = candidate.substring(0, candidate.indexOf("("));
    			String args = candidate.replace(pred+"(", "");
    			args = args.substring(0, args.length()-1);
    			float correctArg = 0;
    			List<String> arguments = Arrays.asList(args.split(","));
    			for (String arg : arguments) {
    				//System.out.println(arg);
    				if (sentence.contains(arg))
    					correctArg++;
    				if (arg.contains("(")) {
    					String inPred = arg.substring(0, arg.indexOf("("));
    					String arg1 = arg.replace(inPred+"(", "");
    					arg1 = arg1.substring(0, arg1.length()-1);
    					//System.out.println(arg1);
    					if (sentence.contains(arg1)) 
    						correctArg++;
    				}
    				//default
    				if (arg.contains("currentYear"))
    	    			correctArg++;
    				//variable
    				//if (allowed.contains(arg) && !sentence.contains(arg))
    					//correctArg--;
    			}
    			//System.out.println(correctArg/arguments.size()+"|"+sentence);
    			step = step + correctArg/arguments.size();
    		}
    		score += step/sentences.length;
    	}
    	//System.out.println(score);
    	score = score/candidates.size();
    	features[3] = score;
    	//feature 5 - word order argument order
    	features[4] = 0;
    	/*for (String candidate : candidates) {
    		if (candidate.isEmpty())
    			continue;
    		String pred = candidate.substring(0, candidate.indexOf("("));
			String args = candidate.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			List<String> arguments = Arrays.asList(args.split(", "));
    		int[][] indices = new int[arguments.size()][];
    		for (int i = 0; i < arguments.size(); i++) {
    			//https://stackoverflow.com/questions/5034442/indexes-of-all-occurrences-of-character-in-a-string
    			int index = text.indexOf(arguments.get(i));
    			int j = 0;
    			int[] matches = new int[10];
    			while (index >= 0) {
    				matches[j] = index;
    				index = text.indexOf(arguments.get(i), index + 1);
    				j++;
    			}
    			indices[i] = new int[j];
    			for (int k = 0; k < j; k++)
    				indices[i][k] = matches[k];
    		}
    		//for (int i = 0; i < arguments.size(); i++) {
        		//for (int k = 0; k < indices[i].length; k++) {
        		//System.out.print(indices[i][k] + " ");
        	//}
        	//System.out.print("\n");
    	//}
    	//System.out.println("-----------");
    		float orderScore = 0;
    		for (int i = 0; i < arguments.size() - 1; i++) {
    			boolean flag = true;
    			if (indices[i].length == 0) 
    				continue;
    			for (int k1 = 0; k1 < indices[i].length && flag; k1++) {
    				int counter = i+1;
    				while (counter < arguments.size() && indices[counter].length == 0) {
    					counter++;
    				}
    				if (counter == arguments.size())
    					continue;
    				for (int k2 = 0; k2 < indices[counter].length && flag; k2++) {
    				//System.out.println(indices[i][k1]+"|"+indices[counter][k2]);
    					if (indices[i][k1] < indices[counter][k2]) {
    						if (indices[i][k1] != -1 && indices[counter][k2] != -1) {
    							orderScore++;
    							flag = false;
    							break;
    						}
    					}
    				}
    			}
    		}
    		orderScore = orderScore/(arguments.size() - 1);
    		features[4] += orderScore;
    	}
    	features[4] /= candidates.size();*/
    	features[4] = 1;
    	/*float sc = 0;
    	for (String candidate : candidates) {
    		float inSc = 0;
    		String pred = candidate.substring(0, candidate.indexOf("("));
			String args = candidate.replace(pred+"(", "");
			args = args.substring(0, args.length()-1);
			for (String arg : args.split(",")) {
				if (text.contains(arg))
					inSc++;
				if (arg.contains("(")) {
					String inPred = arg.substring(0, arg.indexOf("("));
					String arg1 = arg.replace(inPred+"(", "");
					arg1 = arg1.substring(0, arg1.length()-1);
					//System.out.println(arg1);
					if (text.contains(arg1)) 
						inSc++;
				}
			}
			sc += inSc/args.split(",").length;
    	}
    	features[4] = sc/candidates.size();*/
    	features[5] = 0;
    	float val = 0;
    	String[] words = orig.split("\\s");
    	for (String candidate : candidates) {
    		if (candidate.isEmpty())
    			continue;
    		String pred = candidate.substring(0, candidate.indexOf("("));
    		float[] predVec = Word2VecLoader.wordVecMap.get(pred).vec;
    		//feature 6 - language model feature - avg cosine similarity
    		//build average word vector
    		float[] avgVec = new float[predVec.length];
    		for (int i = 0; i < avgVec.length; i++)
    			avgVec[i] = 0;
    		int c = 0;
    		for (String word : words) {
    			String w = word.toLowerCase();
    			if (checkCopy.contains(w))
    				continue;
    			if (Word2VecLoader.wordVecMap.containsKey(w)) {
    				float[] vec = Word2VecLoader.wordVecMap.get(w).vec;
    				for (int i = 0; i < vec.length; i++)
    					avgVec[i] += vec[i];
    				c++;
    			}
    		}
    		if (c != 0) {
    			for (int i = 0; i < predVec.length; i++) {
    				avgVec[i] /= c;
    			}
    		}
    		val += WordVec.cosSim(avgVec, predVec);
    	}
    	val = val / candidates.size();
    	features[5] += val;
    	
    	//features[5] /= sentences.length;
    	return features;
		
	}
	public static void main(String[] args) {
		String wordProblem = "Eric is 10 years younger than Jason. Jason will be 2 times as old as Eric in 6 years. How old are they now?";
		CandidateProgramGenerator.genCandidates(wordProblem, allowed, "", "year");
		HashMap<String, ArrayList<String>> pos = CandidateProgramGenerator.pos;
		HashMap<String, ArrayList<String>> ner = CandidateProgramGenerator.ner;
		Word2VecLoader.load();
		//List<String> candidates = Arrays.asList("difference(jason,xyear,jason,currentYear,6)");
		List<String> candidates = Arrays.asList("ratio(eric,shiftPlus(6),jason,shiftPlus(6),2)");
		//List<String> candidates = Arrays.asList("age(nicole, currentYear, 26)");
		float[] features = computeFeatures(candidates, wordProblem, pos, ner);
		for (int i = 0; i < features.length; i++) {
			System.out.print(features[i] + " ");
		}
		System.out.println("\nsentence" + features[3]);
		System.out.println("\ncoverage" + features[1]);
		float[] trained = {(float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.16, (float)0.2};
		System.out.println("\nh"+Model.score(trained, candidates, wordProblem, pos, ner));
		candidates = Arrays.asList("difference(jason,xyear,jason,currentYear,6)");
		features = computeFeatures(candidates, wordProblem, pos, ner);
		for (int i = 0; i < features.length; i++) {
			System.out.print(features[i] + " ");
		}
		System.out.println("\nsentence" + features[3]);
		System.out.println("\ncoverage" + features[1]);
		System.out.println("\nh"+Model.score(trained, candidates, wordProblem, pos, ner));
		
	}
}

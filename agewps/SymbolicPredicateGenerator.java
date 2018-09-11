package agewps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class SymbolicPredicateGenerator {

	public static String genProgram(String wordProblem, String allowed) {
		String program = "";
		String[] incPatterns = {"^in\\s(([a-zA-Z])*\\s)*(\\d)+\\syear(s)?", "\\sin\\s(([a-zA-Z])*\\s)*(\\d)+\\syear(s)?", "(\\d)+\\syear(s)?\\sfrom\\snow"};
		String[] decPatterns = {"(\\d)+\\syear(s)?\\sago"}; 
		String[] agePatterns = {"(\\d)+\\syear(s)?\\sold(\\.|\\s|,|;)","(\\d)+\\syear(s)?\\sof\\age","age\\s(is|was|will\\sbe)\\s(\\d)+"};
		String[] incDiffPatterns = {"(\\d)+\\syear(s)?\\solder", "(\\d)+\\syear(s)?\\smore"};
		String[] decDiffPatterns = {"(\\d)+\\syear(s)?\\syounger", "(\\d)+\\syear(s)?\\sless", "difference"};
		String[] ratioPatterns = {"(\\d)+\\stimes"};
		String[] keywords = {"age", "ages", "year", "years", "old", "time", "times", "young", "sum", "difference"};
		String[] sentences = wordProblem.toLowerCase().split("\\.");
		/*String wp = "";
		for (int i = 0; i < sentences.length-1; i++)
			wp = wp + sentences[i]+".";*/
		CandidateProgramGenerator.genCandidates(wordProblem, allowed, "", "year");
		HashMap<String, ArrayList<String>> pos = CandidateProgramGenerator.pos;
		HashMap<String, ArrayList<String>> ner = CandidateProgramGenerator.ner;
		ArrayList<String> yearsMap = ner.get("DATE");
		ArrayList<String> actors1 = pos.get("JJ+NN");
		LinkedHashSet<String> actors = new LinkedHashSet<String>();
		for (String actor : actors1)
			actors.add(actor);
		for (String keyword : keywords) {
			if (actors.contains(keyword)) {
				actors.remove(keyword);
			}
		}
		ArrayList<String> del = new ArrayList<String>();
		ArrayList<String> add = new ArrayList<String>();
		for (String keyword : keywords) {
			for (String actor: actors) {
				if (actor.contains(keyword)) {
					del.add(actor);
					if (!actor.equals(keyword)) {
						actor = actor.replace(keyword, "").trim();
						add.add(actor);
					}
				}
			}
		}
		actors.removeAll(del);
		actors.addAll(add);
		System.out.println(actors);
		for (String actor : actors) {
			String pred = "thing("+actor+")\n";
			if (!program.contains(pred))
				program = program + pred;
		}
		for (String sentence : sentences) {
			sentence = sentence + ".";
			String year = "currentYear", pred = "";
			//question
			if (sentence.contains("find") || sentence.contains("how")) {
				
				String clause = "";
				if (sentence.contains("find"))
					clause = sentence.substring(sentence.indexOf("find"));
				if (sentence.contains("how"))
					clause = sentence.substring(sentence.indexOf("how"));
				year = "currentYear";
				for (String y : yearsMap) {
					if (y.matches("\\d+") && clause.contains(y)) {
						year = y;
					}
				}
				//shiftPlus
				for (String pat : incPatterns) {
					Pattern pattern = Pattern.compile(pat);
					Matcher matcher = pattern.matcher(clause);
					if (matcher.find()) {
						year = matcher.group();
						year = year.replace("in ", "");
						year = year.replace("from ", "");
						year = year.replace("year", "");
						year = year.replace("s", "");
						year = year.replace("now", "");
						year = "shiftPlus("+year.trim()+")";
					}
				}
				//shiftMinus
				for (String pat : decPatterns) {
					Pattern pattern = Pattern.compile(pat);
					Matcher matcher = pattern.matcher(clause);
					if (matcher.find()) {
						year = matcher.group();
						year = year.replace("ago", "");
						year = year.replace("year", "");
						year = year.replace("s", "");
						year = "shiftMinus("+year.trim()+")";
					}
				}
				
				String ratioAmount= "";
				Pattern pattern = Pattern.compile("(\\d)+\\stimes");
				Matcher matcher = pattern.matcher(clause);
				if (matcher.find()) {
					pred = matcher.group();
					ratioAmount = pred.replace(" times", "").trim();
					year = "year";
					String actor1 = "", actor2 = "";
					int maxPos = -1, minPos = 1000;
					for (String possActor : actors) {
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) < sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) > maxPos) {
								actor1 = possActor;
								maxPos = sentence.indexOf(possActor);
							}
						}
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) < minPos) {
								actor2 = possActor;
								minPos = sentence.indexOf(possActor);
							}
						}
					}
					program = program + "ratio("+actor1+", "+year+", " + actor2 + ", " + year + ", " + ratioAmount + ")\n";
					program = program + "offset(year)?\n";
				}
				else {
					ArrayList<String> quesActors = new ArrayList<String>();
					for (String actor : actors) {
						if (clause.contains(actor))
							quesActors.add(actor);
					}
					if (quesActors.isEmpty()) {
						quesActors = new ArrayList<String>(actors);
					}
					for (String actor : quesActors) {
						String addActor = "age("+actor+", "+year+")?\n";
						if (!program.contains(addActor))
							program = program + addActor;
					}
				}
				continue;
			}
			ArrayList<String> years = new ArrayList<String>();
			for (String y : yearsMap) {
				if (y.matches("\\d+") && sentence.contains(y)) {
					year = y;
					years.add(year);
				}
			}
			//shiftPlus
			for (String pat : incPatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				if (matcher.find()) {
					System.out.println(pat);
					Pattern pattern1 = Pattern.compile("(\\d)+");
					String text = matcher.group();
					Matcher matcher1 = pattern1.matcher(text);
					if (matcher1.find()) {
						year = "shiftPlus("+matcher1.group().trim()+")";
						years.add(year);
					}
				}
			}
			//shiftMinus
			for (String pat : decPatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				if (matcher.find()) {
					year = matcher.group();
					year = year.replace("ago", "");
					year = year.replace("year", "");
					year = year.replace("s", "");
					year = "shiftMinus("+year.trim()+")";
					years.add(year);
				}
			}
			//age
			for (String pat : agePatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				while (matcher.find()) {
					String actor = "";
					int maxPos = -1;
					pred = matcher.group();
					System.out.println(pred);
					for (String possActor : actors) {
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) < sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) > maxPos) {
								actor = possActor;
								maxPos = sentence.indexOf(possActor);
							}
						}
					}
					pred = pred.replace("age", "");
					pred = pred.replace("is", "");
					pred = pred.replace("year", "");
					pred = pred.replace("s", "");
					pred = pred.replace("was", "");
					pred = pred.replace("will be", "");
					pred = pred.replace(".", "");
					pred = pred.replace(",", "");
					pred = pred.replace(";", "");
					pred = pred.replace("old", "");
					pred = pred.trim();
					program = program + "age("+actor+", "+year+", "+pred+")\n";
				}
			}
			//ratio
			for (String pat : ratioPatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				if (matcher.find()) {
					pred = matcher.group();
					String ratio = pred.replace("times", "");
					ratio = ratio.trim();
					String actor1 = "", actor2 = "";
					int maxPos = -1, minPos = 1000;
					System.out.println(sentence);
					for (String possActor : actors) {
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) < sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) > maxPos) {
								actor1 = possActor;
								maxPos = sentence.indexOf(possActor);
							}
						}
						if (sentence.indexOf(possActor) != -1 && sentence.lastIndexOf(possActor) > sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) < minPos) {
								actor2 = possActor;
								minPos = sentence.indexOf(possActor);
							}
						}
					}
					if (years.size() > 1)
						program = program + "ratio("+actor1+", "+years.get(0)+", " + actor2 + ", " + years.get(1) + ", " + ratio + ")\n";
					else
						program = program + "ratio("+actor1+", "+year+", " + actor2 + ", " + year + ", " + ratio + ")\n";
				}
			}
			//diff - inc
			for (String pat : incDiffPatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				if (matcher.find()) {
					pred = matcher.group();
					String diff = pred.replace("older", "");
					diff = diff.replace("more", "");
					diff = diff.replace("year", "");
					diff = diff.replace("s", "");
					diff = diff.trim();
					String actor1 = "", actor2 = "";
					int maxPos = -1, minPos = 1000;
					for (String possActor : actors) {
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) < sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) > maxPos) {
								actor1 = possActor;
								maxPos = sentence.indexOf(possActor);
							}
						}
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) < minPos) {
								actor2 = possActor;
								minPos = sentence.indexOf(possActor);
							}
						}
					}
					program = program + "difference("+actor1+", "+year+", " + actor2 + ", " + year + ", " + diff + ")\n";
				}
			}
			//diff - dec
			for (String pat : decDiffPatterns) {
				Pattern pattern = Pattern.compile(pat);
				Matcher matcher = pattern.matcher(sentence);
				if (matcher.find()) {
					pred = matcher.group();
					String diff = pred.replace("younger", "");
					diff = diff.replace("less", "");
					diff = diff.replace("year", "");
					diff = diff.replace("s", "");
					diff = diff.replace("difference", "");
					diff = diff.trim();
					if (diff.isEmpty()) {
						String test = sentence.substring(sentence.indexOf("difference"));
						if (test.contains("is"))
							test = test.substring(test.indexOf("is"));
						if (test.contains("was"))
							test = test.substring(test.indexOf("was"));
						if (test.contains("be"))
							test = test.substring(test.indexOf("be"));
						Pattern pattern1 = Pattern.compile("(\\d)+");
						Matcher matcher1 = pattern1.matcher(test);
						if (matcher1.find()) {
							diff = matcher1.group();
						}
					}
					String actor1 = "", actor2 = "";
					int maxPos = -1, minPos = 1000;
					for (String possActor : actors) {
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) < sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) > maxPos) {
								actor1 = possActor;
								maxPos = sentence.indexOf(possActor);
							}
						}
						if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
							if (sentence.indexOf(possActor) < minPos) {
								actor2 = possActor;
								minPos = sentence.indexOf(possActor);
							}
						}
					}
					if (pred.equals("difference")) {
						minPos = 1000;
						int minPos2 = 1000;
						for (String possActor : actors) {
							if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
								if (sentence.indexOf(possActor) < minPos) {
									actor2 = possActor;
									minPos = sentence.indexOf(possActor);
								}
							}
						}
						for (String possActor : actors) {
							if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
								if (sentence.indexOf(possActor) < minPos2 && sentence.indexOf(possActor) > minPos) {
									actor1 = possActor;
									minPos = sentence.indexOf(possActor);
								}
							}
						}
						System.out.println(actor1+"|||"+actor2);
						if (actor1.isEmpty())
							actor1 = actors.iterator().next();
						if (actor2.isEmpty() || actor1.equals(actor2)) {
							Iterator<String> it  = actors.iterator();
							String n = it.next();
							if (!actor1.isEmpty() && !n.equals(actor1))
								actor2 = n;
							else
								actor2 = it.next();
						}
					}
					program = program + "difference("+actor2+", "+year+", " + actor1 + ", " + year + ", " + diff + ")\n";
				}
			}
			//sum
			if (sentence.contains("sum")) {
				String actor1 = "", actor2 = "";
				int minPos = 1000, minPos2 = 1000;
				for (String possActor : actors) {
					if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
						if (sentence.indexOf(possActor) < minPos) {
							actor2 = possActor;
							minPos = sentence.indexOf(possActor);
						}
					}
				}
				for (String possActor : actors) {
					if (sentence.indexOf(possActor) != -1 && sentence.indexOf(possActor) > sentence.indexOf(pred)) {
						if (sentence.indexOf(possActor) < minPos2 && sentence.indexOf(possActor) > minPos) {
							actor1 = possActor;
							minPos = sentence.indexOf(possActor);
						}
					}
				}
				if (actor1.isEmpty())
					actor1 = actors.iterator().next();
				if (actor2.isEmpty() || actor1.equals(actor2)) {
					Iterator<String> it  = actors.iterator();
					it.next();
					actor2 = it.next();
				}
					
				String test = sentence.substring(sentence.indexOf("sum"));
				if (test.contains("is"))
					test = test.substring(test.indexOf("is"));
				if (test.contains("was"))
					test = test.substring(test.indexOf("was"));
				if (test.contains("be"))
					test = test.substring(test.indexOf("be"));
				String sumAmount= "";
				Pattern pattern = Pattern.compile("(\\d)+");
				Matcher matcher = pattern.matcher(test);
				if (matcher.find()) {
					sumAmount = matcher.group();
					sumAmount = sumAmount.replace("year", "");
					sumAmount = sumAmount.replace("s", "");
				}
				program = program + "sum("+actor2+", "+year+", " + actor1 + ", " + year + ", " + sumAmount + ")\n";
			}
			
		}
		return program;
	}
	public static void main(String[] args) {
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,diffcount,0})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,ratiocount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,prodcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,sumcount})\n";
		String wordProblem = "Nicole is 26 years old. Emma is 2 years old. In how many years will Nicole be triple Emma's age?";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,parse,mention,coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		wordProblem = CorefResolver.coref(wordProblem, pipeline);
	    wordProblem = Preprocessor.convert(wordProblem, pipeline);
	    
		
		System.out.println(genProgram(wordProblem, allowed));

	}

}

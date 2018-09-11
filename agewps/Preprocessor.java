package agewps;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Preprocessor {

	//private static HashMap <String, String> cardinal = new HashMap<String, String>();
	public static String convert(String wordProblem, StanfordCoreNLP pipeline) {
		String ans = "";
		String[] adv1 = {"", "once", "twice", "thrice", "quance", "quintce", "hexonce"};
		String[] adv2 = {"", "single", "double", "triple", "quadruple", "quintuple", "sextuple"};
		List<String> advList1 = Arrays.asList(adv1);
		List<String> advList2 = Arrays.asList(adv2);
		wordProblem = wordProblem.replaceAll("I ", "Tom ");
		wordProblem = wordProblem.replaceAll(" I\\.", " Tom.");
		wordProblem = wordProblem.replaceAll(" I\\?", " Tom?");
		wordProblem = wordProblem.replaceAll("'s?", "");
		Annotation document = new Annotation(wordProblem);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			String sen = "";
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				if (token.tag().equals("CD") && token.originalText().matches("[A-Za-z]+")) {
					sen = sen + " " + Word2Num.convert(token.originalText());
					continue;
				}
				if (token.tag().contains("RB") && token.originalText().matches("[A-Za-z]+")) {
					if (advList1.contains(token.originalText())) {
						sen = sen + " " + advList1.indexOf(token.originalText()) + " times";
						continue;
					}
				}
				if (token.tag().contains("JJ") && token.originalText().matches("[A-Za-z]+")) {
					if (advList2.contains(token.originalText())) {
						sen = sen + " " + advList2.indexOf(token.originalText()) + " times";
						continue;
					}
				}
				if (token.originalText().equals("."))
					sen = sen + token.originalText();
				else
					sen = sen + " " + token.originalText() ;
			}
			ans = ans + " " + sen.trim();
		}
		return ans.trim();
	}
	public static void main(String[] args) {
		String wordProblem = "Chelsea's age is double Daniel's age. Eight years ago the sum of their ages was 32. How old are they now?";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		System.out.println(convert(wordProblem, pipeline));
	}

}

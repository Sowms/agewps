package agewps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class TextDatasetTester {

	public static boolean checkAns(String sysAns, String ans) {
		//System.out.println(ans+"|"+sysAns);
		if (sysAns.isEmpty())
			return false;
		if (sysAns == null || ans == null)
			return false;
		sysAns = sysAns.replaceAll(",", "");
		ans = ans.replaceAll(",", "");
		int num = sysAns.split(" ").length;
		int num1 = ans.split(" ").length;
		double[] sysAnswers = new double[num];
		double[] answers = new double[num1];
		for (int i = 0; i < num; i++) {
			sysAnswers[i] = Double.parseDouble(sysAns.split(" ")[i]);
		}
		for (int i = 0; i < num1; i++) {
			answers[i] = Double.parseDouble(ans.split(" ")[i]);
			boolean found = false;
			for (int j = 0; j < num && !found; j++) {
				if (sysAnswers[j] == answers[i] || Math.round(100*sysAnswers[j]) == Math.round(100*answers[i]))
					found = true;
			}
			if (!found)
				return false;
		}
		
		return true;
	}
	public static void main(String[] args) {
		BufferedReader br1 = null, br2 = null;
		BufferedWriter br = null;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,ner,parse,mention,coref");
	    props.setProperty("ner.useSUTime", "false");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    int count = 0, total = 0, attempted = 0;
	    double precision = 0;
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("wp1"));
 			br2 = new BufferedReader(new FileReader("ans1"));
 			br = new BufferedWriter(new FileWriter("output1"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				String sysAns = "", ques = sCurrentLine, ans = br2.readLine();
 				PredicateController.reset();
 				if (ans.isEmpty()) {
 					total++;
 					continue;
 				}
				try{
				sysAns = WordProblemSolver.solve(ques,pipeline, null);
				System.out.println("s"+attempted+"|"+PredicateController.solveFlag);
				if (PredicateController.solveFlag) {
					attempted++;
						
				}
				}
				catch(Exception e) {
				}
				if (checkAns(sysAns,ans)) {
					count++;
					if (!PredicateController.solveFlag) {
						attempted++;
					}	
				}
				else {
					br.write(ques+"\n"+sysAns+"\n"+PredicateController.solveFlag);
					System.out.println("s"+ques+"|"+sysAns);
					//Thread.sleep(10000);
				}
				System.out.println("e"+attempted+"|"+count);
				
				total++;
			}
 			precision = (double)count/attempted;
 			System.out.println(count+"|"+total+"|"+precision+"|"+attempted);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br1 != null)
					br1.close();
				if (br2 != null)
					br2.close();
				br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}

package agewps;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ReprDatasetTester {

	public static boolean checkAns(String sysAns, String ans) {
		System.out.println(ans+"|"+sysAns);
		if (sysAns.isEmpty() || sysAns.contains("x"))
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
		int count = 0, total = 0, attempted = 0;
	    double precision = 0;
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("repr"));
 			br2 = new BufferedReader(new FileReader("ans"));
 			br = new BufferedWriter(new FileWriter("output1-1"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				String sysAns = "", ques = sCurrentLine, ans = br2.readLine();
 				PredicateController.reset();
 				String program = "";
 				while(sCurrentLine != null && !sCurrentLine.isEmpty()) {
 					sCurrentLine = br1.readLine();
 					if (sCurrentLine != null && !sCurrentLine.isEmpty())
 						program = program + sCurrentLine+"\n";
 				}
 				System.out.println(program);
 				try {
 					sysAns = WordProblemSolver.solveRepr(program);
 				} catch (Exception e) {
 					e.printStackTrace();
 					continue;
 				}
 				if (ans.isEmpty()) {
 					total++;
 					continue;
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
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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

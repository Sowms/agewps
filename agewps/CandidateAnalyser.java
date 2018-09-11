package agewps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CandidateAnalyser {
	public static void main(String[] args) {
		BufferedReader br1 = null;
		BufferedWriter br = null;
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,dcount})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,rcount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,pcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,scount})\n";
		String qAllowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "offset({ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "difference(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD})\n"+
				  "ratio(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD)\n"+
				  "sum(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD})\n";
		try {
 			String sCurrentLine;
 			br1 = new BufferedReader(new FileReader("wp"));
 			br = new BufferedWriter(new FileWriter("candidates"));
 			while ((sCurrentLine = br1.readLine()) != null) {
 				br.write(sCurrentLine+"\n");
 				ArrayList<String> candidates = CandidateProgramGenerator.genCandidates(sCurrentLine, allowed, qAllowed, "year");
 				for (String candidate: candidates)
 					br.write(candidate+"\n");
 				br.write("\n");
			}
 		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br1 != null)
					br1.close();
				br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}

package agewps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
public class DatasetCreator {

	public static void main(String[] args) {
		BufferedReader br = null;
		BufferedWriter br1 = null, br2 = null;
		try {
 			String sCurrentLine;
 			br = new BufferedReader(new FileReader("age1"));
 			br1 = new BufferedWriter(new FileWriter("wp2"));
 			br2 = new BufferedWriter(new FileWriter("ans2"));
 			while ((sCurrentLine = br.readLine()) != null) {
 				String ques = sCurrentLine.substring(sCurrentLine.indexOf(". "));
 				ques = ques.substring(2);
 				String ans = "";
 				for (int i = ques.length()-1; i>=0; i--) {
 					char ch = ques.charAt(i);
 					if (Character.isDigit(ch) || ch == ',' || ch == ' ' || ch == ':')
 						ans = ch + ans;
 					else
 						break;
 				}
 				ques = ques.replace(ans, "") + "\n";
 				if (!ans.isEmpty() && ans.startsWith(" "))
 					ans = ans.substring(1);
 				ans = ans + "\n";
 				System.out.println(ques+"|"+ans);
 				br1.write(ques);
 				br2.write(ans);
 			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				br1.close();
				br2.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

}

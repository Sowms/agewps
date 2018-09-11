package agewps;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {

	public static String getYear(String year) {
		String pred = "";
		if (year.startsWith("difference")) {
			pred = pred + "shiftMinus";
			String shift = year.replace("difference(currentYear,", "").replace(")", "");
			pred = pred + "(" + shift + "),";
		}
		else if (year.startsWith("sum")) {
			pred = pred + "shiftPlus";
			String shift = year.replace("sum(currentYear,", "").replace(")", "");
			pred = pred + "(" + shift + "),";
		}
		else
			pred = year;
		return pred;
	}
	public static String translate(String input) {
		String ans = "";
		//thing
		Pattern pattern = Pattern.compile("age\\([a-z]+\\,");
		Matcher matcher = pattern.matcher(input);
		LinkedHashSet<String> actors = new LinkedHashSet<String>();
		String things = "";
		while (matcher.find()) {
			String name = matcher.group();
			name = name.replace("age(", "");
			name = name.replace(",", "");
			actors.add(name);
		}
		for (String actor : actors) {
			things = things + "thing(" + actor + ")\n";
		}
		int dCount = 0, consCount = 0;
		for (String line : input.split("\n")) {
			if (line.startsWith("age(")) {
				String pred = "age(";
				String num = line.split("=")[1];
				int i  = 0;
				for (i = line.indexOf('(') + 1; line.charAt(i) != ','; i++) {
					pred = pred + line.charAt(i);
				}
				pred = pred + ",";
				String year = line.replace(pred, "");
				year = year.replace("=", "");
				year = year.replace(year, "");
				pred = getYear(year) + "," + num + ")";
				ans = ans + pred + "\n";
			}
			else {
				String num = line.split("=")[1];
				pattern = Pattern.compile(".*\\(.*\\,.*\\)");
				matcher = pattern.matcher(line.split("=")[0]);
				//if (matcher.find())
				String pred = "", arg1 = "", arg2 = "";
				int i = 0;
				for (; line.charAt(i) != '('; i++)
					pred = pred + line.charAt(i);
				for (i++;line.charAt(i) != ',';i++) {
					arg1 = arg1 + line.charAt(i);
				}
				for (i++;line.charAt(i) != ')';i++) {
					arg2 = arg2 + line.charAt(i);
				}
				/*while (!arg1.startsWith("age") || !arg2.startsWith("age")) {
					/*ArrayList<String> expArgs = new ArrayList<String>();
					if (!arg1.startsWith("age")) 
						expArgs.add(arg1);
					if (!arg2.startsWith("age")) 
						expArgs.add(arg2);
					for (String arg : expArgs) {
						
					}
				}*/
				ArrayList<String> expArgs = new ArrayList<String>();
				expArgs.add(arg1);
				expArgs.add(arg2);
				System.out.println(expArgs);
				pred = pred + "("; 
				for(String arg : expArgs) {
					i  = 0;
					for (i = arg.indexOf('(') + 1; arg.charAt(i) != ','; i++) {
						pred = pred + arg.charAt(i);
					}
					pred = pred + ",";
					String year = line.replace(pred, "");
					year = year.replace("=", "");
					year = year.replace(year, "");
					pred = getYear(year) + ",";
				}
				pred = pred + num + ")\n";
				ans = ans + pred;
			}
		}
		ans = things + ans;
		return ans;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String input = "ratio(age(davidm,difference(currentYear,6)),age(david,difference(currentYear,6)))=13\n"+
					   "ratio(age(davidm,currentYear),age(david,currentYear))=4\n";
		System.out.println(translate(input));
	}

}

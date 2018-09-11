package agewps;

import java.util.Calendar;

public class Age {
	static int currentYear = Calendar.getInstance().get(Calendar.YEAR);
	public static int shiftPlus(int offset) {
		return currentYear + offset;
	}
	public static int shiftMinus(int offset) {
		return currentYear - offset;
	}
	public static String diff (Person p1, String y1, Person p2, String y2) {
		return p1.name + ".age("+y1+") - " + p2.name + ".age(" + y2 + ")";
	}
	public static String ratio (Person p1, String y1, Person p2, String y2) {
		return p1.name + ".age("+y1+") / " + p2.name + ".age(" + y2 + ")";
	}
	public static String sum (Person[] p, String[] y) {
		String sum = p[0].name + ".age(" + y[0] + ")";
		for (int i = 1; i < p.length; i++) {
			sum = sum + " + " + p[i].name + ".age(" + y[i] + ") ";
		}
		return sum;
	}
	public static String prod (Person[] p, String[] y) {
		String sum = p[0].name + ".age(" + y[0] + ")";
		for (int i = 0; i < p.length; i++) {
			sum = sum + " * " + p[i].name + ".age(" + y[i] + ")";
		}
		return sum;
	}
	
}

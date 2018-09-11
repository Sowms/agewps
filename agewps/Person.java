package agewps;

public class Person {
	double birthYear = -1;
	String name;
	Person(String name) {
		this.name = name;
	}
	double age(double year) {
		if (birthYear == -1)
			return -1;
		return year - birthYear;
	}
	void birthyear(double year) {
		birthYear = year;
	}
	void birthYear(double age, double year) {
		birthYear = year - age;
	}
}

package agewps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

//https://stackoverflow.com/questions/31438822/how-to-implement-word2vec-in-java
//Class to store a hashmap of wordvecs
class WordVecs {

	public static HashMap<String, WordVec> wordvecmap;
	static void loadFromTextFile() {
		String wordvecFile = "wordvecs";
		wordvecmap = new HashMap<String, WordVec>();
		try (FileReader fr = new FileReader(wordvecFile);
			BufferedReader br = new BufferedReader(fr)) {
			String line;
			while ((line = br.readLine()) != null) {
				WordVec wv = new WordVec(line);
				wordvecmap.put(wv.word, wv);
			}
		}
		catch (Exception ex) { ex.printStackTrace(); }        
	}
}

//class for each wordvec
class WordVec {
	float[] vec;
	float norm;
	String word;
	public WordVec(String line) {
		String[] tokens = line.split("\\s+");
		word = tokens[0];
		vec = new float[tokens.length-1];
		norm = 0;
		for (int i = 1; i < tokens.length; i++) {
			vec[i-1] = Float.parseFloat(tokens[i]); 
		}
		norm = getNorm(vec);
	}
	public static float getNorm(float[] vec) {
		float n = 0;
		for (int i = 0; i < vec.length; i++)
			n += vec[i]*vec[i];
		n = (float) Math.sqrt(n);
		return n;
	}
	public static float cosSim(float[] vec1, float[] vec2) {
		float num = 0, den = 0;
		for (int i = 0; i < vec1.length; i++)
			num += vec1[i] * vec2[i];
		den = getNorm(vec1) * getNorm(vec2);
		float ans = Float.MAX_VALUE;
		if (den != 0)
			ans = num/den;
		return ans;
	}
}
public class Word2VecLoader {
	public static HashMap<String, WordVec> wordVecMap;
	public static void load() {
		WordVecs.loadFromTextFile();
		wordVecMap = WordVecs.wordvecmap;
	}
	public static void main(String[] args) {
		load();
		float[] vec = wordVecMap.get("man").vec;
		for (int i = 0; i < vec.length; i++)
			System.out.print(vec[i] + " ");
	}
}

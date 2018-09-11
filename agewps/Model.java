package agewps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Model {

	float[] param;
	public static float score(float[] param, List<String> candidates, String text, HashMap<String,ArrayList<String>> pos, HashMap<String,ArrayList<String>> ner) {
		float value = 0;
		float[] phi = FeatureComputer.computeFeatures(candidates, text, pos, ner);
		for (int i = 0; i < phi.length; i++) {
			value += param[i] * phi[i]; 
		}
		return value;
	}
	public static float[] getProbDistribution(float[] param, List<String> beam, String text, HashMap<String,ArrayList<String>> pos, HashMap<String,ArrayList<String>> ner) {
		float[] distr = new float[beam.size()];
		float denom = 0;
		float max = -1;
		int position = -1;
		for (int i = 0; i < distr.length; i++) {
			String prog = beam.get(i).replaceAll(",", ", ");
			String[] candidates = prog.split("\n");
			List<String> cand = Arrays.asList(candidates);
			double term = Math.exp(score(param, cand, text, pos, ner));
			distr[i] = (float) term;
			denom += (float) term;
		}
		for (int i = 0; i < distr.length; i++) {
			distr[i] = distr[i]/denom;
			System.out.println(distr[i]+"|"+beam.get(i));
			if (distr[i] > max) {
				max = distr[i];
				position = i;
			}
		}
		if (position != -1)
			System.out.println(beam.get(position));
		return distr;
	}
	public static float[] gradient(float[] param, List<String> beam, List<String> tp, String text, HashMap<String,ArrayList<String>> pos, HashMap<String,ArrayList<String>> ner) {
		float[] gradient = new float[param.length];
		float[] pdistr = new float[beam.size()];
		float[] qdistr = new float[beam.size()];
		float denom = 0;
		//System.out.println("beam");
		for (int i = 0; i < beam.size(); i++) {
			String prog = beam.get(i).replaceAll(",", ", ");
			String[] candidates = prog.split("\n");
			List<String> cand = Arrays.asList(candidates);
			float value = score(param, cand, text, pos, ner);
			//System.out.println(value);
			double term = Math.exp(value);
			pdistr[i] = (float) term;
			denom += (float) term;
		}
		//System.out.println("denom"+denom);
		for (int i = 0; i < pdistr.length; i++) {
			pdistr[i] = pdistr[i]/denom;
		}
		float truedenom = 0;
		for (int i = 0; i < beam.size(); i++) {
			String prog = beam.get(i);
			qdistr[i] = 0;
			boolean tflag = false;
			for (int j = 0; j < tp.size(); j++) {
				if (tp.get(j).contains(prog)) {
					tflag = true;
					break;
				}
			}
			prog = prog.replaceAll(",", ", ");
			if (!tflag) 
				continue;
			String[] candidates = prog.split("\n");
			String[] actCand = prog.split("\n");
			for (int j = 0; j < candidates.length-1; j++) {
				actCand[j] = candidates[j];
			}
			List<String> cand = Arrays.asList(actCand);
			double term = Math.exp(score(param, cand, text, pos, ner));
			qdistr[i] = (float) term;
			truedenom += (float) term;
		}
		if (truedenom != 0) {
			for (int i = 0; i < qdistr.length; i++) {
				qdistr[i] = qdistr[i]/truedenom;
			}
		}
		for (int i = 0; i < param.length; i++) {
			gradient[i] = 0;
			//System.out.println("g"+pdistr[i]+"|"+qdistr[i]);
		}
		for (int j = 0; j < pdistr.length; j++) {
			String prog = beam.get(j).replaceAll(",", ", ");
			String[] candidates = prog.split("\n");
			List<String> cand = Arrays.asList(candidates);
			float[] phi = FeatureComputer.computeFeatures(cand, text, pos, ner);
			float diff = qdistr[j] - pdistr[j];
			for (int k = 0; k < param.length; k++) {
				gradient[k] = gradient[k] + (phi[k] * diff);
			}
		}
		return gradient;
	}
	public static void main(String[] args) throws Exception {
		String allowed = "age(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {ner=DURATION,agecount})\n"+
				 "difference(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,dcount})\n"+
				 "ratio(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,rcount})\n"+
				 "product(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,pcount})\n"+
				 "sum(pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,xyear,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, {pos=CD,scount})\n";
		String qAllowed = "age(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "offset({ner=DATE,yeara})\n"+
				  "difference(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "ratio(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "sum(pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n"+
				  "product(pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)}, pos=JJ+NN, {ner=DATE,yeara,currentYear,shiftPlus(ner=DURATION),shiftMinus(ner=DURATION)})\n";
		Word2VecLoader.load();
		ArrayList<String> wp = Learner.loadWordProblems("wp-train");
		float[] trained = {(float)-0.063570105, (float)-1.3632379, (float)-0.5281737, (float)-0.9562842, (float)-2.84, (float)-1.4002669};
		for (String wop : wp) {
			ArrayList<String> beam = Learner.getBeam(wop, trained, allowed, qAllowed, "-1");
			HashMap<String,ArrayList<String>> pos = CandidateProgramGenerator.pos;
			HashMap<String,ArrayList<String>> ner = CandidateProgramGenerator.ner;
			getProbDistribution(trained, beam, wop, pos, ner);
		}
	}
}

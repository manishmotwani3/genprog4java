package ylyu1.wean;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

public class GPDataStorer implements Serializable{
	
	public boolean repair;
	public boolean good;
	public int variant;
	public String errorMessage;
	public ArrayList<ArrayList<Double>> fitscores;
	public ArrayList<ArrayList<Integer>> divscores;
	
	public GPDataStorer(boolean good1, boolean repair1, int variant1, String errorMessage1, ArrayList<ArrayList<Double>> fitscores1, ArrayList<ArrayList<Integer>> div)
	{
		repair=repair1;
		good=good1;
		variant=variant1;
		errorMessage=errorMessage1;
		fitscores=fitscores1;
		divscores = div;
	}
	
}
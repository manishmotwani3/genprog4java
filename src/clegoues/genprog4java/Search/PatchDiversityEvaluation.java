package clegoues.genprog4java.Search;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.java.ClassInfo;
import clegoues.genprog4java.main.Configuration;
import clegoues.genprog4java.mut.EditOperation;
import clegoues.genprog4java.rep.JavaRepresentation;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;

//This is not an actual search algorithm. This evaluates the diversity of patches generated by other techniques.
public class PatchDiversityEvaluation<G extends EditOperation> extends Search<G> {

	public static final ConfigurationBuilder.RegistryToken token =
			ConfigurationBuilder.getToken();
	
	//Pair contains patch info in the form of <Seed, Variant Number>
	public static ArrayList<Pair<String, String>> patches = new ConfigurationBuilder<ArrayList<Pair<String, String>>>()
		.withVarName("patchesToAnalyze")
		.withFlag("patchesToAnalyze")
		.withDefault("_patches.csv")
		.withCast( new ConfigurationBuilder.LexicalCast< ArrayList<Pair<String, String>>>(){
			public ArrayList<Pair<String, String>> parse( String value ) {
				ArrayList<Pair<String, String>> patches = new ArrayList<Pair<String, String>>();
				if (!value.isEmpty()) {
					try (BufferedReader reader = new BufferedReader(new FileReader(value))) {
						String line;
						while(((line = reader.readLine()) != null) && !line.isEmpty()) {
							String[] parts = line.split(",");
							if(parts.length < 2)
								continue;
							String seed = parts[0].trim();
							String variantNum = parts[1].trim();							
							patches.add(Pair.of(seed, variantNum));
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return patches;
			}
		})
		.build();
	
	public PatchDiversityEvaluation(Fitness engine) {
		super(engine);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 
	 * @param original not used
	 * @param incomingPopulation not used
	 * @returns a population containing the patches specified
	 */
	@Override
	protected Population<G> initialize(Representation<G> original, Population<G> incomingPopulation)
			throws RepairFoundException, GiveUpException {
		// TODO Auto-generated method stub
		Population<G> patchesPop = new Population<>();
		
		for(Pair<String, String> patch : patches)
		{
			String seed = patch.getLeft();
			String varNum = patch.getRight();
			
			//path is hardcoded for the Docker containers that Zhen is using
			String path = String.format("%s/__variantsSeed%s/%s/variant%s", Configuration.workingDir, seed, Configuration.outputDir, varNum);
			try {
				JavaRepresentation patchRep = new JavaRepresentation();
				patchRep.load(Configuration.targetClassNames, path);
				patchRep.setVariantID(String.format("seed%s_variant%s", seed, varNum));;
				patchesPop.add((Representation<G>) patchRep);
				logger.info(String.format("Loaded %s\n", patchRep.getVariantID()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return patchesPop;
	}

	@Override
	protected void runAlgorithm(Representation<G> original, Population<G> initialPopulation)
			throws RepairFoundException, GiveUpException {
		// TODO Auto-generated method stub
		Population<G> patches = initialize(null, null);
	}
	
}

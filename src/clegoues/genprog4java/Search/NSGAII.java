package clegoues.genprog4java.Search;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.fitness.Objective;
import clegoues.genprog4java.main.Configuration;
import clegoues.genprog4java.mut.EditOperation;
import clegoues.genprog4java.rep.Representation;
import ylyu1.wean.VariantCheckerMain;

public class NSGAII<G extends EditOperation> extends Search<G> {
	private static final int P_DOMINATES_Q = -1;
	private static final int Q_DOMINATES_P = 1;
	private static final int NO_DOMINATION = 0;
	
	
	private int generationsRun = 0;
	
	protected Objective[] objectivesToTest;
	
	public NSGAII(Fitness engine, Objective[] objectives) {
		super(engine);
		objectivesToTest = objectives;
	}
	
	
	//copied from GeneticProgramming
	/*
	 * prepares for GA by registering available mutations (including templates
	 * if applicable) and reducing the search space, and then generates the
	 * initial population, using [incoming_pop] if non-empty, or by randomly
	 * mutating the [original]. The resulting population is evaluated for
	 * fitness before being returned. This may terminate early if a repair is
	 * found in the initial population (by [calculate_fitness]).
	 * 
	 * @param original original variant
	 * 
	 * @param incoming_pop possibly empty, incoming population
	 * 
	 * @return initial_population generated by mutating the original
	 */
	protected Population<G> initialize(Representation<G> original,
			Population<G> incomingPopulation) throws RepairFoundException, GiveUpException {
		original.getLocalization().reduceSearchSpace();

		Population<G> initialPopulation = incomingPopulation;

		if (incomingPopulation != null
				&& incomingPopulation.size() > incomingPopulation.getPopsize()) {
			initialPopulation = incomingPopulation.firstN(incomingPopulation
					.getPopsize());
		} 
		int stillNeed = initialPopulation.getPopsize()*2
				- initialPopulation.size();
		if (stillNeed > 0) {
			initialPopulation.add(original.copy());
			stillNeed--;
		}
		for (int i = 0; i < stillNeed; i++) {
			Representation<G> newItem = original.copy();
			this.mutate(newItem);
			initialPopulation.add(newItem);
		}

		//i'm keeping this for the sake of compiling & moving mutants & determining # of test cases passed
		for (Representation<G> item : initialPopulation) {
			if (fitnessEngine.testFitness(0, item)) {
				this.noteSuccess(item, original, 0);
				if(!continueSearch) {
					throw new RepairFoundException();
				}
			}
			copyClassFilesIntoOutputDir(item); //relies on testFitness compiling the Representation item
		}
		
		//set each mutant's fitness equal to the reverse of its domination rank
		List<List<Representation<G>>> nonDomFronts = fastNonDominatedSort(initialPopulation, objectivesToTest, 0);
		int numFronts = nonDomFronts.size();
		for(Representation<G> r : initialPopulation)
		{
			r.setFitness(numFronts - r.getDominationRank());
		}
		
		return initialPopulation;
	}
	
	
	protected void runAlgorithm(Representation<G> original, Population<G> initialPopulation) throws RepairFoundException, GiveUpException 
	{
		logger.info("search: NSGA-II begins\n");
		
		assert (Search.generations >= 0);

		// Step 0: run daikon
		System.out.println("mode: "+Configuration.invariantCheckerMode);
		if(Configuration.invariantCheckerMode>0)
		{
			int trials = 0;
			while((trials<5)&&(!(new File(Configuration.workingDir+"/JUSTUSE.ywl")).exists()))
			{
				System.out.println("Here we are");
				VariantCheckerMain.runDaikon();
				trials++;
			}//VariantCheckerMain.checkInvariantOrig();
			if(!(new File(Configuration.workingDir+"/JUSTUSE.ywl")).exists())
			{
				DataProcessor.storeError("weirddaikon");
				Runtime.getRuntime().exit(1);
			}
		}
		
		Population<G> parentPopulation = this.initialize(original, initialPopulation);
		
		int gen = 1;
		/* for gen=1, assign each representation a fitness score based on the
		 * nondomination level of each representation, then use tournament
		 * selection, recombination, & mutation to create an initial offspring population
		 */

		while(true) //condition used to be gen < Search.generations
		{
			Population<G> offspringPopulation = parentPopulation.copy();
			offspringPopulation.selection(offspringPopulation.getPopsize());
			offspringPopulation.crossover(original);
			ArrayList<Representation<G>> newlist = new ArrayList<Representation<G>>();
			for (Representation<G> item : offspringPopulation) {
				
				Representation<G> newItem =item.copy();
				this.mutate(newItem);
				newlist.add(newItem);
			}
			offspringPopulation.getPopulation().addAll(newlist);
			//i'm keeping this for the sake of compiling & moving mutants & determining # of test cases passed
			for (Representation<G> item : initialPopulation) {
				if (fitnessEngine.testFitness(0, item)) {
					this.noteSuccess(item, original, 0);
					if(!continueSearch) {
						throw new RepairFoundException();
					}
				}
				copyClassFilesIntoOutputDir(item); //relies on testFitness compiling the Representation item
			}
			//offspring population is now prepared
		
			gen++;
			if(gen == Search.generations) break;
			
			Population<G> mergedPop = Population.union(parentPopulation, offspringPopulation);
			List<List<Representation<G>>> nonDomFronts = fastNonDominatedSort(mergedPop, objectivesToTest, gen);
			Population<G> nextGenParentPop = new Population<G>();
			int i = 0;
			for(; i < nonDomFronts.size() 
					&& nextGenParentPop.size() + nonDomFronts.get(i).size() <= parentPopulation.size(); 
					i++)
			{
				crowdingDistanceAssignment(nonDomFronts.get(i), objectivesToTest, gen);
				nextGenParentPop.addAll(nonDomFronts.get(i));
			}
			if (i < nonDomFronts.size() && nextGenParentPop.size() < parentPopulation.size())
			{
				List<Representation<G>> nextFront = nonDomFronts.get(i);
				crowdingDistanceAssignment(nextFront, objectivesToTest, gen);
				Collections.sort(nextFront, new Comparator<Representation<G>>() 
				{
					@Override
					public int compare(Representation<G> o1, Representation<G> o2) {
						assert o1.getDominationRank() == o2.getDominationRank(); //these representations come from the same front
						double crowdDistDiff = o2.getCrowdingDistance() - o1.getCrowdingDistance();
						return -1 * (crowdDistDiff < 0 ? -1 : crowdDistDiff == 0 ? 0 : 1); //sort in descending order
					}
				});
				int stillNeed = parentPopulation.size() - nextGenParentPop.size();
				assert stillNeed < nextFront.size();
				//otherwise, there's a contradiction, as if we need to fill >= than what nextFront has available, then either we're out of fronts, or we're still in the above for loop
				nextGenParentPop.addAll(nextFront.subList(nextFront.size() - stillNeed, nextFront.size()));
			}
			parentPopulation = nextGenParentPop;
		}
	}
	
	private static <G extends EditOperation> void crowdingDistanceAssignment(List<Representation<G>> solutions, Objective[] objectives, int generation)
	{
		int len = solutions.size();
		for(Representation<G> p : solutions) p.setCrowdingDistance(0);
		for(Objective o : objectives)
		{
			List<Representation<G>> orderedSolutions = new ArrayList<>(solutions);
			Collections.sort(orderedSolutions, new Comparator<Representation<G>>() {
				@Override
				public int compare(Representation<G> r1, Representation<G> r2) 
				{
					double diff = o.getScore(r2, generation) - o.getScore(r1, generation);
					return diff < 0 ? -1 : diff == 0 ? 0 : 1;
				}
			});
			orderedSolutions.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			orderedSolutions.get(len-1).setCrowdingDistance(Double.POSITIVE_INFINITY);
			double objMinVal = o.getScore(orderedSolutions.get(0), generation);
			double objMaxVal = o.getScore(orderedSolutions.get(len-1), generation);
			for(int i = 1; i <= len-2; i++)
			{
				Representation<G> soln = orderedSolutions.get(i);
				Representation<G> prevSoln = orderedSolutions.get(i-1);
				Representation<G> nextSoln = orderedSolutions.get(i+1);
				double normObjDist = (o.getScore(nextSoln, generation) - o.getScore(prevSoln, generation)) / (objMaxVal - objMinVal);
				double newDist = soln.getCrowdingDistance() + normObjDist;
				soln.setCrowdingDistance(newDist);
			}
		}
	}
	
	private static <G extends EditOperation> List<List<Representation<G>>> fastNonDominatedSort(Population<G> population, Objective[] objectives, int generation)
	{
		List<List<Representation<G>>> fronts = new ArrayList<>();
		fronts.add(0, new ArrayList<Representation<G>>());
		Map<Representation<G>, List<Representation<G>>> dominatedProgramsMap = new HashMap<>(); //maps program p -> {programs dominated by p}
		Map<Representation<G>, Integer> dominationCountMap = new HashMap<>(); //maps program p -> num of programs that dominate p
		for(Representation<G> p : population)
		{
			dominatedProgramsMap.put(p, new ArrayList<Representation<G>>());
			dominationCountMap.put(p, 0);
			for(Representation<G> q : population)
			{
				if(p == q) continue; //don't bother comparing the same program, it's a waste of time
				int dominanceStatus = getDominance(p, q, objectives, generation);
				if (dominanceStatus == P_DOMINATES_Q)
				{
					dominatedProgramsMap.get(p).add(q);
				}
				else if(dominanceStatus == Q_DOMINATES_P)
				{
					dominationCountMap.put(p, dominationCountMap.get(p) + 1);
				}
			}
			if(dominationCountMap.get(p) == 0)
			{
				p.setDominationRank(0);
				fronts.get(0).add(p);
			}
		}
		
		int frontNum = 0;
		while (frontNum < fronts.size() && !fronts.get(frontNum).isEmpty()) //if frontNum >= fronts.size(), then there's no frontNum-th front, meaning that it's empty
		{
			assert frontNum == fronts.size() - 1;
			List<Representation<G>> qSet = new ArrayList<>();
			for(Representation<G> p : fronts.get(frontNum))
			{
				for(Representation<G> q : dominatedProgramsMap.get(p))
				{
					Integer nqNew = dominationCountMap.get(q) - 1;
					dominationCountMap.put(q, nqNew);
					if (nqNew == 0)
					{
						q.setDominationRank(frontNum + 1);
						qSet.add(q);
					}
				}
			}
			frontNum++;
			if(!qSet.isEmpty()) fronts.add(qSet);
		}
		
		return fronts;
	}
	
	
	/**
	 * @return -1 if p dominates q, 1 if q dominates p, 0 if there's no relationship
	 */
	private static int getDominance(Representation p, Representation q, Objective[] objectives, int generation)
	{
		boolean pDomQ = true;
		boolean qDomP = true;
		for(Objective o : objectives)
		{
			double pScore = o.getScore(p, generation);
			double qScore = o.getScore(q, generation);
			if(pDomQ && pScore < qScore) pDomQ = false;
			if(qDomP && qScore < pScore) qDomP = false;
		}
		if (pDomQ && qDomP) return NO_DOMINATION; //pDomQ & qDomP => All scores are equal
		if (pDomQ) return P_DOMINATES_Q;
		if (qDomP) return Q_DOMINATES_P;
		return NO_DOMINATION;
	}
	
	
	//copied from GeneticProgramming
	/*
	 * Copies the compiled source files (from classSourceFolder, as defined in the .config file) to the outputDir (default for experiments: the tmp folder)
	 * @param item
	 */
	private void copyClassFilesIntoOutputDir(Representation<G> item)
	{
		if (item.getVariantFolder().equals(""))
		{
			//if there's no variant folder name, do nothing
			return;
		}
		
		String copyDestination = Configuration.outputDir + //no space added
				(Configuration.outputDir.endsWith(File.separator) ? "" : File.separator) + //add a separator if necessary
				"d_" + item.getVariantFolder();
		
		File dFolder = new File(copyDestination);
		if(!dFolder.exists())
			dFolder.mkdirs();
		
		File classSourceFolderFile = new File(Configuration.classSourceFolder);
		if(!classSourceFolderFile.exists())
			System.err.println("classSourceFolder does not exist");
		
		
		/*
		CommandLine cpCommand = CommandLine.parse(
				"cp -R " +
				Configuration.classSourceFolder + //no space added
				(Configuration.classSourceFolder.endsWith(File.separator) ? "" : File.separator) + //add a separator if necessary
				"* " + //a wildcard char may or may not be needed
				copyDestination
				);
		*/
		CommandLine cpCommand = CommandLine.parse(
				"rsync -r " +
				Configuration.classSourceFolder + //no space added
				(Configuration.classSourceFolder.endsWith(File.separator) ? "" : File.separator) + //add a separator if necessary
				" " +
				copyDestination
				);
		
		System.err.println("cp command: " + cpCommand);
		
		ExecuteWatchdog watchdog = new ExecuteWatchdog(1000000);
		DefaultExecutor executor = new DefaultExecutor();
		String workingDirectory = System.getProperty("user.dir");
		executor.setWorkingDirectory(new File(workingDirectory));
		executor.setWatchdog(watchdog);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		executor.setExitValue(0);

		executor.setStreamHandler(new PumpStreamHandler(out));
		
		try
		{
			executor.execute(cpCommand);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try
			{
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

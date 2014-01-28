package clegoues.genprog4java.rep;

import java.util.List;
import clegoues.genprog4java.Fitness.TestCase;

enum Mutation { APPEND, DELETE, REPLACE, SWAP };

public interface Representation<G,C> {

		boolean getVariableLength();
		List<G> getGenome();
		void LoadGenomeFromString(String genome);
		void setGenome(List<G> genome);
		int genomeLength();
		void noteSuccess();
		void load(String filename);
		void serialize(String filename);
		boolean deserialize(String filename);
		void debugInfo();
		int maxAtom(); // atomid type?
		List<AtomPair> getFaultyAtoms();
		List<AtomPair> getFixSourceAtoms();
		boolean sanityCheck() throws SanityCheckException;
		void computeLocalization();
		void fromSource(String filename);
		void outputSource(String filename);
		List<String> sourceName();
		void cleanup();
		void setFitness(double fitness);
		double getFitness();
		boolean fitnessIsValid();
		boolean compile(String sourceName, String exeName);
		boolean testCase(TestCase test);
		String getName();
		List<History<C>> getHistory();
		void addHistory(History<C> newItem);
		void reduceSearchSpace(); // do this?
		void reduceFixSpace(); 
		List<Mutation> availableMutations(int atomId);
		// TODO: do we need  availableCrossoverPoints? Crossover is so stupid.
		void delete(int atomId);
		void append(int whereToAppend, int whatToAppend);
		List<AtomPair> appendSources(int atomId);
		void swap(int swap1, int swap2);
		List<AtomPair> swapSources(int atomId);
		void replace(int whatToReplace, int whatToReplaceWith);
		List<AtomPair> replaceSources(int atomId);
// ignoring subatoms for now
// ignoring atomToStr because I think that should go in whatever implements the code fragments
// also leaving out hash unless we need it

}

package clegoues.genprog4java.Search;

import static clegoues.util.ConfigurationBuilder.INT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.main.Configuration;
import clegoues.genprog4java.mut.EditOperation;
import clegoues.genprog4java.rep.Representation;
import clegoues.util.ConfigurationBuilder;

public class RandomSingleEdit<G extends EditOperation> extends Search<G>{


	public static final ConfigurationBuilder.RegistryToken token =
			ConfigurationBuilder.getToken();

	protected static int maxVariants = ConfigurationBuilder.of( INT )
			.withVarName( "maxVariants" )
			.withDefault( "50" )
			.withHelp( "maximum number of variants to consider" )
			.inGroup( "Search Parameters" )
			.build();

	public RandomSingleEdit(Fitness engine) {
		super(engine);
		engine.initializeModel();
	}

	@Override
	protected Population<G> initialize(Representation<G> original, Population<G> incomingPopulation)
			throws RepairFoundException, GiveUpException {
		original.getLocalization().reduceSearchSpace();
		return null;
	}

	@Override
	protected void runAlgorithm(Representation<G> original, Population<G> initialPopulation)
			throws RepairFoundException, GiveUpException {
		int numVariantsConsidered = 0;
		while(numVariantsConsidered < RandomSingleEdit.maxVariants) {
			Representation<G> variant = original.copy();
			mutate(variant);
			if (fitnessEngine.testToFirstFailure(variant, true)) { 
				this.noteSuccess(variant, original, 0);
				if(!continueSearch) 
					return;
			}
			numVariantsConsidered++;
			copyClassFilesIntoOutputDir(variant);
		}
	}

	/**
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
		
		//System.err.println("cp command: " + cpCommand);
		
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

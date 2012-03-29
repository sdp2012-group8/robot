package sdp.AI.genetic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.io.*;

import sdp.AI.neural.AINeuralNet;


public class GeneticAlgorithm {

	/** Number of generations to simulate */
	final static int GENERATIONS = 50;
	/** Number of games each individual plays against every other */
	final static int GAMES = 1;
	/** Size of the population */
	final static int POPSIZE = 20;
	/** Probability that a crossover will occur */
	final static float CROSSOVER_PROB = 0.6F;
	/** Probability that a mutation will occur */
	final static float MUTATE_PROB = 0.001F;
	/** Number of genes in each individual */
	final static int GENE_NUMBER = AINeuralNet.getWeightsCount();
	/** Number of neighbours each individual plays against. Must be odd*/
	final static int NEIGHBOUR_NUMBER = 11;
	
	final static int MAX_NUM_SIMULT_GAMES = 10;

	int gen = 0;
	long fitTotal = 0;
	long[] popFitness;
	long[] avgFitness;
	double[][] population;
	double[][] intPopulation;
	double[][] finalPopulation;
	double[] finalPopFitness;
	Random rand = new Random();
	private volatile int currNumRunningGames = 0;


	FileWriter fstream;
	PrintWriter out;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new GeneticAlgorithm();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public GeneticAlgorithm() throws IOException {
		fstream = new FileWriter("out.txt");
		out = new PrintWriter(fstream);
		finalPopulation = new double[POPSIZE][GENE_NUMBER];
		finalPopFitness = new double[POPSIZE+1];


		setup();
		out.println("\nInitial population\n");
		printPop();

		for (gen = 1; gen < GENERATIONS+1; gen++) {
			run();
			out.println("\nGeneration " + gen);

			printPop();
		}
		System.arraycopy(population, 0, finalPopulation, 0, POPSIZE);
		System.arraycopy(popFitness, 0, finalPopFitness, 0, POPSIZE);
		finalPopFitness[POPSIZE] = avgFitness[gen-1];
		out.println("\n\n");

		out.close();

		fstream = new FileWriter("finalPopulations.txt");
		out = new PrintWriter(fstream);

		out.println("Final Population\n");

		for (int j = 0; j < POPSIZE; j++) {
			out.print(String.format("%02.3f", finalPopFitness[j]) + " - ");
			for (int k = 0; k < GENE_NUMBER; k++) {
				out.print(finalPopulation[j][k] + " ");
			}
			out.println("");
		}
		out.println("Average - " + finalPopFitness[POPSIZE] + "\n");

		out.close();

		fstream = new FileWriter("Generations Fitness.txt");
		out = new PrintWriter(fstream);

		out.println("Generations Fitness\n");

		for (int i = 0; i < GENERATIONS+1; i=i+50) {
			out.println(avgFitness[i]);
		}
		out.close();

		System.out.println("Finished");
	}

	/** 
	 * Prints the current population and fitness to the file "out.txt"
	 **/
	private void printPop() {
		for (int i = 0; i < POPSIZE; i++) {
			out.print(popFitness[i] + " - ");
			for (int j = 0; j < GENE_NUMBER; j++) {
				out.print(population[i][j] + " ");
			}
			out.println("");
		}
		out.println("Average - " + avgFitness[gen]);
	}


	/** 
	 * Randomly generates the initial population and calculates its fitness
	 **/
	private void setup() {
		// Create initial random population
		population = new double[POPSIZE][GENE_NUMBER];
		intPopulation = new double[POPSIZE][GENE_NUMBER];
		popFitness = new long[POPSIZE];
		avgFitness = new long[GENERATIONS+1];
		gen = 0;
		fitTotal = 0;

		for (int i = 0; i < POPSIZE; i++) {
				population[i] = AINeuralNet.getRandomWeights();
		}
		calcFitness();
	}

	/** 
	 * The main loop that calculates the next generation
	 **/
	private void run() {
		// Calculate the next generation
		SUSampling();
		//roulette();		
		crossover();
		mutate();
		population = new double[POPSIZE][GENE_NUMBER];
		for (int i = 0; i < POPSIZE; i++) {
			System.arraycopy(intPopulation[i],0,population[i],0,GENE_NUMBER);
		}

		calcFitness();
	}

	/** 
	 * Selects the intermediate population using Stochastic Universal Sampling.
	 **/
	private void SUSampling() {
		int j = 0;
		double sum = rand.nextDouble()*fitTotal/POPSIZE; // Choose a random starting place
		for (int i = 0; i < POPSIZE; i++) {
			sum += popFitness[i];
			while (sum > fitTotal/POPSIZE && j < POPSIZE) {
				System.arraycopy(population[i],0,intPopulation[j],0,GENE_NUMBER);
				sum -= fitTotal/POPSIZE;
				j++;
			}
		}
	}

	/** 
	 * Selects each pair of chromosome's and performs crossover at a random point.
	 **/
	private void crossover() {
		for (int i = 0; i < POPSIZE; i++) {
			if (rand.nextInt((int) (1/CROSSOVER_PROB)) == 0) {
				int cross1 = rand.nextInt(POPSIZE);
				int cross2 = rand.nextInt(POPSIZE);
				int crossoverPoint = rand.nextInt(GENE_NUMBER);
				double[] tmp = new double[crossoverPoint];
				System.arraycopy(intPopulation[cross1], 0, tmp, 0, crossoverPoint);
				System.arraycopy(intPopulation[cross2], 0, intPopulation[cross1], 0, crossoverPoint);
				System.arraycopy(tmp, 0, intPopulation[cross2], 0, crossoverPoint);			
			}
		}
	}

	/** 
	 * Loops over each gene and mutates it with a probability determined by mutateProb.
	 **/
	private void mutate() {
		for (int i = 0; i < POPSIZE; i++) {
			for (int j = 0; j < GENE_NUMBER; j++) {
				if (rand.nextInt((int) (1/MUTATE_PROB)) == 0) {
					intPopulation[i][j] = rand.nextDouble();
				}
			}
		}
	}
	

	/** 
	 * Calculates the fitness by running games against each individuals closest neighbours.
	 * Each game is run in its own thread.
	 **/
	private synchronized long[] calcFitness() {

		// reset counter
		currNumRunningGames = 0;

		// initialize result holder
		final HashMap<Integer, HashSet<Long>> results = new HashMap<Integer, HashSet<Long>>();

		// initialize already played holder
		final HashMap<Integer, HashSet<Integer>> alreadyPlayed = new HashMap<Integer, HashSet<Integer>>();

		// initialize thread (Game) holder
		final ArrayList<Game> games = new ArrayList<Game>();

		// traverse through the population
		for (int i = 0; i < POPSIZE; i++) {

			// get the neighbors against I have already played
			HashSet<Integer> currPlayed = alreadyPlayed.get(i);

			// if we don't have it in the DB, create it
			if (currPlayed == null)
				currPlayed = new HashSet<Integer>();

			// find neighbor to play against
			for (int j = 0; j < NEIGHBOUR_NUMBER; j++) {

				// calculate neighbor index
				int index = j - (NEIGHBOUR_NUMBER-1)/2;

				// wrap around neighboring index
				if (index < 0) index = POPSIZE+index;
				if (index > POPSIZE) index = index - POPSIZE;

				// if we have played against this neighbor, skip
				if (currPlayed.contains(index))
					continue;

				// mark neighbor
				currPlayed.add(index);

				// initialize a game with the given neighbor
				games.add(new Game(i, index, population, new GameCallback() {

					// when a game thread finishes
					@Override
					public void onFinished(final long[] fitness, final int[] ids) {

						// save result
						synchronized (results) {

							// for both players
							for (int i = 0; i < 2; i++) {

								// get previous fitness values
								HashSet<Long> prevFitness = results.get(ids[i]);

								// if none exist, create the set
								if (prevFitness == null)
									prevFitness = new HashSet<Long>();

								// add the current fitness to the set
								prevFitness.add(fitness[i]);

								// put the results so far back to the id
								results.put(ids[i], prevFitness);
							}
						}

						// start the next game(s)
						startMoreGames(games, true);
					}
				}));

			}

			// update neighbors list
			alreadyPlayed.put(i, currPlayed);

		}
		
		// start the first few games
		startMoreGames(games, false);

		// block until all threads have finished
		while (currNumRunningGames > 0) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}

		// initialize average array
		final long[] averageFitness = new long[results.size()];
		
		// calculate average
		for (int i = 0; i < averageFitness.length; i++)
			averageFitness[i] = getAverage(results.get(i));

		// return result
		return averageFitness;
	}
	
	/**
	 * Ensures that {@value #MAX_NUM_SIMULT_GAMES} {@link Game}s are running at any time.
	 * 
	 * @param games list of games to be ran/are running
	 * @param oneHasFinished if the number of running threads should be reduced
	 */
	private synchronized void startMoreGames(final ArrayList<Game> games, final boolean oneHasFinished) {

		// reduce number of running games counter
		if (oneHasFinished)
			currNumRunningGames--;
		
		// if too many threads are already running, ignore
		if (currNumRunningGames >= MAX_NUM_SIMULT_GAMES)
			return;

		// loop through all games
		for (final Game game : games) {

			// if game is running or ready, ignore
			if (game.getState() != Game.state.ready)
				continue;

			// if game is not running, start it
			game.startInNewThread();

			// increment the number of running threads
			currNumRunningGames++;

			// if enough threads are running, exit loop
			if (currNumRunningGames >= MAX_NUM_SIMULT_GAMES)
				break;

		}
	}
	
	/**
	 * Get the average of a hashset
	 * @param set
	 * @return
	 */
	public static long getAverage(final HashSet<Long> set) {
		
		// if set does not exist, then average is 0
		if (set == null)
			return 0;
		
		// get set count
		final int set_count = set.size();
		
		// sum holder
		long sum = 0;
		
		// sum all over the set
		for (final long fitness : set)
			sum += fitness;
		
		// calculate average
		return sum / set_count;
	}

}

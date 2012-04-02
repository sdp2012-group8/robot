package sdp.AI.genetic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.io.*;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.util.NeuralNetworkCODEC;

import sdp.AI.genetic.distributed.GameRunnerServer;

import sdp.AI.neural.AINeuralNet;


public class GeneticAlgorithm {

	/** Number of games each individual plays against every other */
	final static int GAMES = 1;
	/** Size of the population */
	final static int POPSIZE = 20;
	/** Probability that a crossover will occur */
	final static float CROSSOVER_PROB = 0.6F;
	/** Probability that a mutation will occur */
	final static float MUTATE_PROB = 0.0001F;
	/** Number of genes in each individual */
	final static int GENE_NUMBER = AINeuralNet.getWeightsCount();
	/** Number of neighbours each individual plays against. Must be odd*/
	final static int NEIGHBOUR_NUMBER = 11;
	/** Number of threads. Every thread can simulate one game at a time */
	final static int LOCAL_GAME_THREADS = 4;
	/** For distributed game simulation */
	final static String[] DISTRIBUTED_GAMES_THREADS_HOST_NAMES = new String[]{"elis"};//[]{"ssh.remote.one", "ssh.rmote.two"};
	
	static long max_fitness;
	
	final static String OUTPUT_DIR = "data/GA/";
	private static final String NNET_FILE = "data/GA/finalPop.nnet";

	int gen = 0;
	long fitTotal = 0;
	long[] popFitness;
	ArrayList<Long> avgFitness;
	double[][] population;
	double[][] intPopulation;
	double[][] finalPopulation;
	Random rand = new Random();
	private int totalGames = 0;
	// locker for concurrency control
	private GameRunner[] workers = new GameRunner[LOCAL_GAME_THREADS + DISTRIBUTED_GAMES_THREADS_HOST_NAMES.length];
	private Object lock = new Object();



	FileWriter fstream;
	PrintWriter out;
	
	private volatile boolean run = true;
	
	private final String ssh_username, ssh_password;
	
	public GeneticAlgorithm(String username, String password) {
		ssh_username = username;
		ssh_password = password;
	}

	public void stop() {
		run = false;
		for (int i = 0; i < workers.length; i++) {
			if (workers[i] != null) {
				workers[i].close();
			}
		}
	}
	
	public void start() {
		run = true;
		
		// start workers
				for (int i = 0; i < LOCAL_GAME_THREADS; i++) {
					workers[i] = new GameRunner();
					workers[i].start();
				}
				
				if (DISTRIBUTED_GAMES_THREADS_HOST_NAMES.length > 0)
					System.out.println("Waiting for clients to connect");
				
				for (int i = LOCAL_GAME_THREADS; i < LOCAL_GAME_THREADS+DISTRIBUTED_GAMES_THREADS_HOST_NAMES.length; i++) {
					System.out.println("Waiting for "+DISTRIBUTED_GAMES_THREADS_HOST_NAMES[i-LOCAL_GAME_THREADS]+" to connect..."); 
					final GameRunnerServer worker = new GameRunnerServer(DISTRIBUTED_GAMES_THREADS_HOST_NAMES[i-LOCAL_GAME_THREADS], ssh_username, ssh_password);
					while (!worker.assigned) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					workers[i] = worker;
				}

				System.out.println("Starting setup");
				setup();
				//out.println("\nInitial population\n");
				//printPop();
				System.out.println("Starting evolution");
				for (gen = 1; run; gen++) {
					
					run();
					long fittest = popFitness[findFittest()];
					System.out.println("Generation: " + gen + "  average fitness: " + avgFitness.get(avgFitness.size()-1) + "  fittest: " + fittest);
					//out.println("\nGeneration " + gen);

					new AINeuralNet(population[findFittest()]).getNetwork().save(OUTPUT_DIR+"finalPop.nnet");
				}

				/* Print the final generation */
				System.out.println("Saving final population");

				new AINeuralNet(population[findFittest()]).getNetwork().save(OUTPUT_DIR+"finalPop.nnet");
				

				/* Print the average fitness of each generation */
				System.out.println("Printing average fitness of each generation");
				try {
					fstream = new FileWriter(OUTPUT_DIR+"Generations Fitness.csv");
				} catch (IOException e) {
					e.printStackTrace();
				}
				out = new PrintWriter(fstream);

				out.println("Generations Fitness\n");

				for ( Long iter : avgFitness) {
					out.print(iter.toString() + ",");
				}
				out.close();

				System.out.println("Finished");

				// stop workers
				for (int i = 0; i < workers.length; i++)
					workers[i].close();
	}


	/** 
	 * Randomly generates the initial population and calculates its fitness
	 **/
	private void setup() {
		// Create initial random population
		population = new double[POPSIZE][GENE_NUMBER];
		intPopulation = new double[POPSIZE][GENE_NUMBER];
		popFitness = new long[POPSIZE];
		avgFitness = new ArrayList<Long>();
		gen = 0;
		fitTotal = 0;

		for (int i = 0; i < POPSIZE; i++) {
			//population[i] = AINeuralNet.getRandomWeights();
			NeuralNetworkCODEC.network2array(NeuralNetwork.load(NNET_FILE), population[i]);
		}
		popFitness = calcFitness();
		avgFitness.add(getAverage(popFitness));
	}

	/** 
	 * The main loop that calculates the next generation
	 **/
	private void run() {
		// Calculate the next generation
		SUSampling();	
		crossover();
		mutate();
		population = new double[POPSIZE][GENE_NUMBER];
		for (int i = 0; i < POPSIZE; i++) {
			System.arraycopy(intPopulation[i],0,population[i],0,GENE_NUMBER);
		}

		popFitness = calcFitness();
		avgFitness.add(getAverage(popFitness));
	}

	/** 
	 * Selects the intermediate population using Stochastic Universal Sampling.
	 **/
	private void SUSampling() {
		/* Elitism 
		 * Find best individual
		 * */
		System.arraycopy(population[findFittest()],0,intPopulation[0],0,GENE_NUMBER);
		
		/* Stochastic sampling
		 * Selects individuals from the population to create the next generation
		 */
		int j = 1;
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
	
	private int findFittest() {
		Long max = null;
		int index = 0;
		for (int i = 0; i < POPSIZE; i++) {
			if (max == null || max < popFitness[i]){
				max = popFitness[i];
				index = i;
			}
		}
		return index;
	}


	/** 
	 * Calculates the fitness by running games against each individuals closest neighbours.
	 * Each game is run in its own thread.
	 **/
	private long[] calcFitness() {

		// ----- INITIALIZATION OF GAMES ----- \\

		// initialise result holder
		final HashMap<Integer, HashSet<Long>> results = new HashMap<Integer, HashSet<Long>>();

		// initialise already played holder
		final HashMap<Integer, HashSet<Integer>> alreadyPlayed = new HashMap<Integer, HashSet<Integer>>();

		// initialise thread (Game) holder
		final ArrayList<Game> games = new ArrayList<Game>();

		// for counting games
		int game_id = 0;

		// traverse through the population
		for (int i = -1; i < POPSIZE; i++) {

			// get the neighbours against I have already played
			HashSet<Integer> currPlayed = alreadyPlayed.get(i);

			// if we don't have it in the DB, create it
			if (currPlayed == null)
				currPlayed = new HashSet<Integer>();

			// find neighbour to play against
			for (int j = 0; j < NEIGHBOUR_NUMBER; j++) {

				// calculate neighbour index
				int index = i - (j - (NEIGHBOUR_NUMBER-1)/2);

				// wrap around neighbouring index
				if (index < -1) index = POPSIZE + index + 1;
				if (index >= POPSIZE) index = index - (POPSIZE + 1);
				
				if (i == -1 && index == -1)
					continue;

				// if we have played against this neighbour, skip
				if (currPlayed.contains(index))
					continue;

				// mark neighbour
				currPlayed.add(index);

				// initialise a game with the given neighbour
				games.add(new Game(i, index, i == -1 ? null : population[i], j == -1 ? null : population[index], game_id++));

			}

			// update neighbours list
			alreadyPlayed.put(i, currPlayed);
			
			for (int index : currPlayed){
				HashSet<Integer> played = alreadyPlayed.get(index);
				if (played == null)
					played = new HashSet<Integer>();
				
				if (!played.contains(i)) {
					played.add(i);
					alreadyPlayed.put(index, played);
				}
			}

		}

		// max number of games
		totalGames = game_id;

		// ----- ASSIGNING GAMES TO WORKERS ----- \\

		// calculate how many games per worker should there be
		final int gamesPerWorker = totalGames / workers.length;

		// pause workers and set callbacks
		for (int i = 0; i < workers.length; i++) {

			workers[i].setPause(true);

			workers[i].callback = new GameRunner.Callback() {

				// when all of the games have been simulated
				@Override
				public void allGamesReady() {

					// check whether all workers have finished
					for (int i = 0; i < workers.length; i++)
						if (workers[i].getGamesInQueueCount() != 0) {
							return;
						}


					// if all workers are 0, unblock main thread
					synchronized (lock) {
						lock.notifyAll();
					}
				}
				
				@Override
				public synchronized void onFinished(final Game caller, final long[] fitness) {

					// for both players
					for (int i = 0; i < 2; i++) {
						// get previous fitness values
						HashSet<Long> prevFitness = results.get(caller.ids[i]);

						// if none exist, create the set
						if (prevFitness == null)
							prevFitness = new HashSet<Long>();

						// add the current fitness to the set
						prevFitness.add(fitness[i]);

						// put the results so far back to the id
						results.put(caller.ids[i], prevFitness);
					}
				}
			};
		}

		// initialise worker id count
		int workerId = 0;

		for (int i = 0; i < totalGames; i++) {

			// decide when to change worker
			if (i % gamesPerWorker == 0 && i != 0)
				workerId++;
			// limit worker id
			if (workerId == workers.length)
				workerId = workers.length -1;

			// assign a game to a worker
			workers[workerId].add(games.get(i));

		}

		// unpause workers
		for (int i = 0; i < workers.length; i++)
			workers[i].setPause(false);

		// ----- ENTER BLOCKING SECTION ----- \\

		// block until all threads have finished
		synchronized (lock) {
			boolean wait = true;
			try {
				while (wait) {
					lock.wait(3000);

					// check if workers are ready
					for (int i = 0; i < workers.length; i++)
						if (workers[i].getGamesInQueueCount() != 0) {
							continue;
						}

					break;

				}
			} catch (Exception e) {
				wait = false;
			}
		}

		// ----- CALCULATIONS HAVE BEEN DONE BY HERE ----- \\

		// initialise average array
		final long[] averageFitness = new long[POPSIZE];

		// calculate average
		for (int i = 0; i < POPSIZE; i++) {
			averageFitness[i] = getAverage(results.get(i));
			//System.out.println("average[" + i + "]: " + averageFitness[i]);
		}

		// return result
		return averageFitness;
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
		for (final long fitness : set){
			sum += fitness;
		}

		// calculate average
		return sum / set_count;
	}

	public static long getAverage(final long[] set) {

		// if set does not exist, then average is 0
		if (set == null)
			return 0;

		// get set count
		final int set_count = set.length;

		// sum holder
		long sum = 0;

		// sum all over the set
		for (final long fitness : set){
			sum += fitness;
		}

		// calculate average
		return sum / set_count;
	}

}

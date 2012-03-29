package sdp.AI.genetic;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.io.*;


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
	final static int GENE_NUMBER = 100;
	/** Number of neighbours each individual plays against */
	final static int NEIGHBOUR_NUMBER = 10;

	int gen = 0;
	long fitTotal = 0;
	long[] popFitness;
	long[] avgFitness;
	Double[][] population;
	Double[][] intPopulation;
	Double[][] finalPopulation;
	double[] finalPopFitness;
	Random rand = new Random();


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
		finalPopulation = new Double[POPSIZE][GENE_NUMBER];
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
			out.print(String.format("%02.3f", popFitness[i]) + " - ");
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
		population = new Double[POPSIZE][GENE_NUMBER];
		intPopulation = new Double[POPSIZE][GENE_NUMBER];
		popFitness = new long[POPSIZE];
		avgFitness = new long[GENERATIONS+1];
		gen = 0;
		fitTotal = 0;

		for (int i = 0; i < POPSIZE; i++) {
			for (int j = 0; j < GENE_NUMBER; j++) {
				population[i][j] = rand.nextDouble();
			}
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
		population = new Double[POPSIZE][GENE_NUMBER];
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
				int[] tmp = new int[crossoverPoint];
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
					if (intPopulation[i][j] == 1D) {
						intPopulation[i][j] = 0D;
					} else {
						intPopulation[i][j] = 1D;
					}
				}
			}
		}
	}
	
	/** 
	 * Calculates the fitness based on the fitness method.
	 **/
	private void calcFitness() {
		fitTotal = 0;
		
		for (int i = 0; i < POPSIZE; i++) {
			popFitness[i] = fitnessAll(population[i]);
			fitTotal += popFitness[i];
		}
		avgFitness[gen] = fitTotal/POPSIZE;
	}
	
	/** 
	 * Calculates the fitness by running games against each individuals closest neighbours.
	 * Each game is run in its own thread.
	 **/
	private void calcFitnessLimited() {
		fitTotal = 0;
		HashMap<Integer[], Future<Long>> results = new HashMap<Integer[], Future<Long>>();
		HashMap<Integer, HashSet<Integer>> alreadyPlayed = new HashMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < POPSIZE; i++) {
			for (int j = 0; j < NEIGHBOUR_NUMBER; j++) {
				int index = j - (NEIGHBOUR_NUMBER-1)/2;
				/* Limit index to POPSIZE */
				if (index < 0) index = POPSIZE+index;
				if (index > POPSIZE) index = index - POPSIZE;
				
				HashSet<Integer> currPlayed = alreadyPlayed.get(i);
				
				if (currPlayed != null && currPlayed.contains(index))
					continue;
				
				if (currPlayed == null)
					currPlayed = new HashSet<Integer>();
					
				currPlayed.add(index);
				alreadyPlayed.put(i, currPlayed);
				
				
				// TODO: Test if the match has already been played  //if (alreadyPlayed.containsKey(i) && alreadyPlayed.get(i));
				Callable<Long> game = new Game(new AINeuralNetwork(population[i]), new AINeuralNetwork(population[j]));
				popFitness[i] = fitnessAll(population[i]);
				fitTotal += popFitness[i];
			}
		}
		
		avgFitness[gen] = fitTotal/POPSIZE;
	}

	/** 
	 * Calculates an individual's fitness by averaging the results of playing 
	 * against each individual in the population. 
	 **/
	private long fitnessAll(Double[] chromosome) {
		long total = 0;
		for (int i = 0; i < POPSIZE; i++) {
			total += play(chromosome, population[i]);
		}

		return total/POPSIZE;
	}

	/** 
	 * Calculates an individual's fitness by averaging the results of playing 
	 * against each individual within a set distance of itself.
	 * The distance is determined by the constant NEIGHBOUR_NUMBER
	 **/
	private long fitnessLimited(Double[] chromosome) {
		long total = 0;
		for (int i = 0; i < POPSIZE; i++) {
			total += play(chromosome, population[i]);
		}

		return total/POPSIZE;
	}

	/** 
	 * Plays p1 against p2 a number times (determined by games constant) and averages the result.
	 * @param p1 The float array representing the chromosome of individual 1.
	 * @param p2 The float array representing the chromosome of individual 2.
	 * @return Returns the average score of p1 against p2.
	 **/
	private long play(Double[] p1, Double[] p2) {
		//TODO: Run simulated games in threads
		return 0;
	}

}

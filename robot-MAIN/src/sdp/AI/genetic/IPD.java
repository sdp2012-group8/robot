package sdp.AI.genetic;
import java.util.Random;
import java.io.*;


public class IPD {

	final int runs = 1;
	final int generations = 1000;
	final int memory = 5;
	final int games = 5;

	final int popSize = 50;
	final double crossProb = 0.6;
	final double mutateProb = 0.001;

	int T = 5;	
	int C = 3;
	int D = 1;
	int S = 0;

	int gen = 0;
	double fitTotal = 0;
	double[] popFitness;
	double[] avgFitness;
	int[][] population;
	int[][] intPopulation;
	int[][][] finalPopulations;
	double[][] finalPopFitness;
	Random rand = new Random();
	int length = (int) Math.pow(2,(memory*2)) + 2*memory;

	FileWriter fstream;
	PrintWriter out;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new IPD();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public IPD() throws IOException {
		fstream = new FileWriter("out.txt");
		out = new PrintWriter(fstream);
		finalPopulations = new int[runs][popSize][length];
		finalPopFitness = new double[runs][popSize+1];

		for (int i = 0; i < runs; i++) {	
			System.out.println("Run " + (i+1));
			setup();
			out.println("Run " + (i+1));
			out.println("\nInitial population\n");
			printPop();

			for (gen = 1; gen < generations+1; gen++) {
				run();
				out.println("\nGeneration " + gen);

				printPop();
			}
			System.arraycopy(population, 0, finalPopulations[i], 0, popSize);
			System.arraycopy(popFitness, 0, finalPopFitness[i], 0, popSize);
			finalPopFitness[i][popSize] = avgFitness[gen-1];
			out.println("\n\n");
		}
		out.close();

		fstream = new FileWriter("finalPopulations.txt");
		out = new PrintWriter(fstream);

		out.println("Final Populations\n");
		for (int i = 0; i < runs; i++) {
			out.println("Run " + (i+1));
			for (int j = 0; j < popSize; j++) {
				out.print(String.format("%02.3f", finalPopFitness[i][j]) + " - ");
				for (int k = 0; k < length; k++) {
					out.print(finalPopulations[i][j][k] + " ");
				}
				out.println("");
			}
			out.println("Average - " + finalPopFitness[i][popSize] + "\n");
		}
		out.close();
		
		fstream = new FileWriter("Generations Fitness.txt");
		out = new PrintWriter(fstream);

		out.println("Generations Fitness\n");
		
		for (int i = 0; i < generations+1; i=i+50) {
			out.println(avgFitness[i]);
		}
		out.close();
		
		System.out.println("Finished");
	}

	/** 
	 * Prints the current population and fitness to the file "out.txt"
	 **/
	private void printPop() {
		for (int i = 0; i < popSize; i++) {
			out.print(String.format("%02.3f", popFitness[i]) + " - ");
			for (int j = 0; j < length; j++) {
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
		population = new int[popSize][length];
		intPopulation = new int[popSize][length];
		popFitness = new double[popSize];
		avgFitness = new double[generations+1];
		gen = 0;
		fitTotal = 0;

		for (int i = 0; i < popSize; i++) {
			for (int j = 0; j < length; j++) {
				population[i][j] = rand.nextInt(2);
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
		population = new int[popSize][length];
		for (int i = 0; i < popSize; i++) {
			System.arraycopy(intPopulation[i],0,population[i],0,length);
		}
		
		calcFitness();
	}

	/** 
	 * Calculates the fitness based on the fitness method.
	 **/
	private void calcFitness() {
		fitTotal = 0;
		for (int i = 0; i < popSize; i++) {
			popFitness[i] = fitness(population[i]);
			fitTotal += popFitness[i];
		}
		avgFitness[gen] = fitTotal/popSize;
	}

	/** 
	 * Selects the intermediate population using the Roulette wheel method.
	 **/
	private void roulette() {
		for (int i = 0; i < popSize; i++) {
			double sum = 0;
			int choose = rand.nextInt((int)fitTotal+1);
			for (int j = 0; j < popSize; j++) {
				sum += popFitness[j];
				if (sum > choose) {
					System.arraycopy(population[j],0,intPopulation[i],0,length);
					break;
				}
			}			
		}
	}

	/** 
	 * Selects the intermediate population using Stochastic Universal Sampling.
	 **/
	private void SUSampling() {
		int j = 0;
		double sum = rand.nextDouble()*fitTotal/popSize; // Choose a random starting place
		for (int i = 0; i < popSize; i++) {
			sum += popFitness[i];
			while (sum > fitTotal/popSize && j < popSize) {
				System.arraycopy(population[i],0,intPopulation[j],0,length);
				sum -= fitTotal/popSize;
				j++;
			}
		}
	}

	/** 
	 * Selects each pair of chromosome's and performs crossover at a random point.
	 **/
	private void crossover() {
		for (int i = 0; i < popSize; i++) {
			if (rand.nextInt((int) (1/crossProb)) == 0) {
				int cross1 = rand.nextInt(popSize);
				int cross2 = rand.nextInt(popSize);
				int crossoverPoint = rand.nextInt(length);
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
		for (int i = 0; i < popSize; i++) {
			for (int j = 0; j < length; j++) {
				if (rand.nextInt((int) (1/mutateProb)) == 0) {
					if (intPopulation[i][j] == 1) {
						intPopulation[i][j] = 0;
					} else {
						intPopulation[i][j] = 1;
					}
				}
			}
		}
	}

	/** 
	 * Calculates a chromosome's fitness by averaging the results of playing 
	 * against each chromosome in the population. 
	 **/
	private double fitness(int[] chromosome) {
		double total = 0;
		for (int i = 0; i < popSize; i++) {
			total += play(chromosome, population[i]);
		}

		return total/popSize;
	}

	/** 
	 * Plays p1 against p2 a number times (determined by games variable) and averages the result.
	 * @param p1 The int array representing the chromosome of player 1.
	 * @param p2 The int array representing the chromosome of player 2.
	 * @return Returns the average score of p1 against p2.
	 **/
	private double play(int[] p1, int[] p2) {
		int previous1;
		int previous2;
		double score1 = 0;

		String tPrev = "";
		for (int i = length-2*memory; i < length; i++) {
			tPrev += p1[i];
		}
		previous1 = Integer.parseInt(tPrev, 2);
		
		tPrev = "";
		for (int i = length-2*memory; i < length; i++) {
			tPrev += p2[i];
		}
		previous2 = Integer.parseInt(tPrev, 2);

		for (int i = 0; i < games; i++) {
			int move1 = p1[previous1];
			int move2 = p2[previous2];

			if (move1 + move2 == 2) { // both cooperate
				score1 += C;
			} else if (move1 > move2) { // p1 cooperates, p2 doesn't
				score1 += S;
			} else if (move1 < move2) {
				score1 += T;
			} else if (move1 + move2 == 0) { // both defect
				score1 += D;
			}

			// Calculate previous move
			previous1 = previous1 << 2; // Clear last bits
			int test1 = (int) Math.pow(2, memory*2 + 1);
			if (previous1 > test1 - 1) previous1 -= test1 ; // Remove extra bits
			if (previous1 > length-2*memory - 1) previous1 -= length-2*memory;
			previous1 += move1*2;
			previous1 += move2;

			previous2 = previous2 << 2; // Clear last bits
			int test2 = (int) Math.pow(2, memory*2 + 1);
			if (previous2 > test2 - 1) previous2 -= test2 ; // Remove extra bits
			if (previous2 > length-2*memory - 1) previous2 -= length-2*memory;
			previous2 += move2*2;
			previous2 += move1;

		}

		return score1/games;
	}

}

package sdp.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import sdp.AI.genetic.GeneticAlgorithm;

public class GeneticAlgorithmTrainer {
	
	private enum commands { start, stop, exit };
	
	private static Thread runner = new Thread() {
		public void run() {
			algorithm = new GeneticAlgorithm();
			algorithm.start();
		};
	};
	private static GeneticAlgorithm algorithm;
	
	public static void execCommand(commands command) {
		switch (command) {
		case stop:
			algorithm.stop();
			break;
		case exit:
			System.exit(0);
			break;
		case start:
			runner.start();
			break;
		default:
			System.err.println("Unimplemented command.");
			break;
		}
	}
	
	public static void main(String[] args) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); 
		commands[] cs = commands.values();
		
		String comText = "("+cs[0];
		for (int i = 1; i < cs.length; i++)
			comText += ", "+cs[i];
		comText += ")";
		
		execCommand(commands.start);
		
		while (true) {
			try {
				System.out.println("Enter a command "+comText+":");
				final String comm = in.readLine().trim();
				execCommand(commands.valueOf(comm));
			} catch (Exception e) {
				System.err.println("Unknown command");
			}
			
		}
	}

}

package sdp.gui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import sdp.AI.genetic.GeneticAlgorithm;

public class GeneticAlgorithmTrainer {
	
	private enum commands { start, stop, exit, password };
	// number of arguments each command must take
	private final static int[] numars = new int[] {0, 0, 0, 0};
	
	private static BufferedReader in;
	
	private static Thread runner = new Thread() {
		public void run() {
			algorithm = new GeneticAlgorithm(uname, password);
			algorithm.start();
		};
	};
	private static GeneticAlgorithm algorithm;
	private static String password = "";
	private static String uname = "";
	
	public static void execCommand(commands command, String[] args) {
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
		case password:
			
			System.out.println("Username:");
			try {
				uname = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			char[] passwd = null;
			Console cons;
			if ((cons = System.console()) != null &&
					(passwd = cons.readPassword("Password for "+uname+":")) != null) {
				password = String.valueOf(passwd);
			} else
				System.err.println("CANT READ PASSWORD!");
			
			break;
		default:
			System.err.println("Unimplemented command.");
			break;
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// open terminal if no arguments are supplied
		if (args.length == 0) {
			try {
				spawnGnomeTerminal("nice java -cp ../robot-NXT/bin:lib/jsch-0.1.46.jar:lib/neuroph-2.6.jar:lib/encog-engine-2.5.0.jar:lib/jbox2d-library-2.1.2.2-jar-with-dependencies.jar:bin sdp.gui.GeneticAlgorithmTrainer dontOpenGnomeTerminal");
				System.out.println("Gnome terminal open");
				System.exit(0);
			} catch (Exception e) {
				System.out.println("Gnome terminal cannot be opened, proceeding with default one");
			}
		}
		
		in = new BufferedReader(new InputStreamReader(System.in)); 
		commands[] cs = commands.values();
		
		String comText = "("+cs[0];
		for (int i = 1; i < cs.length; i++) {
			comText += ", "+cs[i];
			if (numars[i] != 0)
				comText += "{"+numars[i]+"}";
		}
		comText += ")";
		
		if (password.equals(""))
			System.out.println("Note: don't forget to use password to set ssh password before running start!\n\n");
		//execCommand(commands.start, new String[0]);
		
		while (true) {
			try {
				System.out.println("Enter a command "+comText+":");
				final String[] comm = in.readLine().trim().split(" ");
				
				final ArrayList<String> ars = new ArrayList<String>();
				for (int i = 1; i < comm.length; i++) {
					final String a = comm[i].trim();
					if (!a.trim().equals(""))
						ars.add(a);
				}
				
				execCommand(commands.valueOf(comm[0].trim()), ars.toArray(new String[0]));
			} catch (Exception e) {
				System.err.println("Unknown command");
			}
			
		}
	}
	
	private static void spawnGnomeTerminal(String command) throws IOException {
		new ProcessBuilder("gnome-terminal", "-e",  "bash -c \""+command+"; exec bash\"").start();
	}

}

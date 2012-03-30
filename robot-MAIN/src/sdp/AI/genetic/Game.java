package sdp.AI.genetic;

import java.util.Random;

import sdp.AI.neural.AINeuralNet;

/**
 * A game simulation
 */
public class Game {
	
	/** The states that the game could be at
	 * @see Game#getState() */
	public enum state {ready, running, finished};
	
	/** use to access the ids of the robots that play in this game */
	public final int[] ids;
	/** use to get the current game id */
	public final int gameId;
	
	/** callback to be notified when the game ends */
	private Callback callback;
	/** current state of the game */
	private volatile state currentState = state.ready;
	
	/**
	 * Initialize a new game
	 * @param i the left robot id
	 * @param j thr right robot id
	 * @param population arrays to pick from
	 * @param callback to be used to notify caller back when game is finished and pass scores
	 * @param gameId the id of the current game
	 */
	public Game(int i, int j, final double[][] population, Callback callback, int gameId) {
		new AINeuralNet(population[i]);
		new AINeuralNet(population[j]);
		ids = new int[]{i, j};
		this.callback = callback;
		this.gameId = gameId;
	}
	
	/**
	 * Does the simulation (in current thread)
	 */
	public void simulate() {
		currentState = state.running;
		
		// simulate some simulation :)
		Random r = new Random();
		for (int i = 0; i < 5; i++) {
			try {
				Thread.sleep(r.nextInt(5));
			} catch (InterruptedException e) {
			}
		}
		
		currentState = state.finished;
		callback.onFinished(this, new long[]{0, 1});
	}
	
	/**
	 * @return current game state
	 */
	public state getState() {
		return currentState;
	}
	
	// callback section
	
	/**
	 * A callback that receives fitness scores after the game has been finised
	 * 
	 * @author Martin Marinov
	 *
	 */
	public static interface Callback {
		
		/**
		 * When game finishes, this gets called
		 * @param the game that the result is coming from
		 * @param fitness of network 0 and 1
		 */
		public void onFinished(final Game caller, final long[] fitness);

	}

	
}

package sdp.AI.genetic;

import java.util.Random;

import sdp.AI.neural.AINeuralNet;

/**
 * A game simulation
 */
public class Game {
	
	public enum state {ready, running, finished};
	
	private GameCallback callback;
	public final int[] ids;
	public final int gameId;
	private volatile state currentState = state.ready;
	
	public Game(int i, int j, final double[][] population, GameCallback callback, int gameId) {
		new AINeuralNet(population[i]);
		new AINeuralNet(population[j]);
		ids = new int[]{i, j};
		this.callback = callback;
		this.gameId = gameId;
	}
	
	/**
	 * Does the simulation in current thread
	 */
	public void simulate() {
		currentState = state.running;
		
		// simulate some simulation :)
		Random r = new Random();
		for (int i = 0; i < 5; i++) {
			try {
				Thread.sleep(r.nextInt(100));
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

	
}

package sdp.AI.genetic;

import sdp.AI.neural.AINeuralNet;

/**
 * A game simulation
 */
public class Game {
	
	public enum state {ready, running, finished};
	
	private GameCallback callback;
	private final int[] ids;
	private volatile state currentState = state.ready;
	
	public Game(int i, int j, final double[][] population, GameCallback callback) {
		new AINeuralNet(population[i]);
		new AINeuralNet(population[j]);
		ids = new int[]{i, j};
		this.callback = callback;
	}
	
	/**
	 * Simulate game in new thread
	 */
	public void startInNewThread() {
		
		// if already running, don't start
		if (currentState != state.ready)
			return;
		
		// set running
		currentState = state.running;
		
		// simulate in a new thread
		new Thread() {
			public void run() {
				simulate();
			};
		}.start();
	}
	
	/**
	 * Does the simulation in current thread
	 */
	public void simulate() {
		currentState = state.running;
		
		currentState = state.finished;
		callback.onFinished(new long[]{0, 1}, ids);
	}
	
	/**
	 * @return current game state
	 */
	public state getState() {
		return currentState;
	}

	
}

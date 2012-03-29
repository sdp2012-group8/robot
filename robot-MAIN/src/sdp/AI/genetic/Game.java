package sdp.AI.genetic;

import java.util.concurrent.Callable;

import sdp.AI.neural.AINeuralNet;

public class Game implements Callable<Long>{
	
	private GameCallback callback;
	private final int[] ids;
	private boolean isRunning = false;
	
	public Game(int i, int j, final double[][] population, GameCallback callback) {
		new AINeuralNet(population[i]);
		new AINeuralNet(population[j]);
		ids = new int[]{i, j};
		this.callback = callback;
	}
	
	public void startInNewThread() {
		new Thread() {
			public void run() {
				isRunning = true;
				
				// simulation
				
				isRunning = false;
				callback.onFinished(new long[]{0, 1}, ids);
				
			};
		}.start();
	}
	
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public Long call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	
}

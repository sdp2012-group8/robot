package sdp.AI.genetic;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Runs Games that are passed to it
 * 
 * @author Martin Marinov
 *
 */
public class GameRunner extends Thread {
	
	/** game container */
	private Queue<Game> games_to_run = new LinkedList<Game>();
	private boolean pause = false;
	
	/**
	 * Add game to be run
	 * @param game
	 */
	public void add(final Game game) {
		games_to_run.add(game);
	}

	/**
	 * Use {@link #start()} to access this method.
	 */
	@Override
	public void run() {
		while(!interrupted()) {
			if (!pause) {
				
				final Game gameToExecute = games_to_run.poll();

				if (gameToExecute != null) {
					
					// if not empty, simulate
					gameToExecute.simulate();
					
				} else {
					// if empty, sleep a bit
					try {
						sleep(10);
					} catch (InterruptedException e) {}
				}
				
			} else {
				// if paused, sleep a bit
				try {
					sleep(10);
				} catch (InterruptedException e) {}
			}

		}
	}

	/**
	 * Get the number of games left
	 * @return
	 */
	public int getRemainingGames() {
		return games_to_run.size();
	}
	
	/**
	 * Will not simulate any games if paused
	 * @param paused
	 */
	public void setPause(boolean paused) {
		this.pause = paused;
	}
	
	/**
	 * Is simulation paused
	 * @return
	 */
	public boolean isPaused() {
		return pause;
	}
}

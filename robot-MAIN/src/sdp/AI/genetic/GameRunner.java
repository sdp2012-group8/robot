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
	
	/** get the number of games that have not finished. Don't modify this! */
	public volatile int count = 0;
	/** set calback to receive updates. Only one callback can be set per worker */
	public Callback callback = null;
	
	/** game container */
	private volatile Queue<Game> games_to_run = new LinkedList<Game>();
	private boolean pause = false;
	
	/**
	 * Add game to be run
	 * @param game
	 */
	public void add(final Game game) {
		count++;
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
					count--;
					
					// notify callback
					if (callback != null)
						callback.allGamesReady();
					
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
	
	
	// callback section
	
	/**
	 * Override this to receive a notification when
	 * all queued games have been simulated.
	 * 
	 * @author Martin Marinov
	 *
	 */
	public static interface Callback {
		
		/**
		 * Will get triggered when all queued games have been simulated
		 */
		public void allGamesReady();

	}
}

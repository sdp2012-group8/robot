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
	
	/** set calback to receive updates. Only one callback can be set per worker */
	public Callback callback = null;
	
	/** game container */
	protected volatile Queue<Game> games_to_run = new LinkedList<Game>();
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
						
					announceFinished(gameToExecute, gameToExecute.simulate());
					
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
	
	protected void announceFinished(final Game caller, final long[] fitness) {
		if (callback != null) {
			callback.onFinished(caller, caller.simulate());
			if (games_to_run.size() == 0)
				callback.allGamesReady();
		}
	}
	
	public void close() {
		
	}
	
	public int getGamesInQueueCount() {
		return games_to_run.size();
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
		
		/**
		 * When game finishes, this gets called
		 * @param the game that the result is coming from
		 * @param fitness of network 0 and 1
		 */
		public void onFinished(final Game caller, final long[] fitness);


	}
}

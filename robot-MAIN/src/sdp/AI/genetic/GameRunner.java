package sdp.AI.genetic;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Runs Games that are passed to it
 * 
 * @author Martin Marinov
 *
 */
public class GameRunner {
	
	/** set calback to receive updates. Only one callback can be set per worker */
	public Callback callback = null;
	
	/** game container */
	protected volatile Queue<Game> games_to_run = new LinkedList<Game>();
	private boolean pause = false;
	
	private Thread worker;
	
	/**
	 * Add game to be run
	 * @param game
	 */
	public void add(final Game game) {
		games_to_run.add(game);
	}


	public void start() {
		worker = new Thread() {
			@Override
			public void run() {
				while(!interrupted()) {
					if (!pause) {
						Game gameToExecute = null;
						
						try {
							gameToExecute = games_to_run.poll();
						} catch (NoSuchElementException e) {}
						
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
		};
		worker.start();
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
			callback.onFinished(caller, fitness);
			if (games_to_run.size() == 0)
				callback.allGamesReady();
		}
	}
	
	public void close() {
		worker.interrupt();
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

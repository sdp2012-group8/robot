package sdp.common;

import java.util.Observable;
import java.util.Observer;


/**
 * A wrapper for WorldStateProvider.getCurrentState() method that allows to
 * access the next unseen world state.
 * 
 * @author Gediminas Liktaras
 */
public final class WorldStateObserver implements Observer {
	
	/** Next fresh state. */
	private WorldState worldState;
	/** Whether the currently stored state has been returned. */
	private boolean stateIsFresh;
	
	/** Observer's thread lock. Use this object to wait() and notifyAll(). */
	private Object lock;
	
	
	/**
	 * The main constructor.
	 * 
	 * @param provider World state provider to listen to.
	 */
	public WorldStateObserver(WorldStateProvider provider) {
		provider.addObserver(this);
		
		stateIsFresh = false;
		worldState = null;
		
		lock = new Object();
	}
	
	
	/**
	 * Get the next unseen world state from the state provider.
	 * 
	 * If there is no fresh frame available yet, this method will block until
	 * it becomes available.
	 * 
	 * @return The next unseen world state.
	 */
	public WorldState getNextState() {
		while (!stateIsFresh) {
			try {
				synchronized (lock) {
					lock.wait();
				}
			} catch (InterruptedException e) {
				// TODO: handle in a meaningful way. Or at least log it.
			}
		}
		
		synchronized (lock) {
			stateIsFresh = false;
			return worldState;
		}
	}


	/**
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof WorldStateProvider) {
			synchronized(lock) {
				worldState = (WorldState) arg;
				stateIsFresh = true;
				lock.notifyAll();
			}
		}
	}
}

package sdp.common;

import java.util.Observable;
import java.util.Observer;


/**
 * Base class for all classes that provide world state changes to the system.
 * 
 * @author Gediminas Liktaras
 */
public abstract class WorldStateProvider extends Observable {
	
	/** Current world state. */
	private WorldState state;
	
	
	/**
	 * The main constructor.
	 */
	public WorldStateProvider() {
		state = null;
	}
	
	
	/**
	 * Get the current state of the world.
	 * 
	 * @return The current state of the world.
	 */
	public synchronized WorldState getCurrentState() {
		return state;
	}
	
	/**
	 * Set the current state of the world.
	 * 
	 * @param newState The current state of the world.
	 */
	protected synchronized void setCurrentState(WorldState newState) {
		if (newState == null) {
			throw new NullPointerException("Tried to set a null world state in WorldStateProvider.");
		} else {
			state = newState;
			setChanged();
			notifyObservers(state);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Observable#addObserver(java.util.Observer)
	 */
	@Override
	public synchronized void addObserver(Observer o) {
		if (o instanceof WorldStateObserver) {
			super.addObserver(o);
		} else {
			throw new IllegalArgumentException("Tried to add invalid observer to WorldStateProvider.");
		}
	}
}
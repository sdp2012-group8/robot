package sdp.common;


/**
 * An interface for objects that wish to receive information about the changes
 * of the world state.
 * 
 * @author Gediminas Liktaras
 */
public interface WorldStateCallback {
	
	/**
	 * This method is called when the next world state becomes available.
	 * 
	 * @param state The next world state.
	 */
	public void nextWorldState(WorldState state);
	
}

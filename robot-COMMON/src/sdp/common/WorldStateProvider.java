package sdp.common;


/**
 * Interface for all classes that provide the world state.
 * 
 * @author Gediminas Liktaras
 */
public interface WorldStateProvider {

	/**
	 * Set the callback object that will receive world state updates.
	 * 
	 * @param callback The callback object.
	 */
	public void setCallback(WorldStateCallback callback);
}

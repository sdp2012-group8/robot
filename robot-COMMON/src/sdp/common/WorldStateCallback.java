package sdp.common;

import java.awt.image.BufferedImage;

/**
 * Interface for the world state provider's callback.
 * 
 * @author Gediminas Liktaras
 */
public interface WorldStateCallback {
	
	/**
	 * This method is called when the next world state becomes available.
	 * 
	 * @param state The next world state.
	 * @param frame The frame that corresponds to this world state.
	 */
	public void nextWorldState(WorldState state, BufferedImage frame);
}

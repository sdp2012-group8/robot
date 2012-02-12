package sdp.vision.visualinput;

import java.awt.image.BufferedImage;

/**
 * An interface for all classes that wish to receive messages from
 * VisualInputProvider.
 * 
 * @author Gediminas Liktaras
 */
public interface VisualInputCallback {

	/**
	 * This method is called by the visual input source, whenever the next
	 * frame becomes available.
	 * 
	 * @param frame A fresh frame.
	 */
	public void nextFrame(BufferedImage frame);
	
}

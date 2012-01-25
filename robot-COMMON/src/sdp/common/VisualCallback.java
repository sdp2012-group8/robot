package sdp.common;

import java.awt.image.BufferedImage;


/**
 * An interface for objects that wish to receive information about the next
 * available frame from a visual source.
 * 
 * @author Gediminas Liktaras
 */
public interface VisualCallback {
	
	/**
	 * This method is called when the next frame is available from the visual
	 * input source.
	 * 
	 * @param frame The next frame.
	 */
	public void nextFrame(BufferedImage frame);

}

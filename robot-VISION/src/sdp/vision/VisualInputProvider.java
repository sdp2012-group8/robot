package sdp.vision;

import java.awt.image.BufferedImage;


/**
 * Interface for all classes that provide visual information to the vision
 * subsystem.
 * 
 * @author Gediminas Liktaras
 */
public abstract class VisualInputProvider {
	
	/** Callback world state provider. */
	private VisualInputCallback callback;
	
	
	/**
	 * Begin video capture, using the provided object as a callback.
	 * 
	 * This method also starts the vision system's capture thread.
	 */
	public abstract void startCapture();
	
	
	/**
	 * Send the given image (the next image in the visual input stream) to the
	 * callback object.
	 * 
	 * @param frame The next frame.
	 */
	protected void sendNextFrame(BufferedImage frame) {
		if (callback == null) {
			throw new NullPointerException("Trying to send next frame, even though callback has not been set.");
		} else {
			callback.nextFrame(frame);
		}
	}
	
	/**
	 * Set a new callback object.
	 * 
	 * @param callback The callback object.
	 */
	public void setCallback(VisualInputCallback callback) {
		this.callback = callback;
	}
	
}

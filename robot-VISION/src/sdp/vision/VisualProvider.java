package sdp.vision;

import sdp.common.VisualCallback;


/**
 * Interface for all classes that provide visual information to the vision 
 * subsystem.
 * 
 * @author Gediminas Liktaras
 */
public interface VisualProvider {
	
	/**
	 * Set the provider's callback object.
	 * 
	 * @param callback The callback object.
	 */
	public void setCallback(VisualCallback callback);

	/**
	 * Begin video capture, using the provided object as a callback.
	 */
	public void startCapture();
}

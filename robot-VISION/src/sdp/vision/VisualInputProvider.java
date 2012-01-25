package sdp.vision;

import au.edu.jcu.v4l4j.CaptureCallback;


/**
 * Interface for all classes that provide visual information to the vision 
 * subsystem.
 * 
 * @author Gediminas Liktaras
 */
public interface VisualInputProvider {

	/**
	 * Begin video capture, using the provided object as a callback.
	 * 
	 * @param callback The callback object.
	 */
	public void startCapture(CaptureCallback callback);
}

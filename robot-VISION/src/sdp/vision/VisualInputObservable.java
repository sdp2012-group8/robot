package sdp.vision;

import java.util.Observable;


/**
 * Base class for all classes that provide visual information to the vision
 * subsystem.
 * 
 * @author Gediminas Liktaras
 */
public abstract class VisualInputObservable extends Observable {
	
	/**
	 * Begin video capture, using the provided object as a callback.
	 */
	public abstract void startCapture();
}

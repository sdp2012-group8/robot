package sdp.vision;

import java.awt.Point;
import java.awt.image.BufferedImage;

import sdp.common.Robot;
import sdp.common.VisualCallback;
import sdp.common.WorldState;
import sdp.common.WorldStateCallback;
import sdp.common.WorldStateProvider;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * The main vision subsystem class.
 * 
 * @author Gediminas Liktaras
 */
public class Vision implements VisualCallback, WorldStateProvider {
	
	/** Image processor. */
	OldImageProcessor imageProcessor;
	
	/** The callback object. */
	WorldStateCallback callback;
	
	
	/**
	 * The main constructor.
	 */
	public Vision() {
		imageProcessor = new OldImageProcessor();
	}
	
	
	/**
	 * Set the callback object that will receive world state updates.
	 * 
	 * @param callback The callback object.
	 */
	@Override
	public void setCallback(WorldStateCallback callback) {
		this.callback = callback;
	}
	

	/**
	 * This method is called when the next frame is available from the visual
	 * input source.
	 * 
	 * @param frame The next frame.
	 */
	@Override
	public void nextFrame(BufferedImage frame) {
		if (callback == null) {
			System.err.println("Vision callback has not been set.");
		}
		
		WorldState ws = new WorldState(new Point(0, 0), new Robot(new Point(0, 0), 0.0), new Robot(new Point(0, 0), 0.0));
		// WorldState ws = imageProcessor.getWorldState(frame.getBufferedImage());
		callback.nextWorldState(ws, frame);
	}
	
}
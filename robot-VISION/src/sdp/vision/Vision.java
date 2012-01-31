package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;


/**
 * The main vision subsystem class.
 * 
 * For the love of FSM, do not set an instance of this class as a callback to
 * multiple VisualInputProviders.
 * 
 * @author Gediminas Liktaras
 */
public class Vision extends WorldStateProvider implements VisualInputCallback {
	
	/** Old image processor. TO BE REMOVED. */
	OldImageProcessor oldImageProcessor;
	
	/** Image processor. */
	NewImageProcessor imageProcessor;
	
	
	/**
	 * The main constructor.
	 */
	public Vision() {
		oldImageProcessor = new OldImageProcessor();
		imageProcessor = new NewImageProcessor();
	}
	
	
	/**
	 * Get a copy of the image processor's configuration.
	 * 
	 * @return The image processor's configuration.
	 */
	public ImageProcessorConfiguration getConfiguration() {
		return imageProcessor.getConfiguration();
	}
	
	/**
	 * Set the image processor's configuration.
	 * 
	 * @param config The new image processor's configuration.
	 */
	public void setConfiguration(ImageProcessorConfiguration config) {
		imageProcessor.setConfiguration(config);
	}
	

	/* (non-Javadoc)
	 * @see sdp.vision.VisualInputCallback#nextFrame(java.awt.image.BufferedImage)
	 */
	@Override
	public void nextFrame(BufferedImage frame) {
		oldImageProcessor.process(frame);
		WorldState state = oldImageProcessor.worldState;
		
		setChanged();
		notifyObservers(state);
	}
	
}
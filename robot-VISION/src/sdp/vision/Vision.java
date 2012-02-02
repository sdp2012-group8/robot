package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.AlternativeImageProcessor;
import sdp.vision.processing.ImageProcessor;
import sdp.vision.processing.Team9ImageProcessor;


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
	ImageProcessor imageProcessor;
	
	
	/**
	 * The main constructor.
	 */
	public Vision() {
		oldImageProcessor = new OldImageProcessor();
		imageProcessor = new AlternativeImageProcessor();
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
//		oldImageProcessor.process(frame);
//		WorldState nextState = oldImageProcessor.worldState;
		
		WorldState nextState = imageProcessor.extractWorldState(frame);
		
		setChanged();
		notifyObservers(nextState);
	}
	
}
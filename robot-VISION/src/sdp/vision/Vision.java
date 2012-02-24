package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.vision.processing.MainImageProcessor;
import sdp.vision.processing.BaseImageProcessor;
import sdp.vision.visualinput.VisualInputCallback;


/**
 * The main vision subsystem class.
 * 
 * For the love of FSM, do not set an instance of this class as a callback to
 * multiple VisualInputProviders.
 * 
 * @author Gediminas Liktaras
 */
public class Vision extends WorldStateProvider implements VisualInputCallback {
	
	/** Image processor. */
	BaseImageProcessor imageProcessor;
	
	
	/**
	 * The main constructor.
	 */
	public Vision() {
		imageProcessor = new MainImageProcessor();
	}
	/**
	 * Create a vision with custom image processor
	 */
	public Vision(BaseImageProcessor processor) {
		imageProcessor = processor;
	}
	
	
	/**
	 * Get a copy of the image processor's configuration.
	 * 
	 * @return The image processor's configuration.
	 */
	public ImageProcessorConfig getConfiguration() {
		return imageProcessor.getConfiguration();
	}
	
	/**
	 * Set the image processor's configuration.
	 * 
	 * @param config The new image processor's configuration.
	 */
	public void setConfiguration(ImageProcessorConfig config) {
		imageProcessor.setConfiguration(config);
	}
	
	
	/**
	 * A convenience function to get the world state out of an image.
	 * 
	 * @param frame Image to process.
	 * @return World state, contained within the image.
	 */
	public WorldState extractWorldState(BufferedImage frame){
		return imageProcessor.extractWorldState(frame);	
	}

	/* (non-Javadoc)
	 * @see sdp.vision.VisualInputCallback#nextFrame(java.awt.image.BufferedImage)
	 */	
	@Override
	public void nextFrame(BufferedImage frame) {
		WorldState nextState = extractWorldState(frame);
		setChanged();
		notifyObservers(nextState);
	}
	
}

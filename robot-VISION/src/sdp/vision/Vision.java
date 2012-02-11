package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.MainImageProcessor;
import sdp.vision.processing.BaseImageProcessor;


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
	
	
	public WorldState worldImageData(BufferedImage frame){
		WorldState nextState = imageProcessor.extractWorldState(frame);
		return nextState;
	
	}

	/* (non-Javadoc)
	 * @see sdp.vision.VisualInputCallback#nextFrame(java.awt.image.BufferedImage)
	 */	
	@Override
	public void nextFrame(BufferedImage frame) {
		WorldState nextState = imageProcessor.extractWorldState(frame);
		setChanged();
		notifyObservers(nextState);
	}
	
}

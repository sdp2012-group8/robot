package sdp.vision;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.vision.processing.MainImageProcessor;
import sdp.vision.processing.ImageProcessor;


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
	ImageProcessor imageProcessor;
	
	
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
	 * @see sdp.vision.VisualInputCallback#nextFramejava.awt.image.BufferedImage)
	 */
	
	public WorldState worldImageData(BufferedImage frame){
		WorldState nextState = imageProcessor.extractWorldState(frame);
		return nextState;
	
	}
	
	@Override
	public void nextFrame(BufferedImage frame) {
		WorldState nextState = imageProcessor.extractWorldState(frame);
		System.out.print(nextState.getBallCoords());
		setChanged();
		notifyObservers(nextState);
		
	}
	
}
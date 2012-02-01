package sdp.vision.processing;

import java.awt.image.BufferedImage;

import sdp.common.WorldState;
import sdp.vision.ImageProcessorConfiguration;


/**
 * A base class for camera image information extract classes.
 * 
 * @author Gediminas Liktaras
 */
public abstract class ImageProcessor {

	/** The processor's configuration. */
	protected ImageProcessorConfiguration config;

	
	/**
	 * The default constructor.
	 */
	public ImageProcessor() {
		config = new ImageProcessorConfiguration();
	}

	
	/**
	 * Extract the world state from the supplied image.
	 * 
	 * @param frame The image to process.
	 * @return The world state, present in the image.
	 */
	public abstract WorldState extractWorldState(BufferedImage frame);

	
	/**
	 * Get current image processor configuration.
	 * 
	 * @return The current image processor configuration.
	 */
	public final ImageProcessorConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Set a new image processor configuration.
	 * 
	 * @param config The new configuration.
	 */
	public final void setConfiguration(ImageProcessorConfiguration config) {
		this.config = config;
	}

}
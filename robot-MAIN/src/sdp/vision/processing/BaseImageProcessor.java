package sdp.vision.processing;

import java.awt.image.BufferedImage;

import sdp.common.world.WorldState;


/**
 * A base class for camera image information extract classes.
 * 
 * @author Gediminas Liktaras
 */
public abstract class BaseImageProcessor {

	/** The processor's configuration. */
	protected ImageProcessorConfig config;
	


	
	/**
	 * The default constructor.
	 */
	public BaseImageProcessor() {
		config = new ImageProcessorConfig();
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
	public final synchronized ImageProcessorConfig getConfiguration() {
		return config;
	}

	/**
	 * Set a new image processor configuration.
	 * 
	 * @param config The new configuration.
	 */
	public final synchronized void setConfiguration(ImageProcessorConfig config) {
		this.config = config;
	}
	
}
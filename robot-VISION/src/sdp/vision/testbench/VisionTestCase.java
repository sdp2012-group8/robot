package sdp.vision.testbench;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import sdp.common.WorldState;

/**
 * A container for a vision test. 
 * 
 * @author Gediminas Liktaras
 */
public class VisionTestCase {
	
	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.vision.testbench.VisionTestCase");
	
	/** The image to process. */
	private BufferedImage image;
	/** Filename of the test's image. */
	private String imageFilename;
	/** Expected world state. */
	private WorldState expectedState;
	
	
	/**
	 * Create a new test case.
	 * 
	 * @param imageFilename Filename of the image file to use for testing.
	 * @param expectedState The expected world state.
	 */
	public VisionTestCase(String imageFilename, WorldState expectedState) {
		this.imageFilename = imageFilename;
		this.expectedState = expectedState;
		
		image = null;
		try {
			image = ImageIO.read(new File(imageFilename));
		} catch (IOException e) {
			LOGGER.warning("Could not read the image " + imageFilename + " for a test case.");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get the image for the vision system to process.
	 * 
	 * @return The image.
	 */
	public BufferedImage getImage() {
		return image;
	}
	
	/**
	 * Get the filename of the test's image.
	 * 
	 * @return Image's filename.
	 */
	public String getImageFilename() {
		return imageFilename;
	}

	/**
	 * Get the target world state.
	 * 
	 * @return The target world state.
	 */
	public WorldState getExpectedState() {
		return expectedState;
	}

}

package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.awt.image.BufferedImage;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import sdp.common.WorldState;


/**
 * A base class for camera image information extract classes.
 * 
 * @author Gediminas Liktaras
 */
public abstract class BaseImageProcessor {

	/** The processor's configuration. */
	protected ImageProcessorConfig config;
	
	/** Distortion coefficients for the undistortion operation. */
	private CvMat distortion;
	/** Intristic coefficients for the undistortion operation. */
	private CvMat intristic;

	
	/**
	 * The default constructor.
	 */
	public BaseImageProcessor() {
		config = new ImageProcessorConfig();
		
		distortion = CvMat.create(1, 4);
		distortion.put(0.0, 0.0, 0.0, 0.0);
		
		intristic = CvMat.create(3, 3);
		intristic.put(1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0);
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
	
	
	/**
	 * A convenience function to undistort images.
	 * 
	 * @param image Image to undistort.
	 * @return Undistorted image.
	 */
	protected synchronized IplImage undistortImage(IplImage image) {
		intristic.put(0, 0, config.getUndistort_cx());
		intristic.put(0, 2, config.getUndistort_fx());
		intristic.put(1, 1, config.getUndistort_cy());
		intristic.put(1, 2, config.getUndistort_fy());
		
		distortion.put(0, config.getUndistort_k1());
		distortion.put(1, config.getUndistort_k2());
		distortion.put(2, config.getUndistort_p1());
		distortion.put(3, config.getUndistort_p2());

		IplImage newImage = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		cvUndistort2(image, newImage, intristic, distortion);		
		return newImage;
	}

}
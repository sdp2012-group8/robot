package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_imgproc.cvUndistort2;

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
	
	
	/**
	 * A convenience function to undistort images.
	 * 
	 * @param image Image to undistort.
	 * @return Undistorted image.
	 */
	protected synchronized IplImage undistortImage(IplImage image) {
		IplImage newImage = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		
		CvMat intristic = CvMat.create(3, 3);
		intristic.put(
				config.getUndistort_cx(), 0.0, config.getUndistort_fx(),
				0.0, config.getUndistort_cy(), config.getUndistort_fy(),
				0.0, 0.0, 1.0
		);

		CvMat dist = CvMat.create(1, 5);
		dist.put(
				config.getUndistort_k1(), config.getUndistort_k2(),
				config.getUndistort_p1(), config.getUndistort_p2()
		);

		cvUndistort2(image, newImage, intristic, dist);
		
		intristic.deallocate();
		dist.deallocate();
		
		return newImage;
	}

}
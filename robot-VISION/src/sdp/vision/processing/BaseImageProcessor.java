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
	public final ImageProcessorConfig getConfiguration() {
		return config;
	}

	/**
	 * Set a new image processor configuration.
	 * 
	 * @param config The new configuration.
	 */
	public final void setConfiguration(ImageProcessorConfig config) {
		this.config = config;
	}
	
	
	/**
	 * A convenience function to undistort images.
	 * 
	 * @param image Image to undistort.
	 * @return Undistorted image.
	 */
	protected IplImage undistortImage(IplImage image) {
		IplImage newImage = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		
		CvMat intristic = CvMat.create(3, 3);
		intristic.put(
			    8.6980146658682384e+02, 0, 3.7426130495414304e+02,
			    0, 8.7340754327613899e+02, 2.8428760615670581e+02,
			    0, 0, 1);
//		intristic.put(
//    8.48981055e+03, 0, 3.08300323e+02, 0, 9.37250000e+03, 2.44421371e+02, 0,
//    0, 1);
		
		CvMat dist = CvMat.create(1, 4);
		dist.put(
			    -3.1740235091903346e-01, -8.6157434640872499e-02, 9.2026812110876845e-03,
			    4.4950266773574115e-03);
//		dist.put(
//			    -4.53893204e+01, 1.41253042e-00, -1.08707204e-01, -3.45645750e-01
//				);
		
		cvUndistort2(image, newImage, intristic, dist);
		
		intristic.deallocate();
		dist.deallocate();
		
		return newImage;
	}

}
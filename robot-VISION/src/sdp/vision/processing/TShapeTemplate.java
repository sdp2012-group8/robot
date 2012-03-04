package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.cv2DRotationMatrix;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvWarpAffine;

import java.awt.Point;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


/**
 * A container for the ideal T shape. Provides useful data about this image,
 * such as ability to retrieve an arbitrarily rotated copy or a frame-sized
 * mask.
 * 
 * @author Gediminas Liktaras
 */
public class TShapeTemplate {
	
	/** Path to the image of the ideal T shape. */
	private static final String T_TEMPLATE_PATH = "../robot-VISION/data/tShape.png";
	
	/** An image with the ideal expected T shape on robot plates. */
	private IplImage shapeImage;
	
	/** Coordinates of the shape's center. */
	private Point center = new Point(25, 25);
	/** Coordinates of the shape's center in OpenCV point object. */
	private CvPoint2D32f cvCenter = new CvPoint2D32f(center.x, center.y);
	
	/** Height of the shape. */
	private int height = 51;
	/** Width of the shape. */
	private int width = 51;
	
	
	/**
	 * Create a new ideal T shape instance.
	 */
	public TShapeTemplate() {
		shapeImage = cvLoadImage(T_TEMPLATE_PATH, CV_LOAD_IMAGE_GRAYSCALE);
	}
	
	
	/**
	 * Get a copy of the T's IplImage at the given orientation.
	 * 
	 * @param angle Orientation of the shape in degrees.
	 * @return IplImage of the shape.
	 */
	public IplImage getIplImage(int angle) {
		angle = (angle + 270) % 360;
		
		IplImage rotatedShape = IplImage.createCompatible(shapeImage);
		CvMat rotMatrix = CvMat.create(2, 3);
		
		cv2DRotationMatrix(cvCenter, angle, 1.0, rotMatrix);
		cvWarpAffine(shapeImage, rotatedShape, rotMatrix);
		
		return rotatedShape; 
	}
	
	
	/**
	 * Get the shape's center coordinates.
	 *
	 * @return Shape's center.
	 */
	public Point getCenter() {
		return center;
	}
	
	/**
	 * Get the shape's height.
	 * 
	 * @return Shape's height.
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Get the shape's width.
	 * 
	 * @return Shape's width.
	 */
	public int getWidth() {
		return width;
	}
	
}

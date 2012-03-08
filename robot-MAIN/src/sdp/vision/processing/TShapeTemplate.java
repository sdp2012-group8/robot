package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.awt.Point;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
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
	private static final String T_TEMPLATE_PATH = "data/tShape.png";
	
	/** An image with the ideal expected T shape on robot plates. */
	private IplImage shapeImage;
	
	/** Coordinates of the shape's center. */
	private Point center;
	/** Coordinates of the shape's center in OpenCV point object. */
	private CvPoint2D32f cvCenter;
	
	/** Height of the shape. */
	private int height;
	/** Width of the shape. */
	private int width;
	
	/** Area of the shape. */
	private int area;
	
	
	/**
	 * Create a new ideal T shape instance.
	 */
	public TShapeTemplate() {
		shapeImage = cvLoadImage(T_TEMPLATE_PATH, CV_LOAD_IMAGE_GRAYSCALE);
		
		CvSize shapeSize = shapeImage.cvSize();
		height = shapeSize.height();
		width = shapeSize.width();
		
		center = new Point(height / 2, width / 2);
		cvCenter = new CvPoint2D32f(center.x, center.y);
		
		area = cvCountNonZero(shapeImage);
	}
	
	
	/**
	 * Get a copy of T's IplImage at the given orientation.
	 * 
	 * @param angle Orientation of the shape in degrees.
	 * @return IplImage of the shape.
	 */
	public IplImage getShapeImage(double angle) {
		angle = (angle + 270) % 360;
		
		IplImage rotatedShape = IplImage.createCompatible(shapeImage);
		CvMat rotMatrix = CvMat.create(2, 3);
		
		cv2DRotationMatrix(cvCenter, angle, 1.0, rotMatrix);
		cvWarpAffine(shapeImage, rotatedShape, rotMatrix);
		
		return rotatedShape; 
	}
	
	
	/**
	 * Create a black image of the specified size and place the T at the
	 * specified location and orientation.
	 * 
	 * Use this function to generate frame-sized masks. Also, make sure that
	 * the object is actually on the field (so no out of bounds arguments).
	 * 
	 * @param fieldW Width of the frame.
	 * @param fieldH Height of the frame.
	 * @param x X coordinate of the T's center.
	 * @param y Y coordinate of the T's center.
	 * @param angle T's orientation.
	 * @return Image as described above.
	 */
	public IplImage getFrameImage(int fieldW, int fieldH, int x, int y, double angle) {
		CvRect[] roiRects = ProcUtils.getOverlappingRect(fieldW, fieldH, x, y, width, height);
        CvRect frameRoiRect = roiRects[0];
        CvRect tShapeRoiRect = roiRects[1];

		IplImage frameImage = IplImage.create(fieldW, fieldH, IPL_DEPTH_8U, 1);
		cvSetZero(frameImage);
		cvSetImageROI(frameImage, frameRoiRect);
		
		IplImage tShape = getShapeImage(angle);
		cvSetImageROI(tShape, tShapeRoiRect);
		
		cvCopy(tShape, frameImage);
		
		cvResetImageROI(frameImage);
		
		return frameImage;
	}
	
	
	/**
	 * Get the area of the shape.
	 * 
	 * @return Shape's area.
	 */
	public int getArea() {
		return area;
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

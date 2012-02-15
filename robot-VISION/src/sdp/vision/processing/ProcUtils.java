package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_core.*;

import java.awt.Point;
import java.awt.geom.Point2D;

import sdp.common.Utilities;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;


/**
 * A container for utility image processing functions.
 * 
 * @author Gediminas Liktaras
 */
public class ProcUtils {

	/**
	 * Compute the area of an OpenCV polygon.
	 * 
	 * @param polygon Polygon in question.
	 * @return Area of the polygon.
	 */
	public static double getPolygonArea(CvSeq polygon) {
		double area = 0.0;
		
		for (int i = 0; i < polygon.total(); ++i) {
			int j = (i + 1) % polygon.total();			
			CvPoint pt1 = new CvPoint(cvGetSeqElem(polygon, i));
			CvPoint pt2 = new CvPoint(cvGetSeqElem(polygon, j));			
			
			area = area + pt1.x() * pt2.y() - pt1.y() * pt2.x();	
		}
		
		return Math.abs(area);
	}
	
	
	/**
	 * Convert frame coordinates to normal ones.
	 * 
	 * @param config Processor configuration to use as reference.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param withinROI Whether the given coordinates are offset to the region
	 * 		of interest.
	 * @return Normalised coordinates.
	 */
	public static Point2D.Double frameToNormalCoordinates(ImageProcessorConfig config,
			double x, double y, boolean withinROI) {
		double scaleFactor = (double)(config.getFieldWidth());
		
		if (!withinROI) {
			x += config.getFieldLowX();
			y += config.getFieldLowY();
		}
		return new Point2D.Double(x / scaleFactor, y / scaleFactor);
	}
	
	/**
	 * Convert normal coordinates to frame ones.
	 * 
	 * @param config Processor configuration to use as reference.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param withinROI Whether the given coordinates should be offset to the
	 * 		region of interest.
	 * @return Frame coordinates.
	 */
	public static Point2D.Double normalToFrameCoordinates(ImageProcessorConfig config,
			double x, double y, boolean withinROI) {
		int scaleFactor = config.getFieldWidth();
		
		x *= scaleFactor;
		y *= scaleFactor;
		
		if (!withinROI) {
			x += config.getFieldLowX();
			y += config.getFieldLowY();
		}
		return new Point2D.Double(x, y);
	}
	
	/**
	 * Convert normal coordinates to frame ones (in integers).
	 * 
	 * @param config Processor configuration to use as reference.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param withinROI Whether the given coordinates should be offset to the
	 * 		region of interest.
	 * @return Frame coordinates in integers.
	 */
	public static Point normalToFrameCoordinatesInt(ImageProcessorConfig config,
			double x, double y, boolean withinROI) {
		Point2D.Double frameCoords = normalToFrameCoordinates(config, x, y, withinROI);
		return Utilities.pointFromPoint2D(frameCoords);
	}
}

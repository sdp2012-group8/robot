package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.WorldState;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


/**
 * A container for utility image processing functions.
 * 
 * @author Gediminas Liktaras
 */
public class ProcUtils {
	
	/** Distortion coefficients for the undistortion operation. */
	private static CvMat distortion;
	/** Intristic coefficients for the undistortion operation. */
	private static CvMat intristic;
	
	/** Variable initialisation. */
	static {
		distortion = CvMat.create(1, 5);
		distortion.put(0.0, 0.0, 0.0, 0.0, -1.684608201740239);	// TODO
		
		intristic = CvMat.create(3, 3);
		intristic.put(1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0);
	}
	
	
	/**
	 * Convert a sequence points into an array.
	 * 
	 * @param seq Sequence of points in question.
	 * @return Sequence of points as an array.
	 */
	public static CvPoint[] cvSeqToArray(CvSeq seq) {
		CvPoint points[] = new CvPoint[seq.total()];
		for (int i = 0; i < seq.total(); ++i) {
			points[i] = new CvPoint(cvGetSeqElem(seq, i));
		}
		return points;
	}
	
	
	/**
	 * Find the shape with the largest area in the given array.
	 * 
	 * @param shapes A list of shapes to examine.
	 * @return The polygon with the largest area.
	 */
	public static CvSeq getLargestShape(ArrayList<CvSeq> shapes) {
		if (shapes.size() == 0) {
			return null;
		} else {
			CvSeq largestShape = shapes.get(0);
			double largestArea = ProcUtils.getPolygonArea(largestShape);
			
			for (int i = 1; i < shapes.size(); ++i) {
				double curArea = ProcUtils.getPolygonArea(shapes.get(i));
				if (curArea > largestArea) {
					largestArea = curArea;
					largestShape = shapes.get(i);
				}
			}
			
			return largestShape;
		}
	}
	

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
	 * Get rectangles that overlaps both the field and the given object box.
	 * 
	 * @param fieldW Width of the frame.
	 * @param fieldH Height of the frame.
	 * @param objX X coordinate of the object's center.
	 * @param objY Y coordinate of the object's center.
	 * @param objW Width of the object's bounding box.
	 * @param objH Height of the object's bounding box.
	 * @return Union of the field and the object box. First element of the
	 * 		array is a rectangle in field coordinates, the second one is in
	 * 		the object coordinates.
	 */
	public static CvRect[] getOverlappingRect(int fieldW, int fieldH, int objX, int objY,
			int objW, int objH)
	{
	    int westExtents = Math.min(objX, objW / 2);
	    int northExtents = Math.min(objY, objH / 2);
	    int rectWidth = westExtents + Math.min(fieldW - objX, objW / 2);
	    int rectHeight = northExtents + Math.min(fieldH - objY, objH / 2);
	    
	    CvRect fieldRect = cvRect(objX - westExtents, objY - northExtents, rectWidth, rectHeight);
	    CvRect objRect = cvRect(objW / 2 - westExtents, objH / 2 - northExtents, rectWidth, rectHeight);
	    CvRect rects[] = { fieldRect, objRect };
	    
	    return rects;
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
		double yFactor = (WorldState.PITCH_WIDTH_CM * config.getFieldHeight()) / WorldState.PITCH_HEIGHT_CM;

		if (!withinROI) {
			x += config.getFieldLowX();
			y += config.getFieldLowY();
		}
		
		return new Point2D.Double(x / config.getFieldWidth(), (y / yFactor));
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
		double yFactor = (WorldState.PITCH_WIDTH_CM * config.getFieldHeight()) / WorldState.PITCH_HEIGHT_CM;

		x *= config.getFieldWidth();
		y *= yFactor;
		
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
	
	
	/**
	 * Convert world state with normalised coordinates to a world state with
	 * corresponding frame coordinates.
	 * 
	 * @param config Processor configuration to use as reference.
	 * @param state State to process.
	 * @return World state in frame coordinates.
	 */
	public static WorldState stateToFrameCoordinates(ImageProcessorConfig config,
			WorldState state) {
		Point2D.Double ballPos = ProcUtils.normalToFrameCoordinates(config,
				state.getBallCoords().x, state.getBallCoords().y, false);
		Point2D.Double bluePos = ProcUtils.normalToFrameCoordinates(config,
				state.getBlueRobot().getCoords().x,
				state.getBlueRobot().getCoords().y,	false);
		Point2D.Double yellowPos = ProcUtils.normalToFrameCoordinates(config,
				state.getYellowRobot().getCoords().x,
				state.getYellowRobot().getCoords().y, false);
		
		return new WorldState(ballPos,
				new Robot(bluePos, state.getBlueRobot().getAngle()),
				new Robot(yellowPos, state.getYellowRobot().getAngle()),
				state.getWorldImage());
	}
	
	
	/**
	 * Put undistortion coefficients into the appropriate matrices.
	 * 
	 * @param config Image processor configuration to use.
	 */
	private static synchronized void updateUndistortMatrices(ImageProcessorConfig config) {
		intristic.put(0, 0, config.getUndistort_fx());
		intristic.put(0, 2, config.getUndistort_cx());
		intristic.put(1, 1, config.getUndistort_fy());
		intristic.put(1, 2, config.getUndistort_cy());
		
		distortion.put(0, config.getUndistort_k1());
		distortion.put(1, config.getUndistort_k2());
		distortion.put(2, config.getUndistort_p1());
		distortion.put(3, config.getUndistort_p2());
	}
	
	/**
	 * A function to undistort images.
	 * 
	 * @param config Image processor configuration to use.
	 * @param image Image to undistort.
	 * @return Undistorted image.
	 */
	public static synchronized IplImage undistortImage(ImageProcessorConfig config,
			IplImage image) {
		updateUndistortMatrices(config);

		IplImage newImage = IplImage.createCompatible(image);
		cvUndistort2(image, newImage, intristic, distortion);	
		return newImage;
	}
	
	/**
	 * A function to undistort a point.
	 * 
	 * @param config Image processor configuration to use.
	 * @param point Point to undistort.
	 * @return Undistorted point.
	 */
	public static synchronized Point2D.Double undistortPoint(ImageProcessorConfig config,
			Point2D.Double point) {		
		updateUndistortMatrices(config);

		CvMat inPointMat = CvMat.create(1, 1, CV_32FC2);
		CvMat outPointMat = CvMat.create(1, 1, CV_32FC2);
		
		inPointMat.put(0, 0, 0, point.x);
		inPointMat.put(0, 0, 1, point.y);
			
		cvUndistortPoints(inPointMat, outPointMat, intristic, distortion, null, null);
		
		point.x = outPointMat.get(0, 0, 0);
		point.x = point.x * config.getUndistort_fx() + config.getUndistort_cx();
		
		point.y = outPointMat.get(0, 0, 1);
		point.y = point.y * config.getUndistort_fy() + config.getUndistort_cy();
		
		return point;
	}
	
	/**
	 * A function to undistort a world state.
	 * 
	 * @param config Image processor configuration to use.
	 * @param worldState World state to undistort.
	 * @return Correct world state.
	 */
	public static WorldState undistortWorldState(ImageProcessorConfig config, WorldState worldState) {
		Point2D.Double newBallPos = undistortPoint(config, worldState.getBallCoords());
		Point2D.Double newBluePos = undistortPoint(config, worldState.getBlueRobot().getCoords());
		Point2D.Double newYellowPos = undistortPoint(config, worldState.getYellowRobot().getCoords());
		
		Robot blueRobot = new Robot(newBluePos, worldState.getBlueRobot().getAngle());
		Robot yellowRobot = new Robot(newYellowPos, worldState.getYellowRobot().getAngle());
		
		return new WorldState(newBallPos, blueRobot, yellowRobot, worldState.getWorldImage());
	}
}

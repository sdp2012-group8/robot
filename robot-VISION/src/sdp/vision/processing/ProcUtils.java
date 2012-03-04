package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_core.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.WorldState;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;


/**
 * A container for utility image processing functions.
 * 
 * @author Gediminas Liktaras
 */
public class ProcUtils {
	
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
		double yFactor = Tools.PITCH_HEIGHT_CM / (Tools.PITCH_WIDTH_CM * config.getFieldHeight());

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
		double yFactor = Tools.PITCH_HEIGHT_CM / (Tools.PITCH_WIDTH_CM * config.getFieldHeight());

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
}

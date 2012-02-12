package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.googlecode.javacpp.Loader;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.WorldState;


/**
 * An image processor where I try to explore different alternative ideas. Or
 * just trying to reimplement team 9's system.
 * 
 * @author Gediminas Liktaras
 */
public class MainImageProcessor extends BaseImageProcessor {
	
	/** Polygon approximation error (arg in cvApproxPoly). */
	private static final double POLY_APPROX_ERROR = 0.02;
	
	/** Length of the direction line. */
	private static final int DIR_LINE_LENGTH = 40;
	/** Size of the position marker. */
	private static final int POSITION_MARKER_SIZE = 4;
	
	
	/** OpenCV memory storage. */
	private CvMemStorage storage;

	
	/**
	 * The main constructor.
	 */
	public MainImageProcessor() {
		super();		
		storage = CvMemStorage.create();
	}
	

	/* (non-Javadoc)
	 * @see sdp.vision.processing.ImageProcessor#extractWorldState(java.awt.image.BufferedImage)
	 */
	@Override
	public synchronized WorldState extractWorldState(BufferedImage frame) {
		frame = preprocessFrame(frame);
		
		BufferedImage thresholds[] = thresholdFrame(frame);
		IplImage frame_ipl = IplImage.createFrom(thresholds[0]);
		IplImage ballThreshold = IplImage.createFrom(thresholds[1]);		
		IplImage blueThreshold = IplImage.createFrom(thresholds[2]);
		IplImage yellowThreshold = IplImage.createFrom(thresholds[3]);	
		cvSetImageROI(frame_ipl, getCurrentROI());

		Point2D.Double ballPos = findBall(frame_ipl, ballThreshold);
		Robot blueRobot = findRobot(frame_ipl, blueThreshold,
				config.getBlueSizeMinValue(), config.getBlueSizeMaxValue());
		Robot yellowRobot = findRobot(frame_ipl, yellowThreshold,
				config.getYellowSizeMinValue(), config.getYellowSizeMaxValue());
		BufferedImage worldImage = finaliseWorldImage(frame_ipl, ballPos, blueRobot, yellowRobot);
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}
	
	
	/**
	 * Preprocess the frame for world state extraction.
	 * 
	 * @param frame Frame to preprocess.
	 */
	private BufferedImage preprocessFrame(BufferedImage frame) {
		IplImage frame_ipl = IplImage.createFrom(frame);		
		cvSetImageROI(frame_ipl, getCurrentROI());		
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		return frame_ipl.getBufferedImage();
	}
	
	
	/**
	 * Threshold the image for the different features.
	 * 
	 * The thresholded components are returned as an array, which contains
	 * the original frame, ball, blue T and yellow T components, in that 
	 * order.
	 * 
	 * The reason I have used BufferedImage here instead of opencv's IplImage
	 * is because opencv's thresholding functions are somewhat limited. I do 
	 * know of a way to threshold by both RGB and HSV values, for instance.
	 * One cannot use arbitrary conditions either.
	 * 
	 * @param frame Frame to threshold.
	 * @return Thresholded components.
	 */
	private BufferedImage[] thresholdFrame(BufferedImage frame) {		
		BufferedImage ball = new BufferedImage(config.getFieldWidth(),
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage blue = new BufferedImage(config.getFieldWidth(),
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage yellow = new BufferedImage(config.getFieldWidth(),
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		for (int x = 0; x < config.getFieldWidth(); ++x) {
			for (int y = 0; y < config.getFieldHeight(); ++y) {
				int ox = x + config.getFieldLowX();
				int oy = y + config.getFieldLowY();
				
				// Extract current pixel values.
				Color px = new Color(frame.getRGB(ox, oy));				
				int r = px.getRed();
				int g = px.getGreen();
				int b = px.getBlue();
				
				float hsv[] = Color.RGBtoHSB(r, g, b, null);
				int h = (int) (hsv[0] * 360);
				int s = (int) (hsv[1] * 100);
				int v = (int) (hsv[2] * 100);
				
				// Whether to hide current pixel.
				if (!config.isShowWorld()) {
					frame.setRGB(ox, oy, Color.black.getRGB());
				}
				
				// Ball thresholding.
				if (Utilities.valueWithinBounds(h, config.getBallHueMinValue(), 
								config.getBallHueMaxValue())
						&& Utilities.valueWithinBounds(s, config.getBallSatMinValue(),
								config.getBallSatMaxValue())
						&& Utilities.valueWithinBounds(v, config.getBallValMinValue(),
								config.getBallValMaxValue())) {	
					
					ball.setRGB(x, y, Color.white.getRGB());
					if (config.isShowThresholds()) {
						frame.setRGB(ox, oy, Color.red.getRGB());
					}
				}
				
				// Blue T thresholding.
				if (Utilities.valueWithinBounds(h, config.getBlueHueMinValue(), 
								config.getBlueHueMaxValue())
						&& Utilities.valueWithinBounds(s, config.getBlueSatMinValue(),
								config.getBlueSatMaxValue())
						&& Utilities.valueWithinBounds(v, config.getBlueValMinValue(),
								config.getBlueValMaxValue())
						&& (g < (int)(b * 1.5))) {
					
					blue.setRGB(x, y, Color.white.getRGB());
					if (config.isShowThresholds()) {
						frame.setRGB(ox, oy, Color.blue.getRGB());
					}
			    }
				
				// Yellow T thresholding.
				if (Utilities.valueWithinBounds(h, config.getYellowHueMinValue(), 
								config.getYellowHueMaxValue())
						&& Utilities.valueWithinBounds(s, config.getYellowSatMinValue(),
								config.getYellowSatMaxValue())
						&& Utilities.valueWithinBounds(v, config.getYellowValMinValue(),
								config.getYellowValMaxValue())) {
					
			    	yellow.setRGB(x, y, Color.white.getRGB());
			    	if (config.isShowThresholds()) {
			    		frame.setRGB(ox, oy, Color.orange.getRGB());
			    	}
			    }
			}
		}
		
		BufferedImage retValue[] = { frame, ball, blue, yellow };
		return retValue;
	}
	
	
	/**
	 * Locate the ball in the world.
	 * 
	 * @param frame_ipl The original frame.
	 * @param ballThresh Thresholded image to search in.
	 * @return The position of the ball.
	 */
	private Point2D.Double findBall(IplImage frame_ipl, IplImage ballThresh) {
		CvSeq fullBallContour = findAllContours(ballThresh);		
		ArrayList<CvSeq> ballShapes = sizeFilterContours(fullBallContour, config.getBallSizeMinValue(), config.getBallSizeMaxValue());
		
		if (ballShapes.size() == 0) {
			return new Point2D.Double(-1.0, -1.0);
		} else {
			CvSeq curBallShape = ballShapes.get(0);
			
            CvRect ballBoundingRect = cvBoundingRect(curBallShape, 0);
            int bX = ballBoundingRect.x();
            int bY = ballBoundingRect.y();
            int bW = ballBoundingRect.width();
            int bH = ballBoundingRect.height();
            
            if (config.isShowContours()) {
            	cvDrawContours(frame_ipl, curBallShape, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
            }            
            if (config.isShowBoundingBoxes()) {
	            CvPoint pt1 = new CvPoint(bX, bY);
	            CvPoint pt2 = new CvPoint(bX + bW, bY + bH);
	            cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            }
            
            return ProcUtils.frameToNormalCoordinates(config, bX + bW / 2, bY + bH / 2, true);
		}
	}
	
	/**
	 * Locate a robot in the world.
	 * 
	 * @param frame_ipl The original frame.
	 * @param robotThresh Thresholded image to search for the robot's T.
	 * @param markerThresh Thresholded image to search for direction marker.
	 * @return The position of the ball.
	 */
	private Robot findRobot(IplImage frame_ipl, IplImage robotThresh,
			int minSize, int maxSize) {
		CvSeq fullRobotContour = findAllContours(robotThresh);
		ArrayList<CvSeq> robotShapes = sizeFilterContours(fullRobotContour, minSize, maxSize);
		
		if (robotShapes.size() == 0) {
			return new Robot(new Point2D.Double(-1.0, -1.0), 0.0);
		} else {
			// Find the best marching T shape.
			CvSeq bestRobotShape = getLargestShape(robotShapes);
			
			// Find shape's bounding box dimensions.
            CvRect robotBoundingRect = cvBoundingRect(bestRobotShape, 0);
            int rX = robotBoundingRect.x();
            int rY = robotBoundingRect.y();
            int rW = robotBoundingRect.width();
            int rH = robotBoundingRect.height();
            
            // Debug output.
            if (config.isShowContours()) {
            	cvDrawContours(frame_ipl, bestRobotShape, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
            }
            if (config.isShowBoundingBoxes()) {
	            CvPoint pt1 = new CvPoint(rX, rY);
	            CvPoint pt2 = new CvPoint(rX + rW, rY + rH);
	            cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            }
            
            // Find the shape's (and robot's) mass center.
            CvMoments moments = new CvMoments();
            cvMoments(bestRobotShape, moments, 1);
            
            double massCenterX = moments.m10() / moments.m00();
            double massCenterY = moments.m01() / moments.m00();  
            double angle = 0.0;
            
            // Find the contour's farthest point from the mass center. 
            double maxShapeDist = Double.MIN_VALUE;
            int farthestPoint = -1;
            
            for (int i = 0; i < bestRobotShape.total(); ++i) {
            	CvPoint pt = new CvPoint(cvGetSeqElem(bestRobotShape, i));
            	double curDist = Point2D.distance(massCenterX, massCenterY, pt.x(), pt.y());
            	
            	if (curDist > maxShapeDist) {
            		maxShapeDist = curDist;
            		farthestPoint = i;            		
            	}
            }
            
            // Smooth angle using adjacent equally distance points.
            double xSum = 0.0;
            double ySum = 0.0;
            
            for (int i = -1; i <= 1; ++i) {
            	int j = (farthestPoint + i + bestRobotShape.total()) % bestRobotShape.total();
            	CvPoint pt = new CvPoint(cvGetSeqElem(bestRobotShape, j));
            	
            	double curDist = Point2D.distance(massCenterX, massCenterY, pt.x(), pt.y());            	
            	if (curDist >= (maxShapeDist - 2.0)) {
            		xSum = xSum + pt.x() - massCenterX;
            		ySum = ySum + pt.y() - massCenterY;
            	}
            }

            // Compute final robot position and direction.
            Point2D.Double robotPos = ProcUtils.frameToNormalCoordinates(config, massCenterX, massCenterY, true);
            angle = Math.toDegrees( Math.atan2(ySum, -xSum) ) + 180.0;
            
            return new Robot(robotPos, angle);
		}
	}
	
	
	/**
	 * Add finishing details to frame.
	 * 
	 * @param frame_ipl Frame to process.
	 * @return Final world image.
	 */
	private BufferedImage finaliseWorldImage(IplImage frame_ipl,
			Point2D.Double ball, Robot blueRobot, Robot yellowRobot) {
		BufferedImage finalFrame = frame_ipl.getBufferedImage();		
		Graphics2D graphics = finalFrame.createGraphics();
		graphics.setColor(Color.white);
		
		graphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		if (config.isShowStateData()) {
			Point pt1, pt2;
			
			// Draw ball position.
			pt1 = ProcUtils.normalToFrameCoordinates(config, ball.x, ball.y, false);
			drawPositionMarker(graphics, Color.red, pt1, null);
			
			// Draw blue robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinates(config, blueRobot.getCoords().x,
					blueRobot.getCoords().y, false);
			pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
					blueRobot.getAngle());
			drawPositionMarker(graphics, Color.blue, pt1, pt2);
			
			// Draw yellow robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinates(config, yellowRobot.getCoords().x,
					yellowRobot.getCoords().y, false);
			pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
					yellowRobot.getAngle());
			drawPositionMarker(graphics, Color.orange, pt1, pt2);
		}

		return finalFrame;
	}
	
	
	/**
	 * Find the all contours in the specified image.
	 * 
	 * @param image Image to search.
	 * @return Image shapes' contour.
	 */
	private CvSeq findAllContours(IplImage image) {
		CvSeq contour = new CvSeq();
		cvFindContours(image, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_NONE);        
		return contour;
	}
	
	/**
	 * Take a contour and return all shapes in it that fit within the
	 * specified dimensions.
	 * 
	 * @param contour Contour to bisect.
	 * @param minWidth Minimum required shape width.
	 * @param maxWidth Maximum required shape width.
	 * @param minHeight Minimum required shape height.
	 * @param maxHeight Maximum required shape height.
	 * @return A list of fitting shapes in the contour.
	 */
	private ArrayList<CvSeq> sizeFilterContours(CvSeq contour, int minSize, int maxSize) {
		ArrayList<CvSeq> shapes = new ArrayList<CvSeq>();
		CvSeq contourSeq = contour;
		
		while (contourSeq != null && !contourSeq.isNull()) {
            if (contourSeq.elem_size() > 0) {
            	double epsilon = cvContourPerimeter(contourSeq) * POLY_APPROX_ERROR;
                CvSeq points = cvApproxPoly(contourSeq, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, epsilon, 0);
                
                CvRect boundingRect = cvBoundingRect(points, 0);
                int w = boundingRect.width();
                int h = boundingRect.height();
                
                if ((w >= minSize) && (w <= maxSize) && (h >= minSize) && (h <= maxSize)) {
                	shapes.add(points);
                }
            }
            
            contourSeq = contourSeq.h_next();
        }
		
		return shapes;
	}
	
	
	/**
	 * Find the shape with the largest area in the given array.
	 * 
	 * @param shapes A list of shapes to examine.
	 * @return The polygon with the largest area.
	 */
	private CvSeq getLargestShape(ArrayList<CvSeq> shapes) {
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
	 * Get the current frame's region of interest.
	 * 
	 * @return CvRect with the current ROI.
	 */
	private CvRect getCurrentROI() {
		return cvRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
	}
	
	
	/**
	 * Draw a position marker.
	 * 
	 * @param g Graphics that will do the drawing.
	 * @param col Color of the marker
	 * @param posPt Point that corresponds to the position of the marker.
	 * @param dirPt Point that corresponds to the direction of the marker. If
	 * 		null, direction line will not be drawn.
	 */
	private void drawPositionMarker(Graphics2D g, Color col, Point posPt, Point dirPt) {
		g.setColor(col);
		g.fillArc(posPt.x - POSITION_MARKER_SIZE, posPt.y - POSITION_MARKER_SIZE, 
				2 * POSITION_MARKER_SIZE, 2 * POSITION_MARKER_SIZE, 0, 360);
		
		g.setColor(Color.white);
		g.drawArc(posPt.x - POSITION_MARKER_SIZE, posPt.y - POSITION_MARKER_SIZE, 
				2 * POSITION_MARKER_SIZE, 2 * POSITION_MARKER_SIZE, 0, 360);
		
		if (dirPt != null) {
			g.drawLine(posPt.x, posPt.y, dirPt.x, dirPt.y);
		}
	}
	
}

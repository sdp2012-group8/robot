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
	
	/** The size of the direction cone angle. */
	private static final int DIRECTION_CONE_ANGLE = 30;
	/** Robot height normalisation constant. */
	private static final double HEIGHT_NORM_VALUE = 40.0;
	/** How many directions the outline distance calculations will use. */
	private static final int OUTLINE_ANGLE_COUNT = 360;
	
	/** Polygon approximation error (arg in cvApproxPoly). */
	private static final double POLY_APPROX_ERROR = 0.0001;
	
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
	

	/**
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
				config.getBlueSizeMin(), config.getBlueSizeMax());
		Robot yellowRobot = findRobot(frame_ipl, yellowThreshold,
				config.getYellowSizeMin(), config.getYellowSizeMax());
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
		frame_ipl = ProcUtils.undistortImage(config, frame_ipl);
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
				if (Utilities.valueWithinBounds(h, config.getBallThreshs().getHueMin(), 
								config.getBallThreshs().getHueMax())
						&& Utilities.valueWithinBounds(s, config.getBallThreshs().getSatMin(),
								config.getBallThreshs().getSatMax())
						&& Utilities.valueWithinBounds(v, config.getBallThreshs().getValMin(),
								config.getBallThreshs().getValMax())) {	
					
					ball.setRGB(x, y, Color.white.getRGB());
					if (config.isShowThresholds()) {
						frame.setRGB(ox, oy, Color.red.getRGB());
					}
				}
				
				// Blue T thresholding.
				if (Utilities.valueWithinBounds(h, config.getBlueThreshs().getHueMin(), 
								config.getBlueThreshs().getHueMax())
						&& Utilities.valueWithinBounds(s, config.getBlueThreshs().getSatMin(),
								config.getBlueThreshs().getSatMax())
						&& Utilities.valueWithinBounds(v, config.getBlueThreshs().getValMin(),
								config.getBlueThreshs().getValMax())
						&& (g < (int)(b * 1.5))) {
					
					blue.setRGB(x, y, Color.white.getRGB());
					if (config.isShowThresholds()) {
						frame.setRGB(ox, oy, Color.blue.getRGB());
					}
			    }
				
				// Yellow T thresholding.
				if (Utilities.valueWithinBounds(h, config.getYellowThreshs().getHueMin(), 
								config.getYellowThreshs().getHueMax())
						&& Utilities.valueWithinBounds(s, config.getYellowThreshs().getSatMin(),
								config.getYellowThreshs().getSatMax())
						&& Utilities.valueWithinBounds(v, config.getYellowThreshs().getValMin(),
								config.getYellowThreshs().getValMax())) {
					
			    	yellow.setRGB(x, y, Color.white.getRGB());
			    	if (config.isShowThresholds()) {
			    		frame.setRGB(ox, oy, Color.orange.getRGB());
			    	}
			    }
			}
		}
		
		BufferedImage ret[] = { frame, ball, blue, yellow };
		return ret;
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
		ArrayList<CvSeq> ballShapes = sizeFilterContours(fullBallContour, config.getBallSizeMin(), config.getBallSizeMax());
		
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
			return new Robot(new Point2D.Double(-1.0, -1.0), -1.0);
		} else {
			// Find the best marching T shape.
			CvSeq bestRobotShape = ProcUtils.getLargestShape(robotShapes);
			
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
            
            // Find the contour's outline distances.
            double[] dists = getContourOutlineDistances(bestRobotShape, massCenterX, massCenterY);
            
            // Find the robot's direction.
            int angle = 0;
            double bestArea = 0.0;
            
            for (int i = 0; i < OUTLINE_ANGLE_COUNT; ++i) {
            	double curArea = 0.0;
            	
	            for (int j = -(DIRECTION_CONE_ANGLE / 2); j <= (DIRECTION_CONE_ANGLE / 2); ++j) {
	            	int k = (i + j + OUTLINE_ANGLE_COUNT) % OUTLINE_ANGLE_COUNT;
	            	int w = DIRECTION_CONE_ANGLE / 2 - Math.abs(j);
	            	curArea += dists[k] * w;
	            }
	            
	            if (curArea > bestArea) {
	            	bestArea = curArea;
	            	angle = i;
	            }
            }
            
            // Adjust mass center to account for robot's height.
            double f = config.getFrameWidth() / HEIGHT_NORM_VALUE;
            massCenterX -= (massCenterX - config.getFieldWidth() / 2) / f;
            massCenterY -= (massCenterY - config.getFieldHeight() / 2) / f;
            
            Point2D.Double robotPos = ProcUtils.frameToNormalCoordinates(config, massCenterX, massCenterY, true);
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
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, ball.x, ball.y, false);
			drawPositionMarker(graphics, Color.red, pt1, null);
			
			// Draw blue robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, blueRobot.getCoords().x,
					blueRobot.getCoords().y, false);
			pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
					blueRobot.getAngle());
			drawPositionMarker(graphics, Color.blue, pt1, pt2);
			
			// Draw yellow robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, yellowRobot.getCoords().x,
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
	 * Get contour outline distances.
	 * 
	 * These are distances from the given center of the contour to its border
	 * at each direction. The definition of "center" is up to the caller of
	 * the function.
	 * 
	 * @param contour Contour of interest.
	 * @param centerX X coordinate of the contour's center.
	 * @param centerY Y coordinate of the contour's center.
	 * @return Contour's outline distances.
	 */
	private double[] getContourOutlineDistances(CvSeq contour, double centerX, double centerY) {
		double countFactor = OUTLINE_ANGLE_COUNT / 360.0;
		
		double[] distances = new double[OUTLINE_ANGLE_COUNT];
		for (int i = 0; i < OUTLINE_ANGLE_COUNT; ++i) {
			distances[i] = Double.MAX_VALUE;
		}
		
		for (int i = 0; i < contour.total(); ++i) {
        	CvPoint point1 = new CvPoint(cvGetSeqElem(contour, i));
        	CvPoint point2 = new CvPoint(cvGetSeqElem(contour, (i + 1) % contour.total()));
        	
        	double angle1 = Math.atan2(point1.y() - centerY, centerX - point1.x());
        	int dirIdx1 = (int) ((Math.toDegrees(angle1) + 179) * countFactor);
        	
        	double angle2 = Math.atan2(point2.y() - centerY, centerX - point2.x());
        	int dirIdx2 = (int) ((Math.toDegrees(angle2) + 179) * countFactor);
        	
        	if (dirIdx1 > dirIdx2) {
        		int x = dirIdx1;
        		dirIdx1 = dirIdx2;
        		dirIdx2 = x;
        	}
        	
        	double dist1 = Point.distance(centerX, centerY, point1.x(), point1.y());
        	double dist2 = Point.distance(centerX, centerY, point2.x(), point2.y());
        	
        	int dirSize = dirIdx2 - dirIdx1;
        	int revDirSize = OUTLINE_ANGLE_COUNT - dirSize;
        	
        	if (revDirSize < dirSize) {
        		for (int j = dirIdx2; j < OUTLINE_ANGLE_COUNT; ++j) {
        			double d = dist2 + ((dist1 - dist2) * (j - dirIdx2)) / revDirSize;
        			distances[j] = Math.min(distances[j], d);
        		}
        		for (int j = 0; j < dirIdx1; ++j) {
        			double d = dist2 + ((dist1 - dist2) * (j - dirIdx2 + OUTLINE_ANGLE_COUNT)) / revDirSize;
        			distances[j] = Math.min(distances[j], d);
        		}
        	} else {
        		for (int j = dirIdx1; j < dirIdx2; ++j) {
        			double d = dist1 + ((dist2 - dist1) * (j - dirIdx1)) / dirSize;
        			distances[j] = Math.min(distances[j], d);
        		}
        	}
        }
		
		return distances;
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

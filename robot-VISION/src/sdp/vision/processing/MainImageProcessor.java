package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
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
	
	/** Path to the image of the ideal T shape. */
	private static final String IDEAL_T_PATH = "../robot-VISION/data/tShape.png";
	
	/** Gaussian smoothing mask size. */
	private static final int GAUSSIAN_SMOOTH_MASK_SIZE = 5;
	/** Preferred line type for OpenCV drawing functions. */
	private static final int LINE_TYPE = 8;
	/** Polygon approximation error (arg in cvApproxPoly). */
	private static final double POLY_APPROX_ERROR = 0.0001;
	
	/** Length of the direction line. */
	private static final int DIR_LINE_LENGTH = 40;
	/** Size of the position marker. */
	private static final int POSITION_MARKER_SIZE = 4;
	
	
	/** An image with the ideal expected T shape on robot plates. */
	private IplImage idealTShape;
	/** Size of the ideal T shape. */
	private CvSize idealTSize;
	
	/** OpenCV memory storage. */
	private CvMemStorage storage;
	
	/** World image of the current state. */
	private BufferedImage worldImage;

	
	/**
	 * The main constructor.
	 */
	public MainImageProcessor() {
		super();
		
		idealTShape = cvLoadImage(IDEAL_T_PATH, CV_LOAD_IMAGE_GRAYSCALE);
		idealTSize = idealTShape.cvSize();
		
		storage = CvMemStorage.create();
	}
	

	/**
	 * @see sdp.vision.processing.ImageProcessor#extractWorldState(java.awt.image.BufferedImage)
	 */
	@Override
	public synchronized WorldState extractWorldState(BufferedImage frame) {
		frame = preprocessFrame(frame);		
		worldImage = Utilities.deepBufferedImageCopy(frame);
		
		BufferedImage thresholds[] = thresholdFrame(frame);
		IplImage frame_ipl = IplImage.createFrom(thresholds[0]);
		IplImage ballThreshold = IplImage.createFrom(thresholds[1]);		
		IplImage blueThreshold = IplImage.createFrom(thresholds[2]);
		IplImage yellowThreshold = IplImage.createFrom(thresholds[3]);	
		cvSetImageROI(frame_ipl, getCurrentROI());

		Point2D.Double ballPos = findBall(frame_ipl, ballThreshold);
		Robot blueRobot = findRobot(frame_ipl, blueThreshold, config.getBlueThreshs());
		Robot yellowRobot = findRobot(frame_ipl, yellowThreshold, config.getYellowThreshs());
		finaliseWorldImage(frame_ipl, ballPos, blueRobot, yellowRobot);
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}
	
	
	/**
	 * Preprocess the frame for world state extraction.
	 * 
	 * @param frame Frame to preprocess.
	 */
	private BufferedImage preprocessFrame(BufferedImage frame) {
		IplImage frame_ipl = IplImage.createFrom(frame);
		
		frame_ipl = undistortImage(frame_ipl);
		cvSetImageROI(frame_ipl, getCurrentROI());		
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, GAUSSIAN_SMOOTH_MASK_SIZE);
		
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
					worldImage.setRGB(ox, oy, Color.black.getRGB());
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
						worldImage.setRGB(ox, oy, Color.red.getRGB());
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
						worldImage.setRGB(ox, oy, Color.blue.getRGB());
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
			    		worldImage.setRGB(ox, oy, Color.orange.getRGB());
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
		ArrayList<CvSeq> ballShapes = sizeFilterContours(fullBallContour,
				config.getBallThreshs().getSizeMin(), config.getBallThreshs().getSizeMax());
		
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
            	IplImage worldImage_ipl = IplImage.createFrom(worldImage);
            	cvSetImageROI(worldImage_ipl, getCurrentROI());
            	cvDrawContours(worldImage_ipl, curBallShape, CvScalar.WHITE, CvScalar.WHITE, -1, 1, LINE_TYPE);
            	worldImage = worldImage_ipl.getBufferedImage();
            }            
            if (config.isShowBoundingBoxes()) {
            	Graphics2D g = worldImage.createGraphics();
            	g.setColor(Color.white);
            	g.drawRect(bX + config.getFieldLowX(), bY + config.getFieldLowY(), bW, bH);
            }
            
            return ProcUtils.frameToNormalCoordinates(config, bX + bW / 2, bY + bH / 2, true);
		}
	}
	
	/**
	 * Locate a robot in the world.
	 * 
	 * @param frame_ipl The original frame.
	 * @param robotThresh Thresholded image to search for the robot's T.
	 * @param bounds Thresholding bounds.
	 * @return The position of the ball.
	 */
	private Robot findRobot(IplImage frame_ipl, IplImage robotThresh, ThresholdBounds bounds) {
		CvSeq fullRobotContour = findAllContours(robotThresh);
		ArrayList<CvSeq> robotShapes = sizeFilterContours(fullRobotContour,
				bounds.getSizeMin(), bounds.getSizeMax());
		
		if (robotShapes.size() == 0) {
			return new Robot(new Point2D.Double(-1.0, -1.0), -1.0);
		} else {
			// Find the best matching T shape.
			CvSeq bestRobotShape = ProcUtils.getLargestShape(robotShapes);
			
            // Extra output.
            if (config.isShowContours()) {
            	IplImage worldImage_ipl = IplImage.createFrom(worldImage);
            	cvSetImageROI(worldImage_ipl, getCurrentROI());
            	cvDrawContours(worldImage_ipl, bestRobotShape, CvScalar.WHITE, CvScalar.WHITE, -1, 1, LINE_TYPE);
            	worldImage = worldImage_ipl.getBufferedImage();
            }
            if (config.isShowBoundingBoxes()) {
            	CvRect robotBoundingRect = cvBoundingRect(bestRobotShape, 0);
            	
                int rX = robotBoundingRect.x();
                int rY = robotBoundingRect.y();
                int rW = robotBoundingRect.width();
                int rH = robotBoundingRect.height();
            		            
            	Graphics2D g = worldImage.createGraphics();
            	g.setColor(Color.white);
            	g.drawRect(rX + config.getFieldLowX(), rY + config.getFieldLowY(), rW, rH);
            }
            
            // Find robot position.
            CvMoments moments = new CvMoments();
            cvMoments(bestRobotShape, moments, 1);
            
            int robotX = (int)(moments.m10() / moments.m00());
            int robotY = (int)(moments.m01() / moments.m00());
            
            Point2D.Double normRobotPos = ProcUtils.frameToNormalCoordinates(config, robotX, robotY, true);
            
            // Find surrounding rectangle and isolate robot shape properly.
            int roiXOff = Math.min(robotX, idealTSize.width() / 2);
            int roiYOff = Math.min(robotY, idealTSize.height() / 2);
            int roiWidth = roiXOff + Math.min(config.getFieldWidth() - robotX, idealTSize.width() / 2);
            int roiHeight = roiYOff + Math.min(config.getFieldHeight() - robotY, idealTSize.height() / 2);
            
            IplImage shapeImage = IplImage.create(roiWidth, roiHeight, IPL_DEPTH_8U, 1);
            cvFillPoly(shapeImage, ProcUtils.cvSeqToArray(bestRobotShape),
            		new int[] { bestRobotShape.total() }, 1, CvScalar.WHITE, LINE_TYPE, 0);
      
            // Find the robot direction.
            CvRect threshROI = cvRect(robotX - roiXOff, robotY - roiYOff, roiWidth, roiHeight);
            cvSetImageROI(robotThresh, threshROI);
            
            int angle = 0;
            int bestArea = -1;
            
            for (int i = 0; i < 360; ++i) {
            	IplImage rotShape = getRotatedIdealT(i);
            	
            	CvRect rotShapeROI = cvRect(idealTSize.width() / 2 - roiXOff,
            			idealTSize.height() / 2 - roiYOff, roiWidth, roiHeight);
            	cvSetImageROI(rotShape, rotShapeROI);
          
            	cvAnd(rotShape, robotThresh, rotShape, null);
            	int curArea = cvCountNonZero(rotShape);
            	
            	if (curArea > bestArea) {
            		angle = i;
            		bestArea = curArea;
            	}
            }
            
            // Return the robot data.
            return new Robot(normRobotPos, angle);
		}
	}
	
	
	/**
	 * Add final details to the world image.
	 * 
	 * @param frame_ipl Frame to process.
	 */
	private void finaliseWorldImage(IplImage frame_ipl, Point2D.Double ball, Robot blueRobot, Robot yellowRobot) {		
		Graphics2D g = worldImage.createGraphics();
		g.setColor(Color.white);
		
		g.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		if (config.isShowStateData()) {
			Point pt1, pt2;
			
			// Draw ball position.
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, ball.x, ball.y, false);
			drawPositionMarker(g, Color.red, pt1, null);
			
			// Draw blue robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, blueRobot.getCoords().x,
					blueRobot.getCoords().y, false);
			pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
					blueRobot.getAngle());
			drawPositionMarker(g, Color.blue, pt1, pt2);
			
			// Draw yellow robot position and direction.
			pt1 = ProcUtils.normalToFrameCoordinatesInt(config, yellowRobot.getCoords().x,
					yellowRobot.getCoords().y, false);
			pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
					yellowRobot.getAngle());
			drawPositionMarker(g, Color.orange, pt1, pt2);
		}
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
	 * Get the ideal T shape, rotated to the given angle.
	 * 
	 * @param angle Target angle in degrees.
	 * @return Appropriately rotated ideal T.
	 */
	private IplImage getRotatedIdealT(int angle) {
		angle = (angle + 270) % 360;
		
		IplImage rotatedShape = IplImage.createCompatible(idealTShape);
		CvMat rotMatrix = CvMat.create(2, 3);
		
		cv2DRotationMatrix(new CvPoint2D32f(idealTSize.width() / 2, idealTSize.height() / 2), angle, 1.0, rotMatrix);
		cvWarpAffine(idealTShape, rotatedShape, rotMatrix);
		
		return rotatedShape;
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

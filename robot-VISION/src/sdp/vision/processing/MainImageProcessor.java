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
public class MainImageProcessor extends ImageProcessor {
	
	/** Length of the direction line. */
	private static final int DIR_LINE_LENGTH = 40;
	/** Polygon approximation error (arg in cvApproxPoly). */
	private static final double POLY_APPROX_ERROR = 0.02;
	
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
		frame = thresholds[0];
		BufferedImage ballThreshold = thresholds[1];		
		BufferedImage blueThreshold = thresholds[2];
		BufferedImage yellowThreshold = thresholds[3];

		IplImage frame_ipl = IplImage.createFrom(frame);
		cvSetImageROI(frame_ipl, getCurrentROI());
						
		Point2D.Double ballPos = findBall(frame_ipl, ballThreshold);
		Robot blueRobot = findRobot(frame_ipl, blueThreshold);
		Robot yellowRobot = findRobot(frame_ipl, yellowThreshold);
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
				
				Color px = new Color(frame.getRGB(ox, oy));				
				int r = px.getRed();
				int g = px.getGreen();
				int b = px.getBlue();
				
				float hsv[] = Color.RGBtoHSB(r, g, b, null);
				int h = (int) (hsv[0] * 360);
				int s = (int) (hsv[1] * 100);
				int v = (int) (hsv[2] * 100);
				
				if ((h >= 350 || h <= 20) && (s >= 60) && (s >= 60)) {
					ball.setRGB(x, y, Color.white.getRGB());
					frame.setRGB(ox, oy, Color.red.getRGB());
				}
				if ((h >= 70) && (h <= 210) && (s >= 0) && (v >= 30) 
						&& (g < (int)(b * 1.5))) {
					blue.setRGB(x, y, Color.white.getRGB());
					frame.setRGB(ox, oy, Color.blue.getRGB());
			    }
			    if ((h >= 25) && (h <= 75) && (s >= 60) && (v >= 60)) {
			    	yellow.setRGB(x, y, Color.white.getRGB());
			    	frame.setRGB(ox, oy, Color.orange.getRGB());
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
	 * @param threshImage Thresholded image to search in.
	 * @return The position of the ball.
	 */
	private Point2D.Double findBall(IplImage frame_ipl, BufferedImage threshImage) {
		IplImage ballThresh = IplImage.createFrom(threshImage);
		CvSeq contour = findAllContours(ballThresh);		
		ArrayList<CvSeq> ballShapes = sizeFilterContours(contour, 5, 25, 5, 25);
		
		if (ballShapes.size() == 0) {
			return new Point2D.Double(-1.0, -1.0);
		} else {
			CvSeq ballContour = ballShapes.get(0);
			
            CvRect boundingRect = cvBoundingRect(ballContour, 0);
            int x = boundingRect.x();
            int y = boundingRect.y();
            int w = boundingRect.width();
            int h = boundingRect.height();
            
            cvDrawContours(frame_ipl, ballContour, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
            
            CvPoint pt1 = new CvPoint(x, y);
            CvPoint pt2 = new CvPoint(x + w, y + h);
            cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            
            return frameToNormalCoordinates(x + w/2, y + h/2, true);
		}
	}
	
	/**
	 * Locate a robot in the world.
	 * 
	 * @param frame_ipl The original frame.
	 * @param threshImage Thresholded image to search in.
	 * @return The position of the ball.
	 */
	private Robot findRobot(IplImage frame_ipl, BufferedImage threshImage) {        
        IplImage robotThresh = IplImage.createFrom(threshImage);
		CvSeq contour = findAllContours(robotThresh);
		ArrayList<CvSeq> tShapes = sizeFilterContours(contour, 10, 55, 10, 55);
		
		if (tShapes.size() == 0) {
			return new Robot(new Point2D.Double(-1.0, -1.0), 0.0);
		} else {
			CvSeq tContour = tShapes.get(0);
			
            CvRect boundingRect = cvBoundingRect(tContour, 0);
            int x = boundingRect.x();
            int y = boundingRect.y();
            int w = boundingRect.width();
            int h = boundingRect.height();
            
            cvDrawContours(frame_ipl, tContour, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
            
            CvPoint pt1 = new CvPoint(x, y);
            CvPoint pt2 = new CvPoint(x + w, y + h);
            cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            
            cvSetImageROI(robotThresh, boundingRect);
            CvMoments moments = new CvMoments();
            cvMoments(robotThresh, moments, 1);
            
            double rx = x + w / 2;
            double ry = y + h / 2;
            
            double mx = moments.m10() / moments.m00() + x;
            double my = moments.m01() / moments.m00() + y;
            
            double angle = Math.atan2(ry - my, rx - mx);
            
//            return new Robot(frameToNormalCoordinates(rx, ry, true), 0.0);
            return new Robot(frameToNormalCoordinates(mx, my, true), angle);
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
		Point pt1, pt2;
		
		graphics.setColor(Color.white);
		
		graphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		// Draw ball position.
		pt1 = normalToFrameCoordinates(ball.x, ball.y, false);
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		
		// Draw blue robot position and direction.
		pt1 = normalToFrameCoordinates(blueRobot.getCoords().x,
				blueRobot.getCoords().y, false);
		pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
				blueRobot.getAngle());
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		graphics.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);
		
		// Draw yellow robot position and direction.
		pt1 = normalToFrameCoordinates(yellowRobot.getCoords().x,
				yellowRobot.getCoords().y, false);
		pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
				yellowRobot.getAngle());
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		graphics.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);

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
                CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);        
		return contour;
	}
	
	/**
	 * Take a contour and return all shapes in it that fit within the
	 * specified dimensions.
	 * 
	 * @param contour Contour to disect.
	 * @param minWidth Minimum required shape width.
	 * @param maxWidth Maximum required shape width.
	 * @param minHeight Minimum required shape height.
	 * @param maxHeight Maximum required shape height.
	 * @return A list of fitting shapes in the contour.
	 */
	private ArrayList<CvSeq> sizeFilterContours(CvSeq contour, int minWidth,
			int maxWidth, int minHeight, int maxHeight) {
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
                
                if ((w >= minWidth) && (w <= maxWidth)
                		&& (h >= minHeight) && (h <= maxHeight)) {
                	shapes.add(points);
                }
            }
            
            contourSeq = contourSeq.h_next();
        }
		
		return shapes;
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
	 * Convert frame coordinates to normal ones.
	 * 
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param withinROI Whether the given coordinates are offset to the region
	 * 		of interest.
	 * @return Normalised coordinates.
	 */
	private Point2D.Double frameToNormalCoordinates(double x, double y, boolean withinROI) {
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
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param withinROI Whether the given coordinates should be offset to the
	 * 		region of interest.
	 * @return Frame coordinates.
	 */
	private Point normalToFrameCoordinates(double x, double y, boolean withinROI) {
		int scaleFactor = config.getFieldWidth();
		
		x *= scaleFactor;
		y *= scaleFactor;
		
		if (!withinROI) {
			x += config.getFieldLowX();
			y += config.getFieldLowY();
		}
		return new Point((int)x, (int)y);
	}
}

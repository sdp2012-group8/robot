package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
		CvSeq contour = findContours(ballThresh);
		
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
            	double epsilon = cvContourPerimeter(contour) * POLY_APPROX_ERROR;
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, epsilon, 0);
                
                CvRect boundingRect = cvBoundingRect(contour, 0);
                int x = boundingRect.x();
                int y = boundingRect.y();
                int w = boundingRect.width();
                int h = boundingRect.height();
                
                if ((w >= 5) && (w <= 25) && (h >= 5) && (h <= 25)) {
                    cvDrawContours(frame_ipl, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
                    
                    CvPoint pt1 = new CvPoint(x, y);
                    CvPoint pt2 = new CvPoint(x + w, y + h);
                    cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
                    
                    return frameToNormalCoordinates(x + w/2, y + h/2, true);
                }
            }
            contour = contour.h_next();
        }

		return new Point2D.Double(-1.0, -1.0);
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
		CvSeq contour = findContours(robotThresh);
		
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
            	double epsilon = cvContourPerimeter(contour) * POLY_APPROX_ERROR;
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, epsilon, 0);
                
                CvRect boundingRect = cvBoundingRect(contour, 0);
                int x = boundingRect.x();
                int y = boundingRect.y();
                int w = boundingRect.width();
                int h = boundingRect.height();
                
                if ((w >= 15) && (w <= 55) && (h >= 15) && (h <= 55)) {
                    cvDrawContours(frame_ipl, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
                    
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
                    
//                    return new Robot(frameToNormalCoordinates(rx, ry, true), 0.0);
                    return new Robot(frameToNormalCoordinates(mx, my, true), angle);
                }
            }
            contour = contour.h_next();
        }

		return new Robot(new Point2D.Double(-1.0, -1.0), 0.0);
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
		
		pt1 = normalToFrameCoordinates(ball.x, ball.y, false);
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		
		pt1 = normalToFrameCoordinates(blueRobot.getCoords().x,
				blueRobot.getCoords().y, false);
		pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
				blueRobot.getAngle());
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		graphics.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);
		
		pt1 = normalToFrameCoordinates(yellowRobot.getCoords().x,
				yellowRobot.getCoords().y, false);
		pt2 = Utilities.rotatePoint(pt1, new Point(pt1.x + DIR_LINE_LENGTH, pt1.y),
				yellowRobot.getAngle());
		graphics.drawArc(pt1.x - 2, pt1.y - 2, 4, 4, 0, 360);
		graphics.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);

		return finalFrame;
	}
	
	
	/**
	 * Find the contour in the specified image.
	 * 
	 * @param image Image to search.
	 * @return Image shapes' contour.
	 */
	private CvSeq findContours(IplImage image) {
		CvSeq contour = new CvSeq();
		cvFindContours(image, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);        
		return contour;
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
		double nx, ny;
		
		if (!withinROI) {
			nx = (x + config.getFieldLowX()) / scaleFactor;
			ny = (y + config.getFieldLowY()) / scaleFactor;
		} else {
			nx = x / scaleFactor;
			ny = y / scaleFactor;
		}
		
		return new Point2D.Double(nx, ny);
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
		int nx, ny;
		
		if (withinROI) {
			nx = (int)(x * scaleFactor);
			ny = (int)(y * scaleFactor);
		} else {
			nx = (int)(x * scaleFactor) + config.getFieldLowX();
			ny = (int)(y * scaleFactor) + config.getFieldLowY();
		}
		
		return new Point(nx, ny);
	}
}

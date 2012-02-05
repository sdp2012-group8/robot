package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import com.googlecode.javacpp.Loader;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import sdp.common.Robot;
import sdp.common.WorldState;


/**
 * An image processor where I try to explore different alternative ideas. Or
 * just trying to reimplement team 9's system.
 * 
 * @author Gediminas Liktaras
 */
public class MainImageProcessor extends ImageProcessor {
	
	/**
	 * The main constructor.
	 */
	public MainImageProcessor() {
		super();
	}
	

	/* (non-Javadoc)
	 * @see sdp.vision.processing.ImageProcessor#extractWorldState(java.awt.image.BufferedImage)
	 */
	@Override
	public synchronized WorldState extractWorldState(BufferedImage frame) {
		CvRect frameROI = cvRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());

		IplImage frame_ipl = IplImage.createFrom(frame);		
		cvSetImageROI(frame_ipl, frameROI);		
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		BufferedImage workingImage = frame_ipl.getBufferedImage();
		
		BufferedImage ballThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage blueThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage yellowThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		for (int x = 0; x < config.getFieldWidth(); ++x) {
			for (int y = 0; y < config.getFieldHeight(); ++y) {
				int ox = x + config.getFieldLowX();
				int oy = y + config.getFieldLowY();
				Color px = new Color(workingImage.getRGB(ox, oy));
				
				int r = px.getRed();
				int g = px.getGreen();
				int b = px.getBlue();
				
				float hsv[] = Color.RGBtoHSB(r, g, b, null);
				int h = (int) (hsv[0] * 360);
				int s = (int) (hsv[1] * 100);
				int v = (int) (hsv[2] * 100);
				
				if ((h >= 350 || h <= 20) && s >= 60 && s >= 60) {
					ballThreshold.setRGB(x, y, Color.white.getRGB());
					workingImage.setRGB(ox, oy, Color.red.getRGB());
				}
				if ((h >= 70 && h <= 210 && s >= 10 && v >= 30 && g < (int)(b * 1.5))) {
					blueThreshold.setRGB(x, y, Color.white.getRGB());
					workingImage.setRGB(ox, oy, Color.blue.getRGB());
			    }
			    if ((h >= 25 && h <= 75 && s >= 60 && v >= 60)) {
			    	yellowThreshold.setRGB(x, y, Color.white.getRGB());
			    	workingImage.setRGB(ox, oy, Color.orange.getRGB());
			    }
			}
		}
		
		frame_ipl = IplImage.createFrom(workingImage);
		cvSetImageROI(frame_ipl, frameROI);
		
		CvMemStorage storage = CvMemStorage.create();
        CvSeq contour = new CvSeq(null);

        IplImage ball = IplImage.createFrom(ballThreshold);
        IplImage blue = IplImage.createFrom(blueThreshold);
		IplImage yellow = IplImage.createFrom(yellowThreshold);
		
		cvFindContours(ball, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_NONE);        
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                cvDrawContours(frame_ipl, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
                
                CvRect r = cvBoundingRect(contour, 0);
                CvPoint pt1 = new CvPoint(r.x(), r.y());
                CvPoint pt2 = new CvPoint(r.x() + r.width(), r.y() + r.height());
                cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            }
            contour = contour.h_next();
        }
        
        contour = new CvSeq(null);
        cvFindContours(blue, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_NONE);        
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                cvDrawContours(frame_ipl, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
                
                CvRect r = cvBoundingRect(contour, 0);
                CvPoint pt1 = new CvPoint(r.x(), r.y());
                CvPoint pt2 = new CvPoint(r.x() + r.width(), r.y() + r.height());
                cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            }
            contour = contour.h_next();
        }
		
        contour = new CvSeq(null);
        cvFindContours(yellow, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_NONE);        
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                cvDrawContours(frame_ipl, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, CV_AA);
                
                CvRect r = cvBoundingRect(contour, 0);
                CvPoint pt1 = new CvPoint(r.x(), r.y());
                CvPoint pt2 = new CvPoint(r.x() + r.width(), r.y() + r.height());
                cvDrawRect(frame_ipl, pt1, pt2, CvScalar.WHITE, 1, CV_AA, 0);
            }
            contour = contour.h_next();
        }
				
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = frame_ipl.getBufferedImage();
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}

}

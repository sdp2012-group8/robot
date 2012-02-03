package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.opencv_core.CvMat;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import sdp.common.Robot;
import sdp.common.WorldState;


/**
 * An image processor where I try to explore different alternative ideas. Or
 * just trying to reimplement team 9's system.
 * 
 * @author Gediminas Liktaras
 */
public class AlternativeImageProcessor extends ImageProcessor {
	
	/**
	 * The main constructor.
	 */
	public AlternativeImageProcessor() {
		super();
	}
	

	/* (non-Javadoc)
	 * @see sdp.vision.processing.ImageProcessor#extractWorldState(java.awt.image.BufferedImage)
	 */
	@Override
	public WorldState extractWorldState(BufferedImage frame) {
		CvRect frameROI = cvRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());

		IplImage frame_ipl = IplImage.createFrom(frame);		
		cvSetImageROI(frame_ipl, frameROI);		
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		BufferedImage workingImage = frame_ipl.getBufferedImage();
		
		BufferedImage ballThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_BINARY);
		BufferedImage blueThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_BINARY);
		BufferedImage yellowThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_BYTE_BINARY);
		
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
					frame.setRGB(ox, oy, Color.red.getRGB());
				}
				if ((h >= 150 && h <= 250 && s >= 20 && v >= 20)) {
					blueThreshold.setRGB(x, y, Color.white.getRGB());
		    		frame.setRGB(ox, oy, Color.blue.getRGB());
			    }
			    if ((h >= 25 && h <= 75 && s <= 40 && v >= 30)) {
			    	yellowThreshold.setRGB(x, y, Color.white.getRGB());
		    		frame.setRGB(ox, oy, Color.yellow.getRGB());
			    }
			}
		}
				
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = workingImage;
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}

}

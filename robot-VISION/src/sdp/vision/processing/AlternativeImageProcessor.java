package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
		BufferedImage ballThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_INT_RGB);
		BufferedImage blueThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_INT_RGB);
		BufferedImage yellowThreshold = new BufferedImage(config.getFieldWidth(), 
				config.getFieldHeight(), BufferedImage.TYPE_INT_RGB);
		
		for (int x = config.getFieldLowX(); x <= config.getFieldHighX(); ++x) {
			for (int y = config.getFieldLowY(); y <= config.getFieldHighY(); ++y) {
				Color px = new Color(frame.getRGB(x, y));
				int r = px.getRed();
				int g = px.getGreen();
				int b = px.getBlue();
				
				float hsv[] = Color.RGBtoHSB(r, g, b, null);
				int h = (int) (hsv[0] * 360);
				int s = (int) (hsv[1] * 100);
				int v = (int) (hsv[2] * 100);
				
				if ((h >= 350 || h <= 20) && s >= 60 && s >= 60) {
					frame.setRGB(x, y, Color.red.getRGB());
				}
				if ((h >= 170 && h <= 230 && s >= 20 && v >= 20)) {
		    		frame.setRGB(x, y, Color.blue.getRGB());
			    }
			    if ((h >= 25 && h <= 75 && s >= 10 && s <= 30 && v >= 30)) {
		    		frame.setRGB(x, y, Color.yellow.getRGB());
			    }
			}
		}
				
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = frame;
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}

}

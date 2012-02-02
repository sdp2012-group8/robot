package sdp.vision.processing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import sdp.common.Robot;
import sdp.common.WorldState;

/**
 * An image processor where I try to explore different alternative ideas.
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
		BufferedImage normalised = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < frame.getWidth(); ++i) {
			for (int j = 0; j < frame.getHeight(); ++j) {
				Color pixel = new Color(frame.getRGB(i, j));
				int sum = pixel.getRed() + pixel.getBlue() + pixel.getGreen();
				if (sum > 30) {
					pixel = new Color(pixel.getRed() * 255 / sum, pixel.getGreen() * 255 / sum, pixel.getBlue() * 255 / sum);
				} else {
					pixel = Color.black;
				}
				normalised.setRGB(i, j, pixel.getRGB());
			}
		}
		
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = normalised;
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}

}

package sdp.vision;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;


public class NewImageProcessor {
	
	/** The processor's configuration. */
	private ImageProcessorConfiguration config;

	private static int RED = 0;
	private static int GREEN = 1;
	private static int BLUE = 2;
	
	private int[] red = new int[] { 255, 0, 0 };
	private int[] yell = new int[] { 255, 255, 0 };
	private int[] blue = new int[] { 0, 0, 255 };
	
	private static int blueThreshold = 350;
	private static int yellThreshold = 150;
	
	public static void main(String[] args) {
		ImageProcessorConfiguration cfg = new ImageProcessorConfiguration();
		
		String bgImagePath = "data/testImages/bg.jpg";
		String testImage = "data/testImages/start_positions.jpg"; 
		
		BufferedImage image = null;
		
		try {
			image = javax.imageio.ImageIO.read(new File(bgImagePath));
		} catch (IOException ex){
			ex.printStackTrace();
		}
		try {
			cfg.setBackground(javax.imageio.ImageIO.read(new File(testImage))); 
		} catch (IOException ex){
			ex.printStackTrace();
		}
		
		NewImageProcessor br = new NewImageProcessor(cfg);
		if (image != null) {
	
			//displays the resulting image
			ImageIcon imageIcon = new ImageIcon();
			imageIcon.setImage(br.processing(image));
			JOptionPane.showMessageDialog(null, null, "Resulting image", JOptionPane.PLAIN_MESSAGE, imageIcon);
		}
		
	}
	
	public NewImageProcessor() {
		config = new ImageProcessorConfiguration();
	}
	
	public NewImageProcessor(ImageProcessorConfiguration cfg) {
		this.config = cfg;
	}
	
	/**
	 * @param image
	 * @param cfg.getBackground()
	 * the function that does the actual processing, based on cfg.getBackground() subtraction and thresholding
	 * for R, G, B and H
	 * */
	public synchronized BufferedImage processing(BufferedImage image) {
		Raster bgData = null;
		Raster data = null;
		
		
		try {
			data = image.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		try {
			bgData = config.getBackground().getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}
		
		
		for (int i = config.getFieldLowX(); i < config.getFieldHighX(); i = i + 1) { // for every
			for (int j = config.getFieldLowY(); j < config.getFieldHighY(); j = j + 1) {
			
				int[] bgPixel = new int[3];
				data.getPixel(i, j, bgPixel);
				
				int[] imagePixel = new int[3];
				bgData.getPixel(i, j, imagePixel);
				
				
				int[] difference = getDifference(imagePixel, bgPixel);
				
				// create a new colour with the rgb of the difference
				
				int	r = difference[0] > 0 ? difference[0] : 0;
				
				int g = difference[1] > 0 ? difference[1] : 0;
				
				int b = difference[2]> 0 ? difference[2] : 0;
				
				int sum = r+g+b;
				
				Color colour;
				
				if (sum < 40)
					colour = new Color(0,0,0);			
				else
					colour = new Color(r*255/sum,g*255/sum,b*255/sum);
				
				int rgb = colour.getRGB();
			
				if (b > 40){
					colour = new Color(255, 255, 255);
					rgb = colour.getRGB();
				} else {
					colour = new Color(0, 0, 0);
					rgb = colour.getRGB();
				}
						
				image.setRGB(i,j,rgb); 
				
			}
		}
			
		
		
		return image;

	}
	
	public int getColourDifference(int[] colour1, int[] colour2) {
		return Math.abs(colour1[RED] - colour2[RED])
				+ Math.abs(colour1[GREEN] - colour2[GREEN])
				+ Math.abs(colour1[BLUE] - colour2[BLUE]);
	}
	
	public int[] getDifference(int[] colour1, int[] colour2){
		int[] difference = {0,0,0};
		difference[0] =colour1[0] - colour2[0];
		difference[1] = colour1[1] - colour2[1];
		difference[2] = colour1[2] - colour2[2];
		
		return difference;
	}
	
	public boolean isGrey(int red, int green, int blue, int x, int y){
	
		if (red < 100 && green < 100 && blue < 100 && (x > (config.getFieldLowX() + 30)) && (x < (config.getFieldHighX() - 30))
				&& (y > (config.getFieldLowY() + 30)) && (y < (config.getFieldHighY() - 30)))
			return true;
		
		return false;
	}
	
	boolean isBrightGreen(int[] colour) {
		return (colour[RED] < 65 && colour[GREEN] > 180 && colour[BLUE] < 170);
	}

	boolean isYellow(int[] colour) {
		int ytDifference = getColourDifference(yell, colour);
		return (ytDifference < yellThreshold && colour[RED] > 150 && colour[GREEN] > 170);
	}

	boolean isBlue(int[] colour) {
		int btDifference = getColourDifference(blue, colour);
		return (btDifference < blueThreshold && colour[BLUE] > 130);
	}

	
	/**
	 * Get the image processor's configuration.
	 * 
	 * @return The configuration.
	 */
	public synchronized ImageProcessorConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Set the image processor's configuration.
	 * 
	 * @param config The new configuration.
	 */
	public synchronized void setConfiguration(ImageProcessorConfiguration config) {
		this.config = config;
	}
	
}

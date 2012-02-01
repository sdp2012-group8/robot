package sdp.vision;

import java.awt.image.BufferedImage;

/**
 * Vision subsystem configuration container.
 * 
 * @author Gediminas Liktaras
 */
public class ImageProcessorConfiguration {
	
	/** Height of the incoming images. */
	private int frameHeight = 480;
	
	/** Width of the incoming images. */
	private int frameWidth = 640;
	
	
	/** The background image. */
	private BufferedImage background;

	
	/** The position of the left wall of the field (x low). */
	private double fieldLowX = 0.05;
	
	/** The position of the right wall of the field (x high). */
	private double fieldHighX = 0.95;
	
	/** The position of the top wall of the field (y low). */
	private double fieldLowY = 0.05;
	
	/** The position of the right wall of the field (y high). */
	private double fieldHighY = 0.95;
		

	/**
	 * The default constructor. Creates the default vision configuration.
	 */
	public ImageProcessorConfiguration() {
		background = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB);
	}
	

	/**
	 * Get the background image.
	 * 
	 * @return The background.
	 */
	public BufferedImage getBackground() {
		return background;
	}

	/**
	 * Set the background image.
	 * 
	 * @param background The background.
	 */
	public void setBackground(BufferedImage background) {
		if (background == null) {
			throw new NullPointerException("null background image given to the image processor.");
		} else {
			this.background = background;
		}
	}
	
	
	/**
	 * Get the position of the field's left wall.
	 * 
	 * @return Position of the field's left wall.
	 */
	public int getFieldLowX() {
		return (int) (fieldLowX * frameWidth);
	}

	/**
	 * Get the relative position of the field's left wall.
	 * 
	 * @return Relative position of the field's left wall.
	 */
	public double getRawFieldLowX() {
		return fieldLowX;
	}

	/**
	 * Set the relative position of the field's left wall.
	 * 
	 * @param fieldLowX Relative position of the field's left wall.
	 */
	public void setRawFieldLowX(double fieldLowX) {
		if (fieldLowX >= 0.0) {
			this.fieldLowX = fieldLowX;
		} else {
			throw new IllegalArgumentException("Tried to set negative field wall low x.");
		}
	}

	
	/**
	 * Get the position of the field's right wall.
	 * 
	 * @return Position of the field's right wall.
	 */
	public int getFieldHighX() {
		return (int) (fieldHighX * frameWidth);
	}
	
	/**
	 * Get the relative position of the field's right wall.
	 * 
	 * @return Relative position of the field's right wall.
	 */
	public double getRawFieldHighX() {
		return fieldHighX;
	}

	/**
	 * Set the relative position of the field's right wall.
	 * 
	 * @param fieldHighX Relative position of the field's right wall.
	 */
	public void setRawFieldHighX(double fieldHighX) {
		if (fieldHighX >= 0.0) {
			this.fieldHighX = fieldHighX;
		} else {
			throw new IllegalArgumentException("Tried to set negative field wall high x.");
		}
	}

	
	/**
	 * Get the position of the field's top wall.
	 * 
	 * @return Position of the field's top wall.
	 */
	public int getFieldLowY() {
		return (int) (fieldLowY * frameHeight);
	}
	
	/**
	 * Get the relative position of the field's top wall.
	 * 
	 * @return Relative position of the field's top wall.
	 */
	public double getRawFieldLowY() {
		return fieldLowY;
	}

	/**
	 * Set the relative position of the field's top wall.
	 * 
	 * @param fieldLowY Relative position of the field's top wall.
	 */
	public void setRawFieldLowY(double fieldLowY) {
		if (fieldLowY >= 0.0) {
			this.fieldLowY = fieldLowY;
		} else {
			throw new IllegalArgumentException("Tried to set negative field wall low y.");
		}
	}

	
	/**
	 * Get the position of the field's bottom wall.
	 * 
	 * @return Position of the field's bottom wall.
	 */
	public int getFieldHighY() {
		return (int) (fieldHighY * frameHeight);
	}
	
	/**
	 * Get the relative position of the field's bottom wall.
	 * 
	 * @return Relative position of the field's bottom wall.
	 */
	public double getRawFieldHighY() {
		return fieldHighY;
	}

	/**
	 * Set the relative position of the field's bottom wall.
	 * 
	 * @param fieldHighY Relative position of the field's bottom wall.
	 */
	public void setRawFieldHighY(double fieldHighY) {
		if (fieldHighY >= 0.0) {
			this.fieldHighY = fieldHighY;
		} else {
			throw new IllegalArgumentException("Tried to set negative field wall high y.");
		}
	}
	
	
	/**
	 * Get the height of the field in pixels.
	 * 
	 * @return The height of the field.
	 */
	public int getFieldHeight() {
		return (getFieldHighY() - getFieldLowY());
	}
	
	/**
	 * Get the width of the field in pixels.
	 * 
	 * @return The width of the field.
	 */
	public int getFieldWidth() {
		return (getFieldHighX() - getFieldLowX());
	}
	
	
	/**
	 * Get the height of incoming images.
	 * 
	 * @return The height of the frames.
	 */
	public int getFrameHeight() {
		return frameHeight;
	}

	/**
	 * Set the height of incoming images.
	 * 
	 * @param frameHeight The height of the frames.
	 */
	public void setFrameHeight(int frameHeight) {
		if (frameHeight > 0) {
			this.frameHeight = frameHeight;
		} else {
			throw new IllegalArgumentException("Tried to set negative frame height.");
		}
	}

	
	/**
	 * Get the width of incoming images.
	 * 
	 * @return The width of the frame.
	 */
	public int getFrameWidth() {
		return frameWidth;
	}

	/**
	 * Set the width of incoming images.
	 * 
	 * @param frameWidth The width of the frames.
	 */
	public void setFrameWidth(int frameWidth) {
		if (frameWidth > 0) {
			this.frameWidth = frameWidth;
		} else {
			throw new IllegalArgumentException("Tried to set negative frame width.");
		}
	}
	
}

package sdp.vision;


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
	
	/** The position of the left wall of the field (x low). */
	private double fieldLowX = 0.05;	
	/** The position of the right wall of the field (x high). */
	private double fieldHighX = 0.95;	
	/** The position of the top wall of the field (y low). */
	private double fieldLowY = 0.2;	
	/** The position of the right wall of the field (y high). */
	private double fieldHighY = 0.8;
	
	/** Lower bound of the ball threshold's hue value. */
	private int ballHueMinValue = 350;
	/** Lower bound of the ball threshold's saturation value. */
	private int ballSatMinValue = 60;
	/** Lower bound of the ball threshold's value value. */
	private int ballValMinValue = 40;
	/** Lower bound of the ball contour size. */
	private int ballSizeMinValue = 5;
	/** Upper bound of the ball threshold's hue value. */
	private int ballHueMaxValue = 20;
	/** Upper bound of the ball threshold's saturation value. */
	private int ballSatMaxValue = 100;
	/** Upper bound of the ball threshold's value value. */
	private int ballValMaxValue = 100;
	/** Upper bound of the ball contour size. */
	private int ballSizeMaxValue = 25;
	/** Whether to show thresholded ball image. */
	private boolean showBallThreshold = false;
		

	/**
	 * The default constructor. Creates the default vision configuration.
	 */
	public ImageProcessorConfiguration() { }
	
	
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


	/**
	 * Get the lower bound of the ball threshold's hue value.
	 * 
	 * @return Minimum ball hue value.
	 */
	public int getBallHueMinValue() {
		return ballHueMinValue;
	}

	/**
	 * Set the lower bound of the ball threshold's hue value.
	 * 
	 * @param ballHueMinValue The minimum ball hue value.
	 */
	public void setBallHueMinValue(int ballHueMinValue) {
		this.ballHueMinValue = ballHueMinValue;
	}


	/**
	 * Get the lower bound of the ball threshold's saturation value.
	 * 
	 * @return Minimum ball saturation value.
	 */
	public int getBallSatMinValue() {
		return ballSatMinValue;
	}

	/**
	 * Set the lower bound of the ball threshold's saturation value.
	 * 
	 * @param ballSatMinValue The minimum ball saturation value.
	 */
	public void setBallSatMinValue(int ballSatMinValue) {
		this.ballSatMinValue = ballSatMinValue;
	}


	/**
	 * Get the lower bound of the ball threshold's value value.
	 * 
	 * @return Minimum ball value value.
	 */
	public int getBallValMinValue() {
		return ballValMinValue;
	}

	/**
	 * Set the lower bound of the ball threshold's value value.
	 * 
	 * @param ballValMinValue The minimum ball value value.
	 */
	public void setBallValMinValue(int ballValMinValue) {
		this.ballValMinValue = ballValMinValue;
	}
	
	
	/**
	 * Get the lower bound of the ball contour's size.
	 * 
	 * @return Minimum ball contour size.
	 */
	public int getBallSizeMinValue() {
		return ballSizeMinValue;
	}

	/**
	 * Set the lower bound of the ball contour's size.
	 * 
	 * @param ballSizeMinValue Minimum ball contour size.
	 */
	public void setBallSizeMinValue(int ballSizeMinValue) {
		this.ballSizeMinValue = ballSizeMinValue;
	}


	/**
	 * Get the upper bound of the ball threshold's hue value.
	 * 
	 * @return Maximum ball hue value.
	 */
	public int getBallHueMaxValue() {
		return ballHueMaxValue;
	}

	/**
	 * Set the upper bound of the ball threshold's hue value.
	 * 
	 * @param ballHueMaxValue The maximum ball hue value.
	 */
	public void setBallHueMaxValue(int ballHueMaxValue) {
		this.ballHueMaxValue = ballHueMaxValue;
	}


	/**
	 * Get the upper bound of the ball threshold's saturation value.
	 * 
	 * @return Maximum ball saturation value.
	 */
	public int getBallSatMaxValue() {
		return ballSatMaxValue;
	}

	/**
	 * Set the upper bound of the ball threshold's saturation value.
	 * 
	 * @param ballSatMaxValue The maximum ball saturation value.
	 */
	public void setBallSatMaxValue(int ballSatMaxValue) {
		this.ballSatMaxValue = ballSatMaxValue;
	}


	/**
	 * Get the upper bound of the ball threshold's value value.
	 * 
	 * @return Maximum ball value value.
	 */
	public int getBallValMaxValue() {
		return ballValMaxValue;
	}

	/**
	 * Set the upper bound of the ball threshold's value value.
	 * 
	 * @param ballValMaxValue The maximum ball value value.
	 */
	public void setBallValMaxValue(int ballValMaxValue) {
		this.ballValMaxValue = ballValMaxValue;
	}

	/**
	 * Get the upper bound of the ball contour's size.
	 * 
	 * @return Maximum ball contour size.
	 */
	public int getBallSizeMaxValue() {
		return ballSizeMaxValue;
	}

	/**
	 * Set the upper bound of the ball contour's size.
	 * 
	 * @param ballSizeMaxValue Maximum ball contour size.
	 */
	public void setBallSizeMaxValue(int ballSizeMaxValue) {
		this.ballSizeMaxValue = ballSizeMaxValue;
	}


	/**
	 * Get whether the ball's threshold should be shown.
	 * 
	 * @return Whether the ball's threshold should be shown.
	 */
	public boolean showBallThreshold() {
		return showBallThreshold;
	}

	/**
	 * Set whether the ball's threshold should be shown.
	 * 
	 * @param showBallThreshold Whether the ball's threshold should be shown.
	 */
	public void setShowBallThreshold(boolean showBallThreshold) {
		this.showBallThreshold = showBallThreshold;
	}
	
}

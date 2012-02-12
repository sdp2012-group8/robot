package sdp.vision;


/**
 * Vision subsystem configuration container.
 * 
 * @author Gediminas Liktaras
 */
public class ImageProcessorConfig {
	
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
	/** Upper bound of the ball threshold's hue value. */
	private int ballHueMaxValue = 20;
	/** Lower bound of the ball threshold's saturation value. */
	private int ballSatMinValue = 60;
	/** Upper bound of the ball threshold's saturation value. */
	private int ballSatMaxValue = 100;
	/** Lower bound of the ball threshold's value value. */
	private int ballValMinValue = 40;
	/** Upper bound of the ball threshold's value value. */
	private int ballValMaxValue = 100;
	/** Lower bound of the ball contour size. */
	private int ballSizeMinValue = 5;
	/** Upper bound of the ball contour size. */
	private int ballSizeMaxValue = 25;
	
	/** Lower bound of the Blue threshold's hue value. */
	private int blueHueMinValue = 150;
	/** Upper bound of the Blue threshold's hue value. */
	private int blueHueMaxValue = 210;
	/** Lower bound of the Blue threshold's saturation value. */
	private int blueSatMinValue = 0;
	/** Upper bound of the Blue threshold's saturation value. */
	private int blueSatMaxValue = 100;
	/** Lower bound of the Blue threshold's value value. */
	private int blueValMinValue = 30;
	/** Upper bound of the Blue threshold's value value. */
	private int blueValMaxValue = 100;
	/** Lower bound of the Blue contour size. */
	private int blueSizeMinValue = 10;
	/** Upper bound of the Blue contour size. */
	private int blueSizeMaxValue = 50;
	
	/** Lower bound of the Yellow threshold's hue value. */
	private int yellowHueMinValue = 25;
	/** Upper bound of the Yellow threshold's hue value. */
	private int yellowHueMaxValue = 75;
	/** Lower bound of the Yellow threshold's saturation value. */
	private int yellowSatMinValue = 0;
	/** Upper bound of the Yellow threshold's saturation value. */
	private int yellowSatMaxValue = 50;
	/** Lower bound of the Yellow threshold's value value. */
	private int yellowValMinValue = 55;
	/** Upper bound of the Yellow threshold's value value. */
	private int yellowValMaxValue = 75;
	/** Lower bound of the Yellow contour size. */
	private int yellowSizeMinValue = 15;
	/** Upper bound of the Yellow contour size. */
	private int yellowSizeMaxValue = 50;
	
	/** Whether to show the captured frame itself. */
	private boolean showWorld = true;
	/** Whether to show thresholded pixels. */
	private boolean showThresholds = false;
	/** Whether to show thresholded contours. */
	private boolean showContours = false;
	/** Whether to show coutour bounding boxes. */
	private boolean showBoundingBoxes = false;
	/** Whether to show world state data. */
	private boolean showStateData = true;


	/**
	 * The default constructor. Creates the default vision configuration.
	 */
	public ImageProcessorConfig() { }
	
	
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
	 * Get the lower bound of the blue threshold's hue value.
	 * 
	 * @return Minimum blue hue value.
	 */
	public int getBlueHueMinValue() {
		return blueHueMinValue;
	}

	/**
	 * Set the lower bound of the blue threshold's hue value.
	 * 
	 * @param blueHueMinValue The minimum blue hue value.
	 */
	public void setBlueHueMinValue(int blueHueMinValue) {
		this.blueHueMinValue = blueHueMinValue;
	}


	/**
	 * Get the lower bound of the blue threshold's saturation value.
	 * 
	 * @return Minimum blue saturation value.
	 */
	public int getBlueSatMinValue() {
		return blueSatMinValue;
	}

	/**
	 * Set the lower bound of the blue threshold's saturation value.
	 * 
	 * @param blueSatMinValue The minimum blue saturation value.
	 */
	public void setBlueSatMinValue(int blueSatMinValue) {
		this.blueSatMinValue = blueSatMinValue;
	}


	/**
	 * Get the lower bound of the blue threshold's value value.
	 * 
	 * @return Minimum blue value value.
	 */
	public int getBlueValMinValue() {
		return blueValMinValue;
	}

	/**
	 * Set the lower bound of the blue threshold's value value.
	 * 
	 * @param blueValMinValue The minimum blue value value.
	 */
	public void setBlueValMinValue(int blueValMinValue) {
		this.blueValMinValue = blueValMinValue;
	}
	
	
	/**
	 * Get the lower bound of the blue contour's size.
	 * 
	 * @return Minimum blue contour size.
	 */
	public int getBlueSizeMinValue() {
		return blueSizeMinValue;
	}

	/**
	 * Set the lower bound of the blue contour's size.
	 * 
	 * @param blueSizeMinValue Minimum blue contour size.
	 */
	public void setBlueSizeMinValue(int blueSizeMinValue) {
		this.blueSizeMinValue = blueSizeMinValue;
	}


	/**
	 * Get the upper bound of the blue threshold's hue value.
	 * 
	 * @return Maximum blue hue value.
	 */
	public int getBlueHueMaxValue() {
		return blueHueMaxValue;
	}

	/**
	 * Set the upper bound of the blue threshold's hue value.
	 * 
	 * @param blueHueMaxValue The maximum blue hue value.
	 */
	public void setBlueHueMaxValue(int blueHueMaxValue) {
		this.blueHueMaxValue = blueHueMaxValue;
	}


	/**
	 * Get the upper bound of the blue threshold's saturation value.
	 * 
	 * @return Maximum blue saturation value.
	 */
	public int getBlueSatMaxValue() {
		return blueSatMaxValue;
	}

	/**
	 * Set the upper bound of the blue threshold's saturation value.
	 * 
	 * @param blueSatMaxValue The maximum blue saturation value.
	 */
	public void setBlueSatMaxValue(int blueSatMaxValue) {
		this.blueSatMaxValue = blueSatMaxValue;
	}


	/**
	 * Get the upper bound of the blue threshold's value value.
	 * 
	 * @return Maximum blue value value.
	 */
	public int getBlueValMaxValue() {
		return blueValMaxValue;
	}

	/**
	 * Set the upper bound of the blue threshold's value value.
	 * 
	 * @param blueValMaxValue The maximum blue value value.
	 */
	public void setBlueValMaxValue(int blueValMaxValue) {
		this.blueValMaxValue = blueValMaxValue;
	}

	/**
	 * Get the upper bound of the blue contour's size.
	 * 
	 * @return Maximum blue contour size.
	 */
	public int getBlueSizeMaxValue() {
		return blueSizeMaxValue;
	}

	/**
	 * Set the upper bound of the blue contour's size.
	 * 
	 * @param blueSizeMaxValue Maximum blue contour size.
	 */
	public void setBlueSizeMaxValue(int blueSizeMaxValue) {
		this.blueSizeMaxValue = blueSizeMaxValue;
	}
	
	

	/**
	 * Get the lower bound of the yellow threshold's hue value.
	 * 
	 * @return Minimum yellow hue value.
	 */
	public int getYellowHueMinValue() {
		return yellowHueMinValue;
	}

	/**
	 * Set the lower bound of the yellow threshold's hue value.
	 * 
	 * @param yellowHueMinValue The minimum yellow hue value.
	 */
	public void setYellowHueMinValue(int yellowHueMinValue) {
		this.yellowHueMinValue = yellowHueMinValue;
	}


	/**
	 * Get the lower bound of the yellow threshold's saturation value.
	 * 
	 * @return Minimum yellow saturation value.
	 */
	public int getYellowSatMinValue() {
		return yellowSatMinValue;
	}

	/**
	 * Set the lower bound of the yellow threshold's saturation value.
	 * 
	 * @param yellowSatMinValue The minimum yellow saturation value.
	 */
	public void setYellowSatMinValue(int yellowSatMinValue) {
		this.yellowSatMinValue = yellowSatMinValue;
	}


	/**
	 * Get the lower bound of the yellow threshold's value value.
	 * 
	 * @return Minimum yellow value value.
	 */
	public int getYellowValMinValue() {
		return yellowValMinValue;
	}

	/**
	 * Set the lower bound of the yellow threshold's value value.
	 * 
	 * @param yellowValMinValue The minimum yellow value value.
	 */
	public void setYellowValMinValue(int yellowValMinValue) {
		this.yellowValMinValue = yellowValMinValue;
	}
	
	
	/**
	 * Get the lower bound of the yellow contour's size.
	 * 
	 * @return Minimum yellow contour size.
	 */
	public int getYellowSizeMinValue() {
		return yellowSizeMinValue;
	}

	/**
	 * Set the lower bound of the yellow contour's size.
	 * 
	 * @param yellowSizeMinValue Minimum yellow contour size.
	 */
	public void setYellowSizeMinValue(int yellowSizeMinValue) {
		this.yellowSizeMinValue = yellowSizeMinValue;
	}


	/**
	 * Get the upper bound of the yellow threshold's hue value.
	 * 
	 * @return Maximum yellow hue value.
	 */
	public int getYellowHueMaxValue() {
		return yellowHueMaxValue;
	}

	/**
	 * Set the upper bound of the yellow threshold's hue value.
	 * 
	 * @param yellowHueMaxValue The maximum yellow hue value.
	 */
	public void setYellowHueMaxValue(int yellowHueMaxValue) {
		this.yellowHueMaxValue = yellowHueMaxValue;
	}


	/**
	 * Get the upper bound of the yellow threshold's saturation value.
	 * 
	 * @return Maximum yellow saturation value.
	 */
	public int getYellowSatMaxValue() {
		return yellowSatMaxValue;
	}

	/**
	 * Set the upper bound of the yellow threshold's saturation value.
	 * 
	 * @param yellowSatMaxValue The maximum yellow saturation value.
	 */
	public void setYellowSatMaxValue(int yellowSatMaxValue) {
		this.yellowSatMaxValue = yellowSatMaxValue;
	}


	/**
	 * Get the upper bound of the yellow threshold's value value.
	 * 
	 * @return Maximum yellow value value.
	 */
	public int getYellowValMaxValue() {
		return yellowValMaxValue;
	}

	/**
	 * Set the upper bound of the yellow threshold's value value.
	 * 
	 * @param yellowValMaxValue The maximum yellow value value.
	 */
	public void setYellowValMaxValue(int yellowValMaxValue) {
		this.yellowValMaxValue = yellowValMaxValue;
	}

	/**
	 * Get the upper bound of the yellow contour's size.
	 * 
	 * @return Maximum yellow contour size.
	 */
	public int getYellowSizeMaxValue() {
		return yellowSizeMaxValue;
	}

	/**
	 * Set the upper bound of the yellow contour's size.
	 * 
	 * @param yellowSizeMaxValue Maximum yellow contour size.
	 */
	public void setYellowSizeMaxValue(int yellowSizeMaxValue) {
		this.yellowSizeMaxValue = yellowSizeMaxValue;
	}
	
	
	/**
	 * Get whether to show the captured frame.
	 * 
	 * @return Whether to show the captured frame.
	 */
	public boolean isShowWorld() {
		return showWorld;
	}

	/**
	 * Set whether to show the captured frame.
	 * 
	 * @param showWorld Whether to show the captured frame.
	 */
	public void setShowWorld(boolean showWorld) {
		this.showWorld = showWorld;
	}


	/**
	 * Get whether to show thresholded pixels.
	 * 
	 * @return Whether to show thresholded pixels.
	 */
	public boolean isShowThresholds() {
		return showThresholds;
	}

	/**
	 * Set whether to show thresholded pixels.
	 * 
	 * @param showThresholds Whether to show thresholded pixels.
	 */
	public void setShowThresholds(boolean showThresholds) {
		this.showThresholds = showThresholds;
	}


	/**
	 * Get whether to show thresholded contours.
	 * 
	 * @return Whether to show thresholded contours.
	 */
	public boolean isShowContours() {
		return showContours;
	}

	/**
	 * Set whether to show thresholded contours.
	 * 
	 * @param showContours Whether to show thresholded contours.
	 */
	public void setShowContours(boolean showContours) {
		this.showContours = showContours;
	}


	/**
	 * Get whether to show contours' bounding boxes.
	 * 
	 * @return Whether to show contours' bounding boxes.
	 */
	public boolean isShowBoundingBoxes() {
		return showBoundingBoxes;
	}

	/**
	 * Set whether to show contours' bounding boxes.
	 * 
	 * @param showBoundingBoxes Whether to show contours' bounding boxes.
	 */
	public void setShowBoundingBoxes(boolean showBoundingBoxes) {
		this.showBoundingBoxes = showBoundingBoxes;
	}
	
	
	/**
	 * Get whether to show world's state data.
	 * 
	 * @return Whether to show world's state data.
	 */
	public boolean isShowStateData() {
		return showStateData;
	}

	/**
	 * Set whether to show world's state data.
	 * 
	 * @param showStateData Whether to show world's state data.
	 */
	public void setShowStateData(boolean showStateData) {
		this.showStateData = showStateData;
	}

}

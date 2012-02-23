package sdp.vision.processing;

import java.io.File;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sdp.common.xml.XmlUtils;


/**
 * Vision subsystem configuration container.
 * 
 * This ungodly collection of auto-generated getters and setters is brought you
 * by the letter MAI EK (U+0E48). Deal with it.
 * 
 * See http://opencv.itseez.com/modules/calib3d/doc/camera_calibration_and_3d_reconstruction.html
 * for the meaning of the undistortion coefficients.
 * 
 * @author Gediminas Liktaras
 */
public class ImageProcessorConfig {
	
	/** Class's logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.vision.ImageProcessorConfig");
	
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
	
	/** Image undistortion coefficient f_x. */
	private double undistort_fx = 1.0;
	/** Image undistortion coefficient f_y. */
	private double undistort_fy = 1.0;
	/** Image undistortion coefficient c_x. */
	private double undistort_cx = 1.0;
	/** Image undistortion coefficient c_y. */
	private double undistort_cy = 1.0;
	
	/** Image undistortion coefficient k_1. */
	private double undistort_k1 = 0.0;
	/** Image undistortion coefficient k_2. */
	private double undistort_k2 = 0.0;
	/** Image undistortion coefficient p_1. */
	private double undistort_p1 = 0.0;
	/** Image undistortion coefficient p_2. */
	private double undistort_p2 = 0.0;
	
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
	 * Get the undistortion coefficient f_x.
	 * 
	 * @return Coefficient f_x.
	 */
	public double getUndistort_fx() {
		return undistort_fx;
	}

	/**
	 * Set the undistortion coefficient f_x.
	 * 
	 * @param undistort_fx New value of coefficient f_x.
	 */
	public void setUndistort_fx(double undistort_fx) {
		this.undistort_fx = undistort_fx;
	}


	/**
	 * Get the undistortion coefficient f_y.
	 * 
	 * @return Coefficient f_y.
	 */
	public double getUndistort_fy() {
		return undistort_fy;
	}

	/**
	 * Set the undistortion coefficient f_y.
	 * 
	 * @param undistort_fy New value of coefficient f_y.
	 */
	public void setUndistort_fy(double undistort_fy) {
		this.undistort_fy = undistort_fy;
	}


	/**
	 * Get the undistortion coefficient c_x.
	 * 
	 * @return Coefficient c_x.
	 */
	public double getUndistort_cx() {
		return undistort_cx;
	}

	/**
	 * Set the undistortion coefficient c_x.
	 * 
	 * @param undistort_cx New value of coefficient c_x.
	 */
	public void setUndistort_cx(double undistort_cx) {
		this.undistort_cx = undistort_cx;
	}


	/**
	 * Get the undistortion coefficient c_y.
	 * 
	 * @return Coefficient c_y.
	 */
	public double getUndistort_cy() {
		return undistort_cy;
	}

	/**
	 * Set the undistortion coefficient c_y.
	 * 
	 * @param undistort_cy New value of coefficient c_y.
	 */
	public void setUndistort_cy(double undistort_cy) {
		this.undistort_cy = undistort_cy;
	}


	/**
	 * Get the undistortion coefficient k_1.
	 * 
	 * @return Coefficient k_1.
	 */
	public double getUndistort_k1() {
		return undistort_k1;
	}

	/**
	 * Set the undistortion coefficient k_1.
	 * 
	 * @param undistort_k1 New value of coefficient k_1.
	 */
	public void setUndistort_k1(double undistort_k1) {
		this.undistort_k1 = undistort_k1;
	}


	/**
	 * Get the undistortion coefficient k_2.
	 * 
	 * @return Coefficient k_2.
	 */
	public double getUndistort_k2() {
		return undistort_k2;
	}

	/**
	 * Set the undistortion coefficient k_2.
	 * 
	 * @param undistort_k2 New value of coefficient k_2.
	 */
	public void setUndistort_k2(double undistort_k2) {
		this.undistort_k2 = undistort_k2;
	}


	/**
	 * Get the undistortion coefficient p_1.
	 * 
	 * @return Coefficient p_1.
	 */
	public double getUndistort_p1() {
		return undistort_p1;
	}

	/**
	 * Set the undistortion coefficient p_1.
	 * 
	 * @param undistort_p1 New value of coefficient p_1.
	 */
	public void setUndistort_p1(double undistort_p1) {
		this.undistort_p1 = undistort_p1;
	}


	/**
	 * Get the undistortion coefficient p_2.
	 * 
	 * @return Coefficient p_2.
	 */
	public double getUndistort_p2() {
		return undistort_p2;
	}

	/**
	 * Set the undistortion coefficient p_2.
	 * 
	 * @param undistort_p2 New value of coefficient p_2.
	 */
	public void setUndistort_p2(double undistort_p2) {
		this.undistort_p2 = undistort_p2;
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
	
	
	/**
	 * Reads an image processor configuration from a file and returns it as
	 * a ImageProcessorConfiguration instance.
	 * 
	 * @param filename Name of the file that contains the configuration.
	 * @return An appropriate ImageProcessorConfiguration instance.
	 */
	public static ImageProcessorConfig loadConfiguration(String filename) {
		File configFile = new File(filename);
		if (!configFile.exists()) {
			LOGGER.info("The given image processor configuration file does not exist.");
			return null;
		}
				
		Document doc = XmlUtils.openXmlDocument(configFile);		
		ImageProcessorConfig config = new ImageProcessorConfig();
		
		Element rootElement = (Element)doc.getElementsByTagName("config").item(0);
		
		Element undistortElement = (Element)doc.getElementsByTagName("undistortion").item(0);
		config.setUndistort_cx(XmlUtils.getChildDouble(undistortElement, "cx"));
		config.setUndistort_cy(XmlUtils.getChildDouble(undistortElement, "cy"));
		config.setUndistort_fx(XmlUtils.getChildDouble(undistortElement, "fx"));
		config.setUndistort_fy(XmlUtils.getChildDouble(undistortElement, "fy"));
		config.setUndistort_k1(XmlUtils.getChildDouble(undistortElement, "k1"));
		config.setUndistort_k2(XmlUtils.getChildDouble(undistortElement, "k2"));
		config.setUndistort_p1(XmlUtils.getChildDouble(undistortElement, "p1"));
		config.setUndistort_p2(XmlUtils.getChildDouble(undistortElement, "p2"));
		
		Element fieldElement = (Element)rootElement.getElementsByTagName("field").item(0);
		config.setRawFieldLowX(XmlUtils.getChildDouble(fieldElement, "lowX"));
		config.setRawFieldLowY(XmlUtils.getChildDouble(fieldElement, "lowY"));
		config.setRawFieldHighX(XmlUtils.getChildDouble(fieldElement, "highX"));
		config.setRawFieldHighY(XmlUtils.getChildDouble(fieldElement, "highY"));
		
		Element ballElement = (Element)rootElement.getElementsByTagName("ball").item(0);
		config.setBallHueMinValue(XmlUtils.getChildInt(ballElement, "hueMin"));
		config.setBallHueMaxValue(XmlUtils.getChildInt(ballElement, "hueMax"));
		config.setBallSatMinValue(XmlUtils.getChildInt(ballElement, "satMin"));
		config.setBallSatMaxValue(XmlUtils.getChildInt(ballElement, "satMax"));
		config.setBallValMinValue(XmlUtils.getChildInt(ballElement, "valMin"));
		config.setBallValMaxValue(XmlUtils.getChildInt(ballElement, "valMax"));
		config.setBallSizeMinValue(XmlUtils.getChildInt(ballElement, "sizeMin"));
		config.setBallSizeMaxValue(XmlUtils.getChildInt(ballElement, "sizeMax"));
		
		Element blueElement = (Element)rootElement.getElementsByTagName("blue").item(0);
		config.setBlueHueMinValue(XmlUtils.getChildInt(blueElement, "hueMin"));
		config.setBlueHueMaxValue(XmlUtils.getChildInt(blueElement, "hueMax"));
		config.setBlueSatMinValue(XmlUtils.getChildInt(blueElement, "satMin"));
		config.setBlueSatMaxValue(XmlUtils.getChildInt(blueElement, "satMax"));
		config.setBlueValMinValue(XmlUtils.getChildInt(blueElement, "valMin"));
		config.setBlueValMaxValue(XmlUtils.getChildInt(blueElement, "valMax"));
		config.setBlueSizeMinValue(XmlUtils.getChildInt(blueElement, "sizeMin"));
		config.setBlueSizeMaxValue(XmlUtils.getChildInt(blueElement, "sizeMax"));
		
		Element yellowElement = (Element)rootElement.getElementsByTagName("yellow").item(0);
		config.setYellowHueMinValue(XmlUtils.getChildInt(yellowElement, "hueMin"));
		config.setYellowHueMaxValue(XmlUtils.getChildInt(yellowElement, "hueMax"));
		config.setYellowSatMinValue(XmlUtils.getChildInt(yellowElement, "satMin"));
		config.setYellowSatMaxValue(XmlUtils.getChildInt(yellowElement, "satMax"));
		config.setYellowValMinValue(XmlUtils.getChildInt(yellowElement, "valMin"));
		config.setYellowValMaxValue(XmlUtils.getChildInt(yellowElement, "valMax"));
		config.setYellowSizeMinValue(XmlUtils.getChildInt(yellowElement, "sizeMin"));
		config.setYellowSizeMaxValue(XmlUtils.getChildInt(yellowElement, "sizeMax"));
		
		return config;
	}
	
	
	/**
	 * Writes the given image processor configuration into an XML file.
	 * 
	 * @param config Configuration to output.
	 * @param filename Output filename.
	 */
	public static void saveConfiguration(ImageProcessorConfig config, String filename) {
		Document doc = XmlUtils.createBlankXmlDocument();
		
		Element rootElement = doc.createElement("config");
		doc.appendChild(rootElement);
		
		Element undistortElement = doc.createElement("undistortion");
		XmlUtils.addChildDouble(doc, undistortElement, "cx", config.getUndistort_cx());
		XmlUtils.addChildDouble(doc, undistortElement, "cy", config.getUndistort_cy());
		XmlUtils.addChildDouble(doc, undistortElement, "fx", config.getUndistort_fx());
		XmlUtils.addChildDouble(doc, undistortElement, "fy", config.getUndistort_fy());
		XmlUtils.addChildDouble(doc, undistortElement, "k1", config.getUndistort_k1());
		XmlUtils.addChildDouble(doc, undistortElement, "k2", config.getUndistort_k2());
		XmlUtils.addChildDouble(doc, undistortElement, "p1", config.getUndistort_p1());
		XmlUtils.addChildDouble(doc, undistortElement, "p2", config.getUndistort_p2());
		rootElement.appendChild(undistortElement);
		
		Element fieldElement = doc.createElement("field");
		XmlUtils.addChildDouble(doc, fieldElement, "lowX", config.getRawFieldLowX());
		XmlUtils.addChildDouble(doc, fieldElement, "lowY", config.getRawFieldLowY());
		XmlUtils.addChildDouble(doc, fieldElement, "highX", config.getRawFieldHighX());
		XmlUtils.addChildDouble(doc, fieldElement, "highY", config.getRawFieldHighY());
		rootElement.appendChild(fieldElement);
		
		Element ballElement = doc.createElement("ball");
		XmlUtils.addChildInt(doc, ballElement, "hueMin", config.getBallHueMinValue());
		XmlUtils.addChildInt(doc, ballElement, "hueMax", config.getBallHueMaxValue());
		XmlUtils.addChildInt(doc, ballElement, "satMin", config.getBallSatMinValue());
		XmlUtils.addChildInt(doc, ballElement, "satMax", config.getBallSatMaxValue());
		XmlUtils.addChildInt(doc, ballElement, "valMin", config.getBallValMinValue());
		XmlUtils.addChildInt(doc, ballElement, "valMax", config.getBallValMaxValue());
		XmlUtils.addChildInt(doc, ballElement, "sizeMin", config.getBallSizeMinValue());
		XmlUtils.addChildInt(doc, ballElement, "sizeMax", config.getBallSizeMaxValue());
		rootElement.appendChild(ballElement);
		
		Element blueElement = doc.createElement("blue");
		XmlUtils.addChildInt(doc, blueElement, "hueMin", config.getBlueHueMinValue());
		XmlUtils.addChildInt(doc, blueElement, "hueMax", config.getBlueHueMaxValue());
		XmlUtils.addChildInt(doc, blueElement, "satMin", config.getBlueSatMinValue());
		XmlUtils.addChildInt(doc, blueElement, "satMax", config.getBlueSatMaxValue());
		XmlUtils.addChildInt(doc, blueElement, "valMin", config.getBlueValMinValue());
		XmlUtils.addChildInt(doc, blueElement, "valMax", config.getBlueValMaxValue());
		XmlUtils.addChildInt(doc, blueElement, "sizeMin", config.getBlueSizeMinValue());
		XmlUtils.addChildInt(doc, blueElement, "sizeMax", config.getBlueSizeMaxValue());
		rootElement.appendChild(blueElement);
		
		Element yellowElement = doc.createElement("yellow");
		XmlUtils.addChildInt(doc, yellowElement, "hueMin", config.getYellowHueMinValue());
		XmlUtils.addChildInt(doc, yellowElement, "hueMax", config.getYellowHueMaxValue());
		XmlUtils.addChildInt(doc, yellowElement, "satMin", config.getYellowSatMinValue());
		XmlUtils.addChildInt(doc, yellowElement, "satMax", config.getYellowSatMaxValue());
		XmlUtils.addChildInt(doc, yellowElement, "valMin", config.getYellowValMinValue());
		XmlUtils.addChildInt(doc, yellowElement, "valMax", config.getYellowValMaxValue());
		XmlUtils.addChildInt(doc, yellowElement, "sizeMin", config.getYellowSizeMinValue());
		XmlUtils.addChildInt(doc, yellowElement, "sizeMax", config.getYellowSizeMaxValue());
		rootElement.appendChild(yellowElement);
		
		XmlUtils.writeXmlDocument(doc, filename);
	}

}

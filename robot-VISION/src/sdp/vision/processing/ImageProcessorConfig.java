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
	
	/** Ball threshold bound container. */
	private ThresholdBounds ballThresholds = new ThresholdBounds();
	/** Lower bound of ball contour size. */
	private int ballSizeMin = 5;
	/** Upper bound of ball contour size. */
	private int ballSizeMax = 25;
	
	/** Blue robot threshold bound container. */
	private ThresholdBounds blueThresholds = new ThresholdBounds();
	/** Lower bound of ball contour size. */
	private int blueSizeMin = 10;
	/** Upper bound of ball contour size. */
	private int blueSizeMax = 60;
	
	/** Yellow robot threshold bound container. */
	private ThresholdBounds yellowThresholds = new ThresholdBounds();
	/** Lower bound of ball contour size. */
	private int yellowSizeMin = 10;
	/** Upper bound of ball contour size. */
	private int yellowSizeMax = 60;
	
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
	 * Get the ball threshold bounds.
	 * 
	 * @return Ball threshold bounds.
	 */
	public ThresholdBounds getBallThreshs() {
		return ballThresholds;
	}
	
	/**
	 * Set the ball threshold bounds.
	 * 
	 * @param bounds Ball threshold bounds.
	 */
	public void setBallThreshs(ThresholdBounds bounds) {
		ballThresholds = bounds;
	}

	
	/**
	 * Get the lower bound of the ball contour size.
	 * 
	 * @return Lower bound of ball contour size.
	 */
	public int getBallSizeMin() {
		return ballSizeMin;
	}

	/**
	 * Set the lower bound of the ball contour size.
	 * 
	 * @param ballSizeMin Lower bound of ball contour size.
	 */
	public void setBallSizeMin(int ballSizeMin) {
		this.ballSizeMin = ballSizeMin;
	}


	/**
	 * Get the upper bound of the ball contour size.
	 * 
	 * @return Upper bound of ball contour size.
	 */
	public int getBallSizeMax() {
		return ballSizeMax;
	}

	/**
	 * Set the upper bound of the ball contour size.
	 * 
	 * @param ballSizeMax Upper bound of ball contour size.
	 */
	public void setBallSizeMax(int ballSizeMax) {
		this.ballSizeMax = ballSizeMax;
	}


	/**
	 * Get the blue robot threshold bounds.
	 * 
	 * @return Blue robot threshold bounds.
	 */
	public ThresholdBounds getBlueThreshs() {
		return blueThresholds;
	}
	
	/**
	 * Set the blue robot threshold bounds.
	 * 
	 * @param bounds Blue robot threshold bounds.
	 */
	public void setBlueThreshs(ThresholdBounds bounds) {
		blueThresholds = bounds;
	}
	
	
	/**
	 * Get the lower bound of the blue robot contour size.
	 * 
	 * @return Lower bound of blue robot contour size.
	 */
	public int getBlueSizeMin() {
		return blueSizeMin;
	}

	/**
	 * Set the lower bound of the blue robot contour size.
	 * 
	 * @param blueSizeMin Lower bound of blue robot contour size.
	 */
	public void setBlueSizeMin(int blueSizeMin) {
		this.blueSizeMin = blueSizeMin;
	}


	/**
	 * Get the upper bound of the blue robot contour size.
	 * 
	 * @return Upper bound of blue robot contour size.
	 */
	public int getBlueSizeMax() {
		return blueSizeMax;
	}

	/**
	 * Set the upper bound of the blue robot contour size.
	 * 
	 * @param blueSizeMax Upper bound of blue robot contour size.
	 */
	public void setBlueSizeMax(int blueSizeMax) {
		this.blueSizeMax = blueSizeMax;
	}
	

	/**
	 * Get the yellow robot threshold bounds.
	 * 
	 * @return Yellow robot threshold bounds.
	 */
	public ThresholdBounds getYellowThreshs() {
		return yellowThresholds;
	}
	
	/**
	 * Set the yellow robot threshold bounds.
	 * 
	 * @param bounds Yellow robot threshold bounds.
	 */
	public void setYellowThreshs(ThresholdBounds bounds) {
		yellowThresholds = bounds;
	}
	
	
	/**
	 * Get the lower bound of the yellow robot contour size.
	 * 
	 * @return Lower bound of yellow robot contour size.
	 */
	public int getYellowSizeMin() {
		return yellowSizeMin;
	}

	/**
	 * Set the lower bound of the yellow robot contour size.
	 * 
	 * @param yellowSizeMin Lower bound of yellow robot contour size.
	 */
	public void setYellowSizeMin(int yellowSizeMin) {
		this.yellowSizeMin = yellowSizeMin;
	}


	/**
	 * Get the upper bound of the yellow robot contour size.
	 * 
	 * @return Upper bound of yellow robot contour size.
	 */
	public int getYellowSizeMax() {
		return yellowSizeMax;
	}

	/**
	 * Set the upper bound of the yellow robot contour size.
	 * 
	 * @param yellowSizeMax Upper bound of yellow robot contour size.
	 */
	public void setYellowSizeMax(int yellowSizeMax) {
		this.yellowSizeMax = yellowSizeMax;
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
		config.getBallThreshs().setHueMin(XmlUtils.getChildInt(ballElement, "hueMin"));
		config.getBallThreshs().setHueMax(XmlUtils.getChildInt(ballElement, "hueMax"));
		config.getBallThreshs().setSatMin(XmlUtils.getChildInt(ballElement, "satMin"));
		config.getBallThreshs().setSatMax(XmlUtils.getChildInt(ballElement, "satMax"));
		config.getBallThreshs().setValMin(XmlUtils.getChildInt(ballElement, "valMin"));
		config.getBallThreshs().setValMax(XmlUtils.getChildInt(ballElement, "valMax"));
		config.setBallSizeMin(XmlUtils.getChildInt(ballElement, "sizeMin"));
		config.setBallSizeMax(XmlUtils.getChildInt(ballElement, "sizeMax"));
		
		Element blueElement = (Element)rootElement.getElementsByTagName("blue").item(0);
		config.getBlueThreshs().setHueMin(XmlUtils.getChildInt(blueElement, "hueMin"));
		config.getBlueThreshs().setHueMax(XmlUtils.getChildInt(blueElement, "hueMax"));
		config.getBlueThreshs().setSatMin(XmlUtils.getChildInt(blueElement, "satMin"));
		config.getBlueThreshs().setSatMax(XmlUtils.getChildInt(blueElement, "satMax"));
		config.getBlueThreshs().setValMin(XmlUtils.getChildInt(blueElement, "valMin"));
		config.getBlueThreshs().setValMax(XmlUtils.getChildInt(blueElement, "valMax"));
		config.setBlueSizeMin(XmlUtils.getChildInt(blueElement, "sizeMin"));
		config.setBlueSizeMax(XmlUtils.getChildInt(blueElement, "sizeMax"));
		
		Element yellowElement = (Element)rootElement.getElementsByTagName("yellow").item(0);
		config.getYellowThreshs().setHueMin(XmlUtils.getChildInt(yellowElement, "hueMin"));
		config.getYellowThreshs().setHueMax(XmlUtils.getChildInt(yellowElement, "hueMax"));
		config.getYellowThreshs().setSatMin(XmlUtils.getChildInt(yellowElement, "satMin"));
		config.getYellowThreshs().setSatMax(XmlUtils.getChildInt(yellowElement, "satMax"));
		config.getYellowThreshs().setValMin(XmlUtils.getChildInt(yellowElement, "valMin"));
		config.getYellowThreshs().setValMax(XmlUtils.getChildInt(yellowElement, "valMax"));
		config.setYellowSizeMin(XmlUtils.getChildInt(yellowElement, "sizeMin"));
		config.setYellowSizeMax(XmlUtils.getChildInt(yellowElement, "sizeMax"));
		
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
		XmlUtils.addChildInt(doc, ballElement, "hueMin", config.getBallThreshs().getHueMin());
		XmlUtils.addChildInt(doc, ballElement, "hueMax", config.getBallThreshs().getHueMax());
		XmlUtils.addChildInt(doc, ballElement, "satMin", config.getBallThreshs().getSatMin());
		XmlUtils.addChildInt(doc, ballElement, "satMax", config.getBallThreshs().getSatMax());
		XmlUtils.addChildInt(doc, ballElement, "valMin", config.getBallThreshs().getValMin());
		XmlUtils.addChildInt(doc, ballElement, "valMax", config.getBallThreshs().getValMax());
		XmlUtils.addChildInt(doc, ballElement, "sizeMin", config.getBallSizeMin());
		XmlUtils.addChildInt(doc, ballElement, "sizeMax", config.getBallSizeMax());
		rootElement.appendChild(ballElement);
		
		Element blueElement = doc.createElement("blue");
		XmlUtils.addChildInt(doc, blueElement, "hueMin", config.getBlueThreshs().getHueMin());
		XmlUtils.addChildInt(doc, blueElement, "hueMax", config.getBlueThreshs().getHueMax());
		XmlUtils.addChildInt(doc, blueElement, "satMin", config.getBlueThreshs().getSatMin());
		XmlUtils.addChildInt(doc, blueElement, "satMax", config.getBlueThreshs().getSatMax());
		XmlUtils.addChildInt(doc, blueElement, "valMin", config.getBlueThreshs().getValMin());
		XmlUtils.addChildInt(doc, blueElement, "valMax", config.getBlueThreshs().getValMax());
		XmlUtils.addChildInt(doc, blueElement, "sizeMin", config.getBlueSizeMin());
		XmlUtils.addChildInt(doc, blueElement, "sizeMax", config.getBlueSizeMax());
		rootElement.appendChild(blueElement);
		
		Element yellowElement = doc.createElement("yellow");
		XmlUtils.addChildInt(doc, yellowElement, "hueMin", config.getYellowThreshs().getHueMin());
		XmlUtils.addChildInt(doc, yellowElement, "hueMax", config.getYellowThreshs().getHueMax());
		XmlUtils.addChildInt(doc, yellowElement, "satMin", config.getYellowThreshs().getSatMin());
		XmlUtils.addChildInt(doc, yellowElement, "satMax", config.getYellowThreshs().getSatMax());
		XmlUtils.addChildInt(doc, yellowElement, "valMin", config.getYellowThreshs().getValMin());
		XmlUtils.addChildInt(doc, yellowElement, "valMax", config.getYellowThreshs().getValMax());
		XmlUtils.addChildInt(doc, yellowElement, "sizeMin", config.getYellowSizeMin());
		XmlUtils.addChildInt(doc, yellowElement, "sizeMax", config.getYellowSizeMax());
		rootElement.appendChild(yellowElement);
		
		XmlUtils.writeXmlDocument(doc, filename);
	}

}

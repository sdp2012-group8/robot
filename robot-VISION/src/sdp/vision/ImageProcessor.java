package sdp.vision;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.logging.*;
import java.io.File;
import java.io.IOException;

import com.googlecode.javacpp.Loader;
import javax.imageio.ImageIO;

import sdp.common.Robot;
import sdp.common.WorldState;

import com.googlecode.javacv.CanvasFrame;


/**
 * The main object recognition class.
 *  
 * @author Andrei-ionut Manolache
 * @author Gediminas Liktaras
 * @author Laura Mihaela Ionescu
 */
public class ImageProcessor {
	
	/** The processor's configuration. */
	private ImageProcessorConfiguration config;


	private static int RED = 0;
	private static int GREEN = 1;
	private static int BLUE = 2;
	private static int YELLOW = 2;

	Point2D.Double btPos = new Point2D.Double(-1, -1);
	Point2D.Double ytPos = new Point2D.Double(-1, -1);
	Point2D.Double ourPos = new Point2D.Double(-1, -1);
	Point2D.Double lastBallPos = new Point2D.Double(-1, -1);

	Logger logger = Logger.getLogger("sdp.vision");
	
	Point2D btPos = new Point2D.Double(-1, -1);
	Point2D ytPos = new Point2D.Double(-1, -1);
	Point2D ourPos = new Point2D.Double(-1, -1);
	Point2D lastBallPos = new Point2D.Double(-1, -1);
	
	
	/**
	 * Create a new image processor with the default configuration.
	 */
	public ImageProcessor() {
		config = new ImageProcessorConfiguration();
	}
	
	/**
	 * Create a new image processor with the specified configuration.
	 * @param config Configuration to use.
	 */
	public ImageProcessor(ImageProcessorConfiguration config) {
		this();
		this.config = config;
	}
	
	
	/**
	 * Extract the world state from the supplied image.
	 * 
	 * @param frame The image to process.
	 * @return The world state, present in the image.
	 */
	public WorldState extractWorldState(BufferedImage frame) {
		IplImage background = cvLoadImage("../robot-VISION/data/testImages/bg.jpg");		
		IplImage image = IplImage.createFrom(frame);
		
		IplImage differenceImage = IplImage.createFrom(getDifferenceImage(
				image.getBufferedImage(), background.getBufferedImage()));

		IplImage redChannel = getChannels(differenceImage, RED);
		IplImage greenChannel = getChannels(differenceImage, GREEN);
		IplImage blueChannel = getChannels(differenceImage, BLUE);
		
		IplImage blueThreshold = thresholdChannel(blueChannel, BLUE);
		IplImage redThreshold = thresholdChannel(redChannel, RED);
		IplImage greenThreshold = thresholdChannel(greenChannel, GREEN);

		IplImage grayImage = cvCreateImage(cvGetSize(image), 8, 1);
		cvCvtColor(image, grayImage,CV_RGB2GRAY);
	//	IplImage thresholdImage = ip.thresholdChannel(grayImage, RED);
		
		findBall(image);		
		
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = image.getBufferedImage();
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}
	
	


	/**
	 * @param image
	 * @param background
	 * @return BufferedImage Takes an image and the background and performs
	 *         background subtraction The image returned is normalized, to
	 *         compensate for shadows and the differences in lighting
	 */

	public BufferedImage getDifferenceImage(BufferedImage image,
			BufferedImage background) {
		Raster backgroundData = null;
		Raster data = null;

		try {
			data = image.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		try {
			backgroundData = background.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		for (int i = 0; i < image.getWidth(); i = i + 1) { // for every
			for (int j = 0; j < image.getHeight(); j = j + 1) {

				int[] backgroundPixel = new int[3];
				data.getPixel(i, j, backgroundPixel);

				int[] imagePixel = new int[3];
				backgroundData.getPixel(i, j, imagePixel);

				if (i < config.getFieldLowX()
						|| i > config.getFieldHighX()
						|| j < config.getFieldLowY()
						|| j > config.getFieldHighY()) {
					Color colour = new Color(0, 0, 0);
					int rgb = colour.getRGB();
					image.setRGB(i, j, rgb);
					
				} else {
					
					int[] difference = getDifference(imagePixel,
							backgroundPixel);

					// create a new colour with the rgb of the difference

					int r = difference[0] > 30 ? imagePixel[0] : 0;

					int g = difference[1] > 30 ? imagePixel[1] : 0;

					int b = difference[2] > 30 ? imagePixel[2] : 0;

					int sum = r + g + b;

					Color colour = new Color(r, g, b);
					int rgb = colour.getRGB();

					image.setRGB(i, j, rgb);
				}

			}
		}

		return image;

	}

	/**
	 * @param image
	 * @param colour
	 * @return IplImage splits the image into red, green and blue channels,
	 *         returning the channel specified by the colour argument
	 */
	public IplImage getChannels(IplImage image, int colour) {

		IplImage blueChannel = cvCreateImage(cvGetSize(image), 8, 1);
		IplImage greenChannel = cvCreateImage(cvGetSize(image), 8, 1);
		IplImage redChannel = cvCreateImage(cvGetSize(image), 8, 1);

		cvSplit(image, blueChannel, greenChannel, redChannel, null);

		if (colour == RED)
			return redChannel;
		else if (colour == GREEN)
			return greenChannel;
		else
			return blueChannel;
	}

	/**
	 * @param image
	 * @param colour
	 * @return IplImage The function takes a binary image representing one of
	 *         the r,g,b channels and an int specifying the channel. It returns
	 *         the thresholded image
	 * */
	public IplImage thresholdChannel(IplImage image, int colour) {
		IplImage thresholdImage = cvCreateImage(cvGetSize(image), 8, 1);
		int min;
		int max;
		if (colour == RED) {
			min = 0;
			max = 400;
		} else if (colour == GREEN) {
			min = 100;
			max = 200;
		} else {
			min = 100;
			max = 200;
		}

		cvThreshold(thresholdImage, thresholdImage, min, max, CV_THRESH_BINARY);

		cvSmooth(thresholdImage, thresholdImage, CV_MEDIAN, 13);

		return thresholdImage;
	}

	// returns the difference between two arrays of 3 elements
	public int[] getDifference(int[] colour1, int[] colour2) {
		int[] difference = { 0, 0, 0 };
		difference[0] = colour1[0] - colour2[0];
		difference[1] = colour1[1] - colour2[1];
		difference[2] = colour1[2] - colour2[2];

		return difference;
	}

	/**
	 * Finds the centroid of the yellow or the blue robot, depending on the
	 * colour argument
	 * 
	 * @param thresholdImage
	 * @param colour
	 */
	public Point findCentroid(IplImage thresholdImage, int colour) {

		double posX = 0;
		double posY = 0;
		CvMoments moments = new CvMoments();
		cvMoments(thresholdImage, moments, 1);

		double momX10 = cvGetSpatialMoment(moments, 1, 0);
		double momY01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = momX10 / area;
		posY = momY01 / area;

		if (colour == BLUE)
			btPos.setLocation(posX, posY);
		else if (colour == YELLOW)
			ytPos.setLocation(posX, posY);

		Point point = new Point((int) posX, (int) posY);

		CvPoint pointX = new CvPoint((int) btPos.getX(), 0);
		CvPoint pointY = new CvPoint(0, (int) posY);
		// cvLine(thresholdImage, pointX, pointY, CvScalar.RED, 1, 8, 0);

		System.out.print("Position " + posX + " " + posY);

		return point;

	}

	/**
	 * Finds the centre of the ball
	 * 
	 * @param image
	 */
	public void findBall(IplImage image) {

		CvScalar min = cvScalar(0, 0, 130, 0);
		CvScalar max = cvScalar(140, 110, 255, 0);

		// create binary image of original size
		IplImage imgThreshold = cvCreateImage(cvGetSize(image), 8, 1);

		// apply thresholding
		cvInRangeS(image, min, max, imgThreshold);

		// smooth filter- median
		cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 13);

		// set the position of the ball
		findCentroid(imgThreshold, RED);
	}

	public IplImage getContours(IplImage image) {

		//these will be the opposite corners of the bounding box
		CvPoint boundingCorner1 = new CvPoint(0, 0);
		CvPoint boundingCorner2 = new CvPoint(0, 0);

		int nonZero = cvCountNonZero(image);

		//check the number of non zero pixels, to see if there are any blobs in the image
		logger.info("nonzero pixels " + nonZero);
		
		Point2D.Double centerOfRobot = new Point2D.Double(-1, -1);
		
		
		int maxArea = 0;
		if (nonZero > 20 && nonZero < 10000) {
			CvMemStorage storage = cvCreateMemStorage(0);
			CvSeq contour = new CvSeq(null);
			
			int noOfContours = cvFindContours(image, storage, contour,
					Loader.sizeof(CvContour.class), CV_RETR_LIST,
					CV_CHAIN_APPROX_SIMPLE);

			logger.info("number of contours " + noOfContours);
			
			while (contour != null && !contour.isNull()) {
				if (contour.elem_size() > 0) {
					
					CvSeq points = cvApproxPoly(contour,
							Loader.sizeof(CvContour.class), storage,
							CV_POLY_APPROX_DP,
							cvContourPerimeter(contour) * 0.02, 0);

					cvDrawContours(image, points, CvScalar.BLUE, CvScalar.BLUE,
							-1, 1, CV_AA);
					
					//get the bounding rectangle of the contour and its minimal box	
					CvRect cvRect = cvBoundingRect(contour, 0);
					CvBox2D minBox = cvMinAreaRect2(contour, storage);
					
					//get the center of the box
					CvPoint2D32f point1 = minBox.center();
					
					//get the area of the rectangle and compare it to the maximum area
					int area = cvRect.width() * cvRect.height();

					if (area > maxArea) {
						maxArea = area;
						centerOfRobot.setLocation(point1.x(), point1.y());
						//this is the top-left corner of the box
						CvPoint p1 = new CvPoint((int) point1.x()
								- cvRect.width() / 2, (int) point1.y()
								+ cvRect.height() / 2);
						boundingCorner1 = p1;
						//this is the botom-right corner of the box
						CvPoint p2 = new CvPoint(p1.x() + cvRect.width(),
								p1.y() - cvRect.height());
						boundingCorner2 = p2;
					}

				}
				contour = contour.h_next();
			}
		}

		logger.info("max area = " + maxArea);
		logger.info("centre of robot " + centerOfRobot.x + " "
				+ centerOfRobot.y);
		cvRectangle(image, boundingCorner1, boundingCorner2, CvScalar.BLUE, 1, 8, 0);

		return image;

	}

}
